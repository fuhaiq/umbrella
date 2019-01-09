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
