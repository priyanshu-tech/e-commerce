package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "discount")
@Data
public class DiscountConfig {

    /**
     * Maximum discount percentage allowed on any product.
     * Refreshable at runtime via Spring Cloud Config + POST /actuator/refresh.
     * Default: 90%
     */
    private BigDecimal maxDiscountPct = new BigDecimal("90.00");
}
