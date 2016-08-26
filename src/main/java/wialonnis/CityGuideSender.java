package wialonnis;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Service
public class CityGuideSender {
	private static final Logger logger = LoggerFactory.getLogger(CityGuideSender.class);

	@Value("#{mainSettings['cityguide.url']}")
	private String CITYGUIDE_URL = "http://service.probki.net/xmltrack/api/nytrack";

	public void send(Message message) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			Element pointsElement = doc.createElement("points");
			pointsElement.setAttribute("id", Long.toString(message.getTerminalId()));
			doc.appendChild(pointsElement);

			Element pointElement = doc.createElement("point");
			pointElement.setAttribute("speed", Integer.toString(Double.valueOf(message.getSpeed()).intValue()));
			pointElement.setAttribute("lat", Double.toString(message.getLat()));
			pointElement.setAttribute("lon", Double.toString(message.getLon()));
			pointElement.setAttribute("isotime", parseToIsoTime(utcToLocal(message.getTime()).getTime()));
			pointsElement.appendChild(pointElement);

			sendCityGuideMessage(doc);
		} catch (ParserConfigurationException | IOException | TransformerException
				| TransformerFactoryConfigurationError e) {
			logger.error("CityGuideHandler error.", e);
		}
	}

	private String parseToIsoTime(Date time) {
		// 2014-10-23T11:01:57+0300
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.format(time);
	}

	private void sendCityGuideMessage(Document doc) throws TransformerFactoryConfigurationError, TransformerException,
			IOException {
		String raw = nodeToString(doc.getFirstChild());
		logger.debug(raw);

		URL url = new URL(CITYGUIDE_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "text/plain");
		connection.setDoOutput(true);
		IOUtils.write(raw, connection.getOutputStream(), "UTF-8");
		connection.getOutputStream().flush();
		connection.getOutputStream().close();

		if (connection.getResponseCode() != 200) {
			String reason = IOUtils.toString(connection.getInputStream());
			logger.warn("CityGuideHandler error send point. Error code=" + connection.getResponseCode() + ", reason: "
					+ reason);
		}
		connection.disconnect();
	}

	private String nodeToString(Node node) throws TransformerFactoryConfigurationError, TransformerException {
		StringWriter writer = new StringWriter();
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		return writer.toString();
	}

	private Calendar utcToLocal(Date utcDate) {
		// 1) utcDate without time zone
		Calendar localCalendar = Calendar.getInstance();
		localCalendar.setTime(utcDate);

		// 2) now apply UTC time zone
		Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		utcCalendar.set(localCalendar.get(Calendar.YEAR),
				localCalendar.get(Calendar.MONTH),
				localCalendar.get(Calendar.DAY_OF_MONTH),
				localCalendar.get(Calendar.HOUR_OF_DAY),
				localCalendar.get(Calendar.MINUTE));

		return utcCalendar;
	}
}
