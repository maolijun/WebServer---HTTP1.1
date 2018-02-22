package edu.upenn.cis455.hw1;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class ThreadPool {
	private List<Thread> threads;
	private BlockingQueue<Socket> blockingQueue;
	private List<String> status;
	private static final Logger log = Logger.getLogger(HttpServer.class.getName());;
	
	public ThreadPool(int workerCount, int queueSize, String rootDir, int port) {
		blockingQueue = new BlockingQueue<Socket>(queueSize);
		threads = new ArrayList<Thread>();
		status = new ArrayList<String>();
		for(int i = 0; i < workerCount; i++) {
			Thread t = new Thread(new Worker(blockingQueue, i, rootDir, port, this));
			threads.add(t);
			status.add("Waiting");
			t.start();
		}
	}
	
	public void handleConnection(Socket client) throws InterruptedException {
		blockingQueue.offer(client);
	}
	
	
	public void closeThreads() {
		for(int i = 0; i < threads.size(); i++) {
			threads.get(i).interrupt();
		}
	}
	
	public void setStatus(int number, String message) {
		status.set(number, message);
	}
	
	public List<String> getStatus() {
		return status;
	}
	
	public void checkTerminate() {
		for(int i = 0; i < threads.size(); i++) {
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				log.error("Error when joining thread worker " + i);
			}
		}
	}
}
