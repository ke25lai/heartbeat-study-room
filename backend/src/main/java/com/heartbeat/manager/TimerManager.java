package com.heartbeat.manager;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * 番茄钟定时任务管理器。
 *
 * 用 ScheduledThreadPoolExecutor 而非 Timer：
 * - Timer 单线程，一个任务抛异常会终止整个调度；
 * - ScheduledThreadPoolExecutor 多线程、任务异常不影响其它任务。
 * 虚拟线程开启后，这里的工作线程也由 JVM 调度。
 */
@Component
public class TimerManager {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "pomodoro-timer");
        t.setDaemon(true);
        return t;
    });

    /** roomId:userId -> 定时任务句柄 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public void schedule(String key, Runnable task, long delaySeconds) {
        cancel(key);
        ScheduledFuture<?> f = scheduler.schedule(task, delaySeconds, TimeUnit.SECONDS);
        tasks.put(key, f);
    }

    public void cancel(String key) {
        ScheduledFuture<?> f = tasks.remove(key);
        if (f != null) f.cancel(false);
    }

    public static String key(Long roomId, Long userId) { return roomId + ":" + userId; }
}
