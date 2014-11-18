package com.umbrella.session;

public interface Session<T> {
	public void start() throws SessionException;

	public void close() throws SessionException;

	public T get() throws SessionException;
	
	public boolean isClosed();
}
