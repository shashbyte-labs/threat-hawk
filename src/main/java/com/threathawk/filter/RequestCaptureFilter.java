package com.threathawk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class RequestCaptureFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Capture request details
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        System.out.println("🔎 [" + LocalDateTime.now() + "] " +
                "Method=" + method +
                ", URI=" + uri +
                ", IP=" + ip +
                ", User-Agent=" + userAgent);

        // Continue request flow
        chain.doFilter(request, response);
    }
}
