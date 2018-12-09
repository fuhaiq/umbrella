package com.riot.percentiles.pojo;

import com.opencsv.bean.CsvBindByPosition;
import com.riot.percentiles.utils.HttpMethod;

public class AccessLog {

	@CsvBindByPosition(position = 2)
	private String methodAndUri;
	
	@CsvBindByPosition(position = 4)
	private int responseTime;

	public HttpMethod getHttpMethod() {
		for(HttpMethod method : HttpMethod.values()) {
			if(methodAndUri.startsWith(method.getName())) {
				return method;
			}
		}
		return HttpMethod.UNKNOWN;
	}
	
	public void setMethodAndUri(String methodAndUri) {
		this.methodAndUri = methodAndUri;
	}

	public int getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(int responseTime) {
		this.responseTime = responseTime;
	}

}
