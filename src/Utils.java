import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map.Entry;

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
	
	// Headers
	final protected static String CONTENT_TYPE = "Content-Type";
	final protected static String CONTENT_LENGTH = "Content-Length";
	final protected static String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
	
	// Resource Types
	final protected static String IMAGE = "image";
	final protected static String ICON = "icon";
	final protected static String TEXT_HTML = "text/html";
	final protected static String APPLICATION_OCTET_STREAM = "application/octet-stream";
	
	// Responses
	final protected static String OK = "200 OK";
	final protected static String NOT_FOUND = "404 Not Found";
	final protected static String NOT_IMPLEMENTED = "501 Not Implemented";
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
	
	protected static byte[] readFile(File file) throws Exception
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
	
	// return null if the file doesnt exists
	public static File getResuorce(String resourcePath) {
		File resource = null;
		if(resourcePath.equals("/") || resourcePath.equals("./")){
			resource = new File(ROOT + "/" + DEFUALT_PAGE);
		} else {
			resource = new File(ROOT + resourcePath);
		}
		
		// check if file exists
		if(!resource.exists() || resource.isDirectory()){
			resource = null;
		}
		
		return resource;
	}

	public static boolean IsMethodSupported(String method) {
		boolean isSupportedMethod = false;
		switch(method){
		case Utils.GET:
			isSupportedMethod = true;
			break;
		case Utils.POST:
			isSupportedMethod = true;
			break;
		case Utils.TRACE:
			isSupportedMethod = true;
			break;
		case Utils.HEAD:
			isSupportedMethod = true;
			break;
		case Utils.OPTIONS:
			isSupportedMethod = true;
			break;
		default:
			isSupportedMethod = false;
			break;
		}
		
		return isSupportedMethod;
	}

	public static void makeParmasInfo(HashMap<String, String> params) throws IOException {
		File file = new File(ROOT + "/params_info.html");
		FileOutputStream writeToFile = new FileOutputStream(file);
		writeToFile.write("<!DOCTYPE html><html><body>".getBytes());
		if (params != null){
			for (Entry<String, String> entry : params.entrySet())
			{
				writeToFile.write(("<p>" + entry.getKey() + "=" + entry.getValue() + "</p>").getBytes());
			}
		}	
		writeToFile.write("</body></html>".getBytes());
		writeToFile.close();
	}
}
