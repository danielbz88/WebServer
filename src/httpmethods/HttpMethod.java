package httpmethods;


public abstract class HttpMethod {
	String name;
	String responseHeader;
	
	
	protected String getName(){
		return this.name;
	}
	
	public abstract String getResponseHeader();	
}
