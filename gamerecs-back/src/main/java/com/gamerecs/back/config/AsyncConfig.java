package com.gamerecs.back.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 * Configures the thread pool for handling asynchronous operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);
    
    /**
     * Core pool size - number of threads to keep alive even when idle
     */
    private static final int CORE_POOL_SIZE = 4;
    
    /**
     * Maximum pool size - maximum threads that can be created
     */
    private static final int MAX_POOL_SIZE = 8;
    
    /**
     * Queue capacity - how many tasks can wait if all threads are busy
     */
    private static final int QUEUE_CAPACITY = 100;
    
    /**
     * Thread name prefix for better identification in logs
     */
    private static final String THREAD_NAME_PREFIX = "igdb-async-";
    
    /**
     * Creates and configures the thread pool task executor for asynchronous operations
     * 
     * @return configured Executor for async tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        logger.info("Creating Async Task Executor");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.initialize();
        
        return executor;
    }
} 
