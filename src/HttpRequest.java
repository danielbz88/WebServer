import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class HttpRequest implements Runnable {
	final static String CRLF = "\r\n";
	private SynchronizedQueue<Socket> jobsQueue;
	private int id;

	public HttpRequest(SynchronizedQueue<Socket> jobsQueue, int id) {
		this.jobsQueue = jobsQueue;
		this.id = id;
	}

	@Override
	public void run() {
		Socket socket;
        // we get null only when resultsQueue is empty and without producers
        while ((socket = jobsQueue.dequeue()) != null) {
        	try
    		{
        		System.out.println("Thread " + this.id + " starting process the request");
    		    processRequest(socket);
    		    System.out.println("Thread " + this.id + " finish process the request");
    		}
    		catch (Exception e)
    		{
    		    System.out.println(e);
    		}
        }
		
	}

	private void processRequest(Socket socket) throws IOException {
		DataOutputStream os = new DataOutputStream(socket.getOutputStream());
		//Get the source IP
		String sourceIP = socket.getInetAddress().getHostAddress();
		
		// Construct the response message.
		String statusLine = "HTTP/1.0 200 OK" + CRLF;
		String contentTypeLine = "Content-Type: text/html" + CRLF;
		String contentLength = "Content-Length: ";
		
		String entityBody = "<HTML>" + 
		"<HEAD><TITLE>"+ sourceIP +"</TITLE></HEAD>" +
		"<BODY><H1>"+ sourceIP +"</H1></BODY></HTML>";
		
		int x= 0;
		for (int i = 0; i < Math.pow(2, 30); i++) {
			x++;
		}
		
		for (int i = 0; i < Math.pow(2, 30); i++) {
			x++;
		}
		
		// Send the status line.
		os.writeBytes(statusLine);
		
		// Send the content type line.
		os.writeBytes(contentTypeLine);
		
		// Send content length.
		os.writeBytes(contentLength + entityBody.length() + CRLF);
		
		// Send a blank line to indicate the end of the header lines.
		os.writeBytes("" + x);
		os.writeBytes(CRLF);
		
		// Send the content of the HTTP.
		os.writeBytes(entityBody) ;
		
		// Close streams and socket.
		os.close();
		socket.close();
		
	}

}
