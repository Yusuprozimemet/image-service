package hackyourfuture.net.imageservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

// Turns on @Async. The pool itself is Spring Boot's own `applicationTaskExecutor`,
// sized in application.yaml under spring.task.execution — keeping the numbers there
// means they sit with the rest of the runtime settings and need no code change.
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    // An @Async void method has no caller left to catch what it throws, so without
    // a handler the stack trace is swallowed silently. TaggingService handles its
    // own failures; this is the net for anything that still gets out.
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("uncaught exception in async {}", method.getName(), throwable);
    }
}