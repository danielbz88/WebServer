import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {
	
	// HTTP textual elements
	final protected static String CRLF = "\r\n";
	final protected static String END_OF_HEADER = "\r\n\r\n";
	final protected static String HTTP_VERSION_1_0 = "HTTP/1.0";
	final protected static String HTTP_VERSION_1_1 = "HTTP/1.1";
	
	// Methods
	final protected static String GET = "GET";
	final protected static String POST = "POST";
	final protected static String HEAD = "HEAD";
	final protected static String TRACE = "TRACE";
	final protected static String OPTIONS = "OPTIONS";
	//TODO: Move all Utils.METHOD references to Enums 
	
	// Headers
	final protected static String CONTENT_TYPE = "Content-Type: ";
	final protected static String CONTENT_LENGTH = "Content-Length: ";
	final protected static String HEADER_TRANSFER_ENCODING = "Transfer-Encoding: ";
	
	// Resource Types
	final protected static String IMAGE = "image";
	final protected static String ICON = "icon";
	final protected static String TEXT_HTML = "text/html";
	final protected static String APPLICATION_OCTET_STREAM = "application/octet-stream";
	
	// Responses
	final protected static String OK = "200 OK";
	final protected static String NOT_FOUND = "404 Not Found";
	final protected static String NOT_IMPLEMENTED = "501 Not Implemented";//TODO: catch these cases
	final protected static String BAD_REQUEST = "400 Bad Request";
	final protected static String ERROR = "500 Internal Server Error";
	
	// Configurations 
	protected static int PORT;
	protected static String ROOT;
	protected static String DEFUALT_PAGE;
	protected static int MAX_THREADS;
	protected static int CHUNK_SIZE = 200;
	
	protected static boolean parseConfigFile() {
		int numOfParsedProps = 0;
		try {
			String rawConfigFile = new String(readFile(new File("config.ini")));
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
	
	protected static byte[] readFile(File file) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		byte[] bFile = new byte[(int)file.length()];
		// read until the end of the stream.
		while(fis.available() != 0)
		{
			fis.read(bFile, 0, bFile.length);
		}
		fis.close();
		return bFile;
	}
}
