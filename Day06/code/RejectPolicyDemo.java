import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day06 - 拒绝策略专项演示
 *
 * 演示内容：
 * 1. 4 种内置拒绝策略的触发与效果
 * 2. CallerRunsPolicy 的反压（Back Pressure）机制
 * 3. 自定义拒绝策略（记录日志 + 计数 + 带超时重试）
 * 4. 不同参数配置下拒绝策略的触发时机
 */
public class RejectPolicyDemo {

    // ==================== 自定义线程工厂 ====================

    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String poolName) {
            this.namePrefix = poolName + "-worker-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) t.setDaemon(false);
            return t;
        }
    }

    // ==================== 自定义拒绝策略 ====================

    /**
     * 自定义拒绝策略：记录日志 + 统计拒绝次数
     */
    static class LoggingRejectPolicy implements RejectedExecutionHandler {
        private final AtomicInteger rejectedCount = new AtomicInteger(0);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            int count = rejectedCount.incrementAndGet();
            System.out.printf("  [自定义拒绝] 第 %d 次拒绝 | 活跃线程=%d, 队列大小=%d, 已完成=%d%n",
                    count,
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount());
        }

        public int getRejectedCount() {
            return rejectedCount.get();
        }
    }

    /**
     * 自定义拒绝策略：带超时的重试入队
     */
    static class RetryRejectPolicy implements RejectedExecutionHandler {
        private final long timeoutMs;
        private final AtomicInteger retryCount = new AtomicInteger(0);

        RetryRejectPolicy(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            int attempt = retryCount.incrementAndGet();
            System.out.printf("  [重试拒绝] 第 %d 次尝试重新入队（超时=%dms）...%n", attempt, timeoutMs);
            try {
                boolean offered = executor.getQueue().offer(r, timeoutMs, TimeUnit.MILLISECONDS);
                if (offered) {
                    System.out.printf("  [重试拒绝] 重试入队成功！%n");
                } else {
                    System.out.printf("  [重试拒绝] 重试超时，任务被丢弃！%n");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("  [重试拒绝] 重试被中断！%n");
            }
        }

        public int getRetryCount() {
            return retryCount.get();
        }
    }

    // ==================== 演示1：4种内置拒绝策略 ====================

    /**
     * 演示 4 种内置拒绝策略的对比
     *
     * 配置：corePoolSize=1, maximumPoolSize=2, queueCapacity=2
     * 最大同时处理：2（线程）+ 2（队列）= 4 个任务
     * 提交 8 个任务，观察拒绝行为
     */
    static void demo1_BuiltInPolicies() throws InterruptedException {
        System.out.println("========== 演示1：4种内置拒绝策略对比 ==========\n");

        System.out.println("配置：corePoolSize=1, maximumPoolSize=2, queueCapacity=2");
        System.out.println("最大同时处理：2（线程）+ 2（队列）= 4 个任务");
        System.out.println("提交 8 个任务，观察哪些被拒绝\n");

        // --- AbortPolicy ---
        System.out.println("--- 策略1：AbortPolicy（默认，抛出异常） ---");
        testPolicy(new ThreadPoolExecutor.AbortPolicy(), "Abort", 8);
        Thread.sleep(500);

        // --- CallerRunsPolicy ---
        System.out.println("--- 策略2：CallerRunsPolicy（调用者线程执行） ---");
        testPolicy(new ThreadPoolExecutor.CallerRunsPolicy(), "CallerRuns", 8);
        Thread.sleep(500);

        // --- DiscardPolicy ---
        System.out.println("--- 策略3：DiscardPolicy（静默丢弃） ---");
        testPolicy(new ThreadPoolExecutor.DiscardPolicy(), "Discard", 8);
        Thread.sleep(500);

        // --- DiscardOldestPolicy ---
        System.out.println("--- 策略4：DiscardOldestPolicy（丢弃最老任务） ---");
        testPolicy(new ThreadPoolExecutor.DiscardOldestPolicy(), "DiscardOld", 8);
        Thread.sleep(500);

        System.out.println();
    }

    private static void testPolicy(RejectedExecutionHandler handler, String name, int taskCount) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new NamedThreadFactory(name),
                handler
        );

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    String threadName = Thread.currentThread().getName();
                    System.out.printf("  [%s] 任务 %d 开始执行%n", threadName, taskId);
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    completedCount.incrementAndGet();
                    System.out.printf("  [%s] 任务 %d 完成%n", threadName, taskId);
                });
            } catch (RejectedExecutionException e) {
                rejectedCount.incrementAndGet();
                System.out.printf("  任务 %d 被拒绝！(RejectedExecutionException)%n", taskId);
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("  结果: 完成=%d/%d, 被异常拒绝=%d%n%n",
                completedCount.get(), taskCount, rejectedCount.get());
    }

    // ==================== 演示2：CallerRunsPolicy 反压效果 ====================

    /**
     * 专门演示 CallerRunsPolicy 的反压机制
     *
     * 观察点：
     * 1. 主线程（main）会被迫执行被拒绝的任务
     * 2. 执行期间主线程无法提交新任务 → 自然降速
     * 3. 最终所有任务都能完成（不丢任务）
     */
    static void demo2_CallerRunsBackPressure() throws InterruptedException {
        System.out.println("========== 演示2：CallerRunsPolicy 反压效果 ==========\n");

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 3, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new NamedThreadFactory("BackPressure"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        System.out.println("配置：core=2, max=3, queue=2, 总容量=5");
        System.out.println("提交 12 个任务，观察主线程被迫执行任务的情况\n");

        long startTime = System.currentTimeMillis();
        AtomicInteger mainThreadExecuted = new AtomicInteger(0);

        for (int i = 1; i <= 12; i++) {
            final int taskId = i;
            long submitTime = System.currentTimeMillis() - startTime;
            System.out.printf("[%4dms] 准备提交任务 %d...%n", submitTime, taskId);

            pool.execute(() -> {
                String threadName = Thread.currentThread().getName();
                if (threadName.equals("main")) {
                    mainThreadExecuted.incrementAndGet();
                }
                System.out.printf("  [%s] 任务 %d 开始执行%n", threadName, taskId);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.printf("  [%s] 任务 %d 完成%n", threadName, taskId);
            });

            long afterSubmit = System.currentTimeMillis() - startTime;
            System.out.printf("[%4dms] 任务 %d 提交/执行完成%n", afterSubmit, taskId);
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("%n反压效果统计:%n");
        System.out.printf("  总耗时: %dms%n", totalTime);
        System.out.printf("  主线程（main）执行了 %d 个任务%n", mainThreadExecuted.get());
        System.out.printf("  结论: CallerRunsPolicy 让调用者降速，确保不丢任务%n%n");
    }

    // ==================== 演示3：自定义拒绝策略 ====================

    static void demo3_CustomRejectPolicy() throws InterruptedException {
        System.out.println("========== 演示3：自定义拒绝策略 ==========\n");

        // --- 自定义策略1：日志记录 ---
        System.out.println("--- 策略A：日志记录 + 统计 ---\n");

        LoggingRejectPolicy loggingPolicy = new LoggingRejectPolicy();
        ThreadPoolExecutor pool1 = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new NamedThreadFactory("Logging"),
                loggingPolicy
        );

        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            pool1.execute(() -> {
                try { Thread.sleep(500); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        pool1.shutdown();
        pool1.awaitTermination(10, TimeUnit.SECONDS);
        System.out.printf("  总拒绝次数: %d%n%n", loggingPolicy.getRejectedCount());

        // --- 自定义策略2：带超时重试 ---
        System.out.println("--- 策略B：带超时重试入队 ---\n");

        RetryRejectPolicy retryPolicy = new RetryRejectPolicy(2000); // 2秒超时
        ThreadPoolExecutor pool2 = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new NamedThreadFactory("Retry"),
                retryPolicy
        );

        AtomicInteger completed = new AtomicInteger(0);
        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            pool2.execute(() -> {
                System.out.printf("  [%s] 任务 %d 执行中%n",
                        Thread.currentThread().getName(), taskId);
                try { Thread.sleep(800); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                completed.incrementAndGet();
            });
        }

        pool2.shutdown();
        pool2.awaitTermination(15, TimeUnit.SECONDS);
        System.out.printf("  总重试次数: %d, 完成任务: %d/6%n%n",
                retryPolicy.getRetryCount(), completed.get());
    }

    // ==================== 演示4：不同队列对拒绝策略的影响 ====================

    static void demo4_QueueImpactOnRejection() throws InterruptedException {
        System.out.println("========== 演示4：不同队列对拒绝策略的影响 ==========\n");

        // --- 有界队列：ArrayBlockingQueue ---
        System.out.println("--- 有界队列 ArrayBlockingQueue(3) ---");
        testQueueType(new ArrayBlockingQueue<>(3), "Bounded", 10);

        Thread.sleep(500);

        // --- 无界队列：LinkedBlockingQueue ---
        System.out.println("--- 无界队列 LinkedBlockingQueue ---");
        System.out.println("  注意：无界队列永远不会触发拒绝策略！");
        testQueueType(new LinkedBlockingQueue<>(), "Unbounded", 10);

        Thread.sleep(500);

        // --- SynchronousQueue ---
        System.out.println("--- SynchronousQueue（不存储元素） ---");
        System.out.println("  注意：每个任务必须直接有线程接手！");
        testQueueType(new SynchronousQueue<>(), "Sync", 10);

        System.out.println();
    }

    private static void testQueueType(BlockingQueue<Runnable> queue, String name, int taskCount) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                queue,
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.AbortPolicy()
        );

        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 1; i <= taskCount; i++) {
            try {
                pool.execute(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    completed.incrementAndGet();
                });
            } catch (RejectedExecutionException e) {
                rejected.incrementAndGet();
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("  完成: %d/%d, 拒绝: %d%n%n",
                completed.get(), taskCount, rejected.get());
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws InterruptedException {
        demo1_BuiltInPolicies();
        demo2_CallerRunsBackPressure();
        demo3_CustomRejectPolicy();
        demo4_QueueImpactOnRejection();

        System.out.println("========== 所有演示完成 ==========");
    }
}
