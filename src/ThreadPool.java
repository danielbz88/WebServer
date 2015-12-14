import java.net.Socket;

public class ThreadPool {
	
	private static ThreadPool INSTANCE = null;
	private static Object LOCK = new Object();
	
	private SynchronizedQueue<Socket> requestQueue; 
	private int numOfActiveThreads = 0;
	private int maxThreads = 0;
	
	private ThreadPool(int maxThreads){
		this.requestQueue = new SynchronizedQueue<>(1);
		this.maxThreads = maxThreads;
	}
	
	// lazy initialization, thread safe Singleton 
	public static ThreadPool getInstance() {
		
		if (INSTANCE == null) {
			// Thread Safe. Might be costly operation in some case therefore
			// the double lock
			synchronized (LOCK) {
				if (INSTANCE == null) {
					INSTANCE = new ThreadPool(Utils.MAX_THREADS);
				}
			}
		}
		
		return INSTANCE;
	}

	public void addJob(Socket connection) {    
		// If we didn't reach maxThreads we create another one
		if(numOfActiveThreads < maxThreads){
			numOfActiveThreads++;
			System.out.println("Creating thread " + this.numOfActiveThreads);
			RequestHandler requestHandler = new RequestHandler(this.requestQueue, this.numOfActiveThreads);
			Thread thread = new Thread(requestHandler);
			thread.start();
		}
		
		// add the new request to the queue
		this.requestQueue.enqueue(connection);
		
		// If we created maxThreads threads and they're busy we wait
		if(numOfActiveThreads == maxThreads){
			this.requestQueue.waitForFreeThread();
		}
	}

	public void unregister() { 
		// unregister
		this.requestQueue.unregisterProducer();
	}
	
	public void register() { 
		// register as a producer to the queue
		this.requestQueue.registerProducer();	
	}

}
