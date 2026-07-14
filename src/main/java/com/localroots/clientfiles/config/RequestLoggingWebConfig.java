package com.localroots.clientfiles.config;

import com.localroots.clientfiles.common.LoggingContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RequestLoggingWebConfig implements WebMvcConfigurer {

    private final LoggingContextInterceptor loggingContextInterceptor;

    public RequestLoggingWebConfig(LoggingContextInterceptor loggingContextInterceptor) {
        this.loggingContextInterceptor = loggingContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingContextInterceptor)
                .addPathPatterns("/api/**", "/actuator/**");
    }
}
