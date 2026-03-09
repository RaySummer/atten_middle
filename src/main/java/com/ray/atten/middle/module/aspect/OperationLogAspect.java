package com.ray.atten.middle.module.aspect;

import com.ray.atten.middle.module.model.LogEntry;
import com.ray.atten.middle.module.repository.LogRepository;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private LogRepository logRepository; // 你的日志 JPA Repository

    @Around("execution(* com.ray.atten.middle.module.controller..*.*(..))")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 1. 获取请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 2. 执行目标方法
        Object result = joinPoint.proceed();

        // 3. 计算耗时
        long executionTime = System.currentTimeMillis() - startTime;

        // 4. 异步或同步保存日志 (建议异步，不影响接口性能)
        saveLog(joinPoint, request, executionTime);

        return result;
    }

    @Async
    public void saveLog(ProceedingJoinPoint joinPoint, HttpServletRequest request, long time) {
        try {
            LogEntry logEntry = new LogEntry();

            // A. 获取用户名
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                    ? auth.getName() : "匿名用户";
            logEntry.setUsername(username);

            // B. 获取IP地址
            logEntry.setIp(getIpAddress(request));

            // C. 接口信息
            logEntry.setMethod(request.getMethod());
            logEntry.setApiPath(request.getRequestURI());
            logEntry.setClassName(joinPoint.getTarget().getClass().getSimpleName());
            logEntry.setMethodName(joinPoint.getSignature().getName());

            // D. 请求参数 (简单处理)
            Object[] args = joinPoint.getArgs();
            logEntry.setParams(Arrays.toString(args));

            logEntry.setExecutionTime(time);

            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("日志记录异常: {}", e.getMessage());
        }
    }

    // 辅助方法：获取真实IP (处理过代理的情况)
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}