package com.example.order.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j Retry 이벤트 리스너 설정
 * 
 * RetryRegistry를 통해 Retry 이벤트를 구독하고 로깅합니다.
 * - 재시도 시도 시 로그 출력
 * - 재시도 성공 시 로그 출력
 * - 재시도 실패 시 로그 출력
 * - 무시된 오류 시 로그 출력
 */
@Configuration
public class RetryEventListener {

    private static final Logger log = LoggerFactory.getLogger(RetryEventListener.class);

    /**
     * RetryRegistry에 이벤트 리스너 등록
     * 
     * Retry 인스턴스가 생성될 때 자동으로 이벤트 리스너를 등록합니다.
     */
    @Bean
    public RegistryEventConsumer<Retry> customRetryRegistryEventConsumer() {
        return new RegistryEventConsumer<Retry>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                log.info("Retry '{}' registered", retry.getName());
                
                // 재시도 이벤트 리스너 등록
                retry.getEventPublisher()
                    .onRetry(event -> {
                        log.warn(
                            "[RETRY] 재시도 시도 - Name: {}, Attempt: {}, Wait Time: {}ms, Exception: {}",
                            event.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getWaitInterval().toMillis(),
                            event.getLastThrowable() != null 
                                ? event.getLastThrowable().getClass().getSimpleName() 
                                : "N/A"
                        );
                    })
                    .onSuccess(event -> {
                        if (event.getNumberOfRetryAttempts() > 0) {
                            log.info(
                                "[RETRY] 재시도 성공 - Name: {}, Attempt: {}",
                                event.getName(),
                                event.getNumberOfRetryAttempts()
                            );
                        }
                    })
                    .onError(event -> {
                        log.error(
                            "[RETRY] 재시도 실패 - Name: {}, Attempt: {}, Exception: {}",
                            event.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable() != null 
                                ? event.getLastThrowable().getClass().getSimpleName() 
                                : "N/A",
                            event.getLastThrowable()
                        );
                    })
                    .onIgnoredError(event -> {
                        log.debug(
                            "[RETRY] 재시도 무시 - Name: {}, Exception: {} (재시도 대상이 아닌 예외)",
                            event.getName(),
                            event.getLastThrowable() != null 
                                ? event.getLastThrowable().getClass().getSimpleName() 
                                : "N/A"
                        );
                    });
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
                log.info("Retry '{}' removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                log.info("Retry '{}' replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
}

