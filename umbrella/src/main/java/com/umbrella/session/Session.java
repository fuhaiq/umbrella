package com.umbrella.session;

import java.io.Closeable;

public interface Session<T> extends Closeable {
	
	public void start() throws SessionException;

	public T get() throws SessionException;
	
	public boolean isClosed();
}
