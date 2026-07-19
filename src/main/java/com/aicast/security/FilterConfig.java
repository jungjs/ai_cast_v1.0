package com.aicast.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration(ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>(filter);
        // 모든 API 경로에 대해 인증 필터 적용
        registration.addUrlPatterns("/api/*");
        // CorrelationIdFilter 다음으로 실행되도록 순서 지정
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiLogFilter> apiLogFilterRegistration(ApiLogFilter filter) {
        FilterRegistrationBean<ApiLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        // ApiKeyAuthFilter 다음으로 실행되도록 순서 지정
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
}
