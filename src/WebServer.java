import java.io.* ;
import java.net.* ;
import httpmethods.*;

public final class WebServer {


	public static void main(String argv[]) throws Exception
	{
		boolean isParsed = Utils.parseConfigFile();
		if(isParsed){
			startSession();
		} else {
			System.out.println("There was Problem parsing 'config.ini'.");
		}

		System.out.println("bye bye...");
	}

	private static void startSession() {
		// Establish the listen socket.
		ServerSocket socket = null;
		ThreadPool threadPool = null;
		try {
			socket = new ServerSocket(Utils.PORT);
			System.out.println("Listening port is " + Utils.PORT);
			threadPool = ThreadPool.getInstance();
			threadPool.register();
			// Process HTTP service requests in an infinite loop.
			while (true)
			{
				// Listen for a TCP connection request.
				Socket connection = socket.accept();

				// Adding a new job for 'threadPool'
				System.out.println("New request has arrived");
				threadPool.addJob(connection);			
			}
		} catch (IOException e) {
			if (threadPool != null){
				threadPool.unregister();
			}
		}
	}		
}
