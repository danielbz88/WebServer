import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.lang.Class;
import java.lang.reflect.InvocationTargetException;

public class RequestHandler implements Runnable {
	private SynchronizedQueue<Socket> jobsQueue;
	private int id;

	public RequestHandler(SynchronizedQueue<Socket> jobsQueue, int id) {
		this.jobsQueue = jobsQueue;
		this.id = id;
	}

	@Override
	public void run() {
		Socket socket;
		// we get null only when jobsQueue is empty and without producers
		while ((socket = jobsQueue.dequeue()) != null) {
			boolean connectionAlive = true;
			try{
				// Set the time we wait for the client to write a new-line
				// to 10 seconds
				socket.setSoTimeout(10000);
				BufferedReader inFromClient =
						new BufferedReader(new InputStreamReader(socket.getInputStream()));
				DataOutputStream outToClient =
						new DataOutputStream(socket.getOutputStream());
				// persistent connection
				while(connectionAlive){
					System.out.println("Thread " + this.id +" processing request.");
					HTTPRequest httpRequest = processRequest(inFromClient);
					processResponse(outToClient, httpRequest);
				}
			}

			catch (Exception e)
			{
				connectionAlive = false;
				System.out.println("Connection Lost");
			}
		}
	}

	private HTTPRequest processRequest(BufferedReader inFromClient) throws IOException {
		HTTPRequest httpRequest;
		String firstLine;
		StringBuilder requestHeaders = new StringBuilder();

		// Read lines until we recognize the start of an HTTP protocol
		//**Bonus** 
		String line = "";
		while(!line.endsWith(Utils.HTTP_VERSION_1_0) && !line.endsWith(" " + Utils.HTTP_VERSION_1_1)){
			line = inFromClient.readLine();
		}

		firstLine = line;
		httpRequest = new HTTPRequest(firstLine);

		// Read lines until we recognize an empty line
		line = inFromClient.readLine();
		while(!line.equals("")){
			requestHeaders.append(line + Utils.CRLF);
			line = inFromClient.readLine();
		}

		// parse the headers
		if(requestHeaders.length() > 0) {
			httpRequest.parseHeaders(requestHeaders.toString());
		}

		// if the request is 'POST'
		// we continue to read additional 'Content-Length' characters as parameters
		if(httpRequest.getMethod().equals(httpMethod.POST)){
			StringBuilder params = new StringBuilder();			
			String contentLength = httpRequest.getHeader("Content-Length");
			if(contentLength != null){
				int charactersToRead = Integer.parseInt(contentLength);		
				for (int i = 0; i < charactersToRead ; i++) {
					params.append((char) inFromClient.read());
				}

				httpRequest.parseParmas(params.toString());
			}
		}

		/*// Print the request header
		System.out.println("Thread " + this.id + " processing request:");
		System.out.println("==================================================");
		System.out.println(firstLine);
		System.out.println(requestHeaders);
		System.out.println("==================================================");*/

		httpRequest.validate();
		httpRequest.printRequestDebug();
		return httpRequest;
	}

