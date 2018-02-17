package edu.upenn.cis455.hw1;

import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue<T> {
	private int bufferSize;
	private Queue<T> buffer;
	
	public BlockingQueue(int size) {
		bufferSize = size;
		buffer = new LinkedList<T>();
	}
	
	private boolean isEmpty() {
		return buffer.size() == 0;
	}
	
	private boolean isFull() {
		return buffer.size() == bufferSize;
	}
	
	public synchronized T poll() throws InterruptedException {
		while(isEmpty()) wait();
		if(isFull()) notifyAll();
		return buffer.poll();
	}
	
	public synchronized void offer(T obj) throws InterruptedException {
		while(isFull()) wait();
		if(isEmpty()) notifyAll();
		buffer.offer(obj);
	}
}
