//	Copyright 2013, by the California Institute of Technology.
//	ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
//	Any commercial use must be negotiated with the Office of Technology 
//	Transfer at the California Institute of Technology.
//	
//	This software is subject to U. S. export control laws and regulations 
//	(22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software 
//	is subject to U.S. export control laws and regulations, the recipient has 
//	the responsibility to obtain export licenses or other export authority as 
//	may be required before exporting such information to foreign countries or 
//	providing access to foreign nationals.
//	
//	$Id$
//
package gov.nasa.pds.report.manager.util;

import gov.nasa.pds.report.manager.constants.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Formatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author jpadams
 * @version $Revision$
 *
 */
public class Utility {

	/**
	 * Get the current date time.
	 * 
	 * @return A date time.
	 */
	public static String getDateTime() {
		SimpleDateFormat df = new SimpleDateFormat(
				"EEE, MMM dd yyyy 'at' hh:mm:ss a");
		Date date = Calendar.getInstance().getTime();
		return df.format(date);
	}

	public static String getFileDate() {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		Date date = Calendar.getInstance().getTime();
		return df.format(date);
	}

	public static List<String> getLocalFileList(String path) {
		File dir = new File(path);
		return Arrays.asList(dir.list());
	}

	public static String getHomeDirectory() {
		if (System.getenv(Constants.HOME_ENV_VAR) != null) {
			return System.getenv(Constants.HOME_ENV_VAR);
		} else {
			return (new File(System.getProperty("java.class.path")))
				.getParentFile().getParent();
		}
	}

	/**
	 * Method to convert the file path to absolute, if relative
	 * 
	 * @param filePath
	 *            Current path given through the command-line
	 * @return the absolute path from the input file path
	 */
	public static String getAbsolutePath(final String filePath) {
		String finalPath = "";
		File tFile = new File(filePath);
		if (!tFile.isAbsolute()) {
			finalPath = System.getProperty("user.dir") + "/" + filePath;
		} else {
			finalPath = filePath;
		}

		tFile = new File(finalPath);
		return finalPath;
	}

	public static File replaceStringInFile(String oldStr, String newStr,
			File in, File out) throws IOException {
		StringBuffer buffer = new StringBuffer();
		LineIterator it = FileUtils.lineIterator(in, "UTF-8");
		try {
			while (it.hasNext()) {
				buffer.append(it.nextLine().replaceAll(oldStr, newStr)
						+ System.getProperty("line.separator"));
			}
		} finally {
			it.close();
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
		writer.write(buffer.toString());
		writer.flush();
		writer.close();

		return out;
	}
	
	/**
	 * Utility method to extract values from an XML file.
	 * 
	 * @param file
	 * @param tagName			name of the tag we are looking for
	 * @param attributeName		if applicable, else set to null
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<String> getValuesFromXML(File file, String tagName, String attributeName) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		
		List<String> valueList = new ArrayList<String>();
		NodeList nodes = doc.getElementsByTagName(tagName);
		for (int i=0; i<nodes.getLength(); i++) {
			if (attributeName == null) {
				valueList.add(nodes.item(i).getNodeValue());
			} else {
				valueList.add(nodes.item(i).getAttributes().getNamedItem(attributeName).getNodeValue());
			}
		}
		
		return valueList;
	}
}