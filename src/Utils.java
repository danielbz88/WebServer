import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {
	protected static int PORT;
	protected static String ROOT;
	protected static String DEFUALT_PAGE;
	protected static int MAX_THREADS;
	
	protected static boolean parseConfigFile() {
		int numOfParsedProps = 0;
		try {
			String rawConfigFile = readFile(new File("../WebServer/config.ini"));
			String[] properties = rawConfigFile.split("\n");
			for (String property : properties) {
				int delim = property.indexOf('=');
				String key = property.substring(0, delim).trim();
				String value = property.substring(delim + 1).trim();
				switch (key) {
				case "port":
					PORT = Integer.parseInt(value);
					numOfParsedProps++;
					break;
				case "root":
					ROOT = value;
					numOfParsedProps++;
					break;
				case "defaultPage":
					DEFUALT_PAGE = value;
					numOfParsedProps++;
					break;
				case "maxThreads":
					MAX_THREADS = Integer.parseInt(value);
					numOfParsedProps++;
					break;
				default:
					break;
				}
			}
		} catch (Exception e) {
			//
		}
		
		return (numOfParsedProps == 4);		
	}

	protected static String readFile(File file) throws IOException {
		String rawConfigFile = "";
		FileInputStream fis = new FileInputStream(file);
		byte[] bFile = new byte[(int)file.length()];
		// read until the end of the stream.
		while(fis.available() != 0)
		{
			fis.read(bFile, 0, bFile.length);
		}
		fis.close();
		rawConfigFile = new String(bFile);
		
		return rawConfigFile;
	}
}
