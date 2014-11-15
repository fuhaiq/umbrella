package com.umbrella.service.beanstalkd;

public enum Status {
	
//	-2-abort, -1-syntax error, 0-waiting for evaluate, 1-success
	
	ABORT("-2"),SYNTAX_ERROR("-1"),WAITING("0"), SUCCESS("1");

	private String value;
	
	private Status(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
