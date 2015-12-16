
/**
 * A synchronized bounded-size queue for producer-consumer applications,
 * we implemented this class last year in Operating-Systems Course. So we 
 * are re-using it here with some mild modifications.
 *
 * @param <T> Type of data items
 */
public class SynchronizedQueue<T> {
	
	private T[] buffer;
    private int producers;
    private int size;
    private int capacity;


    /**
     * Constructor. Allocates a buffer (an array) with the given capacity and
     * resets pointers and counters.
     * @param capacity Buffer capacity
     */
    public SynchronizedQueue(int capacity) {
        this.buffer = (T[])(new Object[capacity]);
        this.producers = 0; // The only producer in Lab1 is Main-Thread
        this.size = 0;
        this.capacity = capacity;
    }
    

    /**
     * Dequeues the first item from the queue and returns it.
     * If the queue is empty but producers are still registered to this queue,
     * this method blocks until some item is available.
     * If the queue is empty and no more items are planned to be added to this
     * queue (because no producers are registered), this method returns null.
     *
     * @return The first item, or null if there are no more items
     * @see #registerProducer()
     * @see #unregisterProducer()
     */
    public T dequeue() {
        synchronized (buffer) {
            while (this.size == 0) {
                if (this.producers == 0) {
                    return null;
                }
                try {
                    buffer.wait();
                } catch (InterruptedException e) {
                }
            }
            T item = buffer[size - 1];
            size--;
            buffer.notifyAll();
            return item;
        }
    }

    /**
     * Enqueues an item to the end of this queue. If the queue is full, this
     * method blocks until some space becomes available.
     *
     * @param item Item to enqueue
     */
    public void enqueue(T item) {
        synchronized (buffer) {
            while (this.getSize() == this.getCapacity()) {
                try {
                    buffer.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            buffer[this.getSize()] = item;
            size++;
            buffer.notifyAll();
        }

    }
    
    /**
     * This method blocks producer from acquiring new items until space 
     * becomes available.
     * 
     */
    public void waitForFreeThread() {
        synchronized (buffer) {
            while (this.getSize() == this.getCapacity()) {
                try {
                	System.out.println("All Threads are busy.\nwaiting...");
                    buffer.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Returns the capacity of this queue
     * @return queue capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the current size of the queue (number of elements in it)
     * @return queue size
     */
    public int getSize() {
        // this need to be synchronized also
        synchronized (buffer) {
            return size;
        }
    }

    /**
     * Registers a producer to this queue. This method actually increases the
     * internal producers counter of this queue by 1. This counter is used to
     * determine whether the queue is still active and to avoid blocking of
     * consumer threads that try to dequeue elements from an empty queue, when
     * no producer is expected to add any more items.
     * Every producer of this queue must call this method before starting to
     * enqueue items, and must also call <see>{@link #unregisterProducer()}</see> when
     * finishes to enqueue all items.
     *
     * @see #dequeue()
     * @see #unregisterProducer()
     */
    public void registerProducer() {
        synchronized (buffer) {
            this.producers++;
            buffer.notifyAll();
        }

    }

    /**
     * Unregisters a producer from this queue. See <see>{@link #registerProducer()}</see>.
     *
     * @see #dequeue()
     * @see #registerProducer()
     */
    public void unregisterProducer() {
        synchronized (buffer) {
            this.producers--;
            buffer.notifyAll();
        }
    }
}

