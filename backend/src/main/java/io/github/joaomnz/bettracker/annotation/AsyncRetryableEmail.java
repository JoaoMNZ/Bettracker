package io.github.joaomnz.bettracker.annotation;


import org.springframework.mail.MailException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Async
@Retryable(
        includes = {MailException.class},
        delay = 2000,
        multiplier = 2
)
public @interface AsyncRetryableEmail {
}
