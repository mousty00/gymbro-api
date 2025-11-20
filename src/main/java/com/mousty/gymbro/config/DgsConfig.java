package com.mousty.gymbro.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class DgsConfig {

    @PostConstruct
    public void checkSchema() {
        ClassPathResource resource = new ClassPathResource("schema/schema.graphqls");
        System.out.println("Schema exists: " + resource.exists());
    }
}
