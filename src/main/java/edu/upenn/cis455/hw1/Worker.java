package edu.upenn.cis455.hw1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.apache.log4j.Logger;

public class Worker implements Runnable{
	
	private BlockingQueue<Socket> blockingQueue;
	private int number;
	private HttpRequest currReq;
	private static final Logger log = Logger.getLogger(Worker.class.getName());
	
	Worker(BlockingQueue<Socket> queue, int num, String rootDirectory, int port) {
		blockingQueue = queue;
		number = num;
		currReq = new HttpRequest(port, rootDirectory);
		log.info("Worker thread " + number + " starts running");
	}

	/**
	 * Thread body
	 */
	public void run() {
		Socket client = null;
		while(!Thread.interrupted()) {
			try {
				// retrieve socket from the queue and set timeout for read
				client = blockingQueue.poll();
				client.setSoTimeout(15000);
					
				// set up the input stream for reading and process request
	            List<String> req = readRequest(client);
	            if(req.size() != 0) {
	                log.info("Worker " + number + " Receive request: " + req.get(0));
	                parseRequest(req);
	                		
	                //set up output stream for writing and process response
	                sendResponse(client);
	                
	                // shut down server if necessary
	                if(currReq.getInitMap().get("Path").equals("/shutdown")) HttpServer.shutDownServer();
	            } 
			} catch(InterruptedException e) {
				log.error("Interrupted when blocking - Worker " + number);
			} catch(SocketException e) {
				log.error("Error for socket connection - Worker " + number);
			}
			
			//close the socket and clean variables
			currReq.clear();
			try {
				if(client != null && !client.isClosed()) client.close();
			} catch(IOException e) {
				log.error("Error when closing socket - Worker " + number);
			}
			client = null;
		}
		log.info("WORKER " + number + " TERMINATES");
	}
	
	/**
	 * Read the request from the socket
	 * @param client gives the socket connection to the client
	 * @return parsed request as list of strings
	 * @throws InterruptedException thread interrupted when blocking
	 */
	public List<String> readRequest(Socket client) throws InterruptedException{
		List<String> list = new ArrayList<String>();
		currReq.setWork();
		try {
			InputStreamReader reader= new InputStreamReader(client.getInputStream());
	        BufferedReader br = new BufferedReader(reader);
			String line = br.readLine();
			while(line != null && line.length() != 0) {
				if(Thread.interrupted()) throw new InterruptedException();
				if(list.size() == 0) parseFirstLine(line);
				list.add(line);
				line = br.readLine();
			}
            br.close();
            reader.close();
		} catch(SocketTimeoutException e) {
			log.error("Reading timeout from socket - Worker " + number);
			list.clear();
		} catch (IOException e) {
			log.error("Error when getting input stream or reading from stream - Worker " + number);
			list.clear();
		}
		return list;
	}
	
	/**
	 * Parse the HTTP request, check validation and accessibility of the path, and get the requested content
	 * @param req gives the request body
	 */
	public void parseRequest(List<String> req) {
		parseHeader(req);
		if(!currReq.validHeader() || !currReq.canAccess()) return;
		currReq.checkFile();
	}
	
	/**
	 * Parse the first line of the request
	 * @param header gives the first line
	 */
	public void parseFirstLine(String header) {
		String[] words = header.split("\\s+");
		if(words.length != 3) return;
		currReq.setInitMap(words);
	}
	
	/**
	 * Parse the remaining lines of the request
	 * @param list gives the remaining request
	 */
	public void parseHeader(List<String> list) {
		for(int i = 1; i < list.size(); i++) {
			String[] pairs = list.get(i).split(":", 2);
			if(pairs.length == 2) currReq.setHeaderMap(pairs[0], pairs[1]);
			else currReq.setHeaderMap(pairs[0], pairs[0]);
		}
	}
	
	/**
	 * Send response to the client based on the request
	 * @param client gives the socket connection to the client
	 */
	public void sendResponse(Socket client) {
		PrintStream output;
		try {
			output = new PrintStream(client.getOutputStream(), true);
			
			// get status code; generate status line and message body according to status code
			int code = currReq.getCode();
			String status = genStatusLine(code);
			String body = "";
			File file = currReq.getFile();
			if(code == 200) {
				output.print(genStatusLine(100) + "\r\n");
				output.print(status);
				
				//generate the message body according to the request path
				String path = currReq.getInitMap().get("Path");
				if(path.equals("/control")) {
					body = genControl();
				} else if(path.equals("/shutdown")) {
					body = genShutdown();
				} else if(file.isDirectory()){
					body = genFileList();
				} else {
					output.print(genHeader(file.length()));
					genFileContent(file, output);
					output.flush();
					output.close();
					//body = "\n<html><body>Hello world!</body></html>\n";
					return;
				}
				
			} else {
				output.print(status);
				body = genErrorPage(status);
			}
			
			//generate remaining header and send message body with regard to request method
			output.print(genHeader(body.length()));
			if(!currReq.getInitMap().get("Type").equals("HEAD")) output.print(body);
			output.flush();
			output.close();
		} catch (IOException e) {
			log.error("Error when getting output stream or writing to stream - Worker " + number);
		}
	}

