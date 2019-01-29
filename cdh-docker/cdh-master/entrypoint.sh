#!/bin/bash

# 修改内核参数
echo 10 > /proc/sys/vm/swappiness
echo never > /sys/kernel/mm/transparent_hugepage/defrag
echo never > /sys/kernel/mm/transparent_hugepage/enabled

# 开启ntp服务
ntpd

# 开启SSH服务
/usr/sbin/sshd -D &

# 开启kerberos服务
/usr/sbin/krb5kdc -P /var/run/krb5kdc.pid &
/usr/sbin/_kadmind -P /var/run/kadmind.pid &

# 开启CM
/opt/cm-5.16.1/etc/init.d/cloudera-scm-server start

# 等待日志文件
while [ ! -f /opt/cm-5.16.1/log/cloudera-scm-server/cloudera-scm-server.log ]; do
    sleep 1
done

# 使用tail阻塞，这样可以使容器以daemon方式后台运行
tail -f /opt/cm-5.16.1/log/cloudera-scm-server/cloudera-scm-server.log
