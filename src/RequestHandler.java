import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class RequestHandler implements Runnable {
	private SynchronizedQueue<Socket> jobsQueue;
	private int id;
	private String allHeaders;

	public RequestHandler(SynchronizedQueue<Socket> jobsQueue, int id) {
		this.jobsQueue = jobsQueue;
		this.id = id;
		
		//Used for TRACE method
		this.allHeaders = "";
	}

	@Override
	public void run() {
		Socket socket;
		boolean connectionAlive;

		// we get null only when jobsQueue is empty and without producers
		while ((socket = jobsQueue.dequeue()) != null) {
			try{

				connectionAlive = true;
				// Set the time we wait for the client to write a new-line
				// to 10 seconds
				//socket.setSoTimeout(10000);
				BufferedReader inFromClient =
						new BufferedReader(new InputStreamReader(socket.getInputStream()));
				DataOutputStream outToClient =
						new DataOutputStream(socket.getOutputStream());
				// persistent connection
				while(connectionAlive){
					System.out.println("Thread " + this.id +" processing request.");
					HTTPRequest httpRequest = processRequest(inFromClient);
					System.out.println("");
					processResponse(outToClient, httpRequest);	
				}
			}catch(WebServerRuntimeException | IOException e){
				//TODO: Create HTTPResponse about internal server error
				HTTPResponse errorResponse = new HTTPResponse(e);
				try{
					System.out.println(errorResponse.getHTTPVersion() + " " + errorResponse.getResponseCode());
					DataOutputStream outToClient =
							new DataOutputStream(socket.getOutputStream());
					outToClient.writeBytes(errorResponse.getHTTPVersion() + " " + errorResponse.getResponseCode() + Utils.CRLF);
					outToClient.writeBytes(Utils.CONTENT_LENGTH + ": " + 0 + Utils.CRLF + Utils.CRLF);
				}catch(Exception e1){
					connectionAlive = false;
					System.out.println("Connection Lost");
				}
			} catch (Exception e) {
				connectionAlive = false;
				System.out.println("Connection Lost");
			}
		}
	}

	private HTTPRequest processRequest(BufferedReader inFromClient) throws WebServerRuntimeException, Exception {
		HTTPRequest httpRequest = null;
		String firstLine;
		StringBuilder requestHeaders = new StringBuilder();

		// Read lines until we recognize the start of an HTTP protocol
		//**Bonus** 
		try {
			String line = "";
			while(!line.endsWith(Utils.HTTP_VERSION_1_0) && !line.endsWith(" " + Utils.HTTP_VERSION_1_1)){

				line = inFromClient.readLine();
			}

			firstLine = line;
			httpRequest = new HTTPRequest(firstLine);
			if(!httpRequest.isBadRequest()){
				// Read lines until we recognize an empty line
				line = inFromClient.readLine();
				while(!line.equals("")){
					requestHeaders.append(line + Utils.CRLF);
					line = inFromClient.readLine();
				}

				if(httpRequest.isSupportedMethod()){
					// parse the headers
					if(requestHeaders.length() > 0) {
						this.allHeaders = requestHeaders.toString();
						httpRequest.addHeaders(allHeaders);
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

							httpRequest.addParams(params.toString());
						}
					}	
				}
			}
		} catch (IOException e) {
			throw new WebServerRuntimeException("Error dealing with request");
		}
		
		httpRequest.validate();
		httpRequest.printRequestDebug();
		return httpRequest;
	}

	private void processResponse(DataOutputStream outToClient, HTTPRequest httpRequest) throws IOException {
		HTTPResponse httpResponse = new HTTPResponse(httpRequest, this.allHeaders);
		httpResponse.makeResponse();
		if(httpResponse.isWithoutError()){
			try {
				String responseCode = httpResponse.getResponseCode() + Utils.CRLF;
				String responseHeaders = httpResponse.getResponseHeaders() + Utils.CRLF;
				outToClient.writeBytes(responseCode);
				outToClient.writeBytes(responseHeaders);
				
				httpResponse.printResponseDebug();
				
				byte[] entityBody = httpResponse.getResponseBody();
				
				if(entityBody != null){
					if(httpRequest.isChunked()){
						writeChuncked(outToClient, entityBody);
					} else {
						outToClient.write(entityBody, 0, httpResponse.getContentLength());
					}
					outToClient.writeBytes(Utils.CRLF + Utils.CRLF);
				}else{
					outToClient.writeBytes(Utils.CRLF);
				}
			} catch (Exception e) {
				respondError(outToClient, httpResponse);
			}
		}else{
			respondError(outToClient, httpResponse);			
		}		
	}
	
	private void respondError(DataOutputStream outToClient, HTTPResponse httpResponse) throws IOException {
		HTTPResponse errorResponse = new HTTPResponse(httpResponse.getResponseCode());
		System.out.println(errorResponse.getHTTPVersion() + " " + errorResponse.getResponseCode());
		outToClient.writeBytes(errorResponse.getHTTPVersion() + " " + errorResponse.getResponseCode() + Utils.CRLF);
		outToClient.writeBytes(Utils.CONTENT_LENGTH + ": " + 0 + Utils.CRLF + Utils.CRLF);
	}

	private void writeChuncked(DataOutputStream outToClient, byte[] responseBody) throws IOException {
		int offset = 0;
		int len = Utils.CHUNK_SIZE;
		while (offset < responseBody.length) {
			if (offset + len > responseBody.length) {
				len = responseBody.length - offset;
			}

			outToClient.writeBytes(Integer.toHexString(len) + Utils.CRLF);
			outToClient.write(responseBody, offset, len);
			outToClient.writeBytes(Utils.CRLF);

			offset += len;
		}
		
		outToClient.writeBytes("0" + Utils.CRLF + Utils.CRLF);
	}
	
	/*private void processResponse(DataOutputStream outToClient, HTTPRequest httpRequest) throws IOException {	
		try {

			String resourcePath;

			// Check if resource is default page
			if(httpRequest.getResourcePath().equals("/")){ // ^&^ you were using'==' instead of 'equels'
				resourcePath = Utils.ROOT + "/" + Utils.DEFUALT_PAGE; // ^&^ you were missing 'Utils.ROOT'
			}else{
				resourcePath = Utils.ROOT + httpRequest.getResourcePath(); // ^&^ you were missing 'Utils.ROOT'
			}

			File resource = new File(resourcePath);
			String header = getBasicHeader(httpRequest, resource);

			switch (httpRequest.getMethod()) {
			case GET:
				sendHeaderToClient(header, outToClient);
				sendDataToClient(httpRequest, outToClient, resource);
				break;
			case POST:
				//TODO: Figure out difference between GET and POST and implement
				sendHeaderToClient(header, outToClient);
				sendDataToClient(httpRequest, outToClient, resource);
				break;
			case HEAD:
				sendHeaderToClient(header, outToClient);
				//Do nothing
				return;
			case TRACE:
				traceResonse(header, httpRequest, outToClient);
				break;
			case OPTIONS:
				optionsResonse(header, outToClient);
				break;
			default: // ^&^  case of an Error
				sendHeaderToClient(header, outToClient);
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}*/
	
	

	/*private void traceResonse(String header, HTTPRequest httpRequest, DataOutputStream outToClient) throws IOException {
		
		//send HTTP response header
		sendHeaderToClient(header, outToClient);
		
		//display back the headers from the request
		String response = httpRequest.getAllHeaders();
		int len = response.length();
		String output = len + Utils.CRLF + httpRequest.getAllHeaders();
		System.out.println(output);
		outToClient.writeBytes(output);
	}

	//Prints and sends given header out to client, after appending the header end mark: CRLF
	private void sendHeaderToClient(String header,  DataOutputStream outputStream) throws IOException {
		System.out.println(header + Utils.CRLF);
		outputStream.writeBytes(header + Utils.CRLF);
	}

	//Appends options header and sends to client.
	private void optionsResonse(String basicHeader, DataOutputStream outToClient) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (httpMethod method : httpMethod.values()) {
			sb.append(method + ", ");
		}
		
		sendHeaderToClient(basicHeader + sb.toString(), outToClient);
	}
	
	private int getContentLength(File resource) {
		return (int)resource.length();
	}
	
	private String getResourceType(String resourcePath) {
		String type;		
		int delim = resourcePath.indexOf('.'); // ^&^ replaced 'split' method with 'indexOf'
		String suffix = resourcePath.substring(delim + 1);
		switch(suffix){
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
	
	private String getBasicHeader(HTTPRequest httpRequest, File resource){
		String outputHeader = "HTTP/1.1 "; // ^&^ server has to be 1.1 
		
		// ^&^ added 'else' because your flow wasn't correct
		if(httpRequest.isBadRequest()){
			outputHeader += Utils.BAD_REQUEST + Utils.CRLF + Utils.CRLF;
		}
		
		else if(!httpRequest.isSupportedMethod()){
			outputHeader += Utils.NOT_IMPLEMENTED + Utils.CRLF + Utils.CRLF;
		}
		// Check resource existence
		else if(!resource.exists() || resource.isDirectory()) {
			outputHeader = httpRequest.getHTTPVersion() + Utils.NOT_FOUND + Utils.CRLF;
			
		} else { 
			//Start building response header
			StringBuilder sb = new StringBuilder();
			
			//append status-line
			sb.append(Utils.OK + Utils.CRLF);
			
			//append content-type header
			sb.append(Utils.CONTENT_TYPE + getResourceType(resource.getAbsolutePath()) + Utils.CRLF);
					
			if(httpRequest.isChunked()){
				sb.append(Utils.HEADER_TRANSFER_ENCODING + "chunked" + Utils.CRLF);
			}else{
				
				//append content-length header
				sb.append(Utils.CONTENT_LENGTH + getContentLength(resource) + Utils.CRLF);
			}
			
			outputHeader = sb.toString();
			
		}
		
		//return header
		return outputHeader;
	}
	
	
	// ^&^ We are now working only with bytes no need to read files as Strings
	private void sendDataToClient(HTTPRequest httpRequest, DataOutputStream outToClient, File resource) throws IOException {
		byte[] entityBody = Utils.readFile(resource);
		
		// Send file
		if(httpRequest.isChunked()){
			int offset = 0;
			int len = Utils.CHUNK_SIZE;
			while (offset < entityBody.length) {
				if (offset + len > entityBody.length) {
					len = entityBody.length - offset;
				}

				outToClient.writeBytes(Integer.toHexString(len) + Utils.CRLF);
				outToClient.write(entityBody, offset, len);
				outToClient.writeBytes(Utils.CRLF);

				offset += len;
			}
		}else{
			outToClient.write(entityBody, 0, entityBody.length);
			//outToClient.writeBytes(entityBody);
		}
		
		outToClient.writeBytes("0" + Utils.CRLF + Utils.CRLF);
	}*/
}