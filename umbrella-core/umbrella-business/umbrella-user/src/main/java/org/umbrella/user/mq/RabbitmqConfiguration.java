package org.umbrella.user.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitmqConfiguration {
    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(appName, true, false);
    }

    @Bean
    public Queue queue() {
        return new Queue(appName, true);
    }

    @Bean
    public Binding bindingHdfs() {
        return BindingBuilder
                .bind(queue())
                .to(directExchange())
                .with(appName);
    }
}
