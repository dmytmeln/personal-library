package org.example.library.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("@within(org.springframework.stereotype.Service)")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        var signature = joinPoint.getSignature();
        var className = signature.getDeclaringTypeName();
        var methodName = signature.getName();
        var args = Arrays.toString(joinPoint.getArgs());

        log.debug("[SERVICE] Enter: {}.{} with args: {}", className, methodName, args);

        var startTime = System.currentTimeMillis();

        try {
            var result = joinPoint.proceed();
            var executionTime = System.currentTimeMillis() - startTime;

            log.debug("[SERVICE] Exit: {}.{} in {}ms", className, methodName, executionTime);

            return result;
        } catch (Throwable throwable) {
            var executionTime = System.currentTimeMillis() - startTime;
            log.error("[SERVICE] Exit: {}.{} in {}ms with exception", className, methodName, executionTime);
            throw throwable;
        }
    }

}
