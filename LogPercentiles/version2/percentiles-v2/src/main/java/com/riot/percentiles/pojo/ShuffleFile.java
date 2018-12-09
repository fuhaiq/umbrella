package com.riot.percentiles.pojo;

import java.nio.file.Path;

public class ShuffleFile {

	public ShuffleFile(Path path) {
		this.path = path;
		this.setNumberOfLog(0);
	}

	private Path path;
	
	private int numberOfLog;

	public Path getPath() {
		return path;
	}

	public void add() {
		this.setNumberOfLog(this.getNumberOfLog() + 1);
	}
	
	public void setPath(Path path) {
		this.path = path;
	}

	public int getNumberOfLog() {
		return numberOfLog;
	}

	public void setNumberOfLog(int numberOfLog) {
		this.numberOfLog = numberOfLog;
	}

}
