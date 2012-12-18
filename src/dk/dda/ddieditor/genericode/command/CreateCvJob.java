package dk.dda.ddieditor.genericode.command;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.util.LanguageUtil;
import org.ddialliance.ddieditor.util.DdiEditorConfig;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.ddialliance.ddiftp.util.log.Log;
import org.ddialliance.ddiftp.util.log.LogFactory;
import org.ddialliance.ddiftp.util.log.LogType;
import org.ddialliance.ddiftp.util.xml.XmlBeansUtil;
import org.eclipse.core.runtime.Platform;
import org.oasisOpen.docs.codelist.ns.genericode.Agency;
import org.oasisOpen.docs.codelist.ns.genericode.AnyOtherContent;
import org.oasisOpen.docs.codelist.ns.genericode.AnyOtherLanguageContent;
import org.oasisOpen.docs.codelist.ns.genericode.CodeListDocument;
import org.oasisOpen.docs.codelist.ns.genericode.CodeListDocument1;
import org.oasisOpen.docs.codelist.ns.genericode.Column;
import org.oasisOpen.docs.codelist.ns.genericode.ColumnSet;
import org.oasisOpen.docs.codelist.ns.genericode.Key;
import org.oasisOpen.docs.codelist.ns.genericode.LongName;
import org.oasisOpen.docs.codelist.ns.genericode.Row;
import org.oasisOpen.docs.codelist.ns.genericode.ShortName;
import org.oasisOpen.docs.codelist.ns.genericode.UseType;
import org.oasisOpen.docs.codelist.ns.genericode.Value;
import org.w3.x1999.xhtml.DivDocument;
import org.w3.x1999.xhtml.DivType;
import org.w3.x1999.xhtml.PDocument;
import org.w3.x1999.xhtml.PType;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import dk.dda.ddieditor.genericode.wizard.CreateCvWizard;

/*
 * Copyright 2012 Danish Data Archive (http://www.dda.dk) 
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 * The full text of the license is also available on the Internet at 
 * http://www.gnu.org/copyleft/lesser.html
 */

/**
 * Parse CSV and create Genericode controled vocabulary
 */
public class CreateCvJob implements Runnable {
	private Log log = LogFactory.getLog(LogType.SYSTEM, CreateCvJob.class);
	CreateCvWizard createCvWizard;

	CodeListDocument codeListDoc;
	CodeListDocument1 codeListType;

	int lineCount = 1;
	int levelCount = 0;
	String empty = "";

	List<Column> columns = new ArrayList<Column>();
	List<Row> rows = new ArrayList<Row>();
	Map<Integer, Row> levelToRow = new HashMap<Integer, Row>();

	public static String SIMPLE_STRING = "simple-string";
	List<String> simpleStringColId = new ArrayList<String>();

	String keyColumnRefId;
	boolean keyColumnRefIdFound = false;
	int numberOfColums = 0;

	String notDefined = "Not Defined";
	String parentCode = "parentlevelcode";
	String levelCode = "lowestlevelcode";

	CSVParser csvParser = new CSVParser(",".charAt(0), "'".charAt(0));

	public CreateCvJob(CreateCvWizard createCvWizard) {
		this.createCvWizard = createCvWizard;
	}

	@Override
	public void run() {
		try {
			// init
			codeListDoc = CodeListDocument.Factory.newInstance(DdiManager
					.getInstance().getXmlOptions());
			codeListType = codeListDoc.addNewCodeList();

			// parse csv
			try {
				parseCsv();
			} catch (Exception e) {
				if (e instanceof DDIFtpException) {
					throw e;
				}
				DDIFtpException ex = new DDIFtpException(Translator.trans(
						"cv.error.cvsfileparseerror", createCvWizard.csvFile));
				ex.setRealThrowable(e);
				e.printStackTrace();

				throw ex;
			}

			// post process
			postProcess();

			// store gc
			storeGc();
		} catch (Exception e) {
			Editor.showError(e, null);
		}
	}

