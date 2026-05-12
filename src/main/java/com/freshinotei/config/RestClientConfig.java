package com.freshinotei.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * Gemini API 호출용 RestClient.
     * 이미지 업로드 + LLM 응답 대기를 고려해 타임아웃을 넉넉히 잡음.
     */
    @Bean
    public RestClient geminiRestClient() {
        return RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
                    setReadTimeout((int) Duration.ofSeconds(60).toMillis());
                }})
                .build();
    }
}
