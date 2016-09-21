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
	
	private Set<String> needs;
	
	private Secret secret = new Secret();
	
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

	public Set<String> getNeeds() {
		return needs;
	}

	public void setNeeds(Set<String> needs) {
		this.needs = needs;
	}
	
	public Secret getSecret() {
		return secret;
	}

	public void setSecret(Secret secret) {
		this.secret = secret;
	}

	public class Secret {
		
		private String morse;

		public String getMorse() {
			return morse;
		}

		public void setMorse(String morse) {
			this.morse = morse;
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
