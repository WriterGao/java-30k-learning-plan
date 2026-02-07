import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day06 - 线程池工作流程与拒绝策略演示
 *
 * 演示内容：
 * 1. ThreadPoolExecutor 核心参数配置
 * 2. 线程池工作流程（核心线程→队列→最大线程→拒绝）
 * 3. 4 种拒绝策略的效果
 * 4. execute vs submit 的异常处理区别
 * 5. 自定义 ThreadFactory
 * 6. 线程池关闭方式对比
 */
public class ThreadPoolDemo {

    // ==================== 自定义线程工厂 ====================

    /**
     * 自定义 ThreadFactory，为线程设置有意义的名称
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String poolName) {
            this.namePrefix = poolName + "-worker-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            // 设置为非守护线程
            if (t.isDaemon()) t.setDaemon(false);
            // 设置默认优先级
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    // ==================== 演示1：线程池工作流程 ====================

    /**
     * 演示任务提交 → 核心线程 → 队列 → 最大线程 → 拒绝 的完整流程
     *
     * 配置：corePoolSize=2, maximumPoolSize=4, 队列容量=2
     * 效果：最多同时处理 4 个任务，队列缓冲 2 个，超过 6 个就触发拒绝
     */
    static void demo1_WorkFlow() throws InterruptedException {
        System.out.println("========== 演示1：线程池工作流程 ==========\n");

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2,                              // corePoolSize：核心线程数
                4,                              // maximumPoolSize：最大线程数
                60L,                            // keepAliveTime：非核心线程空闲存活时间
                TimeUnit.SECONDS,               // unit：时间单位
                new ArrayBlockingQueue<>(2),     // workQueue：有界阻塞队列，容量为2
                new NamedThreadFactory("demo1"), // threadFactory：自定义线程工厂
                new ThreadPoolExecutor.AbortPolicy()  // handler：拒绝策略（抛异常）
        );

        System.out.println("线程池初始状态：");
        printPoolStatus(pool);
        System.out.println();

