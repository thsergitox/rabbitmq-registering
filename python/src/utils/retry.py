"""Retry utility with exponential backoff."""

import time
import random
import logging
from functools import wraps
from typing import Callable, Optional, Dict, Any

logger = logging.getLogger(__name__)


class RetryOptions:
    """Configuration for retry behavior."""

    def __init__(
        self,
        max_retries: int = 3,
        initial_delay: float = 1.0,
        max_delay: float = 30.0,
        factor: float = 2.0,
        randomize: bool = True,
    ):
        self.max_retries = max_retries
        self.initial_delay = initial_delay
        self.max_delay = max_delay
        self.factor = factor
        self.randomize = randomize


def calculate_delay(retry_count: int, options: RetryOptions) -> float:
    """Calculate exponential backoff delay."""
    delay = options.initial_delay * (options.factor**retry_count)

    # Cap at max delay
    delay = min(delay, options.max_delay)

    # Add jitter if enabled
    if options.randomize:
        jitter = delay * 0.2 * random.random()
        delay = delay + jitter

    return delay


def is_retryable_error(error: Exception) -> bool:
    """Check if error is retryable."""
    error_str = str(error)
    error_type = type(error).__name__

    # Database connection errors
    if any(
        msg in error_str
        for msg in [
            "Lost connection",
            "Connection refused",
            "Connection reset",
            "Timeout",
            "Can't connect",
        ]
    ):
        return True

    # RabbitMQ errors
    if any(
        msg in error_str
        for msg in [
            "ConnectionClosed",
            "ChannelClosed",
            "Connection closed",
            "AMQP connection",
        ]
    ):
        return True

    # Known retryable error types
    retryable_types = [
        "ConnectionError",
        "ConnectionRefusedError",
        "TimeoutError",
        "OperationalError",
        "InterfaceError",
    ]

    return error_type in retryable_types


def with_retry(
    func: Optional[Callable] = None,
    options: Optional[RetryOptions] = None,
    context: str = "operation",
) -> Callable:
    """
    Execute function with retry logic.

    Can be used as a decorator or called directly:
    - @with_retry
    - @with_retry(options=RetryOptions(max_retries=5))
    - result = await with_retry(my_func, options=options)()
    """
    if options is None:
        options = RetryOptions()

    def decorator(f: Callable) -> Callable:
        @wraps(f)
        def sync_wrapper(*args, **kwargs):
            last_error = None

            for attempt in range(options.max_retries + 1):
                try:
                    result = f(*args, **kwargs)
                    if attempt > 0:
                        logger.info(f"{context} succeeded after {attempt} retries")
                    return result
                except Exception as error:
                    last_error = error

                    if attempt < options.max_retries and is_retryable_error(error):
                        delay = calculate_delay(attempt, options)
                        logger.warning(
                            f"{context} failed (attempt {attempt + 1}/{options.max_retries + 1}), "
                            f"retrying in {delay:.1f}s: {error}"
                        )
                        time.sleep(delay)
                    else:
                        logger.error(
                            f"{context} failed after {attempt + 1} attempts: {error}"
                        )
                        break

            raise last_error

        @wraps(f)
        async def async_wrapper(*args, **kwargs):
            import asyncio

            last_error = None

            for attempt in range(options.max_retries + 1):
                try:
                    result = await f(*args, **kwargs)
                    if attempt > 0:
                        logger.info(f"{context} succeeded after {attempt} retries")
                    return result
                except Exception as error:
                    last_error = error

                    if attempt < options.max_retries and is_retryable_error(error):
                        delay = calculate_delay(attempt, options)
                        logger.warning(
                            f"{context} failed (attempt {attempt + 1}/{options.max_retries + 1}), "
                            f"retrying in {delay:.1f}s: {error}"
                        )
                        await asyncio.sleep(delay)
                    else:
                        logger.error(
                            f"{context} failed after {attempt + 1} attempts: {error}"
                        )
                        break

            raise last_error

        # Return appropriate wrapper based on function type
        import asyncio

        if asyncio.iscoroutinefunction(f):
            return async_wrapper
        else:
            return sync_wrapper

    # Handle different decorator usage patterns
    if func is None:
        return decorator
    else:
        return decorator(func)


class CircuitBreaker:
    """Simple circuit breaker implementation."""

    def __init__(
        self,
        failure_threshold: int = 5,
        recovery_timeout: float = 60.0,
        expected_exception: type = Exception,
    ):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.expected_exception = expected_exception
        self.failure_count = 0
        self.last_failure_time = None
        self.state = "closed"  # closed, open, half-open

    def call(self, func: Callable, *args, **kwargs):
        """Execute function through circuit breaker."""
        if self.state == "open":
            if time.time() - self.last_failure_time > self.recovery_timeout:
                self.state = "half-open"
            else:
                raise Exception("Circuit breaker is open")

        try:
            result = func(*args, **kwargs)
            if self.state == "half-open":
                self.state = "closed"
                self.failure_count = 0
            return result
        except self.expected_exception as e:
            self.failure_count += 1
            self.last_failure_time = time.time()

            if self.failure_count >= self.failure_threshold:
                self.state = "open"
                logger.error(
                    f"Circuit breaker opened after {self.failure_count} failures"
                )

            raise e
