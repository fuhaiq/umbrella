package com.umbrella.kernel.link;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class KernelConfig extends GenericObjectPoolConfig{
	
	private String url;
	
	private final Libdir libdir = new Libdir();
	
	private int timeConstrained;
	
	private String imgDir;
	
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
