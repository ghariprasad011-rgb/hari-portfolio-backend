
package com.hari.portfolio_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminSessionInterceptor adminSessionInterceptor;

    public WebConfig(
            AdminSessionInterceptor adminSessionInterceptor) {
        this.adminSessionInterceptor = adminSessionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // Protect admin API requests with the admin session interceptor.
        registry.addInterceptor(adminSessionInterceptor)

                // Apply the interceptor to admin API routes.
                .addPathPatterns("/api/admin/**")

                // Exclude routes intended for public access.
                .excludePathPatterns(
                        "/api/admin/media",
                        "/api/admin/media/public",
                        "/api/admin/media/public/**"
                );
    }
}
