package com.registration.userservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RetryUtil {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_INITIAL_DELAY = 1000L;
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final long DEFAULT_MAX_DELAY = 30000L;

    /**
     * Create a RetryTemplate with exponential backoff
     */
    public static RetryTemplate createRetryTemplate() {
        return createRetryTemplate(DEFAULT_MAX_ATTEMPTS, DEFAULT_INITIAL_DELAY, DEFAULT_MULTIPLIER, DEFAULT_MAX_DELAY);
    }

    /**
     * Create a RetryTemplate with custom parameters
     */
    public static RetryTemplate createRetryTemplate(int maxAttempts, long initialDelay, double multiplier, long maxDelay) {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialDelay);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxDelay);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Add retry listener for logging
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                return true;
            }

            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                if (throwable == null && context.getRetryCount() > 0) {
                    log.info("Operation succeeded after {} retries", context.getRetryCount());
                }
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                if (context.getRetryCount() < maxAttempts) {
                    log.warn("Operation failed (attempt {}/{}), retrying: {}", 
                            context.getRetryCount() + 1, maxAttempts, throwable.getMessage());
                } else {
                    log.error("Operation failed after {} attempts: {}", 
                            maxAttempts, throwable.getMessage());
                }
            }
        });

        return retryTemplate;
    }

    /**
     * Execute an operation with retry logic
     */
    public static <T> T withRetry(RetryCallback<T, Exception> callback) throws Exception {
        RetryTemplate retryTemplate = createRetryTemplate();
        return retryTemplate.execute(callback);
    }

    /**
     * Execute an operation with custom retry configuration
     */
    public static <T> T withRetry(RetryCallback<T, Exception> callback, int maxAttempts, long initialDelay) throws Exception {
        RetryTemplate retryTemplate = createRetryTemplate(maxAttempts, initialDelay, DEFAULT_MULTIPLIER, DEFAULT_MAX_DELAY);
        return retryTemplate.execute(callback);
    }

    /**
     * Check if an exception is retryable
     */
    public static boolean isRetryableException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        String message = throwable.getMessage();
        String className = throwable.getClass().getSimpleName();

        // Database connection errors
        if (message != null && (
                message.contains("Connection refused") ||
                message.contains("Connection reset") ||
                message.contains("Connection timed out") ||
                message.contains("Unable to acquire JDBC Connection"))) {
            return true;
        }

        // RabbitMQ errors
        if (message != null && (
                message.contains("Connection closed") ||
                message.contains("Channel closed") ||
                message.contains("AMQP"))) {
            return true;
        }

        // Known retryable exception types
        return className.contains("ConnectException") ||
               className.contains("TimeoutException") ||
               className.contains("SocketException") ||
               className.contains("IOException");
    }
} 