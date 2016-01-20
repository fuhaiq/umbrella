package com.umbrella.kernel.link;

import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="kernel")
public class KernelConfig extends GenericObjectPoolConfig{
	
	private String url;
	
	private final Libdir libdir = new Libdir();
	
	private int timeConstrained;
	
	private String imgDir;
	
	private Set<Integer> badChar;
	
	private Set<String> escapes;
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Libdir getLibdir() {
		return libdir;
	}

	public int getTimeConstrained() {
		return timeConstrained;
	}

	public void setTimeConstrained(int timeConstrained) {
		this.timeConstrained = timeConstrained;
	}

	public String getImgDir() {
		return imgDir;
	}

	public void setImgDir(String imgDir) {
		this.imgDir = imgDir;
	}

	public Set<Integer> getBadChar() {
		return badChar;
	}

	public void setBadChar(Set<Integer> badChar) {
		this.badChar = badChar;
	}

	public Set<String> getEscapes() {
		return escapes;
	}

	public void setEscapes(Set<String> escapes) {
		this.escapes = escapes;
	}

	public class Libdir {
		
		private String name;
		
		private String dir;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getDir() {
			return dir;
		}
		
		public void setDir(String dir) {
			this.dir = dir;
		}
		
	}
}
