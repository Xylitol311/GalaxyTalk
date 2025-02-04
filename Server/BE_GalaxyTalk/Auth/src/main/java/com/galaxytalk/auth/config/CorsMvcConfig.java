package com.galaxytalk.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {

        corsRegistry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
//                .exposedHeaders("Set-Cookie")
                .allowedOrigins("http://localhost:3000");
    }
}