package com.pprs.sync.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.annotation.Bean;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // // 10s connect, 30s read timeout
        // HttpComponentsClientHttpRequestFactory factory =
        //     new HttpComponentsClientHttpRequestFactory();
        // factory.setConnectTimeout(10_000);
        // //factory.setReadTimeout(30_000);
        // return new RestTemplate(factory);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }
}
