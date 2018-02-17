package edu.upenn.cis455.hw1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HttpRequest {
	private Map<String, String> initMap;
	private Map<String, String> headerMap;
	private Set<String> httpVerb;
	private boolean isWaiting;
	private int code; // 200, 304, 400, 403, 404, 500, 501, 505
	
	HttpRequest() {
		initMap = new HashMap<String, String>();
		headerMap = new HashMap<String, String>();
		httpVerb = new HashSet<String>();
		isWaiting = true;
		code = 200;
		httpVerb.add("GET");
		httpVerb.add("HEAD");
	}
	
	public void clear() {
		initMap.clear();
		headerMap.clear();
		isWaiting = true;
		code = 200;
	}
	
	public void setWork() {
		isWaiting = false;
	}
	
	public void setInitMap(String[] words) {
		initMap.put("Type", words[0]);
		initMap.put("Path", words[1]);
		initMap.put("Protocol", words[2]);
	}
	
	public void setHeaderMap(String key, String value) {
		headerMap.put(key, value);
	}

	public boolean getWorkStatus() {
		return isWaiting;
	}

	public Map<String, String> getInitMap(){
		return initMap;
	}

	public int getCode() {
		return code;
	}
	
	public void checkHeader() {
		if(initMap.size() != 3 || (initMap.get("Protocol").equals("HTTP/1.1") && !headerMap.containsKey("host"))) {
			code = 400;
		} else if(!httpVerb.contains(initMap.get("Type"))) {
			code = 501;
		} else if(!isValidProtocol()) {
			code = 505;
		}
		// TODO
	}
	
	public boolean isValidProtocol(){
		return initMap.get("Protocol").equals("HTTP/1.0") || initMap.get("Protocol").equals("HTTP/1.1");
	}


	
}
