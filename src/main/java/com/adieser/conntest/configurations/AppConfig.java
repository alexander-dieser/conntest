package com.adieser.conntest.configurations;

import com.adieser.conntest.interceptors.RequestLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.*;

/**
 * Global configurations
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {
    private static final int CORE_POOL_SIZE = 3;
    private static final int MAX_POOL_SIZE = 5;
    private static final long KEEP_ALIVE_TIME = 60L;

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public AppConfig(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    /**
     * Makes a {@link ThreadPoolExecutor} available for CDI.
     * {@link ThreadPoolExecutor} is used for handle a ping sessions in parallel, each one in a different thread
     * @return {@link ThreadPoolExecutor} for handling parallel ping sessions
     */
    @Bean
    public ExecutorService threadPoolExecutor() {
        return new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor);
    }
}