	private void postProcess() throws Exception {
		// description
		AnyOtherLanguageContent description = codeListType.addNewAnnotation()
				.addNewDescription();
		DivDocument divDoc;
		try {
			divDoc = DivDocument.Factory
					.parse("<div xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\""
							+ LanguageUtil.getOriginalLanguage()
							+ "\" class=\"Description\"/>");
		} catch (XmlException e) {
			throw new DDIFtpException("Xml Create exception", e);
		}
		
		XmlBeansUtil.setTextOnMixedElement(divDoc.getDiv().addNewP(),
				createCvWizard.annotation);
		XmlBeansUtil.setXmlOnElement(description, divDoc);

		// copy right owner
		AnyOtherContent appInfo = codeListType.getAnnotation().addNewAppInfo();
		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("CopyrightOwner",
								DdiEditorConfig
										.get(DdiEditorConfig.DDI_AGENCY_NAME)));
		
		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("CopyrightOwnerUrl",
								DdiEditorConfig
										.get(DdiEditorConfig.DDI_AGENCY_HP)));
		
		// year
		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("CopyrightYear", ""
								+ Calendar.getInstance().get(Calendar.YEAR)));
		
		// license
		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("LicenseName",
								"Creative Commons Attribution-ShareAlike 3"));
		
		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("LicenseUrl",
								"http://creativecommons.org/licenses/by-sa/3.0/"));

		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("LicenseLogoUrl",
								"http://i.creativecommons.org/l/by-sa/3.0/80x15.png"));

		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("CopyrightText",
								"Copyright ©"));
		
		// app data
		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("Software",
								"DdiEditor-Genericode"));
		XmlBeansUtil.setXmlOnElement(appInfo,
				dk.dda.ddieditor.genericode.model.ddicv.Value
						.createDdiCvValueWithKeyAttValue("SoftwareVersion", ""
								+ Platform.getBundle("ddieditor-genericode")
										.getHeaders().get("Bundle-Version")));

		// identification
		ShortName shortName = codeListType.addNewIdentification()
				.addNewShortName();
		shortName.setLang(LanguageUtil.getOriginalLanguage());
		shortName.setStringValue(createCvWizard.shortname);

		LongName longName = codeListType.getIdentification().addNewLongName();
		longName.setLang(LanguageUtil.getOriginalLanguage());
		longName.setStringValue(createCvWizard.longName);

		// agency
		Agency agency = codeListType.getIdentification().addNewAgency();
		agency.addNewShortName().setStringValue(
				DdiEditorConfig.get(DdiEditorConfig.DDI_AGENCY_NAME));
		agency.addNewLongName().setStringValue(
				DdiEditorConfig.get(DdiEditorConfig.DDI_AGENCY_DESCRIPTION));
		agency.addNewIdentifier().setStringValue(
				DdiEditorConfig.get(DdiEditorConfig.DDI_AGENCY_IDENTIFIER));

		// version
		codeListType.getIdentification().setVersion(createCvWizard.version);

		// uri
		codeListType.getIdentification().setCanonicalUri(
				createCvWizard.canonicalUri);
		codeListType.getIdentification().setCanonicalVersionUri(
				createCvWizard.canonicalVersionUri);
		codeListType.getIdentification().addNewLocationUri()
				.setStringValue(createCvWizard.locationUri);

		// columns
		ColumnSet columnSet = codeListType.addNewColumnSet();
		columnSet.setColumnArray(columns.toArray(new Column[] {}));

		// column key
		Key key = null;
		for (Column col : columnSet.getColumnList()) {
			if (col.getId().equals(keyColumnRefId)) {
				key = columnSet.addNewKey();
				key.addNewColumnRef().setRef(keyColumnRefId);
				key.setId("Key-" + col.getId());
				XmlBeansUtil.setTextOnMixedElement(
						key.addNewShortName(),
						"Key-"
								+ XmlBeansUtil.getTextOnMixedElement(col
										.getShortName()));
			}
		}
		if (key == null) {
			DDIFtpException e = new DDIFtpException(
					Translator.trans("cv.error.nocolumnkeydefined"));
			e.setRealThrowable(new Throwable());
			throw e;
		}

		// rows
		codeListType.addNewSimpleCodeList().setRowArray(
				rows.toArray(new Row[] {}));

		// debug
		if (log.isDebugEnabled()) {
			log.debug(codeListDoc.xmlText(DdiManager.getInstance()
					.getXmlOptions()));
		}
	}

	private void storeGc() throws Exception {
		// file name *.gc

		// create file
		File file = new File(createCvWizard.exportPath + File.separator
				+ createCvWizard.exportFileName);
		if (file.exists()) {
			file.delete();
		}

		// name space prefixes
		HashMap<String, String> suggestedPrefixes = new HashMap<String, String>();
		suggestedPrefixes.put("http://www.w3.org/1999/xhtml", "h");
		suggestedPrefixes.put("urn:ddi-cv", "ddi-cv");
		suggestedPrefixes.put("urn:dda-cv", "dda-cv");
		suggestedPrefixes.put(
				"http://docs.oasis-open.org/codelist/ns/genericode/1.0/", "gc");

		XmlOptions xmlOptions = DdiManager.getInstance().getXmlOptions();
		xmlOptions.setSaveSuggestedPrefixes(suggestedPrefixes);

		// xml
		XmlCursor cursor = codeListDoc.newCursor();
		cursor.toChild(0);
		cursor.setAttributeText(
				new QName("http://www.w3.org/2001/XMLSchema-instance",
						"schemaLocation"),
				"http://docs.oasis-open.org/codelist/ns/genericode/1.0/ "
						+ "http://docs.oasis-open.org/codelist/cs-genericode-1.0/xsd/genericode.xsd");

		StringBuffer xml = new StringBuffer(
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		xml.append(codeListDoc.xmlText(xmlOptions));
		codeListDoc.save(file, xmlOptions);
	}

	public void parseCsv() throws Exception {
		CSVReader reader = new CSVReader(new FileReader(createCvWizard.csvFile));
		String[] cells;

		boolean emptyLine = true;
		boolean dataStart = false;

		// TODO error report on csv file -care for users!!!
		while ((cells = reader.readNext()) != null) {
			if (log.isDebugEnabled()) {
				log.debug("Line:" + lineCount);
				StringBuilder msg = new StringBuilder();
				for (int i = 0; i < cells.length; i++) {
					msg.append(cells[i] + ", ");
				}
				log.debug(msg.toString());
			}

			// test for code begin - aka empty line
			for (int i = 0; i < cells.length; i++) {
				if (!cells[i].equals(empty)) {
					emptyLine = false;
				}
			}
			if (emptyLine) {
				// skip empty line
				dataStart = true;
				continue;
			}

			// levels
			if (!dataStart) {
				levelCount++;

				// define number of columns
				for (int j = 0; j < cells.length; j++) {
					if (!cells[j].equals(empty)) {
						numberOfColums++;
					}
				}

				createColums(levelCount, numberOfColums, cells);
			} else {
				// row
				// define current level numberOfColums
				int offset = 0;
				for (int i = 0; i < cells.length; i++) {
					if (!cells[i].equals(empty)) {
						if (i < numberOfColums) {
							offset = 0;
						}
						for (int j = 0; j < 24; j++) {
							if (i > numberOfColums && i < (j * numberOfColums)) {
								offset = (j - 1) * numberOfColums;
								break;
							}
						}
						break;
					}
				}
				createRow(cells, offset);
			}

			// increment
			emptyLine = true;
			lineCount++;
		}
	}

	void createColums(int levelCount, int numberOfColums, String[] cells)
			throws DDIFtpException {
		for (int j = 0; j < cells.length; j++) {
			if (!cells[j].equals(empty)) {
				String[] elements = cells[j].split(",");
				if (elements.length != 4) {
					DDIFtpException e = new DDIFtpException(Translator.trans(
							"cv.error.columndefinition", "(" + levelCount + ","
									+ j + ")"));
					e.setRealThrowable(new Throwable());
					throw e;
				}
				Column column = createColumn(elements, true);

				// set id key column
				if (!keyColumnRefIdFound) {
					keyColumnRefIdFound = true;
					keyColumnRefId = column.getId();
				}
			}
		}
	}

	/**
	 * Create column
	 * 
	 * @param elements
	 *            {ID, type, label, description}
	 * @param useRequired
	 *            is required
	 * @return column
	 * @throws DDIFtpException
	 */
	private Column createColumn(String[] elements, boolean useRequired)
			throws DDIFtpException {
		// id
		Column column = Column.Factory.newInstance();
		column.setId(elements[0]);

		// name
		ShortName shortName = column.addNewShortName();
		shortName.setStringValue(elements[0]);

		// label
		LongName longName = column.addNewLongName();
		longName.setStringValue(elements[2]);
		longName.setLang(LanguageUtil.getOriginalLanguage());

		// description
		AnyOtherLanguageContent description = column.addNewAnnotation()
				.addNewDescription();
		setTextOnDescription(description, elements[3]);

		// data type
		// TODO data type check
		// available data types are the definitions of:
		// http://www.w3.org/TR/xmlschema11-2/#built-in-datatypes
		String dataType = elements[1].trim();
		if (dataType.equals(SIMPLE_STRING)) {
			dataType = "string";
			simpleStringColId.add(elements[0]);
		}
		column.addNewData().setType(dataType);

		// required
		if (useRequired) {
			column.setUse(UseType.REQUIRED);
		} else {
			column.setUse(UseType.OPTIONAL);
		}

		columns.add(column);
		return column;
	}

	private void createRow(String[] cells, int offset) throws Exception {
		Row row = Row.Factory.newInstance(DdiManager.getInstance()
				.getXmlOptions());
		Value value;

		// 0 - code
		String currentCode = null;
		// empty check
		if (cells[offset].length() == 0) {
			currentCode = defineCode(getPrefered(cells[offset + 1]));
		} else {
			currentCode = defineCode(cells[offset]);
		}
		value = row.addNewValue();
		value.setColumnRef(columns.get(0).getId());
		value.addNewSimpleValue().setStringValue(currentCode);

		// 1 - label
		value = row.addNewValue();
		value.setColumnRef(columns.get(1).getId());
		addComplexValueOnValue(value, cells[offset + 1]);

		// 2 - definition, empty check
		value = row.addNewValue();
		value.setColumnRef(columns.get(2).getId());
		addComplexValueOnValue(value, checkDefinition(cells, offset, 2));

		// 3 + code and custom cells
		if (numberOfColums > 3) {
			for (int i = 3; i < numberOfColums; i++) {
				value = row.addNewValue();
				value.setColumnRef(columns.get(i).getId());

				if (columns.get(i).getData().getType().equals("string")) {
					if (simpleStringColId.contains(columns.get(i).getId())) {
						value.addNewSimpleValue().setStringValue(
								checkDefinition(cells, offset, i));
					} else {
						addComplexValueOnValue(value,
								checkDefinition(cells, offset, i));
					}
				} else {
					value.addNewSimpleValue().setStringValue(
							checkDefinition(cells, offset, i));
				}
			}
		}

		// hirachy code
		int level = offset / numberOfColums;
		if (levelToRow.get(level - 1) != null) {
			// parent
			Row parent = levelToRow.get(level - 1);

			// reset id
			String oldId = row.getValueList().get(0).getSimpleValue()
					.getStringValue();
			String parentId = parent.getValueList().get(0).getSimpleValue()
					.getStringValue();
			row.getValueList().get(0).getSimpleValue()
					.setStringValue(parentId + "." + oldId);

			// check parent column
			boolean foundParentCol = false;
			for (Column col : columns) {
				if (col.getId().equals(parentCode)) {
					foundParentCol = true;
				}
			}
			if (!foundParentCol) {
				// create parent col
				String[] parentElements = { parentCode, "string",
						"Parent Code",
						"The code defined by the parent in the hirachy", "en" };
				createColumn(parentElements, false);
			}

			// set parent
			value = row.addNewValue();
			value.setColumnRef(parentCode);
			value.addNewSimpleValue().setStringValue(parentId);

			// check level column
			boolean foundLevelCol = false;
			for (Column col : columns) {
				if (col.getId().equals(levelCode)) {
					foundLevelCol = true;
				}
			}
			if (!foundLevelCol) {
				// create level col
				String[] levelElements = { levelCode, "string",
						"Lowest Level Code",
						"The code defined at the current level in the hirachy",
						"en" };
				createColumn(levelElements, false);
			}

			// set level
			value = row.addNewValue();
			value.setColumnRef(levelCode);
			value.addNewSimpleValue().setStringValue(oldId);
		}
		levelToRow.put(level, row);
		rows.add(row);
	}

	private void addComplexValueOnValue(Value value, String text)
			throws Exception {
		HashMap<String, String> texts = defineLine(text);
		AnyOtherContent anyOtherContent = value.addNewComplexValue();

		for (Entry<String, String> entry : texts.entrySet()) {
			String lang = entry.getValue();
			if (lang == null) {
				lang = LanguageUtil.getOriginalLanguage();
			}

			XmlBeansUtil.setXmlOnElement(anyOtherContent,
					dk.dda.ddieditor.genericode.model.ddicv.Value
							.createDdiCvValue(
									"http://www.w3.org/XML/1998/namespace",
									"lang", lang, entry.getKey()));
		}
	}

	private String checkDefinition(String[] cells, int offset, int position) {
		String definition;
		if (cells.length > (offset + position)) {
			definition = cells[offset + position];
		} else {
			definition = "";
		}
		return checkValue(definition);
	}

	private String checkValue(String value) {
		if (value.length() == 0) {
			value = "Not Defined";
		}
		return value;
	}

	private String defineCode(String code) {
		code = code.trim();
		code = code.replace(" ", "");
		code = code.replace("-", "");
		code = code.replace(",", "");
		code = code.replace(";", "");
		code = code.replace(".", "");

		return code;
	}

	private HashMap<String, String> defineLine(String line) throws Exception,
			Exception {
		String[] cells = csvParser.parseLine(line);
		// key: label, value: lang
		HashMap<String, String> result = new HashMap<String, String>();
		for (int i = 0; i < cells.length; i++) {
			String[] subPart = cells[i].split("=");
			if (subPart.length == 1) {
				result.put(subPart[0], null);
			} else if (subPart.length > 2) {
				DDIFtpException e = new DDIFtpException(Translator.trans(""),
						new Throwable());
				throw e;
			} else {
				result.put(subPart[0], subPart[1]);
			}
		}

		return result;
	}

	private String getPrefered(String line) throws Exception {
		HashMap<String, String> lineMap = defineLine(line);
		return getPrefered(lineMap);
	}

	private String getPrefered(HashMap<String, String> lineMap)
			throws Exception {
		String result = null;
		for (Entry<String, String> entry : lineMap.entrySet()) {
			if (entry.getValue() == null) {
				result = entry.getKey().trim();
				break;
			}
		}
		if (result == null && lineMap.size() > 0) {
			result = lineMap.keySet().iterator().next().trim();
		}
		if (result == null) {
			DDIFtpException e = new DDIFtpException(Translator.trans(""),
					new Throwable());
		}
		return result;
	}

	private void setTextOnDescription(AnyOtherLanguageContent description,
			String annotation) throws DDIFtpException {
		description.setLang(LanguageUtil.getOriginalLanguage());

		PDocument pDoc = PDocument.Factory.newInstance();
		XmlBeansUtil.setTextOnMixedElement(pDoc.addNewP(), annotation);
		XmlBeansUtil.setXmlOnElement(description, pDoc);
	}
}
