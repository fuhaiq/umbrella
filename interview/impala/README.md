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

---

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

> Most considerations for load balancing and high availability apply to the impalad daemon. **The statestored and catalogd daemons do not have special requirements for high availability**, because problems with those daemons do not result in data loss. If those daemons become unavailable due to an outage on a particular host, you can stop the Impala service, delete the Impala StateStore and Impala Catalog Server roles, add the roles on a different host, and restart the Impala service

---

# How Impala Fits Into the Hadoop Ecosystem

## How Impala Works with Hive

In particular, Impala keeps its table definitions in a traditional MySQL or PostgreSQL database known as the metastore, the same database where Hive keeps this type of data. Thus, Impala can access tables defined or loaded by Hive, as long as all columns use Impala-supported data types, file formats, and compression codecs

The Impala query optimizer can also make use of **table statistics** and **column statistics**. In Impala 1.2.2 and higher, use the Impala `COMPUTE STATS` statement

## Overview of Impala Metadata and the Metastore

 Impala maintains information about table definitions in a central database known as the metastore. Impala also tracks other metadata for the low-level characteristics of data files
 - The physical locations of blocks within HDFS


 For tables with a large volume of data and/or many partitions, retrieving all the metadata for a table can be timeconsuming, taking minutes in some cases. Thus, **each impala daemon** subscribes to `Statestore` at startup, acts as a subscriber and retrieve metadata through one kind of topic *(heartbeat is another kind of topic).* Caches all of this metadata to reuse for future queries against the same table
 > metadata = hive table metastore + `COMPUTE STATS` informations

We could shutdown statestore and catalog services after a daemon has been running for a couple of while, and that daemon is still able to retrieve response against existing tables (cahes works). On another hand, when we execute sql in a new impala daemon (no Caches) without statestore and catalog services running, we will get following error
```
This Impala daemon is not ready to accept user requests. Status: Waiting for catalog update from the StateStore.
```
Base on my experiment, impala daemons retrieve metadata and its update-info only from Statestore service (not catalog service) through broadcast topic message. It only talks to catalog directly when table structure changes happen, like: ALTER, DROP, etc.
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

---

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

---

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

---

# Resource Management for Impala

The use of the Llama component for integrated resource management within YARN is no longer supported with Impala 2.3 and higher. The Llama support code is removed entirely in Impala 2.8 and higher

## How Resource Limits Are Enforced

Limits on memory usage are enforced by Impala's process memory limit (the MEM_LIMIT query option setting). The admission control feature checks this setting to decide how many queries can be safely run at the same time. Then the Impala daemon enforces the limit by activating the spill-to-disk mechanism when necessary, or cancelling a query altogether if the limit is exceeded at runtime

---

# Setting Timeout Periods for Daemons, Queries, and Sessions

Depending on how busy your  cluster is, you might increase or decrease various timeout values. Increase timeouts if Impala is cancelling operations prematurely, when the system is responding slower than usual but the operations are still successful if given extra time. Decrease timeouts if operations are idle or hanging for long periods, and the idle or hung operations are consuming resources and reducing concurrency

## Increasing the Statestore Timeout

If you have an extensive Impala schema, for example with hundreds of databases, tens of thousands of tables, and so on, you might encounter timeout errors during startup as the Impala catalog service broadcasts metadata to all the Impala nodes using the statestore service. To avoid such timeout errors on startup, increase the statestore timeout value from its default of 10 seconds. Specify the timeout value using the statestore_subscriber_timeout_seconds option for the statestore service.
```
Connection with state-store lost
Trying to re-register with state-store
```

## Setting the Idle Query and Idle Session Timeouts for impalad

To keep long-running queries or idle sessions from tying up cluster resources, you can set timeout intervals for both individual queries, and entire sessions

The timeout clock for queries and sessions only starts ticking when the query or session is idle. For queries, this means the query has results ready but is waiting for a client to fetch the data. A query can run for an arbitrary time without triggering a timeout, because the query is computing results rather than sitting idle waiting for the results to be fetched. The timeout period is intended to prevent unclosed queries from consuming resources and taking up slots in the admission count of running queries, potentially preventing other queries from starting

For sessions, this means that no query has been submitted for some period of time

Specify the following startup options for the impalad daemon

- The `--idle_query_timeout` option specifies the time in seconds after which an idle query is cancelled. This could be a query whose results were all fetched but was never closed, or one whose results were partially fetched and then the client program stopped requesting further results. This condition is most likely to occur in a client program using the JDBC or ODBC interfaces, rather than in the interactive `impala-shell` interpreter. Once the query is cancelled, the client program cannot retrieve any further results. You can reduce the idle query timeout by using the `QUERY_TIMEOUT_S` query option. Any non-zero value specified for the `--idle_query_timeout` startup option serves as an upper limit for the `QUERY_TIMEOUT_S` query option. A zero value for `--idle_query_timeout` disables query timeouts

- The `--idle_session_timeout` option specifies the time in seconds after which an idle session is expired. A session is idle when no activity is occurring for any of the queries in that session, and the session has not started any new queries. Once a session is expired, you cannot issue any new query requests to it. The session remains open, but the only operation you can perform is to close it. The default value of 0 means that sessions never expire

## Setting Timeout and Retries for Thrift Connections to the Backend Client

Impala connections to the backend client are subject to failure in cases when the network is momentarily overloaded. To avoid failed queries due to transient network problems, you can configure the number of Thrift connection retries using the following option

- The `--backend_client_connection_num_retries` option specifies the number of times Impala will try connecting to the backend client after the first connection attempt fails. By default, impalad will attempt three re-connections before it returns a failure. You can configure timeouts for sending and receiving data from the backend client. Therefore, if for some reason a query hangs, instead of waiting indefinitely for a response, Impala will terminate the connection after a configurable timeout

- The `--backend_client_rpc_timeout_ms` option can be used to specify the number of milliseconds Impala should wait for a response from the backend client before it terminates the connection and signals a failure. The default value for this property is 300000 milliseconds, or 5 minutes

## Cancelling a Query

Sometimes, an Impala query might run for an unexpectedly long time, tying up resources in the cluster. You can cancel the query explicitly, independent of the timeout period, by going into the web UI for the impalad host (on port 25000 by default), and using the link on the `/queries` tab to cancel the running query. For example, press `^C` in `impala-shell`

---

# Managing Disk Space for Impala Data

Although Impala typically works with many large files in an HDFS storage system with plenty of capacity, there are times when you might perform some file cleanup to reclaim space, or advise developers on techniques to minimize space consumption and file duplication

- Use compact binary file formats where practical. Numeric and time-based data in particular can be stored in more compact form in binary data files. Depending on the file format, various compression and encoding features can reduce file size even further. You can specify the `STORED AS` clause as part of the `CREATE TABLE` statement, or `ALTER TABLE` with the `SET FILEFORMAT` clause for an existing table or partition within a partitioned table

- Use the `DESCRIBE FORMATTED` statement to check if a particular table is internal (managed by Impala) or external, and to see the physical location of the data files in HDFS

- Clean up temporary files after failed `INSERT` statements. If an `INSERT` statement encounters an error, and you see a directory named `.impala_insert_staging` or `_impala_insert_staging` left behind in the data directory for the table, it might contain temporary data files taking up space in HDFS. You might be able to salvage these data files, for example if they are complete but could not be moved into place due to a permission error. Or, you might delete those files through commands such as hadoop fs or hdfs dfs, to reclaim space before re-trying the `INSERT`. Issue `DESCRIBE FORMATTED table_name` to see the HDFS path where you can check for temporary files

- By default, intermediate files used during large sort, join, aggregation, or analytic function operations are stored in the directory `/tmp/impala-scratch` . These files are removed when the operation finishes. (Multiple concurrent queries can perform operations that use the “spill to disk” technique, without any name conflicts for these temporary files.) You can specify a different location by starting the impalad daemon with the `-scratch_dirs="path_to_directory"` configuration option. You can specify a single directory, or a comma-separated list of directories. The scratch directories must be on the local filesystem, not in HDFS. You might specify different directory paths for different hosts, depending on the capacity and speed of the available storage devices. In Impala 2.3 or higher, Impala successfully starts (with a warning Impala successfully starts (with a warning written to the log) if it cannot create or read and write files in one of the scratch directories. If there is less than 1 GB free on the filesystem where that directory resides, Impala still runs, but writes a warning message to its log. If Impala encounters an error reading or writing files in a scratch directory during a query, Impala logs the error and the query fails

---

# Auditing Impala Operations

To monitor how Impala data is being used within your organization, ensure that your Impala authorization and authentication policies are effective. To detect attempts at intrusion or unauthorized access to Impala data, you can use the auditing feature in Impala 1.2.1 and higher

-  Enable auditing by including the option `-audit_event_log_dir=directory_path` in your impalad startup options. The log directory must be a local directory on the server, not an HDFS directory
- Decide how many queries will be represented in each audit event log file. By default, Impala starts a new audit event log file every 5000 queries. To specify a different number, include the option `-max_audit_event_log_file_size=number_of_queries` in the impalad startup options
- In Impala 2.9 and higher, you can control how many audit event log files are kept on each host. Specify the option `--max_audit_event_log_files=number_of_log_files` in the impalad startup options. Once the limit is reached, older files are rotated out using the same mechanism as for other Impala log files. The default value for this setting is 0, representing an unlimited number of audit event log files

---

# Impala SQL Language Reference

## SHOW TABLE STATS Statement

The `SHOW TABLE STATS` and `SHOW COLUMN STATS` variants are important for tuning performance and
diagnosing performance issues, especially with the largest tables and the most complex join queries

Any values that are not available (because the `COMPUTE STATS` statement has not been run yet) are displayed as
`-1`

```
Query: show table stats t_dim_business_user
+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------+
| #Rows | #Files | Size    | Bytes Cached | Cache Replication | Format  | Incremental stats | Location                                                                               |
+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------+
| 872   | 1      | 39.67KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_dim_business_user |
+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------+
```


`SHOW TABLE STATS` provides some general information about the table, such as the number of files, overall size
of the data, whether some or all of the data is in the HDFS cache, and the file format, that is useful whether or not
you have run the `COMPUTE STATS` statement. A -1 in the `#Rows output column` indicates that the` COMPUTE
STATS` statement has never been run for this table. If the table is partitioned, `SHOW TABLE STATS` provides this
information for each partition. (It produces the same output as the `SHOW PARTITIONS` statement in this case.)

The output of `SHOW COLUMN STATS` is primarily only useful after the `COMPUTE STATS statement` has been
run on the table. A `-1` in the #Distinct Values output column indicates that the `COMPUTE STATS` statement
has never been run for this table. Currently, Impala always leaves the `#Nulls` column as `-1`, even after `COMPUTE
STATS` has been run

These `SHOW` statements work on actual tables only, not on views

## SHOW COLUMN STATS Statement

The SHOW TABLE STATS and SHOW COLUMN STATS variants are important for tuning performance and
diagnosing performance issues, especially with the largest tables and the most complex join queries
```
Query: show column stats t_dim_org
+---------------+--------+------------------+--------+----------+-------------------+
| Column        | Type   | #Distinct Values | #Nulls | Max Size | Avg Size          |
+---------------+--------+------------------+--------+----------+-------------------+
| org_no        | STRING | 19               | -1     | 6        | 5.684199810028076 |
| org_id_yxgjx  | STRING | 24               | -1     | 8        | 8                 |
| augrp_yxgjx   | STRING | 23               | -1     | 4        | 4                 |
| org_nm        | STRING | 25               | -1     | 48       | 43.68000030517578 |
| short_no      | STRING | 18               | -1     | 2        | 1.894700050354004 |
| short_nm      | STRING | 25               | -1     | 30       | 27.95999908447266 |
| org_type      | STRING | 3                | -1     | 1        | 1                 |
| org_type_desc | STRING | 3                | -1     | 9        | 8.760000228881836 |
| status        | STRING | 1                | -1     | 1        | 1                 |
| status_desc   | STRING | 1                | -1     | 6        | 6                 |
| etl_time      | STRING | 1                | -1     | 10       | 10                |
+---------------+--------+------------------+--------+----------+-------------------+
```

before the
`COMPUTE STATS` statement is run. Impala deduces some information, such as maximum and average size for fixedlength columns, and leaves and unknown values as `-1`

## SHOW PARTITIONS Statement

`SHOW PARTITIONS` displays information about each partition for a partitioned table. (The output is the same as the
`SHOW TABLE STATS` statement, but `SHOW PARTITIONS` only works on a partitioned table.)

## COMPUTE STATS Statement

The `COMPUTE STATS` statement gathers information about volume and distribution of data in a table and all
associated columns and partitions. The information is stored in the metastore database, and used by Impala to help
optimize queries. For example, if Impala can determine that a table is large or small, or has many or few distinct
values it can organize and parallelize the work appropriately for a join query or insert operation

Syntax:
```
COMPUTE STATS [db_name.]table_name [ ( column_list ) ] [TABLESAMPLE
 SYSTEM(percentage) [REPEATABLE(seed)]]
column_list ::= column_name [ , column_name, ... ]
COMPUTE INCREMENTAL STATS [db_name.]table_name [PARTITION (partition_spec)]
partition_spec ::= simple_partition_spec | complex_partition_spec
simple_partition_spec ::= partition_col=constant_value
complex_partition_spec ::= comparison_expression_on_partition_col
```

The `PARTITION` clause is only allowed in combination with the `INCREMENTAL` clause. It is optional for `COMPUTE
INCREMENTAL STATS`, and required for `DROP INCREMENTAL STATS`. Whenever you specify partitions
through the `PARTITION (partition_spec)` clause in a` COMPUTE INCREMENTAL STATS` or `DROP
INCREMENTAL STATS` statement, you must include all the partitioning columns in the specification, and specify
constant values for all the partition key columns

Originally, Impala relied on users to run the Hive `ANALYZE TABLE` statement, but that method of gathering
statistics proved unreliable and difficult to use. The Impala `COMPUTE STATS` statement was built to improve the
reliability and user-friendliness of this operation. `COMPUTE STATS` does not require any setup steps or special
configuration. You only run a single Impala `COMPUTE STATS` statement to gather both table and column statistics,
rather than separate Hive `ANALYZE TABLE` statements for each kind of statistics

For non-incremental `COMPUTE STATS` statement, the columns for which statistics are computed can be specified
with an optional comma-separate list of columns

If no column list is given, the `COMPUTE STATS` statement computes column-level statistics for all columns of the
table. This adds potentially unneeded work for columns whose stats are not needed by queries. It can be especially
costly for very wide tables and unneeded large string fields

`COMPUTE STATS` returns an error when a specified column cannot be analyzed, such as when the column does not
exist, the column is of an unsupported type for `COMPUTE STATS`, e.g. colums of complex types, or the column is a
partitioning column

If an empty column list is given, no column is analyzed by `COMPUTE STATS`

In Impala 2.12 and higher, an optional `TABLESAMPLE` clause immediately after a table reference specifies that the
`COMPUTE STATS` operation only processes a specified percentage of the table data. For tables that are so large that
a full `COMPUTE STATS` operation is impractical, you can use `COMPUTE STATS` with a `TABLESAMPLE` clause to
extrapolate statistics from a sample of the table data. See Table and Column Statisticsabout the experimental stats
extrapolation and sampling features

The `COMPUTE INCREMENTAL STATS` variation is a shortcut for partitioned tables that works on a subset of
partitions rather than the entire table. The incremental nature makes it suitable for large tables with many partitions,
where a full `COMPUTE STATS` operation takes too long to be practical each time a partition is added or dropped

**Important:**

For a particular table, use either `COMPUTE STATS` or `COMPUTE INCREMENTAL STATS`, but never combine the
two or alternate between them. If you switch from `COMPUTE STATS` to `COMPUTE INCREMENTAL STATS` during
the lifetime of a table, or vice versa, drop all statistics by running `DROP STATS` before making the switch

When you run `COMPUTE INCREMENTAL STATS` on a table for the first time, the statistics are computed again
from scratch regardless of whether the table already has statistics. Therefore, expect a one-time resource-intensive
operation for scanning the entire table when running `COMPUTE INCREMENTAL STATS` for the first time on a
given table

For a table with a huge number of partitions and many columns, the approximately **400 bytes** of metadata **per column
per partition** can add up to significant memory overhead, as it must be cached on the catalogd host and on every
impalad host that is eligible to be a coordinator. If this metadata for all tables combined exceeds 2 GB, you might
experience service downtime

`COMPUTE INCREMENTAL STATS` only applies to partitioned tables. If you use the `INCREMENTAL` clause for an
unpartitioned table, Impala automatically uses the original `COMPUTE STATS` statement. Such tables display false
under the Incremental stats column of the `SHOW TABLE STATS` output

**Note:**

Because many of the most performance-critical and resource-intensive operations rely on table and column statistics
to construct accurate and efficient plans, `COMPUTE STATS` is an important step at the end of your ETL process. Run
`COMPUTE STATS` on all tables as your first step during performance tuning for slow queries, or troubleshooting for
out-of-memory conditions

- Accurate statistics help Impala construct an efficient query plan for join queries, improving performance and
reducing memory usage
- Accurate statistics help Impala distribute the work effectively for insert operations into Parquet tables, improving
performance and reducing memory usage
- Accurate statistics help Impala estimate the memory required for each query, which is important when you use
resource management features, such as admission control and the YARN resource management framework. The
statistics help Impala to achieve high concurrency, full utilization of available memory, and avoid contention with
workloads from other Hadoop components
- In Impala 2.8 and higher, when you run the `COMPUTE STATS` or `COMPUTE INCREMENTAL STATS` statement
against a Parquet table, Impala automatically applies the query option setting `MT_DOP=4` to increase the amount
of intra-node parallelism during this CPU-intensive operation

**Computing stats for groups of partitions**

In Impala 2.8 and higher, you can run `COMPUTE INCREMENTAL STATS` on multiple partitions, instead of the
entire table or one partition at a time. You include comparison operators other than = in the `PARTITION` clause, and
the `COMPUTE INCREMENTAL STATS` statement applies to all partitions that match the comparison expression
```sql
drop stats t_mz_sale_d;
[emr-header-1.cluster-100513:21000] > show partitions t_mz_sale_d;
Query: show partitions t_mz_sale_d
+------------+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------------------+
| bill_dat   | #Rows | #Files | Size    | Bytes Cached | Cache Replication | Format  | Incremental stats | Location                                                                                           |
+------------+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------------------+
| 2018-10-1  | -1    | 1      | 77.30KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-1  |
| 2018-10-10 | -1    | 1      | 79.96KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-10 |
| 2018-10-11 | -1    | 1      | 75.27KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-11 |
| 2018-10-12 | -1    | 1      | 72.85KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-12 |
| 2018-10-15 | -1    | 1      | 81.88KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-15 |
| 2018-10-16 | -1    | 1      | 80.01KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-16 |
| 2018-10-17 | -1    | 1      | 82.69KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-17 |
| 2018-10-18 | -1    | 1      | 80.95KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-18 |
| 2018-10-19 | -1    | 1      | 77.10KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-19 |
| 2018-10-2  | -1    | 1      | 75.96KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-2  |
| 2018-10-22 | -1    | 1      | 72.42KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-22 |

[emr-header-1.cluster-100513:21000] > compute incremental stats t_mz_sale_d partition (bill_dat = '2018-10-1');
+------------------------------------------+
| summary                                  |
+------------------------------------------+
| Updated 1 partition(s) and 14 column(s). |
+------------------------------------------+
Fetched 1 row(s) in 1.13s
[emr-header-1.cluster-100513:21000] > show table stats t_mz_sale_d;
Query: show table stats t_mz_sale_d
+------------+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------------------+
| bill_dat   | #Rows | #Files | Size    | Bytes Cached | Cache Replication | Format  | Incremental stats | Location                                                                                           |
+------------+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------------------+
| 2018-10-1  | 3340  | 1      | 77.30KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-1  |
| 2018-10-10 | -1    | 1      | 79.96KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-10 |
| 2018-10-11 | -1    | 1      | 75.27KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-11 |

[emr-header-1.cluster-100513:21000] > compute incremental stats t_mz_sale_d partition (bill_dat between '2018-10-10' and '2018-10-18');
Query: compute incremental stats t_mz_sale_d partition (bill_dat between '2018-10-10' and '2018-10-18')
+------------------------------------------+
| summary                                  |
+------------------------------------------+
| Updated 7 partition(s) and 14 column(s). |
+------------------------------------------+
Fetched 1 row(s) in 0.95s
[emr-header-1.cluster-100513:21000] > show table stats t_mz_sale_d;
Query: show table stats t_mz_sale_d
+------------+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------------------+
| bill_dat   | #Rows | #Files | Size    | Bytes Cached | Cache Replication | Format  | Incremental stats | Location                                                                                           |
+------------+-------+--------+---------+--------------+-------------------+---------+-------------------+----------------------------------------------------------------------------------------------------+
| 2018-10-1  | 3340  | 1      | 77.30KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-1  |
| 2018-10-10 | 3474  | 1      | 79.96KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-10 |
| 2018-10-11 | 3258  | 1      | 75.27KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-11 |
| 2018-10-12 | 3144  | 1      | 72.85KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-12 |
| 2018-10-15 | 3528  | 1      | 81.88KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-15 |
| 2018-10-16 | 3446  | 1      | 80.01KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-16 |
| 2018-10-17 | 3563  | 1      | 82.69KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-17 |
| 2018-10-18 | 3485  | 1      | 80.95KB | NOT CACHED   | NOT CACHED        | PARQUET | true              | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-18 |
| 2018-10-19 | -1    | 1      | 77.10KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-19 |
| 2018-10-2  | -1    | 1      | 75.96KB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://emr-header-1.cluster-100513:9000/user/hive/warehouse/dwd.db/t_mz_sale_d/bill_dat=2018-10-2  |

-- scans the whole table with all partitions
Query: compute incremental stats t_mz_sale_d
+--------------------------------------------+
| summary                                    |
+--------------------------------------------+
| Updated 123 partition(s) and 14 column(s). |
+--------------------------------------------+
```

**Internal details**

Behind the scenes, the `COMPUTE STATS` statement executes two statements: one to count the rows of each
partition in the table (or the entire table if unpartitioned) through the `COUNT(*)` function, and another to count the
approximate number of distinct values in each column through the `NDV()` function. You might see these queries in
your monitoring and diagnostic displays. The same factors that affect the performance, scalability, and execution of
other queries (such as parallel execution, memory usage, admission control, and timeouts) also apply to the queries
run by the `COMPUTE STATS` statement

## DROP STATS Statement

Removes the specified statistics from a table or partition. The statistics were originally created by the `COMPUTE
STATS` or `COMPUTE INCREMENTAL STATS` statement

**Syntax**

```sql
DROP STATS [database_name.]table_name
DROP INCREMENTAL STATS [database_name.]table_name PARTITION (partition_spec)
partition_spec ::= partition_col=constant_value
```

## EXPLAIN Statement

Returns the execution plan for a statement, showing the low-level mechanisms that Impala will use to read the data,
divide the work among nodes in the cluster, and transmit intermediate and final results across the network. Use
explain followed by a complete `SELECT` query

**Syntax**

```
EXPLAIN { select_query | ctas_stmt | insert_stmt }
```

**Usage notes**

You can interpret the output to judge whether the query is performing efficiently, and adjust the query and/or the
schema if not. For example, you might change the tests in the `WHERE` clause, add hints to make join operations more
efficient, introduce subqueries, change the order of tables in a join, add or change partitioning for a table, collect
column statistics and/or table statistics in Hive, or any other performance tuning steps

The `EXPLAIN` output reminds you if table or column statistics are missing from any table involved in the query.
These statistics are important for optimizing queries involving large tables or multi-table joins

Read the `EXPLAIN` plan from bottom to top

- The last part of the plan shows the low-level details such as the expected amount of data that will be read, where
you can judge the effectiveness of your partitioning strategy and estimate how long it will take to scan a table
based on total data size and the size of the cluster
- As you work your way up, next you see the operations that will be parallelized and performed on each Impala
node
- At the higher levels, you see how data flows when intermediate result sets are combined and transmitted from one
node to another
- `EXPLAIN_LEVEL` lets you customize how much detail to show in the `EXPLAIN` plan depending on whether you are doing high-level
or low-level tuning, dealing with logical or physical aspects of the query

If you come from a traditional database background and are not familiar with data warehousing, keep in mind that
Impala is optimized for full table scans across very large tables. The structure and distribution of this data is typically
**not suitable for** the kind of indexing and single-row lookups that are common in **OLTP environments**. Seeing a query
scan entirely through a large table is common, not necessarily an indication of an inefficient query. Of course, if you
can reduce the volume of scanned data by orders of magnitude, for example by using a query that affects only certain
partitions within a partitioned table, then you might be able to optimize a query so that it executes in seconds rather
than minutes

**Extended EXPLAIN output**

For performance tuning of complex queries, and capacity planning (such as using the admission control and resource
management features), you can enable more detailed and informative output for the `EXPLAIN` statement. In the
impala-shell interpreter, issue the command `SET EXPLAIN_LEVEL=level`, where level is an integer from 0
to 3 or corresponding mnemonic values `minimal`, `standard`, `extended`, or `verbose`

When extended `EXPLAIN` output is enabled, `EXPLAIN` statements print information about estimated memory
requirements, minimum number of virtual cores, and so on.

```sql
[emr-header-1.cluster-100513:21000] > explain select ord_id from t_mz_sale_d where bill_dat = '2018-9-24' group by ord_id;
Query: explain select ord_id from t_mz_sale_d where bill_dat = '2018-9-24' group by ord_id
+--------------------------------------------------+
| Explain String                                   |
+--------------------------------------------------+
| Max Per-Host Resource Reservation: Memory=3.94MB |
| Per-Host Resource Estimates: Memory=36.00MB      |
| Codegen disabled by planner                      |
|                                                  |
| PLAN-ROOT SINK                                   |
| |                                                |
| 04:EXCHANGE [UNPARTITIONED]                      |
| |                                                |
| 03:AGGREGATE [FINALIZE]                          |
| |  group by: ord_id                              |
| |                                                |
| 02:EXCHANGE [HASH(ord_id)]                       |
| |                                                |
| 01:AGGREGATE [STREAMING]                         |
| |  group by: ord_id                              |
| |                                                |
| 00:SCAN HDFS [dwd.t_mz_sale_d]                   |
|    partitions=1/123 files=1 size=75.18KB         |
+--------------------------------------------------+

[emr-header-1.cluster-100513:21000] > set explain_level=extended;
EXPLAIN_LEVEL set to extended
[emr-header-1.cluster-100513:21000] > explain select ord_id from t_mz_sale_d where bill_dat = '2018-9-24' group by ord_id;
Query: explain select ord_id from t_mz_sale_d where bill_dat = '2018-9-24' group by ord_id
+---------------------------------------------------------------------+
| Explain String                                                      |
+---------------------------------------------------------------------+
| Max Per-Host Resource Reservation: Memory=3.94MB                    |
| Per-Host Resource Estimates: Memory=36.00MB                         |
| Codegen disabled by planner                                         |
|                                                                     |
| F02:PLAN FRAGMENT [UNPARTITIONED] hosts=1 instances=1               |
| |  Per-Host Resources: mem-estimate=0B mem-reservation=0B           |
| PLAN-ROOT SINK                                                      |
| |  mem-estimate=0B mem-reservation=0B                               |
| |                                                                   |
| 04:EXCHANGE [UNPARTITIONED]                                         |
| |  mem-estimate=0B mem-reservation=0B                               |
| |  tuple-ids=1 row-size=24B cardinality=3215                        |
| |                                                                   |
| F01:PLAN FRAGMENT [HASH(ord_id)] hosts=1 instances=1                |
| Per-Host Resources: mem-estimate=10.00MB mem-reservation=1.94MB     |
| 03:AGGREGATE [FINALIZE]                                             |
| |  group by: ord_id                                                 |
| |  mem-estimate=10.00MB mem-reservation=1.94MB spill-buffer=64.00KB |
| |  tuple-ids=1 row-size=24B cardinality=3215                        |
| |                                                                   |
| 02:EXCHANGE [HASH(ord_id)]                                          |
| |  mem-estimate=0B mem-reservation=0B                               |
| |  tuple-ids=1 row-size=24B cardinality=3215                        |
| |                                                                   |
| F00:PLAN FRAGMENT [RANDOM] hosts=1 instances=1                      |
| Per-Host Resources: mem-estimate=26.00MB mem-reservation=2.00MB     |
| 01:AGGREGATE [STREAMING]                                            |
| |  group by: ord_id                                                 |
| |  mem-estimate=10.00MB mem-reservation=2.00MB spill-buffer=64.00KB |
| |  tuple-ids=1 row-size=24B cardinality=3215                        |
| |                                                                   |
| 00:SCAN HDFS [dwd.t_mz_sale_d, RANDOM]                              |
|    partitions=1/123 files=1 size=75.18KB                            |
|    stored statistics:                                               |
|      table: rows=386521 size=8.80MB                                 |
|      partitions: 1/1 rows=3215                                      |
|      columns: all                                                   |
|    extrapolated-rows=disabled                                       |
|    mem-estimate=16.00MB mem-reservation=0B                          |
|    tuple-ids=0 row-size=24B cardinality=3215                        |
+---------------------------------------------------------------------+
```

## UPSERT Statement (Impala 2.8 or higher only)

> Warning: This statement **only works** for Impala tables that use the **Kudu** storage engine

Acts as a combination of the `INSERT` and `UPDATE` statements. For each row processed by the `UPSERT` statement

- If another row already exists with the same set of primary key values, the other columns are updated to match the
values from the row being "UPSERTed"

- If there is not any row with the same set of primary key values, the row is created, the same as if the `INSERT`
statement was used

**Syntax**

```sql
UPSERT [hint_clause] INTO [TABLE] [db_name.]table_name
 [(column_list)]
{
 [hint_clause] select_statement
 | VALUES (value [, value ...]) [, (value [, value ...]) ...]
}
```

## Optimizer Hints

The Impala SQL supports query hints, for fine-tuning the inner workings of queries. Specify hints as a temporary
workaround for expensive queries, where missing statistics or other factors cause inefficient performance.

Hints are most often used for the resource-intensive Impala queries, such as

- Join queries involving large tables, where intermediate result sets are transmitted across the network to evaluate
the join conditions

- Inserting into partitioned Parquet tables, where many memory buffers could be allocated on each host to hold
intermediate results for each partition

**Syntax**

```sql
SELECT STRAIGHT_JOIN select_list FROM
join_left_hand_table
 JOIN /* +BROADCAST|SHUFFLE */
join_right_hand_table
remainder_of_query;

SELECT select_list FROM
join_left_hand_table
 JOIN -- +BROADCAST|SHUFFLE
join_right_hand_table
remainder_of_query;

INSERT insert_clauses
 /* +SHUFFLE|NOSHUFFLE */
 SELECT remainder_of_query;
...
```
With both forms of hint syntax, include the `STRAIGHT_JOIN` keyword immediately after the `SELECT` and any
`DISTINCT` or `ALL` keywords to prevent Impala from reordering the tables in a way that makes the join-related hints
ineffective

The `STRAIGHT_JOIN` hint affects the join order of table references in the query block containing the hint. It does
not affect the join order of nested queries, such as views, inline views, or `WHERE`-clause subqueries. To use this hint
for performance tuning of complex queries, apply the hint to all query blocks that need a fixed join order

To reduce the need to use hints, run the `COMPUTE STATS` statement against all tables involved in joins, or used as
the source tables for `INSERT ... SELECT` operations where the destination is a partitioned Parquet table. Do this
operation after loading data or making substantial changes to the data within each table. Having up-to-date statistics
helps Impala choose more efficient query plans without the need for hinting

In particular, the `/* +BROADCAST */` and `/* +SHUFFLE */` hints **are expected to be needed much less
frequently** in Impala 1.2.2 and higher, because the join order optimization feature in combination with the `COMPUTE
STATS` statement now automatically choose join order and join mechanism without the need to rewrite the query and
add hints

---

# Tuning Impala for Performance

The following sections explain the factors affecting the performance of Impala features, and procedures for tuning,
monitoring, and benchmarking Impala queries and other SQL operations

This section also describes techniques for maximizing Impala scalability. Scalability is tied to performance: it means
that performance remains high as the system workload increases. For example, reducing the disk I/O performed
by a query can speed up an individual query, and at the same time improve scalability by making it practical to run
more queries simultaneously. Sometimes, an optimization technique improves scalability more than performance.
For example, reducing memory usage for a query might not change the query performance much, but might improve
scalability by allowing more Impala queries or other kinds of jobs to run at the same time without running out of
memory.

**Subjects**

- Partitioning for Impala Tables. This technique physically divides the data based on the different
values in frequently queried columns, allowing queries to skip reading a large percentage of the data in a table

- Performance Considerations for Join Queries. Joins are the main class of queries that you can tune
at the SQL level, as opposed to changing physical factors such as the file format or the hardware configuration.
The related topics Overview of Column Statistics and Overview of Table Statistics
are also important primarily for join performance

- Overview of Table Statistics and Overview of Column Statistics. Gathering
table and column statistics, using the COMPUTE STATS statement, helps Impala automatically optimize the
performance for join queries, without requiring changes to SQL query statements. (This process is greatly
simplified in Impala 1.2.2 and higher, because the `COMPUTE STATS` statement gathers both kinds of statistics
in one operation, and does not require any setup and configuration as was previously necessary for the `ANALYZE
TABLE` statement in Hive.)

- Testing Impala Performance. Do some post-setup testing to ensure Impala is using optimal
settings for performance, before conducting any benchmark tests

- Benchmarking Impala Queries. The configuration and sample data that you use for initial
experiments with Impala is often not appropriate for doing performance tests

- Controlling Impala Resource Usage. The more memory Impala can utilize, the better query
performance you can expect. In a cluster running other kinds of workloads as well, you must make tradeoffs to
make sure all Hadoop components have enough memory to perform well, so you might cap the memory that
Impala can use

## Impala Performance Guidelines and Best Practices

Here are performance guidelines and best practices that you can use during planning, experimentation, and
performance tuning for an Impala-enabled cluster. All of this information is also available in more detail elsewhere
in the Impala documentation; it is gathered together here to serve as a cookbook and emphasize which performance
techniques typically provide the highest return on investment

### Choose the appropriate file format for the data

Typically, for large volumes of data (multiple gigabytes per table or partition), the Parquet file format performs
best because of its combination of columnar storage layout, large I/O request size, and compression and encoding

### Avoid data ingestion processes that produce many small files

When producing data files outside of Impala, prefer either text format or Avro, where you can build up the files row
by row. Once the data is in Impala, you can convert it to the more efficient Parquet format and split into multiple data
files using a single `INSERT ... SELECT` statement. Or, if you have the infrastructure to produce multi-megabyte
Parquet files as part of your data preparation process, do that and skip the conversion step inside Impala

Always use `INSERT ... SELECT` to copy significant volumes of data from table to table within Impala.
Avoid `INSERT ... VALUES` for any substantial volume of data or performance-critical tables, because each
such statement produces a separate tiny data file

For example, if you have thousands of partitions in a Parquet table, each with less than 256 MB of data, consider
partitioning in a less granular way, such as by year / month rather than year / month / day. If an inefficient data
ingestion process produces thousands of data files in the same table or partition, consider compacting the data by
performing an `INSERT ... SELECT` to copy all the data to a different table; the data will be reorganized into a
smaller number of larger files by this process

### Choose partitioning granularity based on actual data volume

Partitioning is a technique that physically divides the data based on values of one or more columns, such as by year,
month, day, region, city, section of a web site, and so on. When you issue queries that request a specific value or
range of values for the partition key columns, Impala can avoid reading the irrelevant data, potentially yielding a huge
savings in disk I/O

When deciding which column(s) to use for partitioning, choose the right level of granularity. For example, should you
partition by year, month, and day, or only by year and month? Choose a partitioning strategy that puts at least 256 MB
of data in each partition, to take advantage of HDFS bulk I/O and Impala distributed queries

Over-partitioning can also cause query planning to take longer than necessary, as Impala prunes the unnecessary
partitions. Ideally, keep the number of partitions in the table under 30 thousand

When preparing data files to go in a partition directory, create several large files rather than many small ones. If
you receive data in the form of many small files and have no control over the input format, consider using the
`INSERT ... SELECT` syntax to copy data from one table or partition to another, which compacts the files into a
relatively small number (based on the number of nodes in the cluster)

If you need to reduce the overall number of partitions and increase the amount of data in each partition, first look for
partition key columns that are rarely referenced or are referenced in non-critical queries (not subject to an SLA). For
example, your web site log data might be partitioned by year, month, day, and hour, but if most queries roll up the
results by day, perhaps you only need to partition by year, month, and day

If you need to reduce the granularity even more, consider creating "buckets", computed values corresponding to
different sets of partition key values. For example, you can use the `TRUNC()` function with a `TIMESTAMP` column to
group date and time values based on intervals such as week or quarter

### Use smallest appropriate integer types for partition key columns

Although it is tempting to use strings for partition key columns, since those values are turned into HDFS directory
names anyway, you can minimize memory usage by using numeric values for common partition key fields such as
YEAR, MONTH, and DAY. Use the smallest integer type that holds the appropriate range of values, typically `TINYINT`
for MONTH and DAY, and `SMALLINT` for YEAR. Use the `EXTRACT()` function to pull out individual date and time
fields from a `TIMESTAMP` value, and `CAST()` the return value to the appropriate integer type

### Choose an appropriate Parquet block size

By default, the Impala `INSERT ... SELECT` statement creates Parquet files with a 256 MB block size. (This
default was changed in Impala 2.0. Formerly, the limit was 1 GB, but Impala made conservative estimates about
compression, resulting in files that were smaller than 1 GB.)

Each Parquet file written by Impala is a single block, allowing the whole file to be processed as a unit by a single
host. As you copy Parquet files into HDFS or between HDFS filesystems, use `hdfs dfs -pb` to preserve the
original block size

If there is only one or a few data block in your Parquet table, or in a partition that is the only one accessed by a query,
then you might experience a slowdown for a different reason: not enough data to take advantage of Impala's parallel
distributed queries. Each data block is processed by a single core on one of the DataNodes. In a 100-node cluster of
16-core machines, you could potentially process thousands of data files simultaneously. You want to find a sweet
spot between "many tiny files" and "single giant file" that balances bulk I/O and parallel processing. You can set the
`PARQUET_FILE_SIZE` query option before doing an `INSERT ... SELECT` statement to reduce the size of each
generated Parquet file. (Specify the file size as an absolute number of bytes, or in Impala 2.0 and later, in units ending
with m for megabytes or g for gigabytes.) Run benchmarks with different file sizes to find the right balance point for
your particular data volume

### Gather statistics for all tables used in performance-critical or high-volume join queries

Gather the statistics with the `COMPUTE STATS` statement

### Minimize the overhead of transmitting results back to the client

- Aggregation
- Filtering
- `LIMIT` clause

### Verify that your queries are planned in an efficient logical manner

Examine the `EXPLAIN` plan for a query before actually running it

## Performance Considerations for Join Queries

Queries involving join operations often require more tuning than queries that refer to only one table. The maximum
size of the result set from a join query is the product of the number of rows in all the joined tables. When joining
several tables with millions or billions of rows, any missed opportunity to filter the result set, or other inefficiency in
the query, could lead to an operation that does not finish in a practical time and has to be cancelled

The simplest technique for tuning an Impala join query is to collect statistics on each table involved in the join
using the `COMPUTE STATS` statement, and then let Impala automatically optimize the query based on the size of
each table, number of distinct values of each column, and so on. The `COMPUTE STATS` statement and the join
optimization are new features introduced in Impala 1.2.2. For accurate statistics about each table, issue the `COMPUTE
STATS` statement after loading the data into that table, and again if the amount of data changes substantially due to an
`INSERT`, `LOAD DATA`, adding a partition, and so on

If statistics are not available for all the tables in the join query, or if Impala chooses a join order that is not the most
efficient, you can override the automatic join order optimization by specifying the `STRAIGHT_JOIN` keyword immediately after the `SELECT` and any `DISTINCT` or `ALL` keywords. In this case, Impala uses the order the tables
appear in the query to guide how the joins are processed

When you use the `STRAIGHT_JOIN` technique, you must order the tables in the join query manually instead of
relying on the Impala optimizer. The optimizer uses sophisticated techniques to estimate the size of the result set at
each stage of the join. For manual ordering, use this heuristic approach to start with, and then experiment to fine-tune
the order

- Specify the largest table first. This table is read from disk by each Impala node and so its size is not significant in
terms of memory usage during the query
- Next, specify the smallest table. The contents of the second, third, and so on tables are all transmitted across the
network. You want to minimize the size of the result set from each subsequent stage of the join query. The most
likely approach involves joining a small table first, so that the result set remains small even as subsequent larger
tables are processed
- Join the next smallest table, then the next smallest, and so on
- For example, if you had tables BIG, MEDIUM, SMALL, and TINY, the logical join order to try would be BIG,
TINY, SMALL, MEDIUM

The terms "largest" and "smallest" refers to the size of the intermediate result set based on the number of rows and
columns from each table that are part of the result set. For example, if you join one table sales with another table
customers, a query might find results from 100 different customers who made a total of 5000 purchases. In that
case, you would specify `SELECT ... FROM sales JOIN customers ...,` putting customers on the
right side because it is smaller in the context of this query

The Impala query planner chooses between different techniques for performing join queries, depending on the
absolute and relative sizes of the tables. **Broadcast joins** are the default, where the right-hand table is considered
to be smaller than the left-hand table, and its contents are sent to all the other nodes involved in the query. The
alternative technique is known as a **Partitioned join** (not related to a partitioned table), which is more suitable for
large tables of roughly equal size. With this technique, portions of each table are sent to appropriate other nodes
where those subsets of rows can be processed in parallel. The choice of broadcast or partitioned join also depends on
statistics being available for all tables in the join, gathered by the `COMPUTE STATS` statement

To see which join strategy is used for a particular query, issue an `EXPLAIN` statement for the query. If you find that a
query uses a broadcast join when you know through benchmarking that a partitioned join would be more efficient, or
vice versa, add a hint to the query to specify the precise join mechanism to use

> add query hint mannually in join query is less used, consider to use `COMPUTE STATS`, impala would optimize join query efficiently and automatically

### How Joins Are Processed when Statistics Are Unavailable

If table or column statistics are not available for some tables in a join, Impala still reorders the tables using the
information that is available. Tables with statistics are placed on the left side of the join order, in descending order
of cost based on overall size and cardinality. Tables without statistics are treated as zero-size, that is, they are always
placed on the right side of the join order

### Overriding Join Reordering with `STRAIGHT_JOIN`

If an Impala join query is inefficient because of outdated statistics or unexpected data distribution, you can keep
Impala from reordering the joined tables by using the `STRAIGHT_JOIN` keyword immediately after the `SELECT`
and any `DISTINCT` or `ALL` keywords. The `STRAIGHT_JOIN` keyword turns off the reordering of join clauses that
Impala does internally, and produces a plan that relies on the join clauses being ordered optimally in the query text.
In this case, rewrite the query so that the largest table is on the left, followed by the next largest, and so on until the
smallest table is on the right

**Note**

The STRAIGHT_JOIN hint affects the join order of table references in the query block containing the hint. It does
not affect the join order of nested queries, such as views, inline views, or WHERE-clause subqueries. To use this hint
for performance tuning of complex queries, apply the hint to all query blocks that need a fixed join order

In this example, the subselect from the BIG table produces a very small result set, but the table might still be treated
as if it were the biggest and placed first in the join order. Using `STRAIGHT_JOIN` for the last join clause prevents
the final table from being reordered, keeping it as the rightmost table in the join order

```sql
select straight_join x from medium join small join (select * from big where c1 < 10) as big
where medium.id = small.id and small.id = big.id;

 -- If the query contains [DISTINCT | ALL], the hint goes after those keywords.
select distinct straight_join x from medium join small join (select * from big where c1 < 10) as big
where medium.id = small.id and small.id = big.id;
```

### Examples of Join Order Optimization

Here are examples showing joins between tables with 1 billion, 200 million, and 1 million rows. (In this case, the
tables are unpartitioned and using Parquet format.) The smaller tables contain subsets of data from the largest one,
for convenience of joining on the unique ID column. The smallest table only contains a subset of columns from the
others

```sql
[localhost:21000] > create table big stored as parquet as select * from raw_data;
+----------------------------+
| summary                    |
+----------------------------+
| Inserted 1000000000 row(s) |
+----------------------------+
Returned 1 row(s) in 671.56s
[localhost:21000] > desc big;
+-----------+---------+---------+
| name      | type    | comment |
+-----------+---------+---------+
| id        | int     |         |
| val       | int     |         |
| zfill     | string  |         |
| name      | string  |         |
| assertion | boolean |         |
+-----------+---------+---------+
Returned 5 row(s) in 0.01s
[localhost:21000] > create table medium stored as parquet as select * from big limit 200 * floor(1e6);
+---------------------------+
| summary                   |
+---------------------------+
| Inserted 200000000 row(s) |
+---------------------------+
Returned 1 row(s) in 138.31s
[localhost:21000] > create table small stored as parquet as select id,val,name from big where assertion = true limit 1 * floor(1e6);
+-------------------------+
| summary                 |
+-------------------------+
| Inserted 1000000 row(s) |
+-------------------------+
Returned 1 row(s) in 6.32s


```

For any kind of performance experimentation, use the EXPLAIN statement to see how any expensive query will be
performed without actually running it, and enable verbose EXPLAIN plans containing more performance-oriented
detail: The most interesting plan lines are highlighted in bold, showing that without statistics for the joined tables, Impala cannot make a good estimate of the number of rows involved at each stage of processing, and is likely to stick
with the `BROADCAST` join mechanism that sends a complete copy of one of the tables to each node

```sql
[localhost:21000] > set explain_level=verbose;
EXPLAIN_LEVEL set to verbose
[localhost:21000] > explain select count(*) from big join medium where big.id = medium.id;
+----------------------------------------------------------+
| Explain String                                           |
+----------------------------------------------------------+
| Estimated Per-Host Requirements: Memory=2.10GB VCores=2  |
|                                                          |
| PLAN FRAGMENT 0                                          |
|   PARTITION: UNPARTITIONED                               |
|                                                          |
|   6:AGGREGATE (merge finalize)                           |
|   |  output: SUM(COUNT(*))                               |
|   |  cardinality: 1                                      |
|   |  per-host memory: unavailable                        |
|   |  tuple ids: 2                                        |
|   |                                                      |
|   5:EXCHANGE                                             |
|      cardinality: 1                                      |
|      per-host memory: unavailable                        |
|      tuple ids: 2                                        |
|                                                          |
| PLAN FRAGMENT 1                                          |
|   PARTITION: RANDOM                                      |
|                                                          |
|   STREAM DATA SINK                                       |
|     EXCHANGE ID: 5                                       |
|     UNPARTITIONED                                        |
|                                                          |
|   3:AGGREGATE                                            |
|   |  output: COUNT(*)                                    |
|   |  cardinality: 1                                      |
|   |  per-host memory: 10.00MB                            |
|   |  tuple ids: 2                                        |
|   |                                                      |
|   2:HASH JOIN                                            |
|   |  join op: INNER JOIN (BROADCAST)                     |
|   |  hash predicates:                                    |
|   |    big.id = medium.id                                |
|   |  cardinality: unavailable                            |
|   |  per-host memory: 2.00GB                             |
|   |  tuple ids: 0 1                                      |
|   |                                                      |
|   |----4:EXCHANGE                                        |
|   |       cardinality: unavailable                       |
|   |       per-host memory: 0B                            |
|   |       tuple ids: 1                                   |
|   |                                                      |
|   0:SCAN HDFS                                            |
|      table=join_order.big #partitions=1/1 size=23.12GB   |
|      table stats: unavailable                            |
|      column stats: unavailable                           |
|      cardinality: unavailable                            |
|      per-host memory: 88.00MB                            |
|      tuple ids: 0                                        |
|                                                          |
| PLAN FRAGMENT 2                                          |
|   PARTITION: RANDOM                                      |
|                                                          |
|   STREAM DATA SINK                                       |
|     EXCHANGE ID: 4                                       |
|     UNPARTITIONED                                        |
|                                                          |
|   1:SCAN HDFS                                            |
|      table=join_order.medium #partitions=1/1 size=4.62GB |
|      table stats: unavailable                            |
|      column stats: unavailable                           |
|      cardinality: unavailable                            |
|      per-host memory: 88.00MB                            |
|      tuple ids: 1                                        |
+----------------------------------------------------------+
Returned 64 row(s) in 0.04s
```

Gathering statistics for all the tables is straightforward, one `COMPUTE STATS` statement per table

```sql
[localhost:21000] > compute stats small;
+-----------------------------------------+
| summary                                 |
+-----------------------------------------+
| Updated 1 partition(s) and 3 column(s). |
+-----------------------------------------+
Returned 1 row(s) in 4.26s
[localhost:21000] > compute stats medium;
+-----------------------------------------+
| summary                                 |
+-----------------------------------------+
| Updated 1 partition(s) and 5 column(s). |
+-----------------------------------------+
Returned 1 row(s) in 42.11s
[localhost:21000] > compute stats big;
+-----------------------------------------+
| summary                                 |
+-----------------------------------------+
| Updated 1 partition(s) and 5 column(s). |
+-----------------------------------------+
Returned 1 row(s) in 165.44s
```

With statistics in place, Impala can choose a more effective join order rather than following the left-to-right sequence
of tables in the query, and can choose `BROADCAST` or `PARTITIONED` join strategies based on the overall sizes and
number of rows in the table

```sql
[localhost:21000] > explain select count(*) from medium join big where big.id = medium.id;
Query: explain select count(*) from medium join big where big.id = medium.id
+-----------------------------------------------------------+
| Explain String                                            |
+-----------------------------------------------------------+
| Estimated Per-Host Requirements: Memory=937.23MB VCores=2 |
|                                                           |
| PLAN FRAGMENT 0                                           |
|   PARTITION: UNPARTITIONED                                |
|                                                           |
|   6:AGGREGATE (merge finalize)                            |
|   |  output: SUM(COUNT(*))                                |
|   |  cardinality: 1                                       |
|   |  per-host memory: unavailable                         |
|   |  tuple ids: 2                                         |
|   |                                                       |
|   5:EXCHANGE                                              |
|      cardinality: 1                                       |
|      per-host memory: unavailable                         |
|      tuple ids: 2                                         |
|                                                           |
| PLAN FRAGMENT 1                                           |
|   PARTITION: RANDOM                                       |
|                                                           |
|   STREAM DATA SINK                                        |
|     EXCHANGE ID: 5                                        |
|     UNPARTITIONED                                         |
|                                                           |
|   3:AGGREGATE                                             |
|   |  output: COUNT(*)                                     |
|   |  cardinality: 1                                       |
|   |  per-host memory: 10.00MB                             |
|   |  tuple ids: 2                                         |
|   |                                                       |
|   2:HASH JOIN                                             |
|   |  join op: INNER JOIN (BROADCAST)                      |
|   |  hash predicates:                                     |
|   |    big.id = medium.id                                 |
|   |  cardinality: 1443004441                              |
|   |  per-host memory: 839.23MB                            |
|   |  tuple ids: 1 0                                       |
|   |                                                       |
|   |----4:EXCHANGE                                         |
|   |       cardinality: 200000000                          |
|   |       per-host memory: 0B                             |
|   |       tuple ids: 0                                    |
|   |                                                       |
|   1:SCAN HDFS                                             |
|      table=join_order.big #partitions=1/1 size=23.12GB    |
|      table stats: 1000000000 rows total                   |
|      column stats: all                                    |
|      cardinality: 1000000000                              |
|      per-host memory: 88.00MB                             |
|      tuple ids: 1                                         |
|                                                           |
| PLAN FRAGMENT 2                                           |
|   PARTITION: RANDOM                                       |
|                                                           |
|   STREAM DATA SINK                                        |
|     EXCHANGE ID: 4                                        |
|     UNPARTITIONED                                         |
|                                                           |
|   0:SCAN HDFS                                             |
|      table=join_order.medium #partitions=1/1 size=4.62GB  |
|      table stats: 200000000 rows total                    |
|      column stats: all                                    |
|      cardinality: 200000000                               |
|      per-host memory: 88.00MB                             |
|      tuple ids: 0                                         |
+-----------------------------------------------------------+
Returned 64 row(s) in 0.04s

[localhost:21000] > explain select count(*) from small join big where big.id = small.id;
Query: explain select count(*) from small join big where big.id = small.id
+-----------------------------------------------------------+
| Explain String                                            |
+-----------------------------------------------------------+
| Estimated Per-Host Requirements: Memory=101.15MB VCores=2 |
|                                                           |
| PLAN FRAGMENT 0                                           |
|   PARTITION: UNPARTITIONED                                |
|                                                           |
|   6:AGGREGATE (merge finalize)                            |
|   |  output: SUM(COUNT(*))                                |
|   |  cardinality: 1                                       |
|   |  per-host memory: unavailable                         |
|   |  tuple ids: 2                                         |
|   |                                                       |
|   5:EXCHANGE                                              |
|      cardinality: 1                                       |
|      per-host memory: unavailable                         |
|      tuple ids: 2                                         |
|                                                           |
| PLAN FRAGMENT 1                                           |
|   PARTITION: RANDOM                                       |
|                                                           |
|   STREAM DATA SINK                                        |
|     EXCHANGE ID: 5                                        |
|     UNPARTITIONED                                         |
|                                                           |
|   3:AGGREGATE                                             |
|   |  output: COUNT(*)                                     |
|   |  cardinality: 1                                       |
|   |  per-host memory: 10.00MB                             |
|   |  tuple ids: 2                                         |
|   |                                                       |
|   2:HASH JOIN                                             |
|   |  join op: INNER JOIN (BROADCAST)                      |
|   |  hash predicates:                                     |
|   |    big.id = small.id                                  |
|   |  cardinality: 1000000000                              |
|   |  per-host memory: 3.15MB                              |
|   |  tuple ids: 1 0                                       |
|   |                                                       |
|   |----4:EXCHANGE                                         |
|   |       cardinality: 1000000                            |
|   |       per-host memory: 0B                             |
|   |       tuple ids: 0                                    |
|   |                                                       |
|   1:SCAN HDFS                                             |
|      table=join_order.big #partitions=1/1 size=23.12GB    |
|      table stats: 1000000000 rows total                   |
|      column stats: all                                    |
|      cardinality: 1000000000                              |
|      per-host memory: 88.00MB                             |
|      tuple ids: 1                                         |
|                                                           |
| PLAN FRAGMENT 2                                           |
|   PARTITION: RANDOM                                       |
|                                                           |
|   STREAM DATA SINK                                        |
|     EXCHANGE ID: 4                                        |
|     UNPARTITIONED                                         |
|                                                           |
|   0:SCAN HDFS                                             |
|      table=join_order.small #partitions=1/1 size=17.93MB  |
|      table stats: 1000000 rows total                      |
|      column stats: all                                    |
|      cardinality: 1000000                                 |
|      per-host memory: 32.00MB                             |
|      tuple ids: 0                                         |
+-----------------------------------------------------------+
Returned 64 row(s) in 0.03s
```

When queries like these are actually run, the execution times are relatively consistent regardless of the table order in
the query text.

```sql
[localhost:21000] > select count(*) from big join small on (big.id = small.id);
Query: select count(*) from big join small on (big.id = small.id)
+----------+
| count(*) |
+----------+
| 1000000  |
+----------+
Returned 1 row(s) in 21.68s
[localhost:21000] > select count(*) from small join big on (big.id = small.id);
Query: select count(*) from small join big on (big.id = small.id)
+----------+
| count(*) |
+----------+
| 1000000  |
+----------+
Returned 1 row(s) in 20.45s
```

## Table and Column Statistics

Impala can do better optimization for complex or multi-table queries when it has access to statistics about the volume
of data and how the values are distributed. Impala uses this information to help parallelize and distribute the work
for a query. For example, optimizing join queries requires a way of determining if one table is "bigger" than another,
which is a function of the number of rows and the average row size for each table. The following sections describe the
categories of statistics Impala can work with, and how to produce them and keep them up to date

### Overview of Table Statistics

The Impala query planner can make use of statistics about entire tables and partitions. This information includes
physical characteristics such as the number of rows, number of data files, the total size of the data files, and the
file format. For partitioned tables, the numbers are calculated per partition, and as totals for the whole table. This
metadata is stored in the metastore database, and can be updated by either Impala or Hive. If a number is not available, the value -1 is used as a placeholder. Some numbers, such as number and total sizes of data files, are always
kept up to date because they can be calculated cheaply, as part of gathering HDFS block metadata

The following example shows table stats for an unpartitioned Parquet table. The values for the number and sizes of
files are always available. Initially, the number of rows is not known, because it requires a potentially expensive scan
through the entire table, and so that value is displayed as -1. The `COMPUTE STATS` statement fills in any unknown table stats values

```sql
[iZ11syxr6afZ:21000] > show table stats big;   
Query: show table stats big
+-------+--------+--------+--------------+-------------------+---------+-------------------+-------------------------------------------------------------------+
| #Rows | #Files | Size   | Bytes Cached | Cache Replication | Format  | Incremental stats | Location                                                          |
+-------+--------+--------+--------------+-------------------+---------+-------------------+-------------------------------------------------------------------+
| -1    | 1      | 2.41MB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://iZ1151z4vvnZ:8020/aliyun/user/hive/warehouse/testdb.db/big |
+-------+--------+--------+--------------+-------------------+---------+-------------------+-------------------------------------------------------------------+
Fetched 1 row(s) in 0.01s
[iZ11syxr6afZ:21000] > compute stats big;
Query: compute stats big
+-----------------------------------------+
| summary                                 |
+-----------------------------------------+
| Updated 1 partition(s) and 5 column(s). |
+-----------------------------------------+
Fetched 1 row(s) in 0.51s
[iZ11syxr6afZ:21000] > show table stats big;
Query: show table stats big
+--------+--------+--------+--------------+-------------------+---------+-------------------+-------------------------------------------------------------------+
| #Rows  | #Files | Size   | Bytes Cached | Cache Replication | Format  | Incremental stats | Location                                                          |
+--------+--------+--------+--------------+-------------------+---------+-------------------+-------------------------------------------------------------------+
| 100000 | 1      | 2.41MB | NOT CACHED   | NOT CACHED        | PARQUET | false             | hdfs://iZ1151z4vvnZ:8020/aliyun/user/hive/warehouse/testdb.db/big |
+--------+--------+--------+--------------+-------------------+---------+-------------------+-------------------------------------------------------------------+
Fetched 1 row(s) in 0.01s
```

Impala performs some optimizations using this metadata on its own, and other optimizations by using a combination
of table and column statistics

To check that table statistics are available for a table, and see the details of those statistics, use the statement `SHOW TABLE STATS table_name`

### Overview of Column Statistics

The Impala query planner can make use of statistics about individual columns when that metadata is available in
the metastore database. This technique is most valuable for columns compared across tables in join queries, to help
estimate how many rows the query will retrieve from each table. These statistics are also important for correlated subqueries using the `EXISTS()` or `IN()` operators, which are processed internally the same way as join queries

The following example shows column stats for an unpartitioned Parquet table. The values for the maximum and
average sizes of some types are always available, because those figures are constant for numeric and other fixed-size
types. Initially, the number of distinct values is not known, because it requires a potentially expensive scan through
the entire table, and so that value is displayed as `-1`. The same applies to maximum and average sizes of variable-sized
types, such as `STRING`. The `COMPUTE STATS` statement fills in most unknown column stats values. (It does not
record the number of NULL values, because currently Impala does not use that figure for query optimization.)

```sql
[iZ11syxr6afZ:21000] > show column stats big;
Query: show column stats big
+------------+--------+------------------+--------+----------+----------+
| Column     | Type   | #Distinct Values | #Nulls | Max Size | Avg Size |
+------------+--------+------------------+--------+----------+----------+
| id         | INT    | -1               | -1     | 4        | 4        |
| name       | STRING | -1               | -1     | -1       | -1       |
| address    | STRING | -1               | -1     | -1       | -1       |
| company    | STRING | -1               | -1     | -1       | -1       |
| department | STRING | -1               | -1     | -1       | -1       |
+------------+--------+------------------+--------+----------+----------+
Fetched 5 row(s) in 0.01s
[iZ11syxr6afZ:21000] > compute stats big;
Query: compute stats big
+-----------------------------------------+
| summary                                 |
+-----------------------------------------+
| Updated 1 partition(s) and 5 column(s). |
+-----------------------------------------+
Fetched 1 row(s) in 0.51s
[iZ11syxr6afZ:21000] > show column stats big;
Query: show column stats big
+------------+--------+------------------+--------+----------+-------------------+
| Column     | Type   | #Distinct Values | #Nulls | Max Size | Avg Size          |
+------------+--------+------------------+--------+----------+-------------------+
| id         | INT    | 100762           | -1     | 4        | 4                 |
| name       | STRING | 102144           | -1     | 7        | 5.888999938964844 |
| address    | STRING | 98166            | -1     | 7        | 5.888999938964844 |
| company    | STRING | 96413            | -1     | 7        | 5.888999938964844 |
| department | STRING | 103224           | -1     | 7        | 5.888999938964844 |
+------------+--------+------------------+--------+----------+-------------------+
Fetched 5 row(s) in 0.01s
```

To check that column statistics are available for a table, and see the details of those statistics, use the statement `SHOW COLUMN STATS table_name`

### How Table and Column Statistics Work for Partitioned Tables

When you use Impala for "big data", you are highly likely to use partitioning for your biggest tables, the ones
representing data that can be logically divided based on dates, geographic regions, or similar criteria. The table and
column statistics are especially useful for optimizing queries on such tables. For example, a query involving one year
might involve substantially more or less data than a query involving a different year, or a range of several years. Each
query might be optimized differently as a result

### Generating Table and Column Statistics

Use the `COMPUTE STATS` family of commands to collect table and column statistics. The `COMPUTE STATS`
variants offer different tradeoffs between computation cost, staleness, and maintenance workflows which are
explained below

> For a particular table, use either `COMPUTE STATS` or `COMPUTE INCREMENTAL STATS`, but never combine the
two or alternate between them. If you switch from `COMPUTE STATS` to `COMPUTE INCREMENTAL STATS` during
the lifetime of a table, or vice versa, drop all statistics by running `DROP STATS` before making the switch

**COMPUTE STATS**

The `COMPUTE STATS` command collects and sets the table-level and partition-level row counts as well as all column
statistics for a given table. The collection process is CPU-intensive and can take a long time to complete for very large
tables

To speed up `COMPUTE STATS` consider the following options which can be combined

- Limit the number of columns for which statistics are collected to increase the efficiency of `COMPUTE STATS`.
Queries benefit from statistics for those columns involved in filters, join conditions, group by or partition by
clauses. Other columns are good candidates to exclude from `COMPUTE STATS`. This feature is available since Impala 2.12
- Set the `MT_DOP` query option to use more threads within each participating impalad to compute the statistics
faster - but not more efficiently. Note that computing stats on a large table with a high `MT_DOP` value can
negatively affect other queries running at the same time if the `COMPUTE STATS` claims most CPU cycles. This feature is available since Impala 2.8
- Consider the experimental extrapolation and sampling features (see below) to further increase the efficiency of computing stats

`COMPUTE STATS` is intended to be run periodically, e.g. weekly, or on-demand when the contents of a table have
changed significantly. Due to the high resource utilization and long repsonse time of to `COMPUTE STATS`, it is
most practical to run it in a scheduled maintnance window where the Impala cluster is idle enough to accommodate
the expensive operation. The degree of change that qualifies as “significant” depends on the query workload, but
typically, if 30% of the rows have changed then it is recommended to recompute statistics

> If you reload a complete new set of data for a table, but the number of rows and number of distinct values for each
column is relatively unchanged from before, you do not need to recompute stats for the table

**COMPUTE INCREMENTAL STATS**

In Impala 2.1.0 and higher, you can use the `COMPUTE INCREMENTAL STATS` and `DROP INCREMENTAL
STATS` commands. The INCREMENTAL clauses work with incremental statistics, a specialized feature for partitioned tables

When you compute incremental statistics for a partitioned table, by default Impala only processes those partitions that
do not yet have incremental statistics. By processing only newly added partitions, you can keep statistics up to date
without incurring the overhead of reprocessing the entire table each time

You can also compute or drop statistics for a specified subset of partitions by including a `PARTITION` clause in the
`COMPUTE INCREMENTAL STATS` or `DROP INCREMENTAL STATS` statement

> For a table with a huge number of partitions and many columns, the approximately 400 bytes of metadata per column
per partition can add up to significant memory overhead, as it must be cached on the catalogd host and on every
impalad host that is eligible to be a coordinator. If this metadata for all tables combined exceeds 2 GB, you might
experience service downtime

When you run `COMPUTE INCREMENTAL STATS` on a table for the first time, the statistics are computed again
from scratch regardless of whether the table already has statistics. Therefore, expect a one-time resource-intensive
operation for scanning the entire table when running `COMPUTE INCREMENTAL STATS` for the first time on a given table

- Issuing a `COMPUTE INCREMENTAL STATS` without a partition clause causes Impala to compute incremental
stats for all partitions that do not already have incremental stats. This might be the entire table when running
the command for the first time, but subsequent runs should only update new partitions. You can force updating
a partition that already has incremental stats by issuing a `DROP INCREMENTAL STATS` before running `COMPUTE INCREMENTAL STATS`

- The `SHOW TABLE STATS` and `SHOW PARTITIONS` statements now include an additional column showing
whether incremental statistics are available for each column. A partition could already be covered by the original
type of statistics based on a prior `COMPUTE STATS` statement, as indicated by a value other than -1 under the
`#Rows` column. Impala query planning uses either kind of statistics when available

- `COMPUTE INCREMENTAL STATS` takes more time than `COMPUTE STATS` for the same volume of data.
Therefore it is most suitable for tables with large data volume where new partitions are added frequently, making
it impractical to run a full `COMPUTE STATS` operation for each new partition. For unpartitioned tables, or
partitioned tables that are loaded once and not updated with new partitions, use the original `COMPUTE STATS` syntax

- `COMPUTE INCREMENTAL STATS` uses some memory in the catalogd process, proportional to the number
of partitions and number of columns in the applicable table. The memory overhead is approximately 400 bytes for
each column in each partition. This memory is reserved in the catalogd daemon, the statestored daemon,
and in each instance of the impalad daemon

- In cases where new files are added to an existing partition, issue a `REFRESH` statement for the table, followed by a
`DROP INCREMENTAL STATS` and `COMPUTE INCREMENTAL STATS` sequence for the changed partition

- The `DROP INCREMENTAL STATS` statement operates only on a single partition at a time. To remove statistics (whether incremental or not) from all partitions of a table, issue a` DROP STATS` statement with no
`INCREMENTAL` or `PARTITION` clauses

### Manually Setting Table and Column Statistics with ALTER TABLE

> In practice, the `COMPUTE STATS` statement, or `COMPUTE INCREMENTAL STATS` for a partitioned table, should
be fast and convenient enough that this technique is only useful for the very largest partitioned tables. Because the
column statistics might be left in a stale state, do not use this technique as a replacement for `COMPUTE STATS`. Only
use this technique if all other means of collecting statistics are impractical, or as a low-overhead operation that you
run in between periodic `COMPUTE STATS` or `COMPUTE INCREMENTAL STATS` operations

##### Setting Table Statistics

The most crucial piece of data in all the statistics is the number of rows in the table (for an unpartitioned or partitioned
table) and for each partition (for a partitioned table). The `COMPUTE STATS` statement always gathers statistics
about all columns, as well as overall table statistics. If it is not practical to do a full `COMPUTE STATS` or `COMPUTE
INCREMENTAL STATS` operation after adding a partition or inserting data, or if you can see that Impala would
produce a more efficient plan if the number of rows was different, you can manually set the number of rows through
an `ALTER TABLE` statement

```sql
-- Set total number of rows. Applies to both unpartitioned and partitioned tables.
alter table table_name set tblproperties('numRows'='new_value', 'STATS_GENERATED_VIA_STATS_TASK'='true');
-- Set total number of rows for a specific partition. Applies to partitioned tables only.
-- You must specify all the partition key columns in the PARTITION clause.
alter table table_name partition (keycol1=val1,keycol2=val2...) set
 tblproperties('numRows'='new_value', 'STATS_GENERATED_VIA_STATS_TASK'='true');
```

This statement avoids re-scanning any data files.

```SQL
create table analysis_data stored as parquet as select * from raw_data;
compute stats analysis_data;
insert into analysis_data select * from smaller_table_we_forgot_before;
-- Now there are 1001000000 rows. We can update this single data point in the stats.
alter table analysis_data set tblproperties('numRows'='1001000000', 'STATS_GENERATED_VIA_STATS_TASK'='true');
```

For a partitioned table, update both the per-partition number of rows and the number of rows for the whole table

```SQL
-- If the table originally contained 1 million rows, and we add another partition with 30 thousand rows,
-- change the numRows property for the partition and the overall table.
alter table partitioned_data partition(year=2009, month=4) set tblproperties ('numRows'='30000', 'STATS_GENERATED_VIA_STATS_TASK'='true');
alter table partitioned_data set tblproperties ('numRows'='1030000', 'STATS_GENERATED_VIA_STATS_TASK'='true');
```

#### Setting Column Statistics

You specify a case-insensitive symbolic name for the kind of statistics: numDVs, numNulls, avgSize, maxSize.
The key names and values are both quoted. This operation applies to an entire table, not a specific partition. For
example

```SQL
create table t1 (x int, s string);
insert into t1 values (1, 'one'), (2, 'two'), (2, 'deux');
show column stats t1;
+--------+--------+------------------+--------+----------+----------+
| Column | Type   | #Distinct Values | #Nulls | Max Size | Avg Size |
+--------+--------+------------------+--------+----------+----------+
| x      | INT    | -1               | -1     | 4        | 4        |
| s      | STRING | -1               | -1     | -1       | -1       |
+--------+--------+------------------+--------+----------+----------+
alter table t1 set column stats x ('numDVs'='2','numNulls'='0');
alter table t1 set column stats s ('numdvs'='3','maxsize'='4');
show column stats t1;
+--------+--------+------------------+--------+----------+----------+
| Column | Type   | #Distinct Values | #Nulls | Max Size | Avg Size |
+--------+--------+------------------+--------+----------+----------+
| x      | INT    | 2                | 0      | 4        | 4        |
| s      | STRING | 3                | -1     | 4        | -1       |
+--------+--------+------------------+--------+----------+----------+
```

---

# Runtime Filtering for Impala Queries (Impala 2.5 or higher only)

Runtime filtering is a wide-ranging optimization feature available in Impala 2.5 and higher. When only a fraction of
the data in a table is needed for a query against a partitioned table or to evaluate a join condition, Impala determines
the appropriate conditions while the query is running, and broadcasts that information to all the impalad nodes that
are reading the table so that they can avoid unnecessary I/O to read partition data, and avoid unnecessary network
transmission by sending only the subset of rows that match the join keys across the network

This feature is primarily used to optimize queries against large partitioned tables (under the name dynamic partition
pruning) and joins of large tables. The information in this section includes concepts, internals, and troubleshooting
information for the entire runtime filtering feature

## Background Information for Runtime Filtering

To understand how runtime filtering works at a detailed level, you must be familiar with some terminology from the
field of distributed database technology

- What a ***plan fragment*** is. Impala decomposes each query into smaller units of work that are distributed across the cluster. Wherever possible, a data block is read, filtered, and aggregated by plan fragments executing on the
same host. For some operations, such as joins and combining intermediate results into a final result set, data is transmitted across the network from one DataNode to another

- What `SCAN` and `HASH JOIN` plan nodes are, and their role in computing query results:

  - In the Impala query plan, a scan node performs the I/O to read from the underlying data files. Although this is an
  expensive operation from the traditional database perspective, Hadoop clusters and Impala are optimized to do this
  kind of I/O in a highly parallel fashion. The major potential cost savings come from using the columnar Parquet
  format (where Impala can avoid reading data for unneeded columns) and partitioned tables (where Impala can
  avoid reading data for unneeded partitions)

  - Most Impala joins use the hash join mechanism. (It is only fairly recently that Impala started using the nested-loop
  join technique, for certain kinds of non-equijoin queries.) In a hash join, when evaluating join conditions from two
  tables, Impala constructs a hash table in memory with all the different column values from the table on one side of
  the join. Then, for each row from the table on the other side of the join, Impala tests whether the relevant column
  values are in this hash table or not

  - A hash join node constructs such an in-memory hash table, then performs the comparisons to identify which rows
  match the relevant join conditions and should be included in the result set (or at least sent on to the subsequent
  intermediate stage of query processing). Because some of the input for a hash join might be transmitted across the
  network from another host, it is especially important from a performance perspective to prune out ahead of time
  any data that is known to be irrelevant

  - The more distinct values are in the columns used as join keys, the larger the in-memory hash table and thus the
  more memory required to process the query

- The difference between a broadcast join and a shuffle join. (The Hadoop notion of a shuffle join is sometimes
referred to in Impala as a partitioned join.) In a broadcast join, the table from one side of the join (typically the
smaller table) is sent in its entirety to all the hosts involved in the query. Then each host can compare its portion
of the data from the other (larger) table against the full set of possible join keys. In a shuffle join, there is no
obvious “smaller” table, and so the contents of both tables are divided up, and corresponding portions of the data
are transmitted to each host involved in the query

- The notion of the build phase and probe phase when Impala processes a join query. The build phase is where
the rows containing the join key columns, typically for the smaller table, are transmitted across the network and
built into an in-memory hash table data structure on one or more destination nodes. The probe phase is where
data is read locally (typically from the larger table) and the join key columns are compared to the values in the inmemory hash table. The corresponding input sources (tables, subqueries, and so on) for these phases are referred
to as the build side and the probe side

## Runtime Filtering Internals

The filter that is transmitted between plan fragments is essentially a list of values for join key columns. When this list
is values is transmitted in time to a scan node, Impala can filter out non-matching values immediately after reading
them, rather than transmitting the raw data to another host to compare against the in-memory hash table on that host.

For HDFS-based tables, this data structure is implemented as a ***Bloom filter***, which uses a probability-based algorithm
to determine all possible matching values. (The probability-based aspects means that the filter might include some
non-matching values, but if so, that does not cause any inaccuracy in the final results.)

Another kind of filter is the “min-max” filter. It currently **only applies to Kudu tables**. The filter is a data structure
representing a minimum and maximum value. These filters are passed to Kudu to reduce the number of rows returned
to Impala when scanning the probe side of the join

There are different kinds of filters to match the different kinds of joins (partitioned and broadcast). A broadcast filter
reflects the complete list of relevant values and can be immediately evaluated by a scan node. A partitioned filter
reflects only the values processed by one host in the cluster; all the partitioned filters must be combined into one (by
the coordinator node) before the scan nodes can use the results to accurately filter the data as it is read from storage

Broadcast filters are also classified as local or global. With a local broadcast filter, the information in the filter is used
by a subsequent query fragment that is running on the same host that produced the filter. A non-local broadcast filter
must be transmitted across the network to a query fragment that is running on a different host. Impala designates 3 hosts to each produce non-local broadcast filters, to guard against the possibility of a single slow host taking too long.
Depending on the setting of the `RUNTIME_FILTER_MODE` query option (`LOCAL` or `GLOBAL`), Impala either uses
a conservative optimization strategy where filters are only consumed on the same host that produced them, or a more
aggressive strategy where filters are eligible to be transmitted across the network

## File Format Considerations for Runtime Filtering

Parquet tables get the most benefit from the runtime filtering optimizations. Runtime filtering can speed up join
queries against partitioned or unpartitioned Parquet tables, and single-table queries against partitioned Parquet tables

For other file formats (text, Avro, RCFile, and SequenceFile), runtime filtering speeds up queries against partitioned
tables only. Because partitioned tables can use a mixture of formats, Impala produces the filters in all cases, even if
they are not ultimately used to optimize the query

## Wait Intervals for Runtime Filters

Because it takes time to produce runtime filters, especially for partitioned filters that must be combined by the
coordinator node, there is a time interval above which it is more efficient for the scan nodes to go ahead and construct
their intermediate result sets, even if that intermediate data is larger than optimal. If it only takes a few seconds to
produce the filters, it is worth the extra time if pruning the unnecessary data can save minutes in the overall query
time. You can specify the maximum wait time in milliseconds using the `RUNTIME_FILTER_WAIT_TIME_MS` query option

By default, each scan node waits for up to 1 second (1000 milliseconds) for filters to arrive. If all filters have not
arrived within the specified interval, the scan node proceeds, using whatever filters did arrive to help avoid reading
unnecessary data. If a filter arrives after the scan node begins reading data, the scan node applies that filter to the data
that is read after the filter arrives, but not to the data that was already read

If the cluster is relatively busy and your workload contains many resource-intensive or long-running queries, consider
increasing the wait time so that complicated queries do not miss opportunities for optimization. If the cluster is lightly
loaded and your workload contains many small queries taking only a few seconds, consider decreasing the wait time
to avoid the 1 second delay for each query

## Runtime Filtering and Query Plans

In the same way the query plan displayed by the EXPLAIN statement includes information about predicates
used by each plan fragment, it also includes annotations showing whether a plan fragment produces or consumes
a runtime filter. A plan fragment that produces a filter includes an annotation such as `runtime filters: filter_id <- table.column`, while a plan fragment that consumes a filter includes an annotation such as
`runtime filters: filter_id -> table.column`. Setting the query option `EXPLAIN_LEVEL=2`
adds additional annotations showing the type of the filter, either `filter_id[bloom]` (for HDFS-based tables) or
`filter_id[min_max]` (for Kudu tables).

```SQL
[iZ11syxr6afZ:21000] > set EXPLAIN_LEVEL=2;
EXPLAIN_LEVEL set to 2
[iZ11syxr6afZ:21000] > explain select * from big where id in (select id from small where id between 2000 and 3000);
Query: explain select * from big where id in (select id from small where id between 2000 and 3000)
+-------------------------------------------------------------+
| Explain String                                              |
+-------------------------------------------------------------+
| Estimated Per-Host Requirements: Memory=80.00MB VCores=2    |
|                                                             |
| PLAN-ROOT SINK                                              |
| |                                                           |
| 04:EXCHANGE [UNPARTITIONED]                                 |
| |  hosts=1 per-host-mem=unavailable                         |
| |  tuple-ids=0 row-size=92B cardinality=100                 |
| |                                                           |
| 02:HASH JOIN [LEFT SEMI JOIN, BROADCAST]                    |
| |  hash predicates: id = id                                 |
| |  runtime filters: RF000 <- id                             |
| |  hosts=1 per-host-mem=441B                                |
| |  tuple-ids=0 row-size=92B cardinality=100                 |
| |                                                           |
| |--03:EXCHANGE [BROADCAST]                                  |
| |  |  hosts=1 per-host-mem=0B                               |
| |  |  tuple-ids=1 row-size=4B cardinality=100               |
| |  |                                                        |
| |  01:SCAN HDFS [testdb.small, RANDOM]                      |
| |     partitions=1/1 files=1 size=26.56KB                   |
| |     predicates: id <= 3000, id >= 2000                    |
| |     table stats: 1000 rows total                          |
| |     column stats: all                                     |
| |     hosts=1 per-host-mem=16.00MB                          |
| |     tuple-ids=1 row-size=4B cardinality=100               |
| |                                                           |
| 00:SCAN HDFS [testdb.big, RANDOM]                           |
|    partitions=1/1 files=1 size=2.41MB                       |
|    predicates: testdb.big.id <= 3000, testdb.big.id >= 2000 |
|    runtime filters: RF000 -> id                             |
|    table stats: 100000 rows total                           |
|    column stats: all                                        |
|    hosts=1 per-host-mem=80.00MB                             |
|    tuple-ids=0 row-size=92B cardinality=10000               |
+-------------------------------------------------------------+
```

## Limitations and Restrictions for Runtime Filtering

The runtime filtering feature is most effective for the Parquet file formats. For other file formats, filtering only applies
for partitioned tables.

When the `spill-to-disk` mechanism is activated on a particular host during a query, that host does not produce any
filters while processing that query. This limitation does not affect the correctness of results; it only reduces the
amount of optimization that can be applied to the query.

---
# Using HDFS Caching with Impala (Impala 2.1 or higher only)

HDFS caching provides performance and scalability benefits in production environments where Impala queries and
other Hadoop jobs operate on quantities of data much larger than the physical RAM on the DataNodes, making it
impractical to rely on the Linux OS cache, which only keeps the most recently used data in memory. Data read from
the HDFS cache avoids the overhead of checksumming and memory-to-memory copying involved when using data
from the Linux OS cache

**Note**

On a small or lightly loaded cluster, HDFS caching might not produce any speedup. It might even lead to slower
queries, if I/O read operations that were performed in parallel across the entire cluster are replaced by in-memory
operations operating on a smaller number of hosts. The hosts where the HDFS blocks are cached can become
bottlenecks because they experience high CPU load while processing the cached data blocks, while other hosts remain
idle. Therefore, always compare performance with and without this feature enabled, using a realistic workload

In Impala 2.2 and higher, you can spread the CPU load more evenly by specifying the `WITH REPLICATION` clause
of the `CREATE TABLE` and `ALTER TABLE` statements. This clause lets you control the replication factor for HDFS
caching for a specific table or partition. By default, each cached block is only present on a single host, which can
lead to CPU contention if the same host processes each cached block. Increasing the replication factor lets Impala
choose different hosts to process different cached blocks, to better distribute the CPU load. Always use a `WITH
REPLICATION` setting of at least 3, and adjust upward if necessary to match the replication factor for the underlying
HDFS data files

## Overview of HDFS Caching for Impala

Impala can use the HDFS caching feature to make more effective use of RAM, so that
repeated queries can take advantage of data “pinned” in memory regardless of how much data is processed overall.
The HDFS caching feature lets you designate a subset of frequently accessed data to be pinned permanently in
memory, remaining in the cache across multiple queries and never being evicted. This technique ***is suitable for tables
or partitions that are frequently accessed and are small enough to fit entirely within the HDFS memory cache***. For example, you might designate several dimension tables to be pinned in the cache, to speed up many different join
queries that reference them. Or in a partitioned table, you might pin a partition holding data from the most recent
time period because that data will be queried intensively; then when the next set of data arrives, you could unpin the
previous partition and pin the partition holding the new data.

## Setting Up HDFS Caching for Impala

To use HDFS caching with Impala, first set up that feature for your cluster

- Decide how much memory to devote to the HDFS cache on each host. Remember that the total memory available
for cached data is the sum of the cache sizes on all the hosts. By default, any data block is only cached on one
host, although you can cache a block across multiple hosts by increasing the replication factor

- Issue `hdfs cacheadmin` commands to set up one or more cache pools, owned by the same user as the
impalad daemon (typically impala). For example

```shell
hdfs cacheadmin -addPool four_gig_pool -owner impala -limit 4000000000
```

[Refer to this](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/CentralizedCacheManagement.html) for more hdfs cache usage

## Enabling HDFS Caching for Impala Tables and Partitions

//TODO

---

# Understanding Impala Query Performance - EXPLAIN Plans and Query Profiles

To understand the high-level performance considerations for Impala queries, read the output of the EXPLAIN
statement for the query. You can get the `EXPLAIN` plan without actually running the query itself

For an overview of the physical performance characteristics for a query, issue the `SUMMARY` statement in impalashell immediately after executing a query. This condensed information shows which phases of execution took the
most time, and how the estimates for memory usage and number of rows at each phase compare to the actual values

To understand the detailed performance characteristics for a query, issue the `PROFILE` statement in impalashell immediately after executing a query. This low-level information includes physical details about memory,
CPU, I/O, and network usage, and thus is only available after the query is actually run

## Using the EXPLAIN Plan for Performance Tuning

The `EXPLAIN` statement gives you an outline of the logical steps that a query will perform, such as how the work
will be distributed among the nodes and how intermediate results will be combined to produce the final result set. You
can see these details before actually running the query. You can use this information to check that the query will not
operate in some very unexpected or inefficient way

```sql
[iZ11syxr6afZ:21000] > explain select count(*) from big;
Query: explain select count(*) from big
+----------------------------------------------------------+
| Explain String                                           |
+----------------------------------------------------------+
| Estimated Per-Host Requirements: Memory=10.00MB VCores=1 |
|                                                          |
| PLAN-ROOT SINK                                           |
| |                                                        |
| 03:AGGREGATE [FINALIZE]                                  |
| |  output: count:merge(*)                                |
| |  hosts=1 per-host-mem=unavailable                      |
| |  tuple-ids=1 row-size=8B cardinality=1                 |
| |                                                        |
| 02:EXCHANGE [UNPARTITIONED]                              |
| |  hosts=1 per-host-mem=unavailable                      |
| |  tuple-ids=1 row-size=8B cardinality=1                 |
| |                                                        |
| 01:AGGREGATE                                             |
| |  output: count(*)                                      |
| |  hosts=1 per-host-mem=10.00MB                          |
| |  tuple-ids=1 row-size=8B cardinality=1                 |
| |                                                        |
| 00:SCAN HDFS [testdb.big, RANDOM]                        |
|    partitions=1/1 files=1 size=2.41MB                    |
|    table stats: 100000 rows total                        |
|    column stats: all                                     |
|    hosts=1 per-host-mem=0B                               |
|    tuple-ids=0 row-size=0B cardinality=100000            |
+----------------------------------------------------------+
```

Read the `EXPLAIN` plan from bottom to top

- The last part of the plan shows the low-level details such as the expected amount of data that will be read, where
you can judge the effectiveness of your partitioning strategy and estimate how long it will take to scan a table
based on total data size and the size of the cluster

- As you work your way up, next you see the operations that will be parallelized and performed on each Impala
node

- At the higher levels, you see how data flows when intermediate result sets are combined and transmitted from one
node to another

## Using the SUMMARY Report for Performance Tuning

The `SUMMARY` command within the impala-shell interpreter gives you an easy-to-digest overview of the timings
for the different phases of execution for a query. Like the `EXPLAIN` plan, it is easy to see potential performance
bottlenecks. Like the `PROFILE` output, it is available after the query is run and so displays actual timing numbers

For example, here is a query involving an aggregate function, on a single-node VM. The different stages of the
query and their timings are shown (rolled up for all nodes), along with estimated and actual values used in planning
the query. In this case, the `AVG()` function is computed for a subset of data on each node (stage 01) and then the
aggregated results from all nodes are combined at the end (stage 03). You can see which stages took the most time,
and whether any estimates were substantially different than the actual data distribution. (When examining the time
values, be sure to consider the suffixes such as us for microseconds and ms for milliseconds, rather than just looking
for the largest numbers.)

```sql
[iZ11syxr6afZ:21000] > select avg(id) from big;
Query: select avg(id) from big
Query submitted at: 2019-10-09 11:27:13 (Coordinator: http://iZ11syxr6afZ:25000)
Query progress can be monitored at: http://iZ11syxr6afZ:25000/query_plan?query_id=894e002cdff865e7:a724a5a100000000
+---------+
| avg(id) |
+---------+
| 50000.5 |
+---------+
Fetched 1 row(s) in 0.13s
[iZ11syxr6afZ:21000] > summary;
+--------------+--------+----------+----------+---------+------------+-----------+---------------+---------------+
| Operator     | #Hosts | Avg Time | Max Time | #Rows   | Est. #Rows | Peak Mem  | Est. Peak Mem | Detail        |
+--------------+--------+----------+----------+---------+------------+-----------+---------------+---------------+
| 03:AGGREGATE | 1      | 0ns      | 0ns      | 1       | 1          | 20.00 KB  | -1 B          | FINALIZE      |
| 02:EXCHANGE  | 1      | 0ns      | 0ns      | 1       | 1          | 0 B       | -1 B          | UNPARTITIONED |
| 01:AGGREGATE | 1      | 0ns      | 0ns      | 1       | 1          | 172.25 KB | 10.00 MB      |               |
| 00:SCAN HDFS | 1      | 11.00ms  | 11.00ms  | 100.00K | 100.00K    | 849.34 KB | 16.00 MB      | testdb.big    |
+--------------+--------+----------+----------+---------+------------+-----------+---------------+---------------+
```

---
# Scalability Considerations for Impala

This section explains how the size of your cluster and the volume of data influences SQL performance and schema
design for Impala tables. Typically, adding more cluster capacity reduces problems due to memory limits or disk
throughput. On the other hand, larger clusters are more likely to have other kinds of scalability issues, such as a single
slow node that causes performance problems for queries

## Impact of Many Tables or Partitions on Impala Catalog Performance and Memory Usage

Because Hadoop I/O is optimized for reading and writing large files, Impala is optimized for tables containing
relatively few, large data files. Schemas containing thousands of tables, or tables containing thousands of partitions,
can encounter performance issues during startup or during DDL operations such as ALTER TABLE statements

**Important**

Because of a change in the default heap size for the catalogd daemon in Impala 2.5 and higher, the following
procedure to increase the catalogd memory limit might be required following an upgrade to Impala 2.5 even if not
needed previously

For schemas with large numbers of tables, partitions, and data files, the catalogd daemon might encounter an outof-memory error. To increase the memory limit for the catalogd daemon

1. Check current memory usage for the catalogd daemon by running the following commands on the host where
that daemon runs on your cluster
```Shell
jcmd catalogd_pid VM.flags
jmap -heap catalogd_pid
```

2. Decide on a large enough value for the catalogd heap. You express it as an environment variable value as
follows
```
JAVA_TOOL_OPTIONS="-Xmx8g"
```

3. On systems not using cluster management software, put this environment variable setting into the startup script for
the catalogd daemon, then restart the catalogd daemon

4. Use the same jcmd and jmap commands as earlier to verify that the new settings are in effect

## Controlling which Hosts are Coordinators and Executors

By default, each host in the cluster that runs the impalad daemon can act as the coordinator for an Impala query,
execute the fragments of the execution plan for the query, or both. During highly concurrent workloads for large-scale
queries, especially on large clusters, the dual roles can cause scalability issues

- The extra work required for a host to act as the coordinator could interfere with its capacity to perform other work
for the earlier phases of the query. For example, the coordinator can experience significant network and CPU
overhead during queries containing a large number of query fragments. Each coordinator caches metadata for
all table partitions and data files, which can be substantial and contend with memory needed to process joins,
aggregations, and other operations performed by query executors

- Having a large number of hosts act as coordinators can cause unnecessary network overhead, or even timeout
errors, as each of those hosts communicates with the statestored daemon for metadata updates

- The “soft limits” imposed by the admission control feature are more likely to be exceeded when there are a large
number of heavily loaded hosts acting as coordinators

If such scalability bottlenecks occur, you can explicitly specify that certain hosts act as query coordinators, but not
executors for query fragments. These hosts do not participate in I/O-intensive operations such as scans, and CPUintensive operations such as aggregations

Then, you specify that the other hosts act as executors but not coordinators. These hosts ***do not*** communicate with
the statestored daemon or process the final result sets from queries. You cannot connect to these hosts through
clients such as impala-shell or business intelligence tools

This feature is available in Impala 2.9 and higher. To use this feature, you specify one of the following startup flags for the impalad daemon on each host

- `is_executor=false` for each host that does not act as an executor for Impala queries. These hosts act
exclusively as query coordinators. This setting typically applies to a relatively small number of hosts, because the
most common topology is to have nearly all DataNodes doing work for query execution

- `is_coordinator=false` for each host that does not act as a coordinator for Impala queries. These hosts act
exclusively as executors. The number of hosts with this setting typically increases as the cluster grows larger and
handles more table partitions, data files, and concurrent queries. As the overhead for query coordination increases,
it becomes more important to centralize that work on dedicated hosts

By default, both of these settings are enabled for each impalad instance, allowing all such hosts to act as both
executors and coordinators

For example, on a 100-node cluster, you might specify is_executor=false for 10 hosts, to dedicate those hosts
as query coordinators. Then specify is_coordinator=false for the remaining 90 hosts. All explicit or loadbalanced connections must go to the 10 hosts acting as coordinators. These hosts perform the network communication
to keep metadata up-to-date and route query results to the appropriate clients. The remaining 90 hosts perform the
intensive I/O, CPU, and memory operations that make up the bulk of the work for each query. If a bottleneck or
other performance issue arises on a specific host, you can narrow down the cause more easily because each host is
dedicated to specific operations within the overall Impala workload

## Effect of Buffer Pool on Memory Usage (Impala 2.10 and higher)

The buffer pool feature, available in Impala 2.10 and higher, changes the way Impala allocates memory during a
query. Most of the memory needed is reserved at the beginning of the query, avoiding cases where a query might
run for a long time before failing with an out-of-memory error. The actual memory estimates and memory buffers are typically smaller than before, so that more queries can run concurrently or process larger volumes of data than
previously

Most of the effects of the buffer pool are transparent to you as an Impala user. Memory use during spilling is now
steadier and more predictable, instead of increasing rapidly as more data is spilled to disk. The main change from a
user perspective is the need to increase the MAX_ROW_SIZE query option setting when querying tables with columns
containing long strings, many columns, or other combinations of factors that produce very large rows. If Impala
encounters rows that are too large to process with the default query option settings, the query fails with an error
message suggesting to increase the MAX_ROW_SIZE setting

#### BUFFER_POOL_LIMIT Query Option

Defines a limit on the amount of memory that a query can allocate from the internal buffer pool. The value for this
limit applies to the memory on each host, not the aggregate memory across the cluster. Typically not changed by
users, except during diagnosis of out-of-memory errors during queries

**Type**: `integer`

**Default**: The default setting for this option is the lower of 80% of the `MEM_LIMIT` setting, or the `MEM_LIMIT` setting minus
100 MB

**Usage notes**: If queries encounter out-of-memory errors, consider decreasing the `BUFFER_POOL_LIMIT` setting to less than 80%
of the `MEM_LIMIT` setting

```shell
-- Set an absolute value.
set buffer_pool_limit=8GB;
-- Set a relative value based on the MEM_LIMIT setting.
set buffer_pool_limit=80%;
```

#### MAX_ROW_SIZE Query Option

Ensures that Impala can process rows of at least the specified size. (Larger rows might be successfully processed, but
that is not guaranteed.) Applies when constructing intermediate or final rows in the result set. This setting prevents
out-of-control memory use when accessing columns containing huge strings

**Type**: `integer`

**Default**: 524288 (512 KB)

**Units**: A numeric argument represents a size in bytes; you can also use a suffix of m or mb for megabytes, or g or gb
for gigabytes. If you specify a value with unrecognized formats, subsequent queries fail with an error

**Usage notes**:

If a query fails because it involves rows with long strings and/or many columns, causing the total row size to exceed
`MAX_ROW_SIZE` bytes, increase the `MAX_ROW_SIZE` setting to accommodate the total bytes stored in the largest
row. Examine the error messages for any failed queries to see the size of the row that caused the problem

Impala attempts to handle rows that exceed the `MAX_ROW_SIZE` value where practical, so in many cases, queries
succeed despite having rows that are larger than this setting

Specifying a value that is substantially higher than actually needed can cause Impala to reserve more memory than is
necessary to execute the query

In a Hadoop cluster with highly concurrent workloads and queries that process high volumes of data, traditional SQL
tuning advice about minimizing wasted memory is worth remembering. For example, if a table has STRING columns
where a single value might be multiple megabytes, make sure that the `SELECT` lists in queries only refer to columns
that are actually needed in the result set, instead of using the `SELECT *` shorthand

```sql
create table big_strings (s1 string, s2 string, s3 string) stored as parquet;

select max(length(s1) + length(s2) + length(s3)) / 1e6 as megabytes from big_strings;
```

## SQL Operations that Spill to Disk

Certain memory-intensive operations write temporary data to disk (known as spilling to disk) when Impala is close to
exceeding its memory limit on a particular host

The result is a query that completes successfully, rather than failing with an out-of-memory error. The tradeoff is
decreased performance due to the extra disk I/O to write the temporary data and read it back in. The slowdown could
be potentially be significant. Thus, while this feature improves reliability, you should optimize your queries, system
parameters, and hardware configuration to make this spilling a rare occurrence

**What kinds of queries might spill to disk:**

Several SQL clauses and constructs require memory allocations that could activat the spilling mechanism

- when a query uses a `GROUP BY` clause for columns with millions or billions of distinct values, Impala keeps a
similar number of temporary results in memory, to accumulate the aggregate results for each value in the group

- When large tables are joined together, Impala keeps the values of the join columns from one table in memory, to
compare them to incoming values from the other table

- When a large result set is sorted by the `ORDER BY` clause, each node sorts its portion of the result set in memory

- The `DISTINCT` and `UNION` operators build in-memory data structures to represent all values found so far, to
eliminate duplicates as the query progresses

When the spill-to-disk feature is activated for a join node within a query, Impala does not produce any runtime filters
for that join operation on that host. Other join nodes within the query are not affected

**How Impala handles scratch disk space for spilling:**

By default, intermediate files used during large sort, join, aggregation, or analytic function operations are stored
in the directory `/tmp/impala-scratch`. These files are removed when the operation finishes. (Multiple
concurrent queries can perform operations that use the “spill to disk” technique, without any name conflicts
for these temporary files.)  You can specify a different location by starting the impalad daemon with the `--
scratch_dirs="path_to_directory"` configuration option. You can specify a single directory, or a
comma-separated list of directories. The scratch directories must be on the local filesystem, not in HDFS. You might
specify different directory paths for different hosts, depending on the capacity and speed of the available storage
devices. In Impala 2.3 or higher, Impala successfully starts (with a warning Impala successfully starts (with a warning
written to the log) if it cannot create or read and write files in one of the scratch directories. If there is less than 1 GB
free on the filesystem where that directory resides, Impala still runs, but writes a warning message to its log. If Impala
encounters an error reading or writing files in a scratch directory during a query, Impala logs the error and the query
fails

**Memory usage for SQL operators:**

In Impala 2.10 and higher, the way SQL operators such as `GROUP BY`, `DISTINCT`, and `joins`, transition between
using additional memory or activating the spill-to-disk feature is changed. The memory required to spill to disk is
reserved up front, and you can examine it in the `EXPLAIN` plan when the `EXPLAIN_LEVEL` query option is set to `2`
or higher

The infrastructure of the spilling feature affects the way the affected SQL operators, such as GROUP BY, DISTINCT,
and joins, use memory. On each host that participates in the query, each such operator in a query requires memory to
store rows of data and other data structures. Impala reserves a certain amount of memory up front for each operator
that supports spill-to-disk that is sufficient to execute the operator. If an operator accumulates more data than can fit
in the reserved memory, it can either reserve more memory to continue processing data in memory or start spilling
data to temporary scratch files on disk. Thus, operators with spill-to-disk support can adapt to different memory
constraints by using however much memory is available to speed up execution, yet tolerate low memory conditions
by spilling data to disk

The amount data depends on the portion of the data being handled by that host, and thus the operator may end up
consuming different amounts of memory on different hosts

**Added in**: This feature was added to the ORDER BY clause in Impala 1.4. This feature was extended to cover join
queries, aggregation functions, and analytic functions in Impala 2.0. The size of the memory work area required by
each operator that spills was reduced from 512 megabytes to 256 megabytes in Impala 2.2. The spilling mechanism
was reworked to take advantage of the Impala buffer pool feature and be more predictable and stable in Impala 2.10.

#### DEFAULT_SPILLABLE_BUFFER_SIZE Query Option

Specifies the default size for a memory buffer used when the spill-to-disk mechanism is activated, for example for
queries against a large table with no statistics, or large join operations

**Type**: `integer`

**Default**: 2097152 (2 MB)

**Units**: A numeric argument represents a size in bytes; you can also use a suffix of m or mb for megabytes, or g or gb
for gigabytes. If you specify a value with unrecognized formats, subsequent queries fail with an error

**Usage notes**:

This query option sets an upper bound on the size of the internal buffer size that can be used during spill-to-disk
operations. The actual size of the buffer is chosen by the query planner

If overall query performance is limited by the time needed for spilling, consider increasing the
`DEFAULT_SPILLABLE_BUFFER_SIZE` setting. Larger buffer sizes result in Impala issuing larger I/O requests to
storage devices, which might result in higher throughput, particularly on rotational disks

The tradeoff with a large value for this setting is increased memory usage during spill-to-disk operations. Reducing
this value may reduce memory consumption

To determine if the value for this setting is having an effect by capping the spillable buffer size, you can see the buffer
size chosen by the query planner for a particular query. `EXPLAIN` the query while the setting `EXPLAIN_LEVEL=2`
is in effect

```sql
set default_spillable_buffer_size=4MB;
```

#### MIN_SPILLABLE_BUFFER_SIZE Query Option

Specifies the minimum size for a memory buffer used when the spill-to-disk mechanism is activated, for example for
queries against a large table with no statistics, or large join operations

**Type**: `integer`

**Default**: 65536 (64 KB)

**Units**: A numeric argument represents a size in bytes; you can also use a suffix of m or mb for megabytes, or g or gb
for gigabytes. If you specify a value with unrecognized formats, subsequent queries fail with an error

**Usage notes**:

This query option sets a lower bound on the size of the internal buffer size that can be used during spill-to-disk
operations. The actual size of the buffer is chosen by the query planner

If overall query performance is limited by the time needed for spilling, consider increasing the
`MIN_SPILLABLE_BUFFER_SIZE` setting. Larger buffer sizes result in Impala issuing larger I/O requests to
storage devices, which might result in higher throughput, particularly on rotational disks

The tradeoff with a large value for this setting is increased memory usage during spill-to-disk operations. Reducing
this value may reduce memory consumption

To determine if the value for this setting is having an effect by capping the spillable buffer size, you can see the buffer
size chosen by the query planner for a particular query. `EXPLAIN` the query while the setting `EXPLAIN_LEVEL=2`
is in effect

```sql
set min_spillable_buffer_size=128KB;
```
---

# Using the Parquet File Format with Impala Tables

Impala helps you to create, manage, and query Parquet tables. Parquet is a column-oriented binary file format
intended to be highly efficient for the types of large-scale queries that Impala is best at. Parquet is especially good
for queries scanning particular columns within a table, for example to query “wide” tables with many columns, or
to perform aggregation operations such as `SUM()` and `AVG()` that need to process most or all of the values from
a column. Each data file contains the values for a set of rows (the “row group”). Within a data file, the values from
each column are organized so that they are all adjacent, enabling good compression for the values from that column.
Queries against a Parquet table can retrieve and analyze these values from any column quickly and with minimal I/O

File Type|Format|Compression Codecs|Impala Can CREATE?|Impala Can INSERT?
--|:--:|--:|--:|--:
Parquet|Structured|Snappy, gzip;currently Snappy by default|Yes|Yes: `CREATE TABLE,INSERT, LOAD DATA`, and query

## Creating Parquet Tables in Impala

To create a table named `PARQUET_TABLE` that uses the Parquet format, you would use a command like the
following, substituting your own table name, column names, and data types

```sql
[impala-host:21000] > create table parquet_table_name (x INT, y STRING)
 STORED AS PARQUET;
```

Or, to clone the column names and data types of an existing table:

```SQL
[impala-host:21000] > create table parquet_table_name LIKE other_table_name STORED AS PARQUET;
```

In Impala 1.4.0 and higher, you can derive column definitions from a raw Parquet data file, even without an existing
Impala table. For example, you can create an external table pointing to an HDFS directory, and base the column
definitions on one of the files in that directory

```SQL
CREATE EXTERNAL TABLE ingest_existing_files LIKE PARQUET '/user/etl/destination/datafile1.dat' STORED AS PARQUET LOCATION '/user/etl/destination';
```

Or, you can refer to an existing data file and create a new empty table with suitable column definitions. Then you can
use `INSERT` to create new data files or `LOAD DATA` to transfer existing data files into the new table

```SQL
CREATE TABLE columns_from_data_file LIKE PARQUET '/user/etl/destination/datafile1.dat' STORED AS PARQUET;
```

In this example, the new table is partitioned by year, month, and day. These partition key columns are not part of the
data file, so you specify them in the `CREATE TABLE` statement

```SQL
CREATE TABLE columns_from_data_file LIKE PARQUET '/user/etl/destination/datafile1.dat' PARTITION (year INT, month TINYINT, day TINYINT) STORED AS PARQUET;
```
