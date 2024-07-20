    # 创建docker网络
```shell
docker network create -d bridge umbrella
```

# 启动 Mysql

Linux
```shell
docker run --name mysql --hostname mysql \
--network umbrella \
-p 3306:3306 \
-v /Users/haiqing.fu/docker-instances/mysql/mysql.cnf:/etc/mysql/conf.d/mysql.cnf \
-v /Users/haiqing.fu/docker-instances/mysql/data:/var/lib/mysql \
-v /Users/haiqing.fu/docker-instances/mysql/linkerp.sql:/docker-entrypoint-initdb.d/linkerp.sql \
-e MYSQL_ROOT_PASSWORD=jiao1983! \
-d mysql:8.3
```
Windows
```shell
docker run --name mysql --hostname mysql `
--network umbrella `
-p 3306:3306 `
-v D:/docker-instances/mysql/mysql.cnf:/etc/mysql/conf.d/mysql.cnf `
-v D:/docker-instances/mysql/data:/var/lib/mysql `
-e MYSQL_ROOT_PASSWORD=fuhaiqing `
-d mysql:8.3
```

# 启动 Redis
```shell
docker run --name redis --hostname redis `
--network umbrella `
-p 6379:6379 `
-v D:/docker-instances/redis/data:/data `
-d redis:7.2.5 redis-server
```

# 启动 Nacos
Linux
```shell
docker run --name nacos --hostname nacos \
--network umbrella \
-p 8848:8848 \
-p 9848:9848 \
-v /Users/haiqing.fu/docker-instances/nacos/logs:/home/nacos/logs \
-e MYSQL_ROOT_PASSWORD=jiao1983! \
-e PREFER_HOST_MODE=nacos \
-e MODE=standalone \
-e SPRING_DATASOURCE_PLATFORM=mysql \
-e MYSQL_SERVICE_HOST=mysql \
-e MYSQL_SERVICE_DB_NAME=nacos \
-e MYSQL_SERVICE_PORT=3306 \
-e MYSQL_SERVICE_USER=root \
-e MYSQL_SERVICE_PASSWORD=jiao1983! \
-e MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true \
-e NACOS_AUTH_ENABLE=true \
-e NACOS_AUTH_IDENTITY_KEY=accessKey \
-e NACOS_AUTH_IDENTITY_VALUE=accessValue \
-e NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 \
-d nacos/nacos-server:v2.3.2
```
Window
```shell
docker run --name nacos --hostname nacos --network umbrella `
-p 8848:8848 `
-p 9848:9848 `
-v D:/docker-instances/nacos/logs:/home/nacos/logs `
-e MYSQL_ROOT_PASSWORD=fuhaiqing `
-e PREFER_HOST_MODE=nacos `
-e MODE=standalone `
-e SPRING_DATASOURCE_PLATFORM=mysql `
-e MYSQL_SERVICE_HOST=mysql `
-e MYSQL_SERVICE_DB_NAME=nacos `
-e MYSQL_SERVICE_PORT=3306 `
-e MYSQL_SERVICE_USER=root `
-e MYSQL_SERVICE_PASSWORD=fuhaiqing `
-e MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8"&"connectTimeout=1000"&"socketTimeout=3000"&"autoReconnect=true"&"useUnicode=true"&"useSSL=false"&"serverTimezone=Asia/Shanghai"&"allowPublicKeyRetrieval=true `
-e NACOS_AUTH_ENABLE=true `
-e NACOS_AUTH_IDENTITY_KEY=accessKey `
-e NACOS_AUTH_IDENTITY_VALUE=accessValue `
-e NACOS_AUTH_TOKEN=c2VjcmV0a2V5MTIzNDU2Nzg5MTAxMTEyMTMxNDE1MTYxNw== `
-d nacos/nacos-server:v2.3.2
```

