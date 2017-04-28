package com.topfine.recommend.fn;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import scala.Tuple2;

public class LineConsumer implements Consumer<String> {
	
	private static final Logger LOG = Logger.getLogger(LineConsumer.class);
	
	private final SparkSession session;
	
	private final JedisPool pool;

	public LineConsumer(SparkSession session, JedisPool pool) {
		this.session = session;
		this.pool = pool;
	}

	@Override
	public void accept(String line) {
		JSONObject json = JSONObject.parseObject(line);
		
		int goodid = json.getIntValue("goodid");
		
		String uid = json.getString("uid");
		
		/*
		 * 统计classnameid种类，并按自增序号进行对应
		 */
		JavaPairRDD<Integer, Integer> classify = session.sql("SELECT RANK() OVER (ORDER BY classnameid) AS id, classnameid FROM itemdata group by classnameid")
			.javaRDD()
			.mapToPair(row -> new Tuple2<Integer, Integer>(row.getAs("classnameid"), row.getAs("id")));
		
		/*
		 * 计算各项最大值
		 */
		int maxScore = 0;
		int maxOrdernum = 0;
		int maxPeoplenum = 0;
		float maxGrade = 0;
		float maxFrequency = 0;
		float maxPrice = 0;
		
		Row max = session.sql("select max(score) as score, max(ordernum) as ordernum, max(peoplenum) as peoplenum, max(grade) as grade, max(frequency) as frequency, max(price) as price from itemdata").first();
		
		maxScore = max.getAs("score");
		maxOrdernum = max.getAs("ordernum");
		maxPeoplenum = max.getAs("peoplenum");
		maxGrade = max.getAs("grade");
		maxFrequency = max.getAs("frequency");
		maxPrice = max.getAs("price");
		
		//取出所有数据
		Dataset<Row> ds = session.sql("select goodid,classnameid,score,ordernum,isphysical,peoplenum,grade,frequency,price from itemdata");
		//当前行
		Dataset<Row> thisRowDs = ds.filter(value -> goodid == (int) value.getAs("goodid"));
		long count = thisRowDs.count();
		if(count == 0) {
			LOG.info("选择商品不在推荐数据库列表中");
			return;
		}
		
		Row thisRow = thisRowDs.first();
		//其他N-1行
		Dataset<Row> otherRow = ds.filter(value -> goodid != (int) value.getAs("goodid"));
		
		JavaRDD<Row> javardd = otherRow.javaRDD();
		
		int partition = javardd.partitions().size();
		List<Integer> cb = javardd.map(new CBFunction(thisRow, classify.collectAsMap(), maxScore, maxOrdernum, maxPeoplenum, maxGrade, maxFrequency, maxPrice))
			.sortBy(tube -> tube._2, false, partition).map(tube -> tube._1).take(12);
		
		try(Connection conn = ConnectionFactory.createConnection(HBaseConfiguration.create())) {
			Table table = conn.getTable(TableName.valueOf("shop_recommend"));
			// 删除原用户推荐列表
			Delete delete = new Delete(uid.getBytes());
			table.delete(delete);
			
			List<Put> puts = Lists.newArrayList();
			Put put = new Put(uid.getBytes());
			for(int i = 0; i < cb.size(); i++) {
				put.addColumn("re".getBytes(), String.valueOf(i).getBytes(), String.valueOf(cb.get(i)).getBytes());
			}
			puts.add(put);
			table.put(puts);
			LOG.info("写入用户["+uid+"]推荐列表完成");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			Transaction tx = jedis.multi();
			tx.del(uid);
			tx.zrem("recommend~keys", uid);
			tx.exec();
			LOG.info("完成redis操作");
		} finally {
			pool.returnResource(jedis);
		}
	}

}