	private void processResponse(DataOutputStream outToClient, HTTPRequest httpRequest) {
		
		File resource;
		try {
			resource = respondAndGetResource(outToClient, httpRequest);
			
			//TODO: implement different method responses
			switch (httpRequest.getMethod()) {
			case GET:
				//TODO
				sendDataToClient(httpRequest, outToClient, resource);
				break;
			case POST:
				//TODO
				sendDataToClient(httpRequest, outToClient, resource);
				break;
			case HEAD:
				//Do nothing
				return;
			case TRACE:
				//Send back the headers from request
				outToClient.writeBytes(httpRequest.getAllHeaders());
				break;
			case OPTIONS:
				//TODO
				optionsResonse(outToClient);
				break;
			default:
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}
	
	private void optionsResonse(DataOutputStream outToClient) {
//TODO:implement
		StringBuilder sb = new StringBuilder();
		for (httpMethod method : httpMethod.values()) {
			sb.append(method + ", ");
		}
		
		String allMethods = sb.toString();
	}

	private File respondAndGetResource(DataOutputStream outToClient, HTTPRequest httpRequest) throws IOException {
		
		if(httpRequest.isBadRequest()){
			System.out.println(httpRequest.getHTTPVersion() + Utils.BAD_REQUEST + Utils.CRLF + Utils.CRLF);
			outToClient.writeBytes(httpRequest.getHTTPVersion() + Utils.BAD_REQUEST + Utils.CRLF + Utils.CRLF);
			return null;
		}
		
		if(!httpRequest.isSupportedMethod()){
			System.out.println(httpRequest.getHTTPVersion() + Utils.NOT_IMPLEMENTED + Utils.CRLF + Utils.CRLF);
			outToClient.writeBytes(httpRequest.getHTTPVersion() + Utils.NOT_IMPLEMENTED + Utils.CRLF + Utils.CRLF);
			return null;
		}

		String statusLine;
		String resourcePath;
		String resourceType;
		int contentLength = 0;

		// Check if resource is default page
		if(httpRequest.getResourcePath() == "/"){
			resourcePath = Utils.DEFUALT_PAGE;
		}else{
			resourcePath = httpRequest.getResourcePath();
		}
		
		// Create status-line, Check resource existence
		File resource = new File(resourcePath);
		if(resource.exists() && !resource.isDirectory()) {
			statusLine = httpRequest.getHTTPVersion() + Utils.OK + Utils.CRLF;
		}else{
			System.out.println(httpRequest.getHTTPVersion() + Utils.NOT_FOUND + Utils.CRLF + Utils.CRLF);
			outToClient.writeBytes(httpRequest.getHTTPVersion() + Utils.NOT_FOUND + Utils.CRLF + Utils.CRLF);
			return null;
		}
		
		//Start building response header
		StringBuilder sb = new StringBuilder();
		sb.append(statusLine);
		
		resourceType = getResourceType(resourcePath);
		sb.append(Utils.CONTENT_TYPE + resourceType + Utils.CRLF);
		
		contentLength = getContentLength(resource);
		sb.append(Utils.CONTENT_LENGTH + contentLength + Utils.CRLF);
		
		if(httpRequest.isChunked()){
			sb.append(Utils.HEADER_TRANSFER_ENCODING + "chunked" + Utils.CRLF);
		}
		
		System.out.println(sb.append(Utils.CRLF).toString());
		outToClient.writeBytes(sb.append(Utils.CRLF).toString());
		
		//Return resource file
		return resource;
	}
	
	private int getContentLength(File resource) {
		return (int)resource.length();
	}

	private String getResourceType(String resourcePath) {
		String type;		
		String[] tokens = resourcePath.split(".");
		switch(tokens[tokens.length - 1]){
		case "bmp":
		case "jpg":
		case "gif":
		case "png":
			
			//Image
			type = Utils.IMAGE;
			break;
		case "ico":
			
			//Icon
			type = Utils.ICON;
			break;
		case "txt":
		case "html":
			
			//text/html
			type = Utils.TEXT_HTML;
			break;
		default:
			
			//application/octet-stream
			type = Utils.APPLICATION_OCTET_STREAM;
			break;
		}
		
		return type;
	}
	
	private String getBasicHeader(){
		
	}
	
	

	private void sendDataToClient(HTTPRequest httpRequest, DataOutputStream outToClient, File resource) throws IOException {
		String entityBody = Utils.readFile(resource);
		byte[] bodyBytes = entityBody.getBytes();
		
		// Send file
		if(httpRequest.isChunked()){
			int offset = 0;
			int len = Utils.CHUNK_SIZE;
			while (offset < bodyBytes.length) {
				if (offset + len > bodyBytes.length) {
					len = bodyBytes.length - offset;
				}

				outToClient.writeBytes(Integer.toHexString(len) + Utils.CRLF);
				outToClient.write(bodyBytes, offset, len);
				outToClient.writeBytes(Utils.CRLF);

				offset += len;
			}
		}else{
			outToClient.writeBytes(entityBody);
		}
		
		outToClient.writeBytes("0" + Utils.CRLF + Utils.CRLF);
	}
}