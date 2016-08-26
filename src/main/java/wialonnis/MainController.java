package wialonnis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@RequestMapping("/")
@Controller("mainController")
public class MainController {
	private static final Logger logger = LoggerFactory.getLogger(MainController.class);
	private String putCoordResponse;
	@Autowired
	private CityGuideSender sender;

	public MainController() {
		try {
			URL url = getClass().getResource("/wialonnis/PutCoordResponse");
			File file = new File(url.getFile());
			FileInputStream fis = new FileInputStream(file);
			putCoordResponse = IOUtils.toString(fis);
		} catch (IOException e) {
			logger.error("Constructor error.", e);
		}
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	public void rawdata(HttpServletRequest request, HttpServletResponse response) {
		try {
			String envelope = IOUtils.toString(request.getInputStream());
			logger.debug(envelope);

			Message m = createMessage(envelope);
			sender.send(m);

			IOUtils.write(putCoordResponse.replace("{0}", Long.toString(m.getTerminalId())), response.getOutputStream(),
					"UTF-8");
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(500);
			logger.error(e.toString());
			e.printStackTrace();
		}
	}

	private Message createMessage(String envelope) {
		try {
			Document doc = XmlUtils.buildDomDocument(envelope);
			doc.getDocumentElement().normalize();

			String objectId = XmlUtils.getElementContent(doc.getDocumentElement(), "ObjectID");
			Element coord = XmlUtils.getOneElement(doc.getDocumentElement(), "Coord");
			String time = coord.getAttribute("time");
			String lon = coord.getAttribute("lon");
			String lat = coord.getAttribute("lat");
			String speed = coord.getAttribute("speed");

			Message m = new Message();
			m.setTerminalId(Long.parseLong(objectId));
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			m.setTime(df.parse(time));
			m.setLat(Double.parseDouble(lat));
			m.setLon(Double.parseDouble(lon));
			m.setSpeed(Double.parseDouble(speed));

			return m;
		} catch (ParserConfigurationException | SAXException | IOException | ParseException e) {
			logger.error("Parse message error. " + envelope, e);
		}
		return null;
	}
}
