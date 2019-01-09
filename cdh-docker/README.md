# Docker安装CDH5
### 删除之前的，新安装不必执行
```
sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-selinux \
                  docker-engine-selinux \
                  docker-engine
```
### 安装
```
sudo yum install -y yum-utils \
  device-mapper-persistent-data \
  lvm2

sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo

sudo yum install docker-ce
```
### 更改Docker镜像存储目录（可选）
编辑/usr/lib/systemd/system/docker.service

`ExecStart=/usr/bin/dockerd -H unix:// --data-root=/aliyun/docker`

### 启动Docker
`systemctl start docker`

### 创建Docker 网络
`docker network create cdh` 创建名为cdh的docker网络

`docker network ls` 查看已有的网络
![图片alt](https://github.com/fuhaiq/umbrella/blob/master/cdh-docker/img/1.png)

### 构建cdh5/mysql镜像
嗯。。。。。。。不需要构建，直接用官方的mariadb即可

### 构建cdh5/master镜像

#### 准备
下载CDH 安装包以及 CM包，把Dockerfile和所有构建文件放在同一个目录下
![图片alt](https://github.com/fuhaiq/umbrella/blob/master/cdh-docker/img/2.png)
详细信息见Dockerfile，里面有注释。以下文件自己下载
- CDH-5.16.1-1.cdh5.16.1.p0.3-el7.parcel
- CDH-5.16.1-1.cdh5.16.1.p0.3-el7.parcel.sha
- cloudera-manager-centos7-cm5.16.1_x86_64.tar.gz
- jdk-8u191-linux-x64.tar.gz
- manifest.json

#### 开始构建
在当前目录下执行 `docker build . -t="cdh5/master"`

### 构建cdh5/agent镜像
#### 准备
下载CDH 安装包以及 CM包，把Dockerfile和所有构建文件放在同一个目录下
![图片alt](https://github.com/fuhaiq/umbrella/blob/master/cdh-docker/img/3.png)
详细信息见Dockerfile，里面有注释。以下文件自己下载
- cloudera-manager-centos7-cm5.16.1_x86_64.tar.gz
- jdk-8u191-linux-x64.tar.gz

#### 开始构建
在当前目录下执行 `docker build . -t="cdh5/agent"`

### 运行CDH docker 容器
#### cdh-mysql 容器（来自官方mariadb镜像）
根据自己的实际目录修改！！！ 本地目录不存在的要先创建
```
docker run --name cdh-mysql -h cdh-mysql --net=cdh \
-v /aliyun/docker-images/cdh-mysql/conf:/etc/mysql/conf.d \
-v /aliyun/docker-images/cdh-mysql/init:/docker-entrypoint-initdb.d \
-e MYSQL_ROOT_PASSWORD=Topfounder123 -d mariadb
```
这里有2个添加卷
- /etc/mysql/conf.d – 加载数据库配置，因为用到innodb引擎
- /docker-entrypoint-initdb.d – 数据库启动的时候加载的初始化脚本（即CDH数据库初始化）

#### cdh-master容器（来自cdh5/master镜像）
根据自己的实际目录修改！！！ 本地目录不存在的要先创建
```
docker run --name cdh-master -h cdh-master --net=cdh --privileged -p 7180:7180 \
-v /aliyun/docker-containers/cdh-master/cloudera-scm-server:/opt/cm-5.16.1/log/cloudera-scm-server \
-v /aliyun/docker-containers/cdh-master/hadoop:/hadoop \
-d cdh5/master
```
这里有2个添加卷
- /opt/cm-5.16.1/log/cloudera-scm-server – cdh master的日记目录
- /hadoop – 外部数据目录（把数据和容器隔离，方便备份，迁移）

#### cdh-xxx 容器（具体组件，全部来自cdh5/agent镜像）
根据自己的实际目录修改！！！ 本地目录不存在的要先创建。这里有2个添加卷
- /opt/cm-5.16.1/log/cloudera-scm-agent – cdh agent的日记目录
- /hadoop – 外部数据目录（把数据和容器隔离，方便备份，迁移）

到了安装界面的时候，数据根目录修改为/hadoop 如 HDFS配置如下:
![图片alt](https://github.com/fuhaiq/umbrella/blob/master/cdh-docker/img/4.png)
