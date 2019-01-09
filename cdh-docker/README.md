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
