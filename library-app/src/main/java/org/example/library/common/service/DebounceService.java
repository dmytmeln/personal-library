package org.example.library.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class DebounceService {

    private final ThreadPoolTaskScheduler debounceTaskScheduler;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();


    public void debounce(String key, Runnable task, Duration delay) {
        ScheduledFuture<?> next = debounceTaskScheduler.schedule(task, Instant.now().plus(delay));

        ScheduledFuture<?> prev = pending.put(key, next);
        if (prev != null) {
            prev.cancel(false);
        }
    }

}
