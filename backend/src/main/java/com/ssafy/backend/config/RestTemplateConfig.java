package com.ssafy.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// Spring에서 외부 HTTP 요청을 보내는 도구
//Spring이 자동으로 생성해주지않음, 직접 Bean으로 등록
//AiService에서 주입