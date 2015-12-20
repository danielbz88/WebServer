import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class HTTPRequest {

	private String method;
	private String resourcePath;
	private String HTTPVersion = Utils.HTTP_VERSION_1_0; //Default
	protected HashMap<String,String> headers;
	private HashMap<String,String> params;
	private boolean isBadRequest = false;
	private boolean isChunked = false;
	private boolean isSupportedMethod;
	protected boolean internalError = false;

	// Parse from a string like 'GET /index.html HTTP/1.1'
	public HTTPRequest(String rawRequest) {
		this.headers = new HashMap<>();
		this.params = new HashMap<>();
		String[] firstLineArr = rawRequest.split(" ");
		if(firstLineArr.length == 3){
			
			this.method = firstLineArr[0].trim();
			this.isSupportedMethod = Utils.IsMethodSupported(this.method);
			this.resourcePath = firstLineArr[1].trim();
			this.HTTPVersion = firstLineArr[2].trim();

			if(this.resourcePath.contains("../")){
				this.isBadRequest = true;
			}else{
				// the params are separated by '?' from the requested page
				int delim = this.resourcePath.indexOf("?");
				if(delim > 0){				
					this.resourcePath = this.resourcePath.substring(0, delim);
					if(delim + 1 < resourcePath.length()){
						try {
							parseParmas(this.resourcePath.substring(delim + 1));
						} catch (WebServerRuntimeException e) {
							this.internalError = true;
						}
					}
				}
			}
		} else {
			this.isBadRequest = true;
		}
	}

	// Parsing 'headers' from a string like 
	// 'HOST: loaclhost:80[CRLF]Content-Length: 50[CRLF]'
	private void parseHeaders(String rawRequest) throws WebServerRuntimeException{
		String[] contents = rawRequest.split(Utils.CRLF);
		for (int i = 0; i < contents.length; i++) {
			int delim = contents[i].indexOf(":");

			// Ignore bad formatted headers
			if(delim > 0 && delim + 1 < contents[i].length()){
				String key = contents[i].substring(0, delim);
				String value = contents[i].substring(delim + 1).trim();
				this.headers.put(key, value);
			}
		}
	}

	// Parsing 'params' from a string like 'x=1&y=2'
	private void parseParmas(String rawParams) throws WebServerRuntimeException{
		String[] paramsArray = rawParams.split("&");
		for (int i = 0; i < paramsArray.length; i++) {
			int delim = paramsArray[i].indexOf("=");

			// Ignore bad formatted params
			if(delim > 0 && delim + 1 < paramsArray[i].length()){
				String key = paramsArray[i].substring(0, delim);
				String value = paramsArray[i].substring(delim + 1).trim();
				this.params.put(key, value);
			}
		}

	}

	public String getMethod() { 
		return this.method;
	}
	
	public boolean isBadRequest() {
		return this.isBadRequest;
	}
	
	public boolean isChunked() {
		return this.isChunked;
	}
	
	public boolean isSupportedMethod() {
		return this.isSupportedMethod;
	}
	
	public String getHTTPVersion() { 
		return this.HTTPVersion;
	}

	public String getResourcePath() {
		return this.resourcePath;
	}

	public String getHeader(String header) { 		
		return headers.get(header);
	}
	
	public HashMap<String,String> getParams() { 
		return params;
	}

	// Validate the request is up to standards 
	public void validate() throws IOException {
		if(this.HTTPVersion.equals(Utils.HTTP_VERSION_1_1) && !this.isBadRequest){
			this.isBadRequest = !this.headers.containsKey("Host");
		}
		if(this.headers.get("chunked") != null){
			String value = (String) this.headers.get("chunked");
			this.isChunked = value.equals("yes");
		}
		if(this.resourcePath != null){
			if(this.resourcePath.endsWith("/params_info.html")){
				Utils.makeParmasInfo(this.params);
			}
		}

	}
	
	public void printRequestDebug(){
		System.out.println("==================================================");
		System.out.println("Method: " + this.method); 
		System.out.println("Page: " + this.resourcePath);
		if (headers != null){
			System.out.println("Headers:");
			for (Entry<String, String> entry : headers.entrySet())
			{
				System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
			}
		}	
		if (params != null){
			System.out.println("Params:");
			for (Entry<String, String> entry : params.entrySet())
			{
				System.out.println("\t" + entry.getKey() + "=" + entry.getValue());
			}
		}	
		System.out.println("==================================================");
	}

	protected void addParams(String params) {
		try {
			parseParmas(params);
		} catch (WebServerRuntimeException e) {
			this.internalError = true;
		}
	}

	protected void addHeaders(String allHeaders) {
		try {
			parseHeaders(allHeaders);
		} catch (WebServerRuntimeException e) {
			this.internalError = true;
		}
	}
}
