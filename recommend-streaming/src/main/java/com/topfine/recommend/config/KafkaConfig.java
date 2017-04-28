package com.topfine.recommend.config;

import java.util.Map;
import java.util.Set;

public class KafkaConfig {

	private Map<String, String> map;
	
	private Set<String> topics;

	public Map<String, String> getMap() {
		return map;
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}

	public Set<String> getTopics() {
		return topics;
	}

	public void setTopics(Set<String> topics) {
		this.topics = topics;
	}
}
