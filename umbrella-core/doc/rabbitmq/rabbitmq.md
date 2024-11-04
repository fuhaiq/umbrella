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
-d rabbitmq:3.13.4-management
```