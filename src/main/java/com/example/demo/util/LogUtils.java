package com.example.demo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class LogUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private LogUtils() {}

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize [{}] to JSON: {}", obj.getClass().getSimpleName(), ex.getMessage());
            return obj.toString();
        }
    }

    public static void info(Logger logger, String message, Object obj) {
        logger.info("{} : {}", message, toJson(obj));
    }

    public static void debug(Logger logger, String message, Object obj) {
        logger.debug("{} : {}", message, toJson(obj));
    }

    public static void warn(Logger logger, String message, Object obj) {
        logger.warn("{} : {}", message, toJson(obj));
    }

    public static void error(Logger logger, String message, Object obj, Throwable ex) {
        logger.error("{} : {}", message, toJson(obj), ex);
    }
}
