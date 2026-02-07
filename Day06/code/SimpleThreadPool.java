import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day06 - 手写简易线程池
 *
 * 实现要点：
 * 1. 核心线程池 - 固定数量的 Worker 线程
 * 2. 阻塞队列 - 缓冲待执行任务
 * 3. 拒绝策略 - 队列满时的处理方式
 * 4. 优雅关闭 - shutdown + awaitTermination
 *
 * 目标：理解 ThreadPoolExecutor 的核心工作原理
 */
public class SimpleThreadPool {

    // ==================== 拒绝策略接口 ====================

    /**
     * 拒绝策略接口
     */
    @FunctionalInterface
    interface RejectPolicy {
        void reject(Runnable task, SimpleThreadPool pool);
    }

    /**
     * 默认拒绝策略：抛出异常
     */
    static class AbortRejectPolicy implements RejectPolicy {
        @Override
        public void reject(Runnable task, SimpleThreadPool pool) {
            throw new RuntimeException("[拒绝策略] 线程池已满，任务被拒绝！" +
                    " 队列大小=" + pool.taskQueue.size() +
                    " 工作线程=" + pool.workerCount.get());
        }
    }

    /**
     * 调用者执行策略：由提交任务的线程自己执行
     */
    static class CallerRunsRejectPolicy implements RejectPolicy {
        @Override
        public void reject(Runnable task, SimpleThreadPool pool) {
            if (!pool.isStopped.get()) {
                System.out.printf("  [拒绝策略] 由调用者线程 [%s] 执行任务%n",
                        Thread.currentThread().getName());
                task.run();
            }
        }
    }

    // ==================== Worker 工作线程 ====================

    /**
     * Worker 线程：从任务队列取任务并执行
     */
    class Worker implements Runnable {
        private final String name;

        Worker(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            System.out.printf("  [%s] Worker 启动%n", name);
            // 核心循环：不断从队列取任务执行
            while (!isStopped.get() || !taskQueue.isEmpty()) {
                try {
                    // 从阻塞队列取任务（poll 带超时，避免永久阻塞）
                    Runnable task = taskQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);
                    if (task != null) {
                        System.out.printf("  [%s] 取到任务，开始执行%n", name);
                        try {
                            task.run();
                        } catch (Exception e) {
                            System.out.printf("  [%s] 任务执行异常: %s%n", name, e.getMessage());
                        }
                        System.out.printf("  [%s] 任务执行完毕%n", name);
                    }
                } catch (InterruptedException e) {
                    System.out.printf("  [%s] Worker 被中断，准备退出%n", name);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("  [%s] Worker 退出%n", name);
            workerCount.decrementAndGet();
        }
    }

    // ==================== 线程池核心字段 ====================

    /** 核心线程数 */
    private final int corePoolSize;

    /** 任务队列（有界阻塞队列） */
    private final BlockingQueue<Runnable> taskQueue;

    /** 拒绝策略 */
    private final RejectPolicy rejectPolicy;

    /** 工作线程列表 */
    private final List<Thread> workers;

    /** 是否已停止 */
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    /** 当前工作线程数 */
    private final AtomicInteger workerCount = new AtomicInteger(0);

    /** 已提交任务数（统计用） */
    private final AtomicInteger submittedCount = new AtomicInteger(0);

    /** 线程编号 */
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    // ==================== 构造方法 ====================

    /**
     * 创建简易线程池
     *
     * @param corePoolSize 核心线程数
     * @param queueCapacity 任务队列容量
     * @param rejectPolicy 拒绝策略
     */
    public SimpleThreadPool(int corePoolSize, int queueCapacity, RejectPolicy rejectPolicy) {
        if (corePoolSize <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException("corePoolSize 和 queueCapacity 必须大于 0");
        }
        this.corePoolSize = corePoolSize;
        this.taskQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.rejectPolicy = rejectPolicy;
        this.workers = Collections.synchronizedList(new ArrayList<>());

        // 预先创建所有核心线程
        System.out.printf("[SimpleThreadPool] 初始化: corePoolSize=%d, queueCapacity=%d%n",
                corePoolSize, queueCapacity);
        for (int i = 0; i < corePoolSize; i++) {
            createWorker();
        }
    }

    /**
     * 使用默认拒绝策略创建线程池
     */
    public SimpleThreadPool(int corePoolSize, int queueCapacity) {
        this(corePoolSize, queueCapacity, new AbortRejectPolicy());
    }

