package edu.upenn.cis455.hw1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class HttpRequest {
	private Map<String, String> initMap;
	private Map<String, String> headerMap;
	private Set<String> httpVerb;
	private boolean isWaiting;
	private String serverAddr;
	private String root;
	private int code; // 200, 304, 400, 403, 404, 500, 501, 505
	
	HttpRequest(int port, String rootDir) {
		initMap = new HashMap<String, String>();
		headerMap = new HashMap<String, String>();
		httpVerb = new HashSet<String>();
		isWaiting = true;
		code = 200;
		serverAddr = "http://localhost:" + port;
		root = rootDir;
		httpVerb.add("GET");
		httpVerb.add("HEAD");
	}
	
	/**
	 * Clear all the variables to initial status
	 * Used when finishing handling one request
	 */
	public void clear() {
		initMap.clear();
		headerMap.clear();
		isWaiting = true;
		code = 200;
	}
	
	/**
	 * Set the waiting status to false
	 */
	public void setWork() {
		isWaiting = false;
	}
	
	/**
	 * Set the map storing information of the initial line
	 * @param words gives the parameters of the first line of request
	 */
	public void setInitMap(String[] words) {
		initMap.put("Type", words[0]);
		initMap.put("Path", words[1]);
		initMap.put("Protocol", words[2]);
	}
	
	/**
	 * Set the map with key/value pairs from the remaining of the request
	 * @param key
	 * @param value
	 */
	public void setHeaderMap(String key, String value) {
		headerMap.put(key, value);
	}

	/**
	 * Get the status of whether the thread is working on a request
	 * @return
	 */
	public boolean getWorkStatus() {
		return isWaiting;
	}

	/**
	 * Get the initial map
	 * @return the initial line map
	 */
	public Map<String, String> getInitMap(){
		return initMap;
	}

	/**
	 * Get the status code
	 * @return the status code
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Check if the given directory or file could be accessed
	 * @return true if the resource is accessible
	 */
	public boolean canAccess() {
		if(initMap.get("Path").contains("http://")) {
			// absolute path after host(e.g. http://localhost:8080)
			String absolutePath = initMap.get("Path").substring(serverAddr.length());
			initMap.put("Path", absolutePath);
		}
		if(!insideRoot(initMap.get("Path"))) code = 403;
		return code == 200;
	}
	
	/**
	 * Check if the path is outside the root directory
	 * @param path gives the address of the resource
	 * @return true if it is inside the root directory
	 */
	public boolean insideRoot(String path) {
		String[] strs = path.split("/");
		int count = 0;
		for(String s: strs) {
			if(s.length() == 0 || s.equals(".")) {
				continue;
			} else if(s.equals("..")) {
				if(count == 0) return false;
				count--;
			} else {
				count++;
			}
		}
		return true;
	}
	
	/*************************************************/
	// TODO
	public void getContent() {
		String path = simplifyPath();
	}
	
	public String simplifyPath() {
		StringBuilder sb = new StringBuilder();
		
		return sb.toString();
	}
	
	/*************************************************/
	
	/**
	 * Check if the request header is valid by examing the first line of request
	 * @return
	 */
	public boolean validHeader() {
		if(initMap.size() != 3 || badRequest()) {
			code = 400;
		} else if(!httpVerb.contains(initMap.get("Type"))) {
			code = 501;
		} else if(!validProtocol()) {
			code = 505;
		}
		return code == 200;
	}
	
	/**
	 * Check if the protocol is supported
	 * @return true is protocol supported
	 */
	public boolean validProtocol(){
		return initMap.get("Protocol").equals("HTTP/1.0") || initMap.get("Protocol").equals("HTTP/1.1");
	}
	
	/**
	 * Check if the request is well-formed
	 * @return true if the request is well-formed
	 */
	public boolean badRequest() {
		if(initMap.get("Protocol").equals("HTTP/1.1") && !headerMap.containsKey("host")) return true;
		if(initMap.get("Path").contains("http://") && !initMap.get("Protocol").equals("HTTP/1.1")) return true;
		return false;
	}

}