        // 提交 7 个任务，观察线程池行为
        for (int i = 1; i <= 7; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    System.out.printf("  [%s] 执行任务 %d 开始%n",
                            Thread.currentThread().getName(), taskId);
                    try {
                        Thread.sleep(2000); // 模拟任务耗时
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.printf("  [%s] 执行任务 %d 完成%n",
                            Thread.currentThread().getName(), taskId);
                });
                System.out.printf("任务 %d 提交成功 → ", taskId);
                printPoolStatus(pool);
            } catch (RejectedExecutionException e) {
                System.out.printf("任务 %d 被拒绝！（线程池已满）%n", taskId);
            }
        }

        /*
         * 预期行为：
         * 任务1 → 创建核心线程1 执行
         * 任务2 → 创建核心线程2 执行
         * 任务3 → 核心线程满，进入队列（队列: [3]）
         * 任务4 → 核心线程满，进入队列（队列: [3,4]）
         * 任务5 → 队列满，创建非核心线程3 执行
         * 任务6 → 队列满，创建非核心线程4 执行
         * 任务7 → 线程已达最大+队列满 → 触发拒绝策略！
         */

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("\n线程池已关闭\n");
    }

    // ==================== 演示2：4种拒绝策略 ====================

    static void demo2_RejectionPolicies() throws InterruptedException {
        System.out.println("========== 演示2：4种拒绝策略对比 ==========\n");

        // --- AbortPolicy：抛出异常 ---
        System.out.println("--- 策略1：AbortPolicy（默认，抛出异常）---");
        testRejectionPolicy(new ThreadPoolExecutor.AbortPolicy(), "AbortPolicy");

        Thread.sleep(500);

        // --- CallerRunsPolicy：调用者线程执行 ---
        System.out.println("--- 策略2：CallerRunsPolicy（调用者线程执行）---");
        testRejectionPolicy(new ThreadPoolExecutor.CallerRunsPolicy(), "CallerRunsPolicy");

        Thread.sleep(500);

        // --- DiscardPolicy：默默丢弃 ---
        System.out.println("--- 策略3：DiscardPolicy（静默丢弃）---");
        testRejectionPolicy(new ThreadPoolExecutor.DiscardPolicy(), "DiscardPolicy");

        Thread.sleep(500);

        // --- DiscardOldestPolicy：丢弃队头任务 ---
        System.out.println("--- 策略4：DiscardOldestPolicy（丢弃最老任务）---");
        testRejectionPolicy(new ThreadPoolExecutor.DiscardOldestPolicy(), "DiscardOldestPolicy");

        Thread.sleep(1000);
        System.out.println();
    }

    /**
     * 通用测试方法：创建一个 core=1, max=1, queue=1 的线程池
     * 然后提交 4 个任务，观察拒绝行为
     */
    private static void testRejectionPolicy(RejectedExecutionHandler handler, String policyName) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new NamedThreadFactory(policyName),
                handler
        );

        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 1; i <= 4; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    System.out.printf("  [%s] 执行任务 %d%n",
                            Thread.currentThread().getName(), taskId);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    completedCount.incrementAndGet();
                });
                System.out.printf("  任务 %d 提交成功%n", taskId);
            } catch (RejectedExecutionException e) {
                System.out.printf("  任务 %d 被拒绝！异常: %s%n", taskId, e.getMessage().substring(0, 60) + "...");
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("  完成任务数: %d/4%n%n", completedCount.get());
    }

    // ==================== 演示3：execute vs submit ====================

    static void demo3_ExecuteVsSubmit() throws InterruptedException {
        System.out.println("========== 演示3：execute vs submit 异常处理 ==========\n");

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("demo3")
        );

        // --- execute 提交的任务抛异常 ---
        System.out.println("--- execute 方式（异常会直接抛出，线程终止）---");

        // 设置未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                System.out.printf("  UncaughtExceptionHandler 捕获: [%s] %s%n", t.getName(), e.getMessage()));

        pool.execute(() -> {
            System.out.println("  execute 提交的任务开始执行...");
            throw new RuntimeException("execute 任务抛出的异常！");
        });

        Thread.sleep(500);
        System.out.println();

        // --- submit 提交的任务抛异常 ---
        System.out.println("--- submit 方式（异常被 Future 封装，不主动获取就静默吞掉）---");

        Future<?> future = pool.submit(() -> {
            System.out.println("  submit 提交的任务开始执行...");
            throw new RuntimeException("submit 任务抛出的异常！");
        });

        Thread.sleep(500);
        System.out.println("  submit 任务已执行完毕，异常似乎消失了...");

        // 通过 Future.get() 获取异常
        try {
            future.get();
        } catch (ExecutionException e) {
            System.out.printf("  Future.get() 捕获到异常: %s%n", e.getCause().getMessage());
        }

        System.out.println();

        // --- submit + Callable，获取返回值 ---
        System.out.println("--- submit + Callable（有返回值）---");
        Future<String> resultFuture = pool.submit(() -> {
            Thread.sleep(200);
            return "计算结果: 42";
        });
        try {
            String result = resultFuture.get(2, TimeUnit.SECONDS);
            System.out.printf("  Callable 返回值: %s%n", result);
        } catch (ExecutionException | TimeoutException e) {
            System.out.println("  获取结果失败: " + e.getMessage());
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("\n线程池已关闭\n");
    }

    // ==================== 演示4：shutdown vs shutdownNow ====================

    static void demo4_ShutdownComparison() throws InterruptedException {
        System.out.println("========== 演示4：shutdown vs shutdownNow 对比 ==========\n");

        // --- shutdown ---
        System.out.println("--- shutdown()：优雅关闭，等待已提交任务完成 ---");
        ThreadPoolExecutor pool1 = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("shutdown-pool")
        );

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            pool1.execute(() -> {
                System.out.printf("  [shutdown-pool] 任务 %d 开始%n", taskId);
                try { Thread.sleep(1000); } catch (InterruptedException e) {
                    System.out.printf("  [shutdown-pool] 任务 %d 被中断%n", taskId);
                    Thread.currentThread().interrupt();
                }
                System.out.printf("  [shutdown-pool] 任务 %d 完成%n", taskId);
            });
        }

        pool1.shutdown(); // 不再接受新任务，但会执行完队列中的任务
        System.out.println("  shutdown() 已调用，isShutdown=" + pool1.isShutdown());

        try {
            pool1.execute(() -> System.out.println("  新任务"));
        } catch (RejectedExecutionException e) {
            System.out.println("  shutdown 后提交新任务被拒绝！");
        }

        pool1.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("  所有任务执行完毕，isTerminated=" + pool1.isTerminated());
        System.out.println();

        // --- shutdownNow ---
        System.out.println("--- shutdownNow()：立即关闭，中断执行中任务，返回未执行任务 ---");
        ThreadPoolExecutor pool2 = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("shutdownNow-pool")
        );

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            pool2.execute(() -> {
                System.out.printf("  [shutdownNow-pool] 任务 %d 开始%n", taskId);
                try { Thread.sleep(3000); } catch (InterruptedException e) {
                    System.out.printf("  [shutdownNow-pool] 任务 %d 被中断！%n", taskId);
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(500); // 等部分任务开始执行
        java.util.List<Runnable> notExecuted = pool2.shutdownNow();
        System.out.printf("  shutdownNow() 返回 %d 个未执行的任务%n", notExecuted.size());

        pool2.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  isTerminated=" + pool2.isTerminated());
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    static void printPoolStatus(ThreadPoolExecutor pool) {
        System.out.printf("核心=%d, 活跃=%d, 最大=%d, 已创建最大=%d, 队列=%d, 已完成=%d%n",
                pool.getCorePoolSize(),
                pool.getActiveCount(),
                pool.getMaximumPoolSize(),
                pool.getLargestPoolSize(),
                pool.getQueue().size(),
                pool.getCompletedTaskCount());
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws InterruptedException {
        demo1_WorkFlow();
        demo2_RejectionPolicies();
        demo3_ExecuteVsSubmit();
        demo4_ShutdownComparison();

        System.out.println("========== 所有演示完成 ==========");
    }
}
