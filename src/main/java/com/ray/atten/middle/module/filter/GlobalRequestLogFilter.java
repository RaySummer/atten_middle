package com.ray.atten.middle.module.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@Component
public class GlobalRequestLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

   /*     // 1. 包装请求：使用 ContentCachingRequestWrapper 缓存 Body
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpServletRequest);

        String path = wrappedRequest.getRequestURI();
        String method = wrappedRequest.getMethod();
        String query = wrappedRequest.getQueryString();

        // 2. 仅对考勤机相关请求进行日志记录
        if (path.contains("iclock") || path.contains("cdata") || path.contains("devicecmd")) {
            log.debug("========================================");
            log.debug("【全局监控】收到请求:");
            log.debug("URL: " + path);
            log.debug("Method: " + method);
            log.debug("Query: " + query);
            log.debug("Content-Type: " + wrappedRequest.getContentType());
            log.debug("Content-Length: " + wrappedRequest.getContentLength());

            // 3. 读取并打印 Body 内容 (仅针对 POST/PUT 请求)
            if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && wrappedRequest.getContentLength() > 0) {
                // ContentCachingRequestWrapper 的核心方法：获取缓存的 Body
                byte[] content = wrappedRequest.getContentAsByteArray();
                if (content.length > 0) {
                    String body = "";
                    try {
                        // 尝试以设备的默认编码（通常是 UTF-8/GBK）读取
                        body = new String(content, wrappedRequest.getCharacterEncoding() != null ? wrappedRequest.getCharacterEncoding() : "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        body = new String(content);
                    }

                    log.debug("--- RAW POST Body START (Length: " + body.length() + ") ---");
                    log.debug(body.substring(0, Math.min(body.length(), 1000)));
                    if (body.length() > 1000) {
                        log.debug("...(Body Truncated)...");
                    }
                    log.debug("--- RAW POST Body END ---");
                } else {
                    // 打印这个，帮助我们调试
                    log.error("!!! ERROR: Content-Length is " + wrappedRequest.getContentLength() + " but cached content length is 0 !!!");
                }
                String body = "";
                try {
                    // 尝试以 UTF-8 编码读取 Body
                    body = new String(content, wrappedRequest.getCharacterEncoding() != null ? wrappedRequest.getCharacterEncoding() : "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    body = new String(content);
                }

                log.debug("--- RAW POST Body START (Length: " + body.length() + ") ---");
                // 打印 Body 内容，只打印前 1000 个字符以防日志过长
                log.debug(body.substring(0, Math.min(body.length(), 1000)));
                if (body.length() > 1000) {
                    log.debug("...(Body Truncated)...");
                }
                log.debug("--- RAW POST Body END ---");
            }

            log.debug("========================================");
        }

        // 4. 将 **包装过的** 请求向下传递，确保 Controller 仍能读取 Body
        chain.doFilter(wrappedRequest, response);*/
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        String method = req.getMethod();
        String query = req.getQueryString();

        // 只记录跟考勤机相关的请求，避免日志太多
        if (path.contains("iclock") || path.contains("cdata")) {
            log.debug("========================================");
            log.debug("【全局监控】收到请求:");
            log.debug("URL: " + path);
            log.debug("Method: " + method); // 重点看是不是 POST
            log.debug("Query: " + query);
            log.debug("Content-Type: " + req.getContentType());
            log.debug("Content-Length: " + req.getContentLength());
            log.debug("========================================");
        }

        chain.doFilter(request, response);
    }
}
