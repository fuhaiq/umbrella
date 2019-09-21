> impala 2.12
# How Impala Works with Apache Hadoop
The Impala solution is composed of the following components
- Clients - Entities including Hue, ODBC clients, JDBC clients, and the Impala Shell can all interact with Impala. These interfaces are typically used to issue queries or complete administrative tasks such as connecting to Impala
- Hive Metastore - Stores information about the data available to Impala. For example, the metastore lets Impala know what databases are available and what the structure of those databases is. As you create, drop, and alter schema objects, load data into tables, and so on through Impala SQL statements, the relevant metadata changes are automatically broadcast to all Impala nodes by the dedicated catalog service introduced in Impala 1.2.
- Impala - This process, **which runs on DataNodes**, coordinates and executes queries. Each instance of Impala can receive, plan, and coordinate queries from Impala clients. Queries are distributed among Impala nodes, and these nodes then act as workers, executing parallel query fragments
- HBase and HDFS - Storage for data to be queried

Queries executed using Impala are handled as follows
- User applications send SQL queries to Impala through ODBC or JDBC, which provide standardized querying interfaces. The user application may connect to any impalad in the cluster. This impalad becomes the coordinator for the query
- Impala parses the query and analyzes it to determine what tasks need to be performed by impalad instances across the cluster. Execution is planned for optimal efficiency
-  Services such as HDFS and HBase are accessed by local impalad instances to provide data
- Each impalad returns data to the coordinating impalad, which sends these results to the client

# Impala Concepts and Architecture
The Impala server is a distributed, massively parallel processing (MPP) database engine. It consists of different daemon processes that run on specific hosts within your  cluster

## The Impala Daemon
The core Impala component is a daemon process that runs **on each DataNode** of the cluster, physically represented by the impalad process. It reads and writes to data files; accepts queries transmitted from the impala-shell command, Hue, JDBC, or ODBC; parallelizes the queries and distributes work across the cluster; and transmits intermediate query results back to the central coordinator node

You can submit a query to the Impala daemon running on any DataNode, and that instance of the daemon serves as the coordinator node for that query. The other nodes transmit partial results back to the coordinator, which constructs the final result set for a query. When running experiments with functionality through the impala-shell command, you might always connect to the same Impala daemon for convenience. For clusters running production workloads, you might load-balance by submitting each query to a different Impala daemon in round-robin style, using the JDBC or ODBC interfaces(**Using Haproxy**)

The Impala daemons are in constant communication with the statestore, to confirm which nodes are healthy and can accept new work

They also **receive broadcast messages** from the catalogd daemon (introduced in Impala 1.2) whenever any Impala node in the cluster creates, alters, or drops any type of object, or when an INSERT or LOAD DATA statement is processed through Impala. This background communication minimizes the need for REFRESH or INVALIDATE METADATA statements that were needed to coordinate metadata across nodes prior to Impala 1.2

In Impala 2.9 and higher, you can control which hosts act as query coordinators and which act as query executors, to improve scalability for highly concurrent workloads on large clusters

## The Impala Statestore

The Impala component known as the statestore checks on the health of Impala daemons on all the DataNodes in a cluster, and continuously relays its findings to each of those daemons. It is physically represented by a daemon process named statestored; you only need such a process on one host in the cluster. If an Impala daemon goes offline due to hardware failure, network error, software issue, or other reason, the statestore informs all the other Impala daemons so that future queries can avoid making requests to the unreachable node

Because the statestore's purpose is to help when things go wrong, it is not critical to the normal operation of an Impala cluster. If the statestore is not running or becomes unreachable, the Impala daemons continue running and distributing work among themselves as usual; the cluster just becomes less robust if other Impala daemons fail while the statestore is offline. When the statestore comes back online, it re-establishes communication with the Impala daemons and resumes its monitoring function

## The Impala Catalog Service

The Impala component known as the catalog service relays the metadata changes from Impala SQL statements to all the Impala daemons in a cluster. It is physically represented by a daemon process named catalogd; you only need such a process on one host in the cluster. Because the requests are passed through the statestore daemon, it makes sense to run the statestored and catalogd services **on the same host**

The catalog service **avoids the need to** issue REFRESH and INVALIDATE METADATA statements when the metadata changes are performed by statements issued **through Impala**. When you create a table, load data, and so on **through Hive or by manipulating data files directly in HDFS**, you **do need to** issue REFRESH or INVALIDATE METADATA on an Impala node before executing a query there

Use `--load_catalog_in_background` option to control when the metadata of a table is loaded

-  If set to `false`, the metadata of a table is loaded when it is referenced for the first time. This means that the first run of a particular query can be slower than subsequent runs. Starting in Impala 2.2, the default for `load_catalog_in_background` is `false`
- If set to `true`, the catalog service attempts to load metadata for a table even if no query needed that metadata. So metadata will possibly be already loaded when the first query that would need it is run. However, for the following reasons, we recommend not to set the option to `true`
   - Background load can interfere with query-specific metadata loading. This can happen on startup or after invalidating metadata, with a duration depending on the amount of metadata, and can lead to a seemingly random long running queries that are difficult to diagnose
   - Impala may load metadata for tables that are possibly never used, potentially increasing catalog size and consequently memory usage for both catalog service and Impala Daemon
---
Most considerations for load balancing and high availability apply to the impalad daemon. **The statestored and catalogd daemons do not have special requirements for high availability**, because problems with those daemons do not result in data loss. If those daemons become unavailable due to an outage on a particular host, you can stop the Impala service, delete the Impala StateStore and Impala Catalog Server roles, add the roles on a different host, and restart the Impala service

# How Impala Fits Into the Hadoop Ecosystem

## How Impala Works with Hive

In particular, Impala keeps its table definitions in a traditional MySQL or PostgreSQL database known as the metastore, the same database where Hive keeps this type of data. Thus, Impala can access tables defined or loaded by Hive, as long as all columns use Impala-supported data types, file formats, and compression codecs

The Impala query optimizer can also make use of **table statistics** and **column statistics**. In Impala 1.2.2 and higher, use the Impala `COMPUTE STATS` statement

## Overview of Impala Metadata and the Metastore

 Impala maintains information about table definitions in a central database known as the metastore. Impala also tracks other metadata for the low-level characteristics of data files
 - The physical locations of blocks within HDFS


 For tables with a large volume of data and/or many partitions, retrieving all the metadata for a table can be timeconsuming, taking minutes in some cases. Thus, **each impala daemon** subscribes to `Statestore` at startup, acts as a subscriber and retrieve metadata through one kind of topic *(heartbeat is another kind of topic).* Caches all of this metadata to reuse for future queries against the same table
 > metadata = hive table metastore +  physical locations of blocks in HDFS

We could shutdown statestore and catalog services after a daemon has been running for a couple of while, and that daemon is still able to retrieve response against existing tables (cahes works). On the other hand, when we execute sql in a new impala daemon (no Caches) without statestore and catalog services running, we will get following error
```
This Impala daemon is not ready to accept user requests. Status: Waiting for catalog update from the StateStore.
```
Base on my experiment, impala daemons retrieve metadata and its update only from Statestore service (not catalog service) through broadcast topic message. It only talks to catalog directly when table structure changes happen, like: ALTER, DROP, etc.
```bash
# stop catalog server makes this happen
[iZ11wnf8l7wZ:21000] > ALTER TABLE employee DROP phone_no;
Query: alter TABLE employee DROP phone_no
ERROR: Couldn't open transport for iZ11wnf8l7wZ:26000 (connect() failed: Connection refused)
```
## How Impala Uses HDFS

Impala uses the distributed filesystem HDFS as its primary data storage medium. Impala relies on the redundancy provided by HDFS to guard against hardware or network outages on individual nodes. Impala table data is physically represented as data files in HDFS, using familiar HDFS file formats and compression codecs. When data files are present in the directory for a new table, Impala reads them all, regardless of file name. New data is added in files with names controlled by Impala

## How Impala Uses HBase

HBase is an alternative to HDFS as a storage medium for Impala data. It is a database storage system built on top of HDFS, without built-in SQL support. Many Hadoop users already have it configured and store large (often sparse) data sets in it. By defining tables in Impala and mapping them to equivalent tables in HBase, you can query the contents of the HBase tables through Impala, and even perform join queries including both Impala and HBase tables

# Guidelines for Designing Impala Schemas

## Prefer binary file formats over text-based formats

To save space and improve memory usage and query performance, use binary file formats for any large or intensively queried tables. Parquet file format is the most efficient for data warehouse-style analytic queries. Avro is the other binary file format that Impala supports, that you might already have as part of a Hadoop ETL pipeline. Although Impala can create and query tables with the RCFile and SequenceFile file formats, such tables are relatively bulky due to the text-based nature of those formats, and are not optimized for data warehouse-style queries due to their row-oriented layout. Impala does not support INSERT operations for tables with these file formats