    // ==================== 核心方法 ====================

    /**
     * 创建并启动一个 Worker 线程
     */
    private void createWorker() {
        String name = "SimplePool-worker-" + threadNumber.getAndIncrement();
        Worker worker = new Worker(name);
        Thread thread = new Thread(worker, name);
        thread.setDaemon(false);
        workers.add(thread);
        workerCount.incrementAndGet();
        thread.start();
    }

    /**
     * 提交任务到线程池
     *
     * @param task 待执行的任务
     */
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("任务不能为 null");
        }
        if (isStopped.get()) {
            throw new RuntimeException("线程池已关闭，无法提交新任务");
        }

        // 尝试放入队列
        boolean offered = taskQueue.offer(task);
        if (offered) {
            int count = submittedCount.incrementAndGet();
            System.out.printf("[SimpleThreadPool] 任务 #%d 已加入队列（队列大小=%d）%n",
                    count, taskQueue.size());
        } else {
            // 队列满了，触发拒绝策略
            System.out.printf("[SimpleThreadPool] 队列已满！触发拒绝策略%n");
            rejectPolicy.reject(task, this);
        }
    }

    /**
     * 优雅关闭线程池
     * - 不再接受新任务
     * - 等待已提交的任务执行完毕
     */
    public void shutdown() {
        System.out.println("[SimpleThreadPool] 开始关闭...");
        isStopped.set(true);
    }

    /**
     * 等待线程池终止
     *
     * @param timeoutMillis 最大等待时间（毫秒）
     */
    public void awaitTermination(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (Thread worker : workers) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) break;
            worker.join(remaining);
        }
        System.out.printf("[SimpleThreadPool] 已关闭（剩余工作线程=%d）%n", workerCount.get());
    }

    /**
     * 立即关闭：中断所有线程
     */
    public void shutdownNow() {
        System.out.println("[SimpleThreadPool] 立即关闭，中断所有线程...");
        isStopped.set(true);
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }

    /**
     * 获取线程池状态信息
     */
    public String getStatus() {
        return String.format("线程数=%d, 队列大小=%d, 已提交=%d, isStopped=%s",
                workerCount.get(), taskQueue.size(), submittedCount.get(), isStopped.get());
    }

    // ==================== 测试主方法 ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 测试1：基本功能测试 ==========\n");
        testBasicFunction();

        Thread.sleep(1000);
        System.out.println("\n========== 测试2：拒绝策略测试 ==========\n");
        testRejectPolicy();

        Thread.sleep(1000);
        System.out.println("\n========== 测试3：CallerRuns 拒绝策略 ==========\n");
        testCallerRunsPolicy();

        System.out.println("\n========== 所有测试完成 ==========");
    }

    /**
     * 测试1：基本功能 - 提交任务、执行、关闭
     */
    static void testBasicFunction() throws InterruptedException {
        // 创建线程池：3个核心线程，队列容量5
        SimpleThreadPool pool = new SimpleThreadPool(3, 5);

        System.out.println("\n提交 8 个任务：\n");

        // 提交 8 个任务（3 个线程 + 5 个队列 = 最多 8 个任务可被接受）
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        System.out.printf("%n线程池状态: %s%n", pool.getStatus());

        // 关闭线程池
        pool.shutdown();
        pool.awaitTermination(10000);
    }

    /**
     * 测试2：队列满时的 AbortPolicy
     */
    static void testRejectPolicy() throws InterruptedException {
        // 创建线程池：2个核心线程，队列容量2
        SimpleThreadPool pool = new SimpleThreadPool(2, 2);

        System.out.println("\n提交 6 个任务（超出容量会触发拒绝）：\n");

        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RuntimeException e) {
                System.out.printf("  任务 #%d 被拒绝: %s%n", taskId, e.getMessage());
            }
        }

        pool.shutdown();
        pool.awaitTermination(10000);
    }

    /**
     * 测试3：CallerRunsPolicy
     */
    static void testCallerRunsPolicy() throws InterruptedException {
        // 使用 CallerRunsRejectPolicy
        SimpleThreadPool pool = new SimpleThreadPool(2, 2, new CallerRunsRejectPolicy());

        System.out.println("\n提交 6 个任务（多余任务由调用者线程执行）：\n");

        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            pool.execute(() -> {
                System.out.printf("  任务 #%d 在 [%s] 上执行%n",
                        taskId, Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(10000);
    }
}
