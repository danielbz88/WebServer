import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

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
			try
			{
				BufferedReader inFromClient =
						new BufferedReader(new InputStreamReader(socket.getInputStream()));
				DataOutputStream outToClient =
						new DataOutputStream(socket.getOutputStream());


				HTTPRequest httpRequest = processRequest(inFromClient);
				processResponse(outToClient, httpRequest);
				socket.close();
			}
			catch (Exception e)
			{
				System.out.println(e);
			}
		}

	}

	private HTTPRequest processRequest(BufferedReader inFromClient) throws IOException {
		HTTPRequest httpRequest;
		String firstLine;
		StringBuilder requestHeaders = new StringBuilder();

		// Read lines until we recognize the start of an HTTP protocol
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

		// we continue to read additional 'Content-Length' chars as parameters
		StringBuilder params = new StringBuilder();			
		String contentLength = httpRequest.getHeader("Content-Length");
		if(contentLength != null){
			int charsToRead = Integer.parseInt(contentLength);		
			for (int i = 0; i < charsToRead ; i++) {
				params.append((char) inFromClient.read());
			}

			httpRequest.parseParmas(params.toString());
		}


		/*// Print the request header
		System.out.println("Thread " + this.id + " processing request:");
		System.out.println("==================================================");
		System.out.println(firstLine);
		System.out.println(requestHeaders);
		System.out.println("==================================================");*/


		httpRequest.printRequestDebug();
		return httpRequest;
	}

	private void processResponse(DataOutputStream outToClient, HTTPRequest httpRequest) {
		// Construct the response message.
		String statusLine = httpRequest.getHTTPVersion() + " ";
		String contentTypeLine = "Content-Type: text/html" + Utils.CRLF;
		String contentLength = "Content-Length: ";

		if(httpRequest.isBadRequest()){
			statusLine += Utils.BAD_REQUEST + Utils.CRLF;
		} else {
			statusLine += Utils.OK + Utils.CRLF;
		}



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

			// Close streams and socket.
			outToClient.close();
		} catch (IOException e) {

		}



	}


}
