import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class HTTPRequest {

	private String method;
	private String page;
	private String HTTPVersion = Utils.HTTP_VERSION_1_0; //Default
	private HashMap<String,String> headers;
	private HashMap<String,String> params;
	private boolean isBadRequest = false;

	// Parse from a string like 'GET /index.html HTTP/1.1'
	public HTTPRequest(String rawRequest) {
		this.headers = new HashMap<>();
		this.params = new HashMap<>();
		String[] firstLineArr = rawRequest.split(" ");
		if(firstLineArr.length == 3){
			this.method = firstLineArr[0].trim();
			this.page = firstLineArr[1].trim();
			// bonus
			this.HTTPVersion = firstLineArr[2].trim();

			// If the request is GET, the params are separated by '?' from the
			// requested page
			int delim = this.page.indexOf("?");
			if(this.method.equals(Utils.GET) && delim > 0){				
				this.page = this.page.substring(0, delim);
				if(delim + 1 < page.length()){
					parseParmas(this.page.substring(delim + 1));
				}

			}
		} else {
			this.isBadRequest = true;
		}
	}

	// Parsing 'headers' from a string like 
	// 'HOST: loaclhost:80[CRLF]Content-Length: 50[CRLF]'
	protected void parseHeaders(String rawRequest) {
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
	protected void parseParmas(String rawParams) {
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
	
	public String getHTTPVersion() { 
		return this.HTTPVersion;
	}

	public String getPage() {
		return this.page;
	}

	public String getHeader(String header) { 		
		return headers.get(header);
	}
	
	public HashMap<String,String> getParams() { 
		return params;
	}

	public void printRequestDebug(){
		System.out.println("==================================================");
		System.out.println("Method: " + this.method); 
		System.out.println("Page: " + this.page);
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
}