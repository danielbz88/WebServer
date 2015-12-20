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
				// to 30 seconds
				socket.setSoTimeout(30000);
				BufferedReader inFromClient =
						new BufferedReader(new InputStreamReader(socket.getInputStream()));
				DataOutputStream outToClient =
						new DataOutputStream(socket.getOutputStream());
				// persistent connection [bonus]
				while(connectionAlive){
					System.out.println("Thread " + this.id +" processing request.");
					HTTPRequest httpRequest = processRequest(inFromClient);
					System.out.println("");
					processResponse(outToClient, httpRequest);
					
					// [bonus] satisfy persistent connection only with HTTP/1.1
					connectionAlive = httpRequest.getHTTPVersion().equals(Utils.HTTP_VERSION_1_1);
				}
			}catch(WebServerRuntimeException | IOException e){
				//TODO: Create HTTPResponse about internal server error
				HTTPResponse errorResponse = new HTTPResponse(e);
				try{
					System.out.println(errorResponse.getResponseHeader());
					DataOutputStream outToClient =
							new DataOutputStream(socket.getOutputStream());
					outToClient.writeBytes(errorResponse.getResponseHeader() + Utils.CRLF);
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
		
		try {
			String line = "";
			
			// Read lines until we recognize the start of an HTTP protocol
			//[Bonus] 
			while(!line.endsWith(Utils.HTTP_VERSION_1_0) && !line.endsWith(" " + Utils.HTTP_VERSION_1_1)){

				line = inFromClient.readLine();
			}

			firstLine = line;
			httpRequest = new HTTPRequest(firstLine);
			requestHeaders.append(firstLine + Utils.CRLF);
							
			// Read lines until we recognize an empty line
			line = inFromClient.readLine();
			while(!line.equals("")){
				requestHeaders.append(line + Utils.CRLF);
				line = inFromClient.readLine();
			}
			
			if(!httpRequest.isBadRequest()){
				
				// parse the headers
				if(requestHeaders.length() > 0) {
					this.allHeaders = requestHeaders.toString();
					httpRequest.addHeaders(allHeaders);
				}
				
				if(httpRequest.isSupportedMethod()){
					
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
		printHeader(this.id, requestHeaders.toString(), "Request");
		return httpRequest;
	}


	private void processResponse(DataOutputStream outToClient, HTTPRequest httpRequest) throws IOException {
		HTTPResponse httpResponse = new HTTPResponse(httpRequest, this.allHeaders);
		httpResponse.makeResponse();
		if(httpResponse.isWithoutError()){
			try {
				String responseCode = httpResponse.getResponseHeader() + Utils.CRLF;
				String responseHeaders = httpResponse.getResponseHeaders() + Utils.CRLF;
				outToClient.writeBytes(responseCode);
				outToClient.writeBytes(responseHeaders);
				
				printHeader(id, responseCode + responseHeaders, "Response");
				
				
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
	
	private void printHeader(int thread, String header, String type) {       
		System.out.println("================ Thread " + thread +" " + type + " ===============");
		System.out.println(header);
		System.out.println("==================================================");
		
	}
	
	private void respondError(DataOutputStream outToClient, HTTPResponse httpResponse) throws IOException {
		HTTPResponse errorResponse = new HTTPResponse(httpResponse.response());
		System.out.println(errorResponse.getResponseHeader());
		outToClient.writeBytes(errorResponse.getResponseHeader() + Utils.CRLF);
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
}