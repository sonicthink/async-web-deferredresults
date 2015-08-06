package com.codebreeze.rest.server.config;

import com.codebreeze.rest.server.ringbuffer.EchoEvent;
import com.codebreeze.rest.server.ringbuffer.EchoEventHandler;
import com.codebreeze.rest.server.ringbuffer.EchoWorkHandler;
import com.codebreeze.rest.server.services.EchoService;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan(basePackages = {"com.codebreeze.rest.server"})
@EnableWebMvc
@EnableAsync
public class AppConfig extends WebMvcConfigurationSupport implements AsyncConfigurer {
    @Bean
    public EchoService echoService() {
        return new EchoService();
    }

    @Bean
    public AsyncTaskExecutor taskExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(500);
        threadPoolTaskExecutor.setMaxPoolSize(500);
        threadPoolTaskExecutor.setQueueCapacity(500);
        return threadPoolTaskExecutor;
    }

    @Bean
    public ExecutorService disruptorExecutor() {
//        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2 );
        ExecutorService executorService = Executors.newFixedThreadPool(2048);
        return executorService;
    }

//    @Bean
//    public RingBuffer<EchoEvent> ringBuffer() {
//        // 2 - Specify the size of the ring buffer, must be power of 2.
//        int bufferSize = 2048;
//
//        // 3 - initialize the Disruptor object
//        Disruptor<EchoEvent> disruptor = new Disruptor(
//                EchoEvent::new,
//                bufferSize,
//                disruptorExecutor(),
//                ProducerType.MULTI,
//                new YieldingWaitStrategy());//java 8 flavor
//
//        disruptor.handleEventsWith(echoEventHandler());
//
//        disruptor.start();
//        final RingBuffer<EchoEvent> ringBuffer = disruptor.getRingBuffer();
//        return ringBuffer;
//    }

    @Bean
    public RingBuffer<EchoEvent> ringBuffer() {
        final WorkerPool carDeliveryWorkerPool =
                new WorkerPool(EchoEvent::new,
                        new IgnoreExceptionHandler(),
                        echoWorkHandlers(1000));

        final RingBuffer ringBuffer = carDeliveryWorkerPool.start(disruptorExecutor());
        return ringBuffer;
    }

    private EchoWorkHandler[] echoWorkHandlers(int n){
        EchoWorkHandler[] handlers = new EchoWorkHandler[n];
        for(int i = 0; i < n; i++){
            handlers[i] = echoWorkHandler();
        }
        return handlers;
    }

    @Bean
    public EchoEventHandler echoEventHandler() {
        return new EchoEventHandler();
    }


    @Bean
    @Scope("prototype")
    public EchoWorkHandler echoWorkHandler() {
        return new EchoWorkHandler();
    }

    /**
     * this is used for general async, not for mvc customizations
     *
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }

    /**
     * this is used for mvc executor customizations (i.e.
     *
     * @return
     */
    @Bean
    protected WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                configurer.setTaskExecutor(taskExecutor());
            }
        };
    }
}