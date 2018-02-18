package edu.upenn.cis455.hw1;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ThreadPool {
	private List<Thread> threads;
	private BlockingQueue<Socket> blockingQueue;
	
	public ThreadPool(int workerCount, int queueSize, String rootDir, int port) {
		blockingQueue = new BlockingQueue<Socket>(queueSize);
		threads = new ArrayList<Thread>();
		for(int i = 0; i < workerCount; i++) {
			Thread t = new Thread(new Worker(blockingQueue, i, rootDir, port));
			threads.add(t);
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
}
