package edu.upenn.cis455.hw1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.log4j.Logger;

public class HttpRequest {
	private Map<String, String> initMap;
	private Map<String, String> headerMap;
	private File file;
	private String fileType;
	private byte[] binary;
	private Set<String> httpVerb;
	private String serverAddr;
	private String root;
	private int code; // 200, 304, 400, 403, 404, 500, 501, 505
	private static final Logger log = Logger.getLogger(Worker.class.getName());
	
	HttpRequest(int port, String rootDir) {
		initMap = new HashMap<String, String>();
		headerMap = new HashMap<String, String>();
		fileType = "text/html";
		httpVerb = new HashSet<String>();
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
		file = null;
		fileType = "text/html";
		code = 200;
		binary = null;
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
	 * Get the initial map
	 * @return the initial line map
	 */
	public Map<String, String> getInitMap(){
		return initMap;
	}
	
	/**
	 * Get the root directory of the program
	 * @return the root directory
	 */
	public String getRoot() {
		return root;
	}

	/**
	 * Get the status code
	 * @return the status code
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Get the type of the file for the response
	 * @return the file type
	 */
	public String getFileType() {
		return fileType;
	}
	
	/**
	 * Get the file object of the resource
	 * @return the file object
	 */
	public File getFile() {
		return file;
	}
	
	/**
	 * Get the binary data files
	 * @return the content of the binary data file
	 */
	public byte[] getBinary() {
		return binary;
	}
	
	/**
	 * Check if the given directory or file could be accessed
	 * @return true if the resource is accessible
	 */
	public boolean canAccess() {
		boolean absolute = false;
		if(initMap.get("Path").contains("http://")) {
			// absolute path after host(e.g. http://localhost:8080)
			absolute = true;
			initMap.put("Path", initMap.get("Path").substring(serverAddr.length()));
		} 
		//check if path is accessible
		if(!simplifyPath(absolute)) code = 403;
		return code == 200;
	}
	
	/**
	 * Check if the file exists, with valid type, and has satisfying date infomation
	 */
	public void checkFile() {
		String path = initMap.get("Path");
		if(path.equals("/control") || path.equals("/shutdown")) return;
		file = new File(root + path);
		if(!file.exists()) {
			code = 404;
		} else if(file.isFile()) {
			if(!validFileType() || !modifyCheck()) return;
			readContent();
		} 
	}
	
	/**
	 * Check if the file type is valid
	 * @return true if the file type could be handled
	 */
	public boolean validFileType() {
		String fileName = initMap.get("Path");
		int index = fileName.indexOf(".");
		if(index <= 0) {
			code = 500;
		} else {
			String ext = fileName.substring(index + 1);
			if(ext.equals("txt")) fileType = "text/plain";
			else if(ext.equals("gif")) fileType = "image/gif";
			else if(ext.equals("png")) fileType = "image/png";
			else if(ext.equals("jpg")) fileType = "image/jpeg";
			else if(ext.equals("html")) fileType = "text/html";
			else if(ext.equals("pdf")) fileType = "appllication/pdf";
			else code = 500;
		}
		return code == 200;
	}
	
	/**
	 * Read the content of the file and cache it in the byte array
	 */
	public void readContent() {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			binary = new byte[(int)file.length()];
			stream.read(binary);
		} catch (FileNotFoundException e) {
			code = 404;
		} catch (IOException e) {
			code = 500;
		} finally {
			try {
				if(stream != null) stream.close();
			} catch (IOException e) {
				return;
			}
		}
	}
	
	/**
	 * Check if the date condition is satisfied
	 */
	public boolean modifyCheck() {
		Date lastModified = new Date(file.lastModified());
		if(headerMap.containsKey("if-modified-since")) {
			Date requireDate = parseDate(headerMap.get("if-modified-since"));
			if(requireDate != null && requireDate.after(lastModified)) {
				code = 304;
				return false;
			}
		} 
		if(headerMap.containsKey("if-unmodified-since")) {
			Date requireDate = parseDate(headerMap.get("if-unmodified-since"));
			if(requireDate != null && requireDate.before(lastModified)) code = 412;
		}
		return code == 200;
	}
	
	/**
	 * Parse the date information to well-formed date format
	 * @param s gives the raw date information
	 * @return the well-formed date information
	 */
	public Date parseDate(String s) {
		SimpleDateFormat format1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		format1.setTimeZone(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat format2 = new SimpleDateFormat("E, dd-MMM-yy HH:mm:ss z");
		format2.setTimeZone(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat format3 = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
		format3.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date = null;
		try {
			date = format1.parse(s);
		} catch (ParseException e) {
			date = null;
		}
		if (date == null) {
			try {
				date = format2.parse(s);
			} catch (ParseException e) {
				date = null;
			}
		}
		if (date == null) {
			try {
				date = format3.parse(s);
			} catch (ParseException e) {
				date = null;
			}
		}
		return date;
	}
	
	/**
	 * Check if the path is outside the root directory, and simplify the path
	 * @param path gives the address of the resource
	 * @return true if it is inside the root directory
	 */
	public boolean simplifyPath(boolean absolute) {
		StringBuilder sb = new StringBuilder();
		List<String> list = new ArrayList<String>();
		String[] strs = initMap.get("Path").split("/");
		
		//check if the path is within the first level directory
		for(String s: strs) {
			if(s.length() == 0 || s.equals(".")) continue;
			else if(s.equals("..")) {
				if(list.size() == 0) return false;
				list.remove(list.size() - 1);
			}
			else list.add(s);
		}
		
		//compose the simplified path
		for(String s: list) sb.append("/" + s);
		if(sb.length() == 0) sb.append("/");
		
		// update the simplified file path, and check root directory if absolute path
		String update = sb.toString();
		if(absolute) {
			if(!update.startsWith(root)) return false;
			else update = update.substring(root.length());
		}
		initMap.put("Path", update);
		return true;
	}
	
	/**
	 * Check if the request header is valid by examining the first line of request
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
		if(initMap.get("Protocol").equals("HTTP/1.1") && !headerMap.containsKey("Host")) return true;
		if(initMap.get("Path").contains("http://") && !initMap.get("Protocol").equals("HTTP/1.1")) return true;
		return false;
	}

}
