package org.umbrella.common.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.io.Serializable;

public class JsonMessage extends Message {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public JsonMessage(Serializable body) throws JsonProcessingException {
        super(objectMapper.writeValueAsBytes(body), createJsonMessageProperties());
    }

    private static MessageProperties createJsonMessageProperties() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return properties;
    }

    public static <T extends Serializable> T fromMessage(Message message, Class<T> clazz) throws IOException {
        return objectMapper.readValue(message.getBody(), clazz);
    }

}