Guidelines:
- For an efficient and scalable format for large, performance-critical tables, use the Parquet file format.
- To deliver intermediate data during the ETL process, in a format that can also be used by other Hadoop components, Avro is a reasonable choice.
- For convenient import of raw data, use a text table instead of RCFile or SequenceFile, and convert to Parquet in a later stage of the ETL process

## Use Snappy compression where practical

Snappy compression involves low CPU overhead to decompress, while still providing substantial space savings. In cases where you have a choice of compression codecs, such as with the Parquet and Avro file formats, use Snappy compression unless you find a compelling reason to use a different codec

## Prefer numeric types over strings

If you have numeric values that you could treat as either strings or numbers (such as YEAR, MONTH, and DAY for partition key columns), define them as the smallest applicable integer types. For example, YEAR can be SMALLINT, MONTH and DAY can be TINYINT. Although you might not see any difference in the way partitioned tables or text files are laid out on disk, using numeric types will save space in binary formats such as Parquet, and in memory when doing queries, particularly resource-intensive queries such as joins

## Partition, but do not over-partition

If you are moving to Impala from a traditional database system, or just getting started in the Big Data field, you might not have enough data volume to take advantage of Impala parallel queries with your existing partitioning scheme. For example, if you have only a few tens of megabytes of data per day, partitioning by YEAR, MONTH, and DAY columns might be too granular. Most of your cluster might be sitting idle during queries that target a single day, or each node might have very little work to do. Consider reducing the number of partition key columns so that each partition directory contains several gigabytes worth of data

