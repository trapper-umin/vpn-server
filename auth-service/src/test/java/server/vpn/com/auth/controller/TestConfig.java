package server.vpn.com.auth.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@TestConfiguration
@ComponentScan(
        basePackages = "server.vpn.com.auth",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
public class TestConfig {
} 