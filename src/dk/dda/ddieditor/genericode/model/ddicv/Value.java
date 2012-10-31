package dk.dda.ddieditor.genericode.model.ddicv;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

public class Value {
	public static final String DDI_CV_NAESPACE = "urn:ddi-cv";

	public static XmlObject createDdiCvValueWithKeyAttValue(String attrValue,
			String text) {
		return createDdiCvValue(null, "key", attrValue, text);
	}

	public static XmlObject createDdiCvValue(String attrName, String attrValue,
			String text) {
		return createDdiCvValue(null, attrName, attrValue, text);
	}

	public static XmlObject createDdiCvValue(String attrNamespace,
			String attrName, String attrValue, String text) {
		XmlObject xmlObject = XmlObject.Factory.newInstance();

		// element
		XmlCursor xmlCursor = xmlObject.newCursor();
		xmlCursor.toNextToken();
		xmlCursor.beginElement("Value", DDI_CV_NAESPACE);

		// attribute
		if (attrName != null && attrValue != null) {
			xmlCursor.toFirstChild();
			if (attrNamespace != null) {
				xmlCursor.insertAttributeWithValue(attrName, attrNamespace,
						attrValue);
			} else {
				xmlCursor.insertAttributeWithValue(attrName, attrValue);
			}
		}

		// text
		if (text != null) {
			xmlCursor.toFirstContentToken();
			xmlCursor.insertChars(text);
		}
		return xmlObject;
	}
}
