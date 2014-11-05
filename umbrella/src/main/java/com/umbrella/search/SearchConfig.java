package com.umbrella.search;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class SearchConfig extends GenericObjectPoolConfig {

	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
