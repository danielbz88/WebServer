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
		if(httpRequest.getMethod().equals(Utils.POST)){
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
			
			//TODO: impelement different method responses
			switch (httpRequest.getMethod()) {
			case Utils.GET:
				break;
			case Utils.POST:
				break;
			case Utils.HEAD:
				//Do nothing
				return;
			case Utils.TRACE:
				//Send back the headers from request
				outToClient.writeBytes(httpRequest.getAllHeaders());
				break;
			default:
				break;
			}
			
			sendDataToClient(outToClient, resource);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}

	private File respondAndGetResource(DataOutputStream outToClient, HTTPRequest httpRequest) throws IOException {
		
		if(httpRequest.isBadRequest()){
			System.out.println(httpRequest.getHTTPVersion() + Utils.BAD_REQUEST + Utils.CRLF + Utils.CRLF);
			outToClient.writeBytes(httpRequest.getHTTPVersion() + Utils.BAD_REQUEST + Utils.CRLF + Utils.CRLF);
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
		
		StringBuilder sb = new StringBuilder();
		sb.append(statusLine);
		
		resourceType = getResourceType(resourcePath);
		sb.append(Utils.CONTENT_TYPE + resourceType);
		
		contentLength = getContentLength(resource);
		sb.append(Utils.CONTENT_LENGTH + contentLength);
		
		System.out.println(statusLine + httpRequest.getAllHeaders());
		outToClient.writeBytes(statusLine + httpRequest.getAllHeaders() + Utils.CRLF);
		return resource;
	}
	
	private int getContentLength(File resource) {
		// TODO implement
		return 0;
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

	private void sendDataToClient(DataOutputStream outToClient, File resource) throws IOException {
		String entityBody = Utils.readFile(resource);
		
		// Send file
		//TODO: deal with different file types
		//TODO: support chunked - ask Idan
		outToClient.writeBytes(entityBody);
	}
}


//switch (httpRequest.getMethod()) {
//case Utils.GET:
//	break;
//case Utils.POST:
//	break;
//case Utils.HEAD:
//	break;
//case Utils.TRACE:
//	break;
//default:
//	break;
//}
//switch(httpRequest.getHeader(Utils.CONTENT_TYPE)){
//case Utils.IMAGE:
//	break;
//case Utils.ICON:
//	break;
//case Utils.TEXT_HTML:
//	break;
//default:
//	break;	
//}