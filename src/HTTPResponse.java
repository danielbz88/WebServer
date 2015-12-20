import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

public class HTTPResponse {
	private HTTPRequest request;
	private String responseCode;
	private String allHeaders;
	private String HTTPVersion = Utils.HTTP_VERSION_1_1;
	private HashMap<String,String> headers;
	private byte[] body;
	private boolean isFound = false;

	public HTTPResponse(HTTPRequest request, String allHeaders){
		if (request.internalError){
			this.responseCode = Utils.ERROR;
		}
		
		this.request = request;
		this.allHeaders = allHeaders;
		this.headers = new HashMap<>();
		//this.headers.putAll(request.headers);
	}

	public HTTPResponse(Exception exception){
		this.responseCode = Utils.ERROR;
	}
	
	public HTTPResponse(String responseCode){
		this.responseCode = responseCode;
	}

	protected void makeResponse(){
		if(this.request.isBadRequest()){
			this.responseCode = Utils.BAD_REQUEST;	

		} else if (!this.request.isSupportedMethod()){
			this.responseCode = Utils.NOT_IMPLEMENTED;

		} else { 
			this.responseCode = Utils.OK;

			// we have the same behavior in GET and POST
			switch (request.getMethod()) {
			case Utils.GET:
			case Utils.POST:
				getPostResponse();
				break;
			case Utils.HEAD:
				headResponse();
				break;
			case Utils.TRACE:
				traceResponse();
				break;
			case Utils.OPTIONS:
				optionsResponse();
				break;
			default: 
				//TODO: Do I really need this?
				break;
			}			
		}
	}

	private void optionsResponse() {
		this.headers.put("Allow", "GET,POST,HEAD,TRACE,OPTIONS");
		this.headers.put(Utils.CONTENT_LENGTH, "0");	
	}

	private void traceResponse() {
		this.responseCode = Utils.OK;
		this.headers.put(Utils.CONTENT_TYPE, Utils.TEXT_HTML);
		this.body = this.allHeaders.getBytes();
		if(this.request.isChunked()){
			// transfer-encoding header
			this.headers.put(Utils.HEADER_TRANSFER_ENCODING, "chunked");
		}else{				
			//content-length header
			if(!this.headers.containsKey(Utils.CONTENT_LENGTH)){
				this.headers.put(Utils.CONTENT_LENGTH, Integer.toString(this.body.length));	
			}
		}

	}

	private void headResponse() {
		File resource = Utils.getResuorce(this.request.getResourcePath());
		basicResponse(resource);
	}

	private void getPostResponse(){
		File resource = Utils.getResuorce(this.request.getResourcePath());
		basicResponse(resource);
		if(this.isFound){
			try {
				this.body = Utils.readFile(resource);
			} catch (Exception e) {
				this.responseCode = Utils.ERROR;
			}
		}
	}

	private void basicResponse(File resource) {

		if (resource == null){
			this.responseCode = Utils.NOT_FOUND;
			isFound = false;
		} else {
			this.responseCode = Utils.OK;

			//content-type header
			this.headers.put(Utils.CONTENT_TYPE, getContentType(resource.getAbsolutePath()));

			if(this.request.isChunked()){
				// transfer-encoding header
				this.headers.put(Utils.HEADER_TRANSFER_ENCODING, "chunked");
			}else{				
				//content-length header
				if(!this.headers.containsKey(Utils.CONTENT_LENGTH)){
					this.headers.put(Utils.CONTENT_LENGTH, getContentLength(resource));	
				}
			}
			isFound = true;
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

	protected String getResponseHeader() {
		return this.HTTPVersion + " " + this.responseCode;
	}

	protected boolean isWithoutError(){
		return (this.responseCode == Utils.OK);
	}

	public String getResponseHeaders() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, String> entry : headers.entrySet())
		{
			builder.append(entry.getKey() + ": " + entry.getValue() + Utils.CRLF);
		}

		return builder.toString();
	}

	public byte[] getResponseBody(){
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

	protected int getContentLength() throws NumberFormatException{
		int parsedFromRequest = Integer.parseInt(this.headers.get(Utils.CONTENT_LENGTH));
		int contentLength = Math.min(this.body.length, parsedFromRequest);
		return contentLength;
	}

	protected String response() {
		return this.responseCode;
	}
}
