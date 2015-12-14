import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class HTTPResponse {
	
	private HTTPRequest request;
	private String responseCode;
	private String HTTPVersion = Utils.HTTP_VERSION_1_1;
	private HashMap<String,String> headers;
	private byte[] body;
	
	public HTTPResponse(HTTPRequest request){
		this.request = request;
		this.headers = new HashMap<>();
//		headers.put(Utils.CONTENT_TYPE, Utils.TEXT_HTML); // Default 
		// ^%^ This line would cause the server to return the header even if the resource was not found.
	}
	
	protected void makeResponse(){
		if(this.request.isBadRequest()){
			this.responseCode = Utils.BAD_REQUEST;	
			
		} else if (!this.request.isSupportedMethod()){
			this.responseCode = Utils.NOT_IMPLEMENTED;
			
		} else { 
			// we have the same behavior in GET and POST
			switch (request.getMethod()) {
			case Utils.GET:
				this.responseCode = Utils.OK;
				getResponse();
				break;
			case Utils.POST:
				postResponse();
				break;
			case Utils.HEAD:
				this.responseCode = Utils.OK;
				headResponse();
				break;
			case Utils.TRACE:
				this.responseCode = Utils.OK;
				traceResponse();
				break;
			case Utils.OPTIONS:
				this.responseCode = Utils.OK;
				optionsResponse();
				break;
			default: 
				break;
			}			
		}
	}

	private void postResponse() {
		// TODO Auto-generated method stub
		
	}

	private void optionsResponse() {
		// TODO Auto-generated method stub
		
	}

	private void traceResponse() {
		// Add the headers from the REQUEST
		File resource = Utils.getResuorce(this.request.getResourcePath());
		basicResponse(resource);
		this.body = request.getAllHeaders().getBytes();
	}

	private void headResponse() {
		// TODO Auto-generated method stub
		
	}

	private void getResponse(){
		File resource = Utils.getResuorce(this.request.getResourcePath());
		basicResponse(resource);
		try {
			this.body = Utils.readFile(resource);
		} catch (IOException e) {
			this.responseCode = Utils.ERROR;
		}
	}
	
	private void basicResponse(File resource) {
		if (resource == null){
			this.responseCode = Utils.NOT_FOUND;
		} else {
			this.responseCode = Utils.OK;
					
			//content-type header
			this.headers.put(Utils.CONTENT_TYPE, getContentType(this.request.getResourcePath()));
					
			if(this.request.isChunked()){
				// transfer-encoding header
				this.headers.put(Utils.HEADER_TRANSFER_ENCODING, "chunked");
			}else{				
				//content-length header
				this.headers.put(Utils.CONTENT_LENGTH, getContentLength(resource));
			}
		}
	}

	private String getContentLength(File resource) {
		int len = (int)resource.length();
		return Integer.toString(len);
	}
	
	private String getContentType(String resourcePath) {
		String type;		
		int delim = resourcePath.indexOf('.');
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

	public String getResponseCode() {
		return this.HTTPVersion + " " +this.responseCode;
	}

	public String getResponseHeaders() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, String> entry : headers.entrySet())
		{
			builder.append(entry.getKey() + ": " + entry.getValue() + Utils.CRLF);
		}
		
		return builder.toString();
	}

	public byte[] getResponseBody() {
		return this.body;
	}
	
	
	public void printResponseDebug(){
		System.out.println("==================================================");
		System.out.println("Code: " + this.responseCode); 
		if (!headers.isEmpty() && headers != null){
			System.out.println("Headers:");
			for (Entry<String, String> entry : headers.entrySet())
			{
				System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
			}	
		}	
		System.out.println("==================================================");
	}

}
