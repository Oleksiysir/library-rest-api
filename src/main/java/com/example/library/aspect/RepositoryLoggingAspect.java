package com.example.library.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RepositoryLoggingAspect {

    private static final Logger logger =
            LoggerFactory.getLogger(RepositoryLoggingAspect.class);

    @Around("bean(*Repository)")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint)
            throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();

        long startTime = System.nanoTime();

        logger.info("Repository method started: {}", methodName);

        try {
            Object result = joinPoint.proceed();

            long executionTime =
                    (System.nanoTime() - startTime) / 1_000_000;

            logger.info(
                    "Repository method completed: {} in {} ms",
                    methodName,
                    executionTime
            );

            return result;

        } catch (Throwable exception) {

            long executionTime =
                    (System.nanoTime() - startTime) / 1_000_000;

            logger.error(
                    "Repository method failed: {} in {} ms",
                    methodName,
                    executionTime,
                    exception
            );

            throw exception;
        }
    }
}
