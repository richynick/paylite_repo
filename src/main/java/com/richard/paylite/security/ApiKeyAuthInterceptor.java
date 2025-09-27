package com.richard.paylite.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    @Value("${paylite.security.api-keys}")
    private String apiKeys;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestApiKey = request.getHeader("X-API-Key");

        if (requestApiKey == null || requestApiKey.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing X-API-Key header");
            return false;
        }

        List<String> validApiKeys = Arrays.asList(apiKeys.split(","));
        if (validApiKeys.contains(requestApiKey)) {
            return true;
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid X-API-Key");
            return false;
        }
    }
}
