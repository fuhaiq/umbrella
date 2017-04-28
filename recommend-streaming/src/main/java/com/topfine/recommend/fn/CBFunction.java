package com.topfine.recommend.fn;

import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Row;

import com.google.common.collect.Lists;

import scala.Tuple2;

public class CBFunction implements Function<Row, Tuple2<Integer, Double>> {
	
	private static final long serialVersionUID = -7300858079522572243L;

	private final Row current;

	private final int score;

	private final int ordernum;

	private final int peoplenum;

	private final float grade;

	private final float frequency;

	private final float price;

	private final Map<Integer, Integer> classifyMap;

	public CBFunction(Row current, Map<Integer, Integer> classifyMap, int score, int ordernum, int peoplenum,
			float grade, float frequency, float price) {
		this.current = current;
		this.score = score;
		this.ordernum = ordernum;
		this.peoplenum = peoplenum;
		this.grade = grade;
		this.frequency = frequency;
		this.price = price;
		this.classifyMap = classifyMap;
	}

	@Override
	public Tuple2<Integer, Double> call(Row other) throws Exception {
		Integer otherGoodid = other.getAs("goodid");
		if(null == otherGoodid) {
			throw new IllegalArgumentException("goodid不存在");
		}
		
		// 当前向量
		List<Double> currentVector = null;
		// 比较向量
		List<Double> otherVector = null;
		
		// 分割classnameid
		Integer currentClassify = current.getAs("classnameid");
		Integer otherClassify = other.getAs("classnameid");
		if(currentClassify == null || otherClassify == null) {
			throw new IllegalArgumentException("classnameid不存在");
		}
		int currentClassifyPosition = classifyMap.get(currentClassify);
		int otherClassifyPosition = classifyMap.get(otherClassify);
		if(currentClassifyPosition > otherClassifyPosition) {
			currentVector = Lists.newArrayList(1d, 0d);
			otherVector = Lists.newArrayList(0d, 1d);
		} else if(currentClassifyPosition < otherClassifyPosition) {
			currentVector = Lists.newArrayList(0d, 1d);
			otherVector = Lists.newArrayList(1d, 0d);
		} else {
			currentVector = Lists.newArrayList(1d);
			otherVector = Lists.newArrayList(1d);
		}
		
		// score缩放
		Integer currentScore = current.getAs("score");
		Integer otherScore = other.getAs("score");
		if (null != currentScore && null != otherScore) {
			currentVector.add((double) currentScore / this.score);
			otherVector.add((double) otherScore / this.score);
		}

		// ordernum缩放
		Integer currentOrdernum = current.getAs("ordernum");
		Integer otherOrdernum = other.getAs("ordernum");
		if (null != currentOrdernum && null != otherOrdernum) {
			currentVector.add((double) currentOrdernum / this.ordernum);
			otherVector.add((double) otherOrdernum / this.ordernum);
		}
		
		Integer currentIsphysical = current.getAs("isphysical");
		Integer otherIsphysical = other.getAs("isphysical");
		if (null != currentIsphysical && null != otherIsphysical) {
			currentVector.add((double)currentIsphysical);
			otherVector.add((double)otherIsphysical);
		}

		// peoplenum缩放
		Integer currentPeoplenum = current.getAs("peoplenum");
		Integer otherPeoplenum = other.getAs("peoplenum");
		if (null != currentPeoplenum && null != otherPeoplenum) {
			currentVector.add((double) currentPeoplenum / this.peoplenum);
			otherVector.add((double) otherPeoplenum / this.peoplenum);
		}

		// grade缩放
		Float currentGrade = current.getAs("grade");
		Float otherGrade = other.getAs("grade");
		if (null != currentGrade && null != otherGrade) {
			currentVector.add((double) currentGrade / this.grade);
			otherVector.add((double) otherGrade / this.grade);
		}

		// frequency缩放
		Float currentFrequency = current.getAs("frequency");
		Float otherFrequency = other.getAs("frequency");
		if (null != currentFrequency && null != otherFrequency) {
			currentVector.add((double) currentFrequency / this.frequency);
			otherVector.add((double) otherFrequency / this.frequency);
		}

		// price缩放
		Float currentPrice = current.getAs("price");
		Float otherPrice = other.getAs("price");
		if (null != currentPrice && null != otherPrice) {
			currentVector.add((double) currentPrice / this.price);
			otherVector.add((double) otherPrice / this.price);
		}
		
		if(currentVector.size() != otherVector.size()) {
			throw new IllegalStateException("向量长度不一致");
		}
		
		// 分子
		double numerator = 0d;
		// 分母
		double denominator = 0d;
		
		double d1 = 0d;
		
		double d2 = 0d;
		
		for(int i = 0 ; i < currentVector.size(); i++) {
			double x = currentVector.get(i);
			double y = otherVector.get(i);
			numerator = numerator + (x * y);
			d1 = d1 + Math.pow(x, 2);
			d2 = d2 + Math.pow(y, 2);
		}
		
		denominator = Math.sqrt(d1) * Math.sqrt(d2);
		
		// 避免分母为0
		if(denominator == 0d) {
			return new Tuple2<Integer, Double>(otherGoodid.intValue(), 0d);
		} else {
			return new Tuple2<Integer, Double>(otherGoodid.intValue(), numerator/denominator);
		}
		
	}

}