# 启动 keycloak
```shell
docker run --name keycloak --hostname keycloak `
--network umbrella `
-p 8180:8180 `
-p 9000:9000 `
-e KEYCLOAK_ADMIN=admin `
-e KEYCLOAK_ADMIN_PASSWORD=admin `
-e KC_HEALTH_ENABLED=true `
-e KC_METRICS_ENABLED=true `
-d keycloak/keycloak:25.0.0 start-dev --http-port=8180
```

# 启动 elasticsearch
```shell
docker run --name elasticsearch --hostname elasticsearch `
--network umbrella `
-p 9200:9200 `
-p 9300:9300 `
-e "discovery.type=single-node" `
-e ES_JAVA_OPTS="-Xms2g -Xmx2g" `
-e LANG=C.UTF-8 `
-e LC_ALL=C.UTF-8 `
-v D:/docker-instances/elasticsearch/config:/usr/share/elasticsearch/config `
-v D:/docker-instances/elasticsearch/data:/usr/share/elasticsearch/data `
-d elasticsearch:8.13.4
```

# 启动kibana
```shell
docker run --name kibana --hostname kibana `
--net umbrella `
-p 5601:5601 `
-v D:/docker-instances/kibana/config:/usr/share/kibana/config `
-v D:/docker-instances/kibana/data:/usr/share/kibana/data `
-d kibana:8.13.4
```

# 启动logstash
```shell
docker run --name logstash --hostname logstash `
--net umbrella `
-p 9600:9600 `
-p 9061:9061 `
-p 5044:5044 `
-v D:/docker-instances/logstash/config:/usr/share/logstash/config `
-v D:/docker-instances/logstash/pipeline:/usr/share/logstash/pipeline `
-d logstash:8.13.4
```

#启动kong-database
```shell
docker run --name kong-database --hostname kong-database `
--net umbrella `
-p 5432:5432 `
-e "POSTGRES_USER=kong" `
-e "POSTGRES_DB=kong" `
-e "POSTGRES_PASSWORD=kong" `
-d postgres:15
```

# 初始化数据
```shell
docker run --rm `
--net umbrella `
-e "KONG_DATABASE=postgres" `
-e "KONG_PG_HOST=kong-database" `
-e "KONG_PG_USER=kong" `
-e "KONG_PG_PASSWORD=kong" `
-e "KONG_CASSANDRA_CONTACT_POINTS=kong-database" `
kong/kong-gateway:3.4 kong migrations bootstrap
```

# 启动kong
```shell
docker run --name kong --hostname kong `
--net umbrella `
-e "KONG_DATABASE=postgres" `
-e "KONG_PG_HOST=kong-database" `
-e "KONG_PG_PASSWORD=kong" `
-e "KONG_CASSANDRA_CONTACT_POINTS=kong-database" `
-e "KONG_PROXY_ACCESS_LOG=/dev/stdout" `
-e "KONG_ADMIN_ACCESS_LOG=/dev/stdout" `
-e "KONG_PROXY_ERROR_LOG=/dev/stderr" `
-e "KONG_ADMIN_ERROR_LOG=/dev/stderr" `
-e "KONG_ADMIN_LISTEN=0.0.0.0:8001, 0.0.0.0:8444 ssl" `
-p 8000:8000 `
-p 8002:8002 `
-p 8443:8443 `
-p 8001:8001 `
-p 8444:8444 `
-d kong/kong-gateway:3.4
```
测试的时候使用host网络
```shell
docker run --name kong --hostname kong `
--net host `
-e "KONG_DATABASE=postgres" `
-e "KONG_PG_HOST=kong-database" `
-e "KONG_PG_PASSWORD=kong" `
-e "KONG_CASSANDRA_CONTACT_POINTS=kong-database" `
-e "KONG_PROXY_ACCESS_LOG=/dev/stdout" `
-e "KONG_ADMIN_ACCESS_LOG=/dev/stdout" `
-e "KONG_PROXY_ERROR_LOG=/dev/stderr" `
-e "KONG_ADMIN_ERROR_LOG=/dev/stderr" `
-e "KONG_ADMIN_LISTEN=0.0.0.0:8001, 0.0.0.0:8444 ssl" `
-d kong/kong-gateway:3.4
```