	/**********************************/
	// TODO
	public String genFileList() {
		StringBuilder sb = new StringBuilder();
    		sb.append("<html>\n");
    		sb.append("<title>\n");
    		sb.append("Welcome to the Server\n");
    		sb.append("</title>\n");
    		sb.append("<body>\n");
    		sb.append("<font size=\"5\">\n");
    		sb.append("<b>");
    		sb.append("Developer : Lijun Mao<br>");
    		sb.append("SEAS login: maolijun<br>");
    		sb.append("</b>");
    		sb.append("</font>\n");
    		sb.append("<p>");
    		sb.append("<table style=\"font-size:20px;\">");
    		sb.append("<tr><td>");
    		sb.append("<b>");
    		sb.append("Files");
    		sb.append("</b>");
    		sb.append("</td><td>");
    		sb.append("<b>");
    		sb.append("Last Modified");
    		sb.append("</b>");
    		sb.append("</td></tr>");
    		SimpleDateFormat date_format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
    		File file = currReq.getFile();
    		for (File f : file.listFiles()) {								
    			sb.append("<tr><td>");
    			String path = currReq.getInitMap().get("Path");
    			if(path.equals("/")) path = "";
    			String link = path + "/" + f.getName();
    			sb.append("<a href=\"" + link + "\">" +  f.getName() + "</a>");
    			sb.append("</td><td>");
    			sb.append(date_format.format(new Date(f.lastModified())).toString());
    			sb.append("</td></tr>");
    		}
    		sb.append("</table>");
    		sb.append("<p>");
    		sb.append("<a href=\"/shutdown\"><button>" +  "Shutdown" + "</button></a>");
    		sb.append("</body>\n");
    		sb.append("</html>\n");
    		return sb.toString();
	}
	
	public String genControl() {
		return "";
	}
	
	public String genShutdown() {
		StringBuilder sb = new StringBuilder();
    		sb.append("<html>\n");
    		sb.append("<body>\n");
    		sb.append("The Server has been shut down");
    		sb.append("</body>\n");
    		sb.append("</html>\n");
		return sb.toString();
	}
	
	public void genFileContent(File file, PrintStream output) {
		
	}
	/**********************************/
	
	/**
	 * Generate error page with specified error message
	 * @param line gives the error message
	 * @return the message body of response
	 */
	public String genErrorPage(String line) {
		String message = line.substring(line.indexOf(" ") + 1);
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<head>");
		sb.append(message);
		sb.append("</head>");
		sb.append("</html>");
		return sb.toString();
	}
	
	/**
	 * Generate the header of the response
	 * @param len gives the length of the message body
	 * @return header of the response
	 */
	public String genHeader(long len) {
		StringBuilder sb = new StringBuilder();
    		SimpleDateFormat date_format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
    		date_format.setTimeZone(TimeZone.getTimeZone("GMT"));
		sb.append("Date: " + date_format.format(new Date()) + "\n");
		sb.append("Content-Type: " + currReq.getFileType() + "\n");
		sb.append("Content-Length: " + len + "\n");
		sb.append("Connection: Close\n");
		sb.append("\r\n");
		return sb.toString();
	}

	/**
	 * Generate the status line of the response
	 * @param code gives the status of the response with the code
	 * @return the status line of the response
	 */
	public String genStatusLine(int code){
		StringBuilder sb = new StringBuilder("HTTP/1.1");
		if(code == 100) sb.append(" 100 Continue\n");
		else if(code == 200) sb.append(" 200 OK\n");
		else if(code == 304) sb.append(" 304 Not Modified\n");
		else if(code == 400) sb.append(" 400 Bad Request\n");
		else if(code == 403) sb.append(" 403 Request Not Allowed\n");
		else if(code == 404) sb.append(" 404 Not Found\n");
		else if(code == 412) sb.append(" 412 Precondition Failed\n");
		else if(code == 500) sb.append(" 500 Internal Server Error\n");
		else if(code == 501) sb.append(" 501 Not Implemented\n");
		else if(code == 505) sb.append(" 505 HTTP Version Not Supported\n");
		return sb.toString();
	}
	
}
