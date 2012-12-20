package dk.dda.ddieditor.genericode.command;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.preference.PreferenceUtil;
import org.ddialliance.ddieditor.ui.util.LanguageUtil;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class PrintCv extends org.eclipse.core.commands.AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FileDialog fileChooser = new FileDialog(PlatformUI.getWorkbench()
				.getDisplay().getActiveShell());
		fileChooser.setText(Translator
				.trans("OpenFileAction.filechooser.title"));
		fileChooser.setFilterExtensions(new String[] { "*.cv", "*.xml", "*.*" });
		fileChooser.setFilterNames(new String[] {
				Translator.trans("cv.filternames.cvfile"),
				Translator.trans("cv.filternames.xmlfile"),
				Translator.trans("cv.filternames.anyfile") });
		PreferenceUtil.setPathFilter(fileChooser);
		final String fileName = fileChooser.open();
		if (fileName==null) {
			return null;
		} else {
			PreferenceUtil.setLastBrowsedPath(fileName);			
		}

		// print
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								monitor.beginTask(
										Translator
												.trans("cv.print.notofication"),
										1);

								PlatformUI.getWorkbench().getDisplay()
										.asyncExec(new Runnable() {
											@Override
											public void run() {
												File htmlFile = null;
												try {
													File cvFile = new File(fileName);
													htmlFile = File
															.createTempFile(
																	"PrintCV",
																	".html");
													htmlFile.deleteOnExit();

													// transformer
													Transformer transformer = 
															getTransFormer();

													// do transformation
													transformer
															.transform(
																	new StreamSource(
																			cvFile.toURI()
																					.toURL()
																					.toString()),
																	new StreamResult(
																			htmlFile.toURI()
																					.toURL()
																					.toString()));
												} catch (Exception e) {
													MessageDialog
															.openError(
																	PlatformUI
																			.getWorkbench()
																			.getDisplay()
																			.getActiveShell(),
																	Translator
																			.trans("cv.print.error"),
																	e.getMessage());
												}

												// active the external browser
												// with the DDI document
												// - start by using application
												// associated with file type
												if (!Program.launch(htmlFile
														.getAbsolutePath())) {
													// - failed: then use
													// browser
													try {
														PlatformUI
																.getWorkbench()
																.getBrowserSupport()
																.getExternalBrowser()
																.openURL(
																		new URL(
																				"file://"
																						+ htmlFile
																								.getAbsolutePath()));
													} catch (PartInitException e) {
														Editor.showError(e,
																"Browse error");
													} catch (MalformedURLException e) {
														Editor.showError(e,
																"Browse error");
													}
												}
											}
										});
								monitor.worked(1);
							} catch (Exception e) {
								throw new InvocationTargetException(e);
							} finally {
								monitor.done();
							}
						}
					});
		} catch (Exception e) {
			String errMess = MessageFormat
					.format(Translator
							.trans("cv.print.error"), e.getMessage()); //$NON-NLS-1$
			MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
					.getActiveShell(), Translator.trans("ErrorTitle"), errMess);
		}
		return null;
	}

	private Transformer getTransFormer() throws Exception {
		Transformer transformer = null;
		// protocol errors see:
		// https://forums.oracle.com/forums/thread.jspa?messageID=9456878
		System.setProperty("javax.xml.transform.TransformerFactory",
				"net.sf.saxon.TransformerFactoryImpl");

		InputStream xslInput = PrintCv.this.getClass().getClassLoader()
				.getResourceAsStream("resources/gc_ddi-cv2html.xslt");
		StreamSource source = new StreamSource(xslInput);

		source.setSystemId(new File("resources/gc_ddi-cv2html.xslt").toURI()
				.toURL().toString());

		TransformerFactory tFactory = TransformerFactory.newInstance();
		transformer = tFactory.newTransformer(source);

		transformer.setParameter("Language",
				LanguageUtil.getOriginalLanguage());
		return transformer;
	}
}