# 启动kong manager
```shell
docker exec -i kong /bin/sh -c "export KONG_ADMIN_GUI_PATH='/'; export KONG_ADMIN_GUI_URL='http://localhost:8002/manager'; kong reload; exit"
```

# 启动skywalking
```shell
docker run --name skywalking-server --hostname skywalking-server `
--net umbrella `
-p 11800:11800 `
-p 12800:12800 `
-v D:/docker-instances/skywalking/config:/skywalking/config `
-v D:/docker-instances/skywalking/oap-libs/mysql-connector-j-8.4.0.jar:/skywalking/oap-libs/mysql-connector-j-8.4.0.jar `
-d apache/skywalking-oap-server:10.0.1-java21
```

# 启动skywalking-ui
```shell
docker run --name skywalking-ui --hostname skywalking-ui `
--net umbrella `
-p 8888:8080 `
-e SW_OAP_ADDRESS=http://skywalking-server:12800 `
-d apache/skywalking-ui:10.0.1-java21
```

# 配置微服务agent
添加VM选项
```shell
-javaagent:D:/WORK/skywalking-agent/skywalking-agent.jar -Dskywalking.agent.service_name=user-service -Dskywalking.logging.dir=D:/WORK/logs/user-service -Dskywalking.logging.file_name=skywalking-api.log
```

# 启动 prometheus
```shell
docker run --name prometheus --hostname prometheus `
--net umbrella `
-p 9090:9090 `
-v D:/docker-instances/prometheus/prometheus.yml:/opt/bitnami/prometheus/conf/prometheus.yml `
-v D:/docker-instances/prometheus/data:/opt/bitnami/prometheus/data `
-d bitnami/prometheus:2.52.1
```

# 启动 grafana
```shell
docker run --name grafana --hostname grafana `
--net umbrella `
-p 3000:3000 `
-v D:/docker-instances/grafana/data:/var/lib/grafana `
-d grafana/grafana:11.0.0
```

# 启动 rabbitmq
```shell
docker run --sysctl net.core.somaxconn=2048 --name rabbitmq --hostname rabbitmq `
--network umbrella `
-e TZ="Asia/Shanghai" `
-p 5672:5672 `
-p 15672:15672 `
-p 15692:15692 `
-e RABBITMQ_DEFAULT_USER=admin `
-e RABBITMQ_DEFAULT_PASS=admin `
-v D:/docker-instances/rabbitmq/data:/var/lib/rabbitmq `
-d rabbitmq:3.13.3-management
```

# 启动 Loki
```shell
docker run --name loki --hostname loki `
--network umbrella `
-p 3100:3100 `
-v D:/docker-instances/loki/data:/loki `
-d grafana/loki
```

# 启动 hive-metastore
```shell
docker run -d -p 9083:9083 --network umbrella `
--env SERVICE_NAME=metastore `
--env DB_DRIVER=mysql `
--env SERVICE_OPTS="-Djavax.jdo.option.ConnectionDriverName=com.mysql.cj.jdbc.Driver -Djavax.jdo.option.ConnectionURL=jdbc:mysql://mysql-linkerp:3306/hive -Djavax.jdo.option.ConnectionUserName=root -Djavax.jdo.option.ConnectionPassword=jiao1983!" `
-v D:/docker-instances/hive-metastore/warehouse:/opt/hive/data/warehouse `
-v D:/docker-instances/hive-metastore/mysql-connector-java-8.0.28.jar:/opt/hive/lib/mysql-connector-java-8.0.28.jar `
--name metastore --hostname metastore `
apache/hive:4.0.0
```

# 启动 hive-server2
```shell
docker run -d -p 10000:10000 -p 10002:10002 `
--network umbrella --env SERVICE_NAME=hiveserver2 `
--name hive4 --hostname hive4 `
--env SERVICE_OPTS="-Dhive.metastore.uris=thrift://metastore:9083" `
--env IS_RESUME="true" `
apache/hive:4.0.0
```