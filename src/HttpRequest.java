import java.io.* ;
import java.net.* ;
import java.util.* ;

/*
 * This class receives in its constructor the HTTP request and 
 * parses the header fields.
 */

final class HttpRequest implements Runnable
{
    final static String CRLF = "\r\n";
    Socket socket;
    
    // Constructor
    public HttpRequest(Socket socket) throws Exception
	{
		this.socket = socket;
    }
    
    // Implement the run() method of the Runnable interface.
    public void run()
	{
		try
		{
		    processRequest();
		}
		catch (Exception e)
		{
		    System.out.println(e);
		}
	}

	private void processRequest() throws Exception
	{
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
		
		// Send the status line.
		os.writeBytes(statusLine);
		
		// Send the content type line.
		os.writeBytes(contentTypeLine);
		
		// Send content length.
		os.writeBytes(contentLength + entityBody.length() + CRLF);
		
		// Send a blank line to indicate the end of the header lines.
		os.writeBytes(CRLF);
		
		// Send the content of the HTTP.
		os.writeBytes(entityBody) ;
		
		// Close streams and socket.
		os.close();
		socket.close();
    }

}


