Project structure:

ThreadPool class is implemented following a singleton design pattern. 
The threads are created only when needed (lazy creation).
A SynchronizedQueue is used to as a data structure for the thread instances.

SynchronizedQueue is implemented using an array of generic objects, and also uses 
java's syncrhozed feature to allow consumer-producer interactions.

WebServer class holds the main method of the application and initiates the session by creating 
a ServerSocket and threadPool, and begins listening for connection requests.

HTTPRequest class holds all variables and properties of an actual HTTP request. It also
includes some important inner logic.

HTTPResponse class, is similar to the HTTPRequest class, in that it also follows reality
by holding HTTP response variables and properties. It also includes an instance of the relevant 
HTTP request, and the inner logic of building the correct response.

RequestHandler class is responsible to coordinate between the WebServerClass, the HTTP requests, 
and the appropriate HTTP response that goes with it.
RequestHandler implements the Runnable class, and is called upon by the WebServer class 
(through the ThreadPool instance) to begin the process of handling a request correctly and
initiating its response.

Utils class holds default values, and general methods to be used by most of the other classes to process requests and generate responses.

The project also includes an exception (WebServerRuntimeException), which allows the 
application to track an occurence of a server error, and return the appropriate response
to the client.

Project:
A multithreading server, using a thread pool, which implements basic HTTP server functionality. This include HTTP methods: GET, POST, TRACE, HEAD, and OPTIONS.
The server allow the client to request HTML, txt, image, and icon files from the root folder.
The server also enables chunked transfer encoding.
The following HTTP response codes are included:
OK, Not Found, Not Implemented, Bad Request, Internal Server Error.

Implementation:
The server begins by listening for a connection request on a default port.
When an HTTP request arrives, the server analyses and processes it, generates the appropriate response, and sends that response back to the client.
That response may be any of the implemented HTTP response codes, including for example: Internal Server Error.


