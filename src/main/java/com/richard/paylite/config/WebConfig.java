package com.richard.paylite.config;

import com.richard.paylite.security.ApiKeyAuthInterceptor;
import com.richard.paylite.security.CorrelationIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApiKeyAuthInterceptor apiKeyAuthInterceptor;

    @Autowired
    private CorrelationIdInterceptor correlationIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(correlationIdInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/api/v1/payments/**");
    }
}
