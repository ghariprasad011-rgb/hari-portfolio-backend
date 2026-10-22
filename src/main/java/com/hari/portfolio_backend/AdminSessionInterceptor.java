package com.hari.portfolio_backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminSessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        if (session == null ||
                session.getAttribute("ADMIN_USERNAME") == null) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Admin login required."
            );

            return false;
        }

        return true;
    }
}
