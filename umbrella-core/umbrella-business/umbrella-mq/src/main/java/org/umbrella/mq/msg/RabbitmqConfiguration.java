package org.umbrella.mq.msg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitmqConfiguration {
    private final String appName = "user-service";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(appName, true, false);
    }

    @Bean
    public Queue queue() {
        return new Queue(appName, true);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(queue())
                .to(directExchange())
                .with(appName);
    }

    @Bean(destroyMethod = "destroy")
    public SimpleMessageListenerContainer msgListenerContainer(CachingConnectionFactory factory, MsgListener listener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factory);
        container.setQueues(queue());
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(1);
        container.setExclusive(true);
        container.setConsumerTagStrategy(queue -> queue);
        container.setMessageListener(listener);
        return container;
    }
}
