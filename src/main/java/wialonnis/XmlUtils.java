package wialonnis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtils {
	public static String nodeToString(Node node) throws TransformerFactoryConfigurationError, TransformerException {
		StringWriter writer = new StringWriter();
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		return writer.toString();
	}

	public static Document buildDomDocument(InputStream xmlInputStream) throws ParserConfigurationException,
			SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlInputStream);
		return doc;
	}

	public static Document buildDomDocument(String data) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new ByteArrayInputStream(data.getBytes("utf-8"))));
		return doc;
	}

	public static String getElementContent(Element element, String elementName) {
		NodeList nodeList = element.getElementsByTagName(elementName);
		if (nodeList != null && nodeList.getLength() > 0) {
			if (nodeList.item(0).getFirstChild() != null) {
				return nodeList.item(0).getFirstChild().getNodeValue();
			}
		}
		return null;
	}

	public static Element getOneElement(Element element, String elementName) {
		NodeList nodeList = element.getElementsByTagName(elementName);
		if (nodeList != null && nodeList.getLength() == 1) {
			return (Element) nodeList.item(0);
		} else {
			return null;
		}
	}
}
