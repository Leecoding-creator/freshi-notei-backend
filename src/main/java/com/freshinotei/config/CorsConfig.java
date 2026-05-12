package com.freshinotei.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 전역 설정.
 *
 * 프론트엔드(React/Next.js) 가 다른 도메인에서 백엔드 API 를 호출할 수 있게 허용한다.
 *
 * 운영에서는 allowedOrigins 를 실제 프론트 도메인으로 좁힐 것.
 * 데모 단계라 와일드카드 허용.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
