package com.umbrella.session;

public interface Session<T> {
	public void start();

	public void close();

	public T get();
	
	public boolean isClosed();
}
