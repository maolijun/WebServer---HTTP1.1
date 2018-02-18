package edu.upenn.cis455.hw1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.log4j.Logger;

class HttpServer {
	
	private static ServerSocket serverSocket;
	private static ThreadPool pool;
	private static final int blockingQueueSize = 100000;
	private static final int workers = 10;
	private static final Logger log = Logger.getLogger(HttpServer.class.getName());
	
	public static void main(String args[]){
		if(args.length == 0) {
			log.info("Lijun Mao");
			log.info("maolijun@seas.upenn.edu");
			return;
		} else if(args.length != 2) {
			log.error("2 Command line arguments required: Port Number & Root Directory");
			return;
		}
		int portNumber = Integer.valueOf(args[0]);
		String rootDir = args[1];
		try {
			log.info("HTTP server starts running");
			log.info("Binding port number " + portNumber);
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			log.error("Can not bind the specified port number");
			return;
		}
		pool = new ThreadPool(workers, blockingQueueSize, rootDir, portNumber);
		while(true) {
			try {
				Socket client = serverSocket.accept();
				pool.handleConnection(client);
			} catch (IOException e) {
				log.error("Server socket closed");
				break;
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting the blocking queue");
				break;
			}
		}
	}
	
	public static void shutDownServer() {
		try {
			pool.closeThreads();
			serverSocket.close();
		} catch (IOException e) {
			log.error("Problem closing server socket");
		}
	}
}
  
