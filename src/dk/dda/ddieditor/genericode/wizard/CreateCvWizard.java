package dk.dda.ddieditor.genericode.wizard;

import java.io.File;

import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.preference.PreferenceUtil;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

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
 * RCP wizard for classification creation
 */
public class CreateCvWizard extends Wizard {
	public DDIResourceType selectedResource = null;
	public String csvFile = null;
	public String shortname = "", longName = "", annotation = "";
	public String version = "", canonicalUri = "", canonicalVersionUri = "",
			locationUri = "", exportFileName = "", exportPath = "";
	public int codeImpl = 1; // use default nested
	int levels = 0;

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		SelectPage rangePage = new SelectPage();
		addPage(rangePage);
		ExportPage exportPage = new ExportPage();
		addPage(exportPage);
	}

	class SelectPage extends WizardPage {
		public static final String PAGE_NAME = "select";
		
		public SelectPage() {
			super(PAGE_NAME, Translator.trans("cv.wizard.title"), null);
		}

		void pageComplete() {
			if (csvFile != null) {
				setPageComplete(true);
			}
		}

		@Override
		public void createControl(Composite parent) {
			final Editor editor = new Editor();
			Group _group = editor.createGroup(parent,
					Translator.trans("cv.wizard.titleshort"));
			//
			// description
			//
			// short name
			Group descriptionGroup = editor.createGroup(_group,
					Translator.trans("cv.description.group"));
			editor.createLabel(descriptionGroup,
					Translator.trans("cv.description.shortname"));
			Text shortNameTxt = editor.createText(descriptionGroup, "", false);
			shortNameTxt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					shortname = ((Text) event.getSource()).getText();
				}
			});

			// long name
			editor.createLabel(descriptionGroup,
					Translator.trans("cv.description.longname"));
			Text longNameTxt = editor.createText(descriptionGroup, "", false);
			longNameTxt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					longName = ((Text) event.getSource()).getText();
				}
			});

			// annotation
			StyledText annaotationTxt = editor.createTextAreaInput(
					descriptionGroup,
					Translator.trans("cv.description.annotation"), "", false);
			annaotationTxt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					annotation = ((StyledText) event.getSource()).getText();
				}
			});

			//
			// identification
			//
			//
			Group idGroup = editor.createGroup(_group,
					Translator.trans("cv.id.group"));

			// version
			editor.createLabel(idGroup, Translator.trans("cv.id.version"));
			Text versionTxt = editor.createText(idGroup, "", false);
			versionTxt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					version = ((Text) event.getSource()).getText();
				}
			});

			// canonicalUri
			editor.createLabel(idGroup, Translator.trans("cv.id.canonicaluri"));
			Text canonicalUriTxt = editor.createText(idGroup, "", false);
			canonicalUriTxt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					canonicalUri = ((Text) event.getSource()).getText();
				}
			});

			// canonicalVersionUri
			editor.createLabel(idGroup,
					Translator.trans("cv.id.canonicalversionuri"));
			Text canonicalVersionUriTxt = editor.createText(idGroup, "", false);
			canonicalVersionUriTxt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					canonicalVersionUri = ((Text) event.getSource()).getText();
				}
			});

			// locationUri
			editor.createLabel(idGroup, Translator.trans("cv.id.locationuri"));
			Text locationUriTxt = editor.createText(idGroup, "", false);
			locationUriTxt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					locationUri = ((Text) event.getSource()).getText();
				}
			});

			//
			// csv file
			//
			// csv file label
			editor.createLabel(_group, Translator.trans("cv.filechooser.title"));
			final Text pathText = editor.createText(_group, "");
			pathText.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					// on a CR - check if file exist and read it
					if (e.keyCode == SWT.CR) {
						csvFile = readFile(pathText);
						pageComplete();
					}
				}
			});
			
			pathText.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					// on a TAB - check if file exist and read it
					switch (e.detail) {
					case SWT.TRAVERSE_TAB_NEXT:
					case SWT.TRAVERSE_TAB_PREVIOUS: {
						csvFile = readFile(pathText);
						if (csvFile == null) {
							e.doit = false;
						}
					}
					}
				}
			});

			// csv file selection
			Button pathBrowse = editor.createButton(_group,
					Translator.trans("cv.filechooser.browse"));
			pathBrowse.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fileChooser = new FileDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell());
					fileChooser.setText(Translator
							.trans("cv.filechooser.title"));
					fileChooser.setFilterExtensions(new String[] { "*.csv",
							"*.*" });
					fileChooser.setFilterNames(new String[] {
							Translator.trans("cv.filternames.csvfile"),
							Translator.trans("cv.filternames.anyfile") });

					PreferenceUtil.setPathFilter(fileChooser);
					csvFile = fileChooser.open();
					PreferenceUtil.setLastBrowsedPath(csvFile);

					pathText.setText(csvFile);
					pageComplete();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			// finalize
			setControl(_group);
			setPageComplete(false);
		}

		private String readFile(Text pathText) {
			if (!new File(pathText.getText()).exists()) {
				MessageDialog
						.openError(PlatformUI.getWorkbench().getDisplay()
								.getActiveShell(), Translator
								.trans("ErrorTitle"), Translator.trans(
								"cv.filenotfound.message", pathText.getText()));
				setPageComplete(false);
				return null;
			}

			setPageComplete(true);
			return pathText.getText();
		}
	}

	class ExportPage extends WizardPage {
		public static final String PAGE_NAME = "export";

		public ExportPage() {
			super(PAGE_NAME, Translator.trans("cv.wizard.title"), null);
		}

		void pageComplete() {
			if (exportFileName.length() > 0 && exportPath.length() > 0) {
				setPageComplete(true);
			}
		}

		@Override
		public void createControl(Composite parent) {
			final Editor editor = new Editor();

			// export group
			Group exportGroup = editor.createGroup(parent,
					Translator.trans("cv.export.path"));

			// export path
			editor.createLabel(exportGroup,
					Translator.trans("ExportDDI3Action.filechooser.title"));
			final Text exportPathText = editor.createText(exportGroup, "",
					false);
			exportPathText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					exportPath = ((Text) e.getSource()).getText();
				}
			});
			File lastBrowsedPath = PreferenceUtil.getLastBrowsedPath();
			if (lastBrowsedPath != null) {
				exportPathText.setText(lastBrowsedPath.getAbsolutePath());
				exportPath = lastBrowsedPath.getAbsolutePath();
			}

			Button exportPathBrowse = editor.createButton(exportGroup,
					Translator.trans("ExportDDI3Action.filechooser.browse"));
			exportPathBrowse.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DirectoryDialog dirChooser = new DirectoryDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell());
					dirChooser.setText(Translator
							.trans("ExportDDI3Action.filechooser.title"));
					PreferenceUtil.setPathFilter(dirChooser);
					exportPath = dirChooser.open();
					if (exportPath != null) {
						exportPathText.setText(exportPath);
						PreferenceUtil.setLastBrowsedPath(exportPath);
					}
					pageComplete();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			// file name
			Text exportFileNameText = editor.createTextInput(exportGroup,
					Translator.trans("ExportDDI3Action.filename"), "", null);

			exportFileNameText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					Text text = ((Text) e.getSource());
					exportFileName = text.getText();
					pageComplete();
				}
			});

			// finalize
			setControl(exportGroup);
			setPageComplete(false);
		}
	}
}
