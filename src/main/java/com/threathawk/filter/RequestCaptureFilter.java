package com.threathawk.filter;

import com.threathawk.detection.DetectorService;
import com.threathawk.model.Event;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(0)
public class RequestCaptureFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestCaptureFilter.class);

    private final DetectorService detectorService;

    public RequestCaptureFilter(DetectorService detectorService) {
        this.detectorService = detectorService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        log.debug("RequestCaptureFilter START — method={}, uri={}, contentType={}, remote={}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getContentType(),
                httpRequest.getRemoteAddr());

        boolean isMultipart = httpRequest.getContentType() != null &&
                httpRequest.getContentType().toLowerCase().startsWith("multipart/");
        log.debug("Detected multipart/form-data = {}", isMultipart);

        ContentCachingRequestWrapper wrapped =
                new ContentCachingRequestWrapper(httpRequest);

        log.debug("Wrapped request created. Passing request down the chain.");
        chain.doFilter(wrapped, response);

        byte[] bodyBytes = wrapped.getContentAsByteArray();
        String rawBody = new String(bodyBytes, StandardCharsets.UTF_8);

        log.debug("After chain — bodyBytesSize={}, bodyPreview='{}'",
                bodyBytes.length,
                rawBody.length() > 200 ? rawBody.substring(0, 200) + "..." : rawBody);

        Event event = buildEventFromRequest(wrapped, rawBody);

        log.debug("Built Event — method={}, path={}, ip={}, headers={}, params={}, bodyLen={}",
                event.getMethod(),
                event.getPath(),
                event.getIp(),
                event.getHeaders() != null ? event.getHeaders().size() : 0,
                event.getParams() != null ? event.getParams().size() : 0,
                event.getBody() != null ? event.getBody().length() : 0
        );

        log.debug("Passing Event to IDS (DetectorService.analyzeAndSave)");
        detectorService.analyzeAndSave(event);
        log.debug("IDS processing complete for request {}", event.getPath());

        log.debug("RequestCaptureFilter END — method={}, uri={}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI());
    }

    private Event buildEventFromRequest(HttpServletRequest req, String rawBody) {

        log.debug("Building Event object from captured request metadata.");

        Event event = new Event();
        event.setTimestamp(Instant.now());
        event.setPath(req.getRequestURI());
        event.setMethod(req.getMethod());
        event.setIp(req.getRemoteAddr());

        Map<String, String> params = req.getParameterMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.join(",", e.getValue())
                ));
        log.debug("Captured {} request parameters", params.size());
        event.setParams(params);

        Map<String, String> headers = Collections.list(req.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, req::getHeader));
        log.debug("Captured {} request headers", headers.size());
        event.setHeaders(headers);

        event.setStatus(null);
        event.setBody(rawBody);

        log.debug("Event build complete — ready for IDS analysis.");
        return event;
    }
}