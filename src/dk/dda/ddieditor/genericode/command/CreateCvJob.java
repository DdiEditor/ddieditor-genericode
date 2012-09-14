package dk.dda.ddieditor.genericode.command;

import java.io.FileReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategorySchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategorySchemeType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategoryType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeSchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeSchemeType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.LevelDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.LevelType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.AbstractMaintainableType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.AbstractVersionableType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.DescriptionDocument;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.LabelType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.StructuredStringType;
import org.ddialliance.ddieditor.logic.identification.IdentificationManager;
import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.preference.PreferenceUtil;
import org.ddialliance.ddieditor.ui.util.LanguageUtil;
import org.ddialliance.ddieditor.util.LightXmlObjectUtil;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.ddialliance.ddiftp.util.log.Log;
import org.ddialliance.ddiftp.util.log.LogFactory;
import org.ddialliance.ddiftp.util.log.LogType;
import org.ddialliance.ddiftp.util.xml.XmlBeansUtil;
import org.oasisOpen.docs.codelist.ns.genericode.AnyOtherLanguageContent;
import org.oasisOpen.docs.codelist.ns.genericode.Column;
import org.oasisOpen.docs.codelist.ns.genericode.Row;

import au.com.bytecode.opencsv.CSVReader;

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

/*
 * CreateClassificationJob is inspired by the project: Virgil UI by: Samuel Spencer, see: http://code.google.com/p/virgil-ui/
 */

/**
 * Parse CSV and store DDI-L
 */
public class CreateCvJob implements Runnable {
	private Log log = LogFactory.getLog(LogType.SYSTEM, CreateCvJob.class);

	DDIResourceType selectedResource = null;
	String csvFile = null;
	String label;
	String descriptionTxt;
	int codeImpl;

	List<Column> columns = new ArrayList<Column>();
	List<Row> rows = new ArrayList<Row>();
	Map<Integer, String> parentCodeMap = new HashMap<Integer, String>();

	public CreateCvJob(DDIResourceType selectedResource, String inCsvFile,
			String label, String descriptionTxt, int codeImpl) {
		this.selectedResource = selectedResource;
		this.csvFile = inCsvFile;
		this.label = label;
		this.descriptionTxt = descriptionTxt;
		this.codeImpl = codeImpl;
	}

	@Override
	public void run() {
		try {
			// parse csv
			try {
				parseCsv();
			} catch (Exception e) {
				DDIFtpException ex = new DDIFtpException(Translator.trans(
						"classcification.error.cvsfileparseerror", csvFile));
				ex.setRealThrowable(e);
				throw ex;
			}

			// post process
			// postProcess();

			// store ddi
			storeDdi();
		} catch (Exception e) {
			Editor.showError(e, null);
		}
	}

	private void storeDdi() throws Exception {
	}

	public void parseCsv() throws Exception {
		CSVReader reader = new CSVReader(new FileReader(csvFile));
		String[] cells;
		int count = 1;

		String empty = "";
		boolean emptyLine = true;
		boolean dataStart = false;
		int levelCount = 1;

		// TODO error report on csv file -care for users!!!
		while ((cells = reader.readNext()) != null) {
			if (log.isDebugEnabled()) {
				log.debug("Line:" + count);
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

			// parse code,caption,definition
			for (int i = 0; i < cells.length; i++) {
				// levels
				if (!dataStart) {
					levelCount = columns.size();

					// level 1
					if (levelCount == 0) {
						createColumn(levelCount + 1, cells[i], cells[i + 1],
								cells[i + 2]);
						break;
					}

					// level 2
					if (levelCount == 1 && !cells[levelCount + 1].equals(empty)) {
						createColumn(levelCount + 1, cells[(levelCount + 1)],
								cells[(levelCount + 2)],
								cells[(levelCount + 3)]);
						break;
					}

					// level n
					if (levelCount > 0 && !cells[levelCount + 2].equals(empty)) {
						createColumn(levelCount + 1, cells[(levelCount + 2)],
								cells[(levelCount + 3)],
								cells[(levelCount + 4)]);
						break;
					}
				} else {
					// codes
					if (!cells[i].equals(empty)) {
						int reminder = i % 3;
						if (i == 0 || reminder == 0) {
							// code defined
							createRow(i / 3, cells[i], cells[i + 1],
									cells[i + 2]);
						} else {
							// hack to define empty codes ;-)
							createRow((i - 1) / 3, cells[i - 1], cells[i],
									cells[i + 1]);
						}
						break;
					}
				}
			}

			// increment
			emptyLine = true;
			count++;
		}
	}

	private void createRow(int number, String code, String caption,
			String definition) throws Exception {
		// <Row>
		// <Value>
		// <SimpleValue>AF</SimpleValue>
		// </Value>
		// <Value>
		// <SimpleValue>AFGHANISTAN</SimpleValue>
		// </Value>
		// <Value>
		// <SimpleValue>004</SimpleValue>
		// </Value>
		// </Row>
		Row row = Row.Factory.newInstance();

		// code
		String currentCode = null;
		if (code.length() == 0) {
			currentCode = defineCode(caption);
		} else {
			currentCode = defineCode(code);
		}
		parentCodeMap.put(number, currentCode);
		// TODO add parent code links

		row.addNewValue().addNewSimpleValue().setStringValue(currentCode);

		// caption
		row.addNewValue().addNewSimpleValue()
				.setStringValue(checkValue(caption));

		// definition
		row.addNewValue().addNewSimpleValue()
				.setStringValue(checkValue(definition));

		rows.add(row);
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

	private void createColumn(int number, String shortName, String longName,
			String description) throws Exception {
		// create

		// <Column Id="Definition" Use="required">
		// <Annotation>
		// <Description>
		// <xhtml:p>Definition of the code.</xhtml:p>
		// </Description>
		// </Annotation>
		// <ShortName/>
		// <Data Type="string"/>
		// </Column>

		Column column = Column.Factory.newInstance();
		column.addNewShortName().setStringValue(shortName);
		column.addNewLongName().setStringValue(longName);
		AnyOtherLanguageContent descriptionDoc = column.addNewAnnotation()
				.addNewDescription();
		descriptionDoc.setLang(LanguageUtil.getOriginalLanguage());
		XmlBeansUtil.setTextOnMixedElement(descriptionDoc, description);

		columns.add(column);
	}

	private void setText(LabelType label, String text) throws DDIFtpException {
		XmlBeansUtil.setTextOnMixedElement(label, text);
		XmlBeansUtil.addTranslationAttributes(label,
				Translator.getLocaleLanguage(), false, true);
	}

	private void setText(StructuredStringType struct, String text)
			throws DDIFtpException {
		XmlBeansUtil.setTextOnMixedElement(struct, text);
		XmlBeansUtil.addTranslationAttributes(struct,
				Translator.getLocaleLanguage(), false, true);
	}
}
