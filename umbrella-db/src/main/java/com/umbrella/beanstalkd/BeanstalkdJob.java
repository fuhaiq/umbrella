package com.umbrella.beanstalkd;

import java.io.Serializable;

public class BeanstalkdJob implements Serializable{

	private static final long serialVersionUID = -1511282839282328024L;

	@Override
	public String toString() {
		return String.format("beanstalkd job(%s)=>%s", id, data);
	}

	private long id;
	
	private String data;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
