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
		
		String header = getResponseHeader(httpRequest);
		System.out.println(header);
		sendDataToClient(httpRequest);
		
		
//		TODO: Sort-of following lines:
//		Read the file’s content.
		
//		Create HTTP response header:
//		HTTP/1.1 200 OK[CRLF]
//		content-type: text/html[CRLF]
//		content-length: <the length of index.html>[CRLF] [CRLF]
		
//		Print the header.
		
//		Send full response to client (including the page content).		
	}

	private String getResponseHeader(HTTPRequest httpRequest) {
		
		if(httpRequest.isBadRequest()){
			return (httpRequest.getHTTPVersion() + Utils.BAD_REQUEST + Utils.CRLF);
		}

		String statusLine;
		String resourcePath;

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
			return httpRequest.getHTTPVersion() + Utils.NOT_FOUND + Utils.CRLF;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(statusLine);
		
		//TODO: Get all necessary headers and append to make response header.
		
		
		
		return sb.toString();
		
//		switch (httpRequest.getMethod()) {
//		case Utils.GET:
//			break;
//		case Utils.POST:
//			break;
//		case Utils.HEAD:
//			break;
//		case Utils.TRACE:
//			break;
//		default:
//			break;
//		}
//		switch(httpRequest.getHeader(Utils.CONTENT_TYPE)){
//		case Utils.IMAGE:
//			break;
//		case Utils.ICON:
//			break;
//		case Utils.TEXT_HTML:
//			break;
//		default:
//			break;	
//		}
	}
	
	private void sendDataToClient(HTTPRequest httpRequest) {
		//TODO
		try {
			String entityBody = Utils.readFile(new File(Utils.ROOT + httpRequest.getPage()));
			// Send the status line.
			outToClient.writeBytes(statusLine);

			// Send the content type line.
			outToClient.writeBytes(contentTypeLine);

			// Send content length.
			outToClient.writeBytes(contentLength + entityBody.length() + Utils.CRLF);

			// Send a blank line to indicate the end of the header lines.
			outToClient.writeBytes(Utils.CRLF);

			// Send the content of the HTTP.
			outToClient.writeBytes(entityBody) ;
		} catch (IOException e) {

		}	
	}
}


