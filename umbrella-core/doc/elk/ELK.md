# 安装 Elasticsearch


首先启动只挂载数据目录的容器
```shell
docker run --name elasticsearch --hostname elasticsearch `
--network umbrella `
-p 9200:9200 `
-e "discovery.type=single-node" `
-e ES_JAVA_OPTS="-Xms2g -Xmx2g" `
-e LANG=C.UTF-8 `
-e LC_ALL=C.UTF-8 `
-v D:/docker-instances/elasticsearch/data:/usr/share/elasticsearch/data `
-d elasticsearch:8.13.4
```

然后进入容器，将 config 目录拷贝出来
```shell
docker cp elasticsearch:/usr/share/elasticsearch/config .
```

停止并删除容器
```shell
docker stop elasticsearch
docker rm elasticsearch
```

编辑 config/elasticsearch.yml，指定集群名称
```yaml
cluster.name: "umbrella"
```

再次启动一个新的容器，使用之前的 config 目录和 data 目录，并新增2个目录

4个挂载目录
 - /usr/share/elasticsearch/config
 - /usr/share/elasticsearch/data
 - /usr/share/elasticsearch/logs
 - /usr/share/elasticsearch/plugins

```shell
docker run --name elasticsearch --hostname elasticsearch `
--network umbrella `
-p 9200:9200 `
-e "discovery.type=single-node" `
-e ES_JAVA_OPTS="-Xms2g -Xmx2g" `
-e LANG=C.UTF-8 `
-e LC_ALL=C.UTF-8 `
-v D:/docker-instances/elasticsearch/config:/usr/share/elasticsearch/config `
-v D:/docker-instances/elasticsearch/data:/usr/share/elasticsearch/data `
-v D:/docker-instances/elasticsearch/logs:/usr/share/elasticsearch/logs `
-v D:/docker-instances/elasticsearch/plugins:/usr/share/elasticsearch/plugins `
-d elasticsearch:8.13.4
```

进入容器重置密码
```shell
elasticsearch-reset-password -u elastic

...
Password for the [elastic] user successfully reset.
New value: mmNpYqobk6WsD5-X4=ji
```

打开浏览器访问 https://localhost:9200 输入用户名:elastic 密码:<PASSWORD> 可以看到 `You Know, for Search`

# 安装 Kibana

首先启动只挂载数据目录的容器
```shell
docker run --name kibana --hostname kibana `
--net umbrella `
-p 5601:5601 `
-v D:/docker-instances/kibana/data:/usr/share/kibana/data `
-d kibana:8.13.4
```

查看容器日志，会打印一个 `http://0.0.0.0:5601/?code=<code>` 的地址，进入，要求输入token

此时进入 elsaticsearch 容器，执行
```shell
elasticsearch-create-enrollment-token -s kibana
```
生成token，并输入到kibana控制台上，并完成后续配置

然后进入容器，将 config 目录拷贝出来
```shell
docker cp kibana:/usr/share/kibana/config .
```

停止并删除容器
```shell
docker stop kibana
docker rm kibana
```

修改 `kibana.yml` 指定中文
```yaml
i18n.locale: "zh-CN"
```

再次启动一个新的容器，使用之前的 config 目录和 data 目录，并新增2个目录

3个挂载目录
- /usr/share/kibana/config
- /usr/share/kibana/data
- /usr/share/kibana/logs

```shell
docker run --name kibana --hostname kibana `
--net umbrella `
-p 5601:5601 `
-v D:/docker-instances/kibana/config:/usr/share/kibana/config `
-v D:/docker-instances/kibana/data:/usr/share/kibana/data `
-v D:/docker-instances/kibana/logs:/usr/share/kibana/logs `
-d kibana:8.13.4
```


# TF-idf 算法
TF 表示某个词在文档中出现的次数。简单来说，TF 衡量了一个词在一个文档中有多重要

TF(t,d)= (词语 t 在文档 d 中出现的次数) / (文档 d 中所有词语出现的总次数)

idf(t, d) = log(文档总数 / 包含词语 t 的文档数)

得分 = sum(词条TF) * IDF

