package com.topfine.recommend.config;

public class RecommendConfig {
	
	private int durations;
	
	private KafkaConfig kafka;
	
	private RedisConfig redis;

	public int getDurations() {
		return durations;
	}

	public void setDurations(int durations) {
		this.durations = durations;
	}

	public KafkaConfig getKafka() {
		return kafka;
	}

	public void setKafka(KafkaConfig kafka) {
		this.kafka = kafka;
	}

	public RedisConfig getRedis() {
		return redis;
	}

	public void setRedis(RedisConfig redis) {
		this.redis = redis;
	}

}
