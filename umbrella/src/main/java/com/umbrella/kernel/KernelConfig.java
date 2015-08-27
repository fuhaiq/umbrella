package com.umbrella.kernel;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class KernelConfig extends GenericObjectPoolConfig{
	
	private String url;
	
	private Libdir libdir;
	
	private int timeConstrained;
	
	private int timeConstrainedTotal;
	
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

	public void setLibdir(Libdir libdir) {
		this.libdir = libdir;
	}

	public int getTimeConstrained() {
		return timeConstrained;
	}

	public void setTimeConstrained(int timeConstrained) {
		this.timeConstrained = timeConstrained;
	}

	public int getTimeConstrainedTotal() {
		return timeConstrainedTotal;
	}

	public void setTimeConstrainedTotal(int timeConstrainedTotal) {
		this.timeConstrainedTotal = timeConstrainedTotal;
	}

	public String getImgDir() {
		return imgDir;
	}

	public void setImgDir(String imgDir) {
		this.imgDir = imgDir;
	}

	class Libdir {
		
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
