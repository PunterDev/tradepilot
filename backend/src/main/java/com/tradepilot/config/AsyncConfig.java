package com.tradepilot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor(
            @Value("${app.ai.async.core-pool-size:2}") int corePoolSize,
            @Value("${app.ai.async.max-pool-size:6}") int maxPoolSize,
            @Value("${app.ai.async.queue-capacity:50}") int queueCapacity
    ) {
        return buildExecutor("AI", "ai-worker-", corePoolSize, maxPoolSize, queueCapacity, true);
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return buildExecutor("default", "async-worker-", 2, 4, 100, true);
    }

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor(
            @Value("${app.email.async.core-pool-size:1}") int corePoolSize,
            @Value("${app.email.async.max-pool-size:3}") int maxPoolSize,
            @Value("${app.email.async.queue-capacity:100}") int queueCapacity
    ) {
        return buildExecutor("email", "email-worker-", corePoolSize, maxPoolSize, queueCapacity, false);
    }

    private Executor buildExecutor(
            String executorName,
            String threadNamePrefix,
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            boolean runOnCallerWhenSaturated
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler((task, threadPoolExecutor) -> {
            log.warn("{} async executor saturated. active={}, poolSize={}, queued={}",
                    executorName,
                    threadPoolExecutor.getActiveCount(),
                    threadPoolExecutor.getPoolSize(),
                    threadPoolExecutor.getQueue().size());
            if (runOnCallerWhenSaturated) {
                new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(task, threadPoolExecutor);
            }
        });
        executor.initialize();
        return executor;
    }
}
