package org.example.library.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        var signature = joinPoint.getSignature();
        var className = signature.getDeclaringTypeName();
        var methodName = signature.getName();

        var request = getRequest();
        var requestInfo = getRequestInfo(request);

        log.debug("[CONTROLLER] Enter: {}.{} with {}", className, methodName, requestInfo);

        var startTime = System.currentTimeMillis();

        try {
            var result = joinPoint.proceed();
            var executionTime = System.currentTimeMillis() - startTime;

            log.debug("[CONTROLLER] Exit: {}.{} in {}ms", className, methodName, executionTime);

            return result;
        } catch (Throwable throwable) {
            var executionTime = System.currentTimeMillis() - startTime;
            log.debug("[CONTROLLER] Exit: {}.{} in {}ms with exception: {}",
                    className, methodName, executionTime, throwable.getClass().getSimpleName());
            throw throwable;
        }
    }

    private HttpServletRequest getRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    private String getRequestInfo(HttpServletRequest request) {
        if (request == null) {
            return "no request";
        }
        var method = request.getMethod();
        var uri = request.getRequestURI();
        var queryString = request.getQueryString();
        return queryString != null ? method + " " + uri + "?" + queryString : method + " " + uri;
    }

}
