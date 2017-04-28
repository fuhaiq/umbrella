package com.topfine.recommend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;

import com.alibaba.fastjson.JSON;
import com.topfine.recommend.config.RecommendConfig;
import com.topfine.recommend.config.RedisConfig;
import com.topfine.recommend.fn.LineConsumer;

import kafka.serializer.StringDecoder;
import redis.clients.jedis.JedisPool;


public class Recommend {
	
	private static final Logger LOG = Logger.getLogger(Recommend.class);
	
	private static RecommendConfig provideRecommandConfig() throws IOException {
		try
		(BufferedReader reader = new BufferedReader(new InputStreamReader(Recommend.class.getResourceAsStream("/application.json")))){
			StringBuffer bf = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null) {
				bf.append(line);
            }
			return JSON.parseObject(bf.toString(), RecommendConfig.class);
		}
	}
	
	public static JedisPool provideJedisConnFactory(RecommendConfig config) {
		RedisConfig redis = config.getRedis();
		return new JedisPool(redis, redis.getHost(), redis.getPort());
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		RecommendConfig config = provideRecommandConfig();
		final JedisPool pool = provideJedisConnFactory(config);
		SparkSession session = SparkSession.builder().enableHiveSupport().getOrCreate();
		JavaStreamingContext javaStreamingContext = new JavaStreamingContext(new StreamingContext(session.sparkContext(), Durations.seconds(config.getDurations())));
		final JavaPairInputDStream<String, String> stream = KafkaUtils.createDirectStream(javaStreamingContext,
		         String.class, String.class, StringDecoder.class, StringDecoder.class,
		         config.getKafka().getMap(), config.getKafka().getTopics());
		stream.map(tube -> tube._2).foreachRDD(rdd -> rdd.collect().forEach(new LineConsumer(session, pool)));
		javaStreamingContext.start();
		javaStreamingContext.awaitTermination();
		LOG.info("销毁redis连接池");
		pool.close();
	}

}