For example, consider a Parquet table where each data file is 1 HDFS block, with a maximum block size of 1 GB. (In Impala 2.0 and later, the default Parquet block size is reduced to 256 MB. For this exercise, let's assume you have bumped the size back up to 1 GB by setting the query option PARQUET_FILE_SIZE=1g.) if you have a 10-node cluster, you need 10 data files (up to 10 GB) to give each node some work to do for a query. But each core on each machine can process a separate data block in parallel. With 16-core machines on a 10-node cluster, a query could process up to 160 GB fully in parallel. If there are only a few data files per partition, not only are most cluster nodes sitting idle during queries, so are most cores on those machines

You can reduce the **Parquet block size** to as low as 128 MB or 64 MB to increase the number of files per partition and improve parallelism. But also consider reducing the level of partitioning so that analytic queries have enough data to work with

## Always compute stats after loading data

Impala makes extensive use of statistics about data in the overall table and in each column, to help plan resourceintensive operations such as join queries and inserting into partitioned Parquet tables. Because this information is only available after data is loaded, run the `COMPUTE STATS` statement on a table after loading or replacing data in a table or partition

Having accurate statistics can make the difference between a successful operation, or one that fails due to an outof-memory error or a timeout. When you encounter performance or capacity issues, always use the `SHOW STATS` statement to check if the statistics are present and up-to-date for all tables in the query

When doing a join query, Impala consults the statistics for each joined table to determine their relative sizes and to estimate the number of rows produced in each join stage. When doing an `INSERT` into a Parquet table, Impala consults the statistics for the source table to determine how to distribute the work of constructing the data files for each partition

## Verify sensible execution plans with EXPLAIN and SUMMARY

Before executing a resource-intensive query, use the EXPLAIN statement to get an overview of how Impala intends to parallelize the query and distribute the work. If you see that the query plan is inefficient, you can take tuning steps such as changing file formats, using partitioned tables, running the COMPUTE STATS statement, or adding query hints

After you run a query, you can see performance-related information about how it actually ran by issuing the SUMMARY command in impala-shell. Prior to Impala 1.4, you would use the PROFILE command, but its highly technical output was only useful for the most experienced users. SUMMARY, new in Impala 1.4, summarizes the most useful information for all stages of execution, for all nodes rather than splitting out figures for each node

# Admission Control and Query Queuing

Admission control is an Impala feature that imposes limits on concurrent SQL queries, to avoid resource usage spikes and out-of-memory conditions on busy clusters. It is a form of “throttling”. New queries are accepted and executed until certain conditions are met, such as too many queries or too much total memory used across the cluster. When one of these thresholds is reached, incoming queries wait to begin execution. These queries are queued and are admitted (that is, begin executing) when the resources become available

In addition to the threshold values for currently executing queries, you can place limits on the maximum number of queries that are queued (waiting) and a limit on the amount of time they might wait before returning with an error. These queue settings let you ensure that queries do not wait indefinitely, so that you can detect and correct “starvation” scenarios

Enable this feature if your cluster is underutilized at some times and overutilized at others. Overutilization is indicated by performance bottlenecks and queries being cancelled due to out-of-memory conditions, when those same queries are successful and perform well during times with less concurrent load. Admission control works as a safeguard to avoid out-of-memory conditions during heavy concurrent usage

> The use of the Llama component for integrated resource management within YARN is no longer supported with Impala 2.3 and higher. The Llama support code is removed entirely in Impala 2.8 and higher

## Overview of Impala Admission Control

On a busy cluster, you might find there is an optimal number of Impala queries that run concurrently. For example, when the I/O capacity is fully utilized by I/O-intensive queries, you might not find any throughput benefit in running more concurrent queries. By allowing some queries to run at full speed while others wait, rather than having all queries contend for resources and run slowly, admission control can result in higher overall throughput

For another example, consider a memory-bound workload such as many large joins or aggregation queries. Each such query could briefly use many gigabytes of memory to process intermediate results. Because Impala by default cancels queries that exceed the specified memory limit, running multiple large-scale queries at once might require re-running some queries that are cancelled. In this case, admission control improves the reliability and stability of the overall workload by only allowing as many concurrent queries as the overall memory of the cluster can accomodate

The admission control feature lets you set an upper limit on the number of concurrent Impala queries and on the memory used by those queries. Any additional queries are queued until the earlier ones finish, rather than being cancelled or running slowly and causing contention. As other queries finish, the queued queries are allowed to proceed

In Impala 2.5 and higher, you can specify these limits and thresholds for each pool rather than globally. That way, you can balance the resource usage and throughput between steady well-defined workloads, rare resource-intensive queries, and ad hoc exploratory queries

## Concurrent Queries and Admission Control

One way to limit resource usage through admission control is to set an upper limit on the number of concurrent queries. This is the initial technique you might use when you do not have extensive information about memory usage for your workload. This setting can be specified separately for each dynamic resource pool

You can combine this setting with the memory-based approach described in **Memory Limits and Admission Control**. If either the maximum number of or the expected memory usage of the concurrent queries is exceeded, subsequent queries are queued until the concurrent workload falls below the threshold again

## Memory Limits and Admission Control

Each dynamic resource pool can have an upper limit on the cluster-wide memory used by queries executing in that pool. This is the technique to use once you have a stable workload with well-understood memory requirements

Always specify the **Default Query Memory Limit** for the expected maximum amount of RAM that a query might require on **each host**, which is equivalent to setting the `MEM_LIMIT` query option for every query run in that pool. That value affects the execution of each query, preventing it from overallocating memory on each host, and potentially activating the spill-to-disk mechanism or cancelling the query when necessary

Optionally, specify the **Max Memory** setting, a cluster-wide limit that determines how many queries can be safely run concurrently, based on the upper memory limit per host multiplied by the number of Impala nodes in the cluster

For example, consider the following scenario

- The cluster is running impalad daemons on five DataNodes
- A dynamic resource pool has Max Memory set to 100 GB
- The Default Query Memory Limit for the pool is 10 GB. Therefore, any query running in this pool could use up to 50 GB of memory (default query memory limit * number of Impala nodes).
- The maximum number of queries that Impala executes concurrently within this dynamic resource pool is two, which is the most that could be accomodated within the 100 GB Max Memory cluster-wide limit
- There is no memory penalty if queries use less memory than the Default Query Memory Limit per-host setting or the Max Memory cluster-wide limit. These values are only used to estimate how many queries can be run concurrently within the resource constraints for the pool

> If you specify Max Memory for an Impala dynamic resource pool, you must also specify the Default Query Memory Limit. Max Memory relies on the Default Query Memory Limit to produce a reliable estimate of overall memory consumption for a query


To avoid a large backlog of queued requests, you can set an upper limit on the size of the queue for queries that are queued. When the number of queued queries exceeds this limit, further queries are cancelled rather than being queued. You can also configure a timeout period per pool, after which queued queries are cancelled, to avoid indefinite waits. If a cluster reaches this state where queries are cancelled due to too many concurrent requests or long waits for query execution to begin, that is a signal for an administrator to take action, either by provisioning more resources, scheduling work on the cluster to smooth out the load, or by doing Impala performance tuning to enable higher throughput

## How Admission Control works with Impala Clients

Most aspects of admission control work transparently with client interfaces such as JDBC and ODBC
- If a SQL statement is put into a queue rather than running immediately, the API call blocks until the statement is dequeued and begins execution. At that point, the client program can request to fetch results, which might also block until results become available
- If a SQL statement is cancelled because it has been queued for too long or because it exceeded the memory limit during execution, the error is returned to the client program with a descriptive error message

At any time, the set of queued queries could include queries submitted through multiple different Impala daemon hosts. All the queries submitted through a particular host will be executed in order, so a CREATE TABLE followed by an INSERT on the same table would succeed

Queries submitted through different hosts are not guaranteed to be executed in the order they were received. Therefore, if you are using load-balancing or other round-robin scheduling where different statements are submitted through different hosts, set up all table structures ahead of time so that the statements controlled by the queuing system are primarily queries, where order is not significant. Or, if a sequence of statements needs to happen in strict order (such as an INSERT followed by a SELECT), submit all those statements through a single session, while connected to the same Impala daemon host

## SQL and Schema Considerations for Admission Control

When queries complete quickly and are tuned for optimal memory usage, there is less chance of performance or capacity problems during times of heavy load. Before setting up admission control, tune your Impala queries to ensure that the query plans are efficient and the memory estimates are accurate. Understanding the nature of your workload, and which queries are the most resource-intensive, helps you to plan how to divide the queries into different pools and decide what limits to define for each pool

For large tables, especially those involved in join queries, keep their statistics up to date after loading substantial amounts of new data or adding new partitions. Use the `COMPUTE STATS` statement for **unpartitioned tables**, and `COMPUTE INCREMENTAL STATS` for **partitioned tables**

When you use dynamic resource pools with a Max Memory setting enabled, you typically override the memory estimates that Impala makes based on the statistics from the `COMPUTE STATS` statement. You either set the `MEM_LIMIT` query option within a particular session to set an upper memory limit for queries within that session, or a default `MEM_LIMIT` setting for all queries processed by the impalad instance, or a default `MEM_LIMIT` setting for all queries assigned to a particular dynamic resource pool. By designating a consistent memory limit for a set of similar queries that use the same resource pool, you avoid unnecessary query queuing or out-of-memory conditions that can arise during high-concurrency workloads when memory estimates for some queries are inaccurate

## Configuring Admission Control

The configuration options for admission control range from the simple (a single resource pool with a single set of options) to the complex (multiple resource pools with different options, each pool handling queries for a different set of users and groups).

### Impala Service Flags for Admission Control (Advanced)

The following Impala configuration options let you adjust the settings of the admission control feature. When supplying the options on the impalad command line, prepend the option name with --

option|purpose|type|default
---|:--:|---:|---:
queue_wait_timeout_ms|Maximum amount of time (in milliseconds) that a request waits to be admitted before timing out|int64|60000
default_pool_max_requests|Maximum number of concurrent outstanding requests allowed to run before incoming requests are queued. Because this limit applies cluster-wide, but each Impala node makes independent decisions to run queries immediately or queue them, it is a soft limit; the overall number of concurrent queries might be slightly higher during times of heavy load. A negative value indicates no limit. Ignored if `fair_scheduler_config_path` and `llama_site_path` are set|int64| -1, meaning unlimited (prior to Impala 2.5 the default was 200)
default_pool_max_queued| Maximum number of requests allowed to be queued before rejecting requests. Because this limit applies cluster-wide, but each Impala node makes independent decisions to run queries immediately or queue them, it is a soft limit; the overall number of queued queries might be slightly higher during times of heavy load. A negative value or 0 indicates requests are always rejected once the maximum concurrent requests are executing. Ignored if `fair_scheduler_config_path` and `llama_site_path` are set|int64|unlimited
default_pool_mem_limit| Maximum amount of memory (across the entire cluster) that all outstanding requests in this pool can use before new requests to this pool are queued. Specified in bytes, megabytes, or gigabytes by a number followed by the suffix b (optional), m, or g, either uppercase or lowercase. You can specify floatingpoint values for megabytes and gigabytes, to represent fractional numbers such as 1.5. You can also specify it as a percentage of the physical memory by specifying the suffix %. 0 or no setting indicates no limit. Defaults to bytes if no unit is given. Because this limit applies cluster-wide, but each Impala node makes independent decisions to run queries immediately or queue them, it is a soft limit; the overall memory used by concurrent queries might be slightly higher during times of heavy load. Ignored if `fair_scheduler_config_path` and `llama_site_path` are set|string|"" (empty string, meaning unlimited)
disable_pool_max_requests|Disables all per-pool limits on the maximum number of running requests|Boolean|false
disable_pool_mem_limits|Disables all per-pool mem limits|Boolean|false
fair_scheduler_allocation_path|Path to the fair scheduler allocation file (fair-scheduler.xml)|string|"" (empty string)
llama_site_path|Path to the configuration file used by admission control (llama-site.xml). If set, fair_scheduler_allocation_path must also be set|string|"" (empty string)

> Impala relies on the statistics produced by the `COMPUTE STATS` statement to estimate memory usage for each query

### Example of Admission Control Configuration

Here are sample `fair-scheduler.xml` file that define resource pools `root.default`, `root.development`, and `root.production`. These sample files are stripped down: in a real deployment they might contain other settings for use with various aspects of the YARN component. The settings shown here are the significant ones for the Impala admission control feature

```xml
<allocations>    
  <queue name="root">        
  <aclSubmitApps> </aclSubmitApps>        
    <queue name="default">            
      <maxResources>50000 mb, 0 vcores</maxResources>
      <aclSubmitApps>*</aclSubmitApps>        
    </queue>        
    <queue name="development">            
      <maxResources>200000 mb, 0 vcores</maxResources>
      <aclSubmitApps>user1,user2 dev,ops,admin</aclSubmitApps>     
    </queue>        
    <queue name="production">            
      <maxResources>1000000 mb, 0 vcores</maxResources>            
      <aclSubmitApps> ops,admin</aclSubmitApps>        
    </queue>    
  </queue>    

  <queuePlacementPolicy>        
    <rule name="specified" create="false"/>        
    <rule name="default" />    
  </queuePlacementPolicy>
</allocations>
```

### Guidelines for Using Admission Control

To see how admission control works for particular queries, examine the profile output for the query. This information is available through the `PROFILE` statement in impala-shell immediately after running a query in the shell, on the queries page of the Impala debug web UI, or in the Impala log file (basic information at log level 1, more detailed information at log level 2). The profile output contains details about the admission decision, such as whether the query was queued or not and which resource pool it was assigned to. It also includes the estimated and actual memory usage for the query, so you can fine-tune the configuration for the memory limits of the resource pools

Remember that the limits imposed by admission control are “soft” limits. The decentralized nature of this mechanism means that each Impala node makes its own decisions about whether to allow queries to run immediately or to queue them. These decisions rely on information passed back and forth between nodes by the statestore service. If a sudden surge in requests causes more queries than anticipated to run concurrently, then throughput could decrease due to queries spilling to disk or contending for resources; or queries could be cancelled if they exceed the `MEM_LIMIT` setting while running

In impala-shell, you can also specify which resource pool to direct queries to by setting the `REQUEST_POOL` query option

The statements affected by the admission control feature are primarily queries, but also include statements that write data such as `INSERT` and `CREATE TABLE AS SELECT`. Most write operations in Impala are not resource-intensive, but inserting into a Parquet table can require substantial memory due to buffering intermediate data before writing out each Parquet data block

Although admission control does not scrutinize memory usage for other kinds of DDL statements, if a query is queued due to a limit on concurrent queries or memory usage, subsequent statements **in the same session** are also queued so that they are processed in the correct order

```sql
-- This query could be queued to avoid out-of-memory at times of heavy load.
select * from huge_table join enormous_table using (id);
-- If so, this subsequent statement in the same session is also queued
-- until the previous statement completes
drop table huge_table;
```

# Resource Management for Impala

The use of the Llama component for integrated resource management within YARN is no longer supported with Impala 2.3 and higher. The Llama support code is removed entirely in Impala 2.8 and higher

## How Resource Limits Are Enforced

Limits on memory usage are enforced by Impala's process memory limit (the MEM_LIMIT query option setting). The admission control feature checks this setting to decide how many queries can be safely run at the same time. Then the Impala daemon enforces the limit by activating the spill-to-disk mechanism when necessary, or cancelling a query altogether if the limit is exceeded at runtime

# Setting Timeout Periods for Daemons, Queries, and Sessions

Depending on how busy your  cluster is, you might increase or decrease various timeout values. Increase timeouts if Impala is cancelling operations prematurely, when the system is responding slower than usual but the operations are still successful if given extra time. Decrease timeouts if operations are idle or hanging for long periods, and the idle or hung operations are consuming resources and reducing concurrency
