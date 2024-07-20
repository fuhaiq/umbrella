package org.umbrella.mq.msg;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.umbrella.api.entity.TUser;
import org.umbrella.api.feign.RemoteUserService;
import org.umbrella.common.mq.JsonMessage;
import org.umbrella.common.util.R;

import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class MsgListener implements ChannelAwareMessageListener {

    private final Validator validator;

    private final Supplier<Authentication> sysUserAuthSupplier;

    private final RemoteUserService remoteUserService;
    @Override
    @SneakyThrows
    public void onMessage(Message message, Channel channel) {

        Assert.notNull(channel, "RabbitMQ 链接为空");
        TUser tUser = JsonMessage.fromMessage(message, TUser.class);

        log.info("收到消息: {}，远程调用验证数据", tUser);
        R<TUser> ret = remoteUserService.getById(tUser.getId(), StringPool.YES);
        log.info("远程调用结果: {}", ret);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
