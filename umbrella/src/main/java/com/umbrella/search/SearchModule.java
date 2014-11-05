package com.umbrella.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.alibaba.fastjson.JSON;
import com.google.inject.AbstractModule;

public class SearchModule extends AbstractModule{

	private final String config;
	
	public SearchModule(String config) {
		this.config = config;
	}
	
	@Override
	protected void configure() {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(config);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			bind(SearchConfig.class).toInstance(JSON.parseObject(builder.toString(), SearchConfig.class));
		} catch (IOException e) {
			addError(e);
		}
	}
}
