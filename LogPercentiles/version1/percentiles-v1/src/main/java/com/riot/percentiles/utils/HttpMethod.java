package com.riot.percentiles.utils;

public enum HttpMethod {
	
	GET("GET"), POST("POST"), OPTIONS("OPTIONS"), HEAD("HEAD"), PUT("PUT"), DELETE("DELETE"), TRACE("TRACE"), CONNECT("CONNECT"), UNKNOWN("UNKNOWN");
	
	private final String name;
	
	HttpMethod(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
