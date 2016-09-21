package com.umbrella.kernel.link.morse;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="morse")
public class MorseConfig {
	
	private String path;
	
	private Set<Integer> codes;

	public Set<Integer> getCodes() {
		return codes;
	}

	public void setCodes(Set<Integer> codes) {
		this.codes = codes;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
