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
	
	private final Dir dir = new Dir();
	
	private int timeConstrained;
	
	private Set<Integer> badChar;
	
	private Set<String> escapes;
	
	private Set<String> needs;
	
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
	
	public Dir getDir() {
		return dir;
	}

	public Set<String> getNeeds() {
		return needs;
	}

	public void setNeeds(Set<String> needs) {
		this.needs = needs;
	}

	public class Dir {
		private String post;
		private String temp;
		public String getPost() {
			return post;
		}
		public void setPost(String post) {
			this.post = post;
		}
		public String getTemp() {
			return temp;
		}
		public void setTemp(String temp) {
			this.temp = temp;
		}
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
