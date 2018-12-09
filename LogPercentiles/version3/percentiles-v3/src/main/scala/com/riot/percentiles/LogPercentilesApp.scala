package com.riot.percentiles

import org.apache.spark.sql.SparkSession

/*
spark-submit \
--master spark://iZuf6hfawpoc4spzru02ouZ:7077 \
--deploy-mode client \
--class com.riot.percentiles.LogPercentilesApp \
--total-executor-cores 6 \
--executor-cores 1 \
--executor-memory 5g \
--conf spark.sql.shuffle.partitions=12 \
percentiles-v3-1.jar
 */

object LogPercentilesApp extends App {

  val spark = SparkSession
    .builder()
    .appName("Log Percentiles")
    .enableHiveSupport()
    .getOrCreate()

  import org.apache.spark.sql.types.{StructField, StructType, StringType, IntegerType}

  val schema = new StructType(Array(
    new StructField("address", StringType, false),
    new StructField("timestamp", StringType, false),
    new StructField("methodAndUri", StringType, false),
    new StructField("status", IntegerType, false),
    new StructField("responseTime", IntegerType, false)
    ))

  val df = spark.read.format("csv")
    .option("header", "false")
    .option("mode", "FAILFAST")
    .schema(schema)
    .load("/tmp/test")

  import org.apache.spark.sql.functions.{col, when, asc, desc}

  val expression = when(col("methodAndUri").startsWith("GET"), "GET").otherwise("")

  val transferred = df.select("responseTime","methodAndUri").withColumn("method", expression)
    .filter(col("method") === "GET")
    .drop("methodAndUri")
    .orderBy(asc("responseTime"))
    .cache()

  val N = transferred.count();

  import spark.implicits._

  val result = transferred.rdd.map(row => row.getAs[Int]("responseTime"))
      .zipWithIndex().toDF()
      .withColumnRenamed("_1","responseTime")
      .withColumnRenamed("_2","id")

  val percents = Array(0.9d, 0.95d, 0.99d)

  for(percent <- percents) {
    val position = (percent * N).toInt - 1
    val responseTime = result.filter(col("id") === position).first().getAs[Int]("responseTime")
    println(percent * 100 + "% of requests return a response in " + responseTime + " ms ")
  }

  spark.stop()

}
