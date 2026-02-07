import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ThreadLocalDemo.java - ThreadLocal 原理与使用演示
 *
 * 本程序演示：
 * 1. ThreadLocal 基本用法
 * 2. ThreadLocal 实现线程隔离
 * 3. SimpleDateFormat 线程安全问题与 ThreadLocal 解决方案
 * 4. ThreadLocal 内存泄漏问题演示
 * 5. ThreadLocal 最佳实践
 *
 * 运行方式：
 *   javac ThreadLocalDemo.java
 *   java ThreadLocalDemo
 */
public class ThreadLocalDemo {

    // ======== ThreadLocal 实例 ========

    // 用法1：为每个线程提供独立的 ID
    private static final ThreadLocal<Long> threadId = ThreadLocal.withInitial(() -> Thread.currentThread().getId());

    // 用法2：为每个线程提供独立的 SimpleDateFormat（解决线程安全问题）
    private static final ThreadLocal<SimpleDateFormat> dateFormat =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    // 用法3：模拟用户上下文传递
    private static final ThreadLocal<String> userContext = new ThreadLocal<>();

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  ThreadLocal 原理与使用演示");
        System.out.println("========================================\n");

        // 实验1：ThreadLocal 基本用法
        demonstrateBasicUsage();

        // 实验2：线程隔离
        demonstrateIsolation();

        // 实验3：SimpleDateFormat 线程安全问题
        demonstrateDateFormatSafety();

        // 实验4：模拟 Web 应用用户上下文传递
        demonstrateUserContext();

        // 实验5：内存泄漏问题演示与最佳实践
        demonstrateMemoryLeak();

        System.out.println("\n========================================");
        System.out.println("  所有实验完成！");
        System.out.println("========================================");
    }

    // ========================================================
    // 实验1：ThreadLocal 基本用法
    // ========================================================
    private static void demonstrateBasicUsage() throws Exception {
        System.out.println("--- 实验1：ThreadLocal 基本用法 ---");

        // 主线程
        System.out.println("  [主线程] threadId = " + threadId.get());
        System.out.println("  [主线程] 时间 = " + dateFormat.get().format(new Date()));

        // 子线程
        Thread t1 = new Thread(() -> {
            System.out.println("  [线程1] threadId = " + threadId.get());
            System.out.println("  [线程1] 时间 = " + dateFormat.get().format(new Date()));
        }, "thread-1");

        Thread t2 = new Thread(() -> {
            System.out.println("  [线程2] threadId = " + threadId.get());
            System.out.println("  [线程2] 时间 = " + dateFormat.get().format(new Date()));
        }, "thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  结论: 每个线程都有自己独立的 ThreadLocal 副本\n");
    }

    // ========================================================
    // 实验2：线程隔离演示
    // ========================================================
    private static void demonstrateIsolation() throws Exception {
        System.out.println("--- 实验2：线程隔离 ---");

        ThreadLocal<StringBuilder> builder = ThreadLocal.withInitial(StringBuilder::new);

        Thread t1 = new Thread(() -> {
            builder.get().append("A").append("B").append("C");
            System.out.println("  [线程1] builder = " + builder.get());
            builder.remove(); // 用完记得清理
        });

        Thread t2 = new Thread(() -> {
            builder.get().append("X").append("Y").append("Z");
            System.out.println("  [线程2] builder = " + builder.get());
            builder.remove();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  结论: 两个线程的 StringBuilder 互不影响\n");
    }

    // ========================================================
    // 实验3：SimpleDateFormat 线程安全问题
    // ========================================================
    private static void demonstrateDateFormatSafety() throws Exception {
        System.out.println("--- 实验3：SimpleDateFormat 线程安全问题 ---");

        // 反例：共享的 SimpleDateFormat（线程不安全！）
        SimpleDateFormat sharedFormat = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println("  【反例】共享 SimpleDateFormat：");

        int errorCount = 0;
        int totalRuns = 100;

        // 使用多线程并发格式化日期
        ExecutorService pool = Executors.newFixedThreadPool(10);
        java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < totalRuns; i++) {
            pool.submit(() -> {
                try {
                    String result = sharedFormat.format(new Date());
                    // 如果格式不对或抛异常，说明线程不安全
                    if (result.length() != 10) {
                        errors.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  共享 SimpleDateFormat 错误次数: " + errors.get() + "/" + totalRuns);

        // 正例：使用 ThreadLocal 的 SimpleDateFormat
        System.out.println("\n  【正例】ThreadLocal<SimpleDateFormat>：");
        ExecutorService pool2 = Executors.newFixedThreadPool(10);
        java.util.concurrent.atomic.AtomicInteger errors2 = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < totalRuns; i++) {
            pool2.submit(() -> {
                try {
                    String result = dateFormat.get().format(new Date());
                    if (result.length() != 19) { // "yyyy-MM-dd HH:mm:ss" 长度19
                        errors2.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors2.incrementAndGet();
                }
            });
        }

        pool2.shutdown();
        pool2.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  ThreadLocal<SimpleDateFormat> 错误次数: " + errors2.get() + "/" + totalRuns);
        System.out.println("  ✅ ThreadLocal 保证了每个线程使用独立的 SimpleDateFormat\n");
    }

    // ========================================================
    // 实验4：模拟 Web 应用用户上下文传递
    // ========================================================
    private static void demonstrateUserContext() throws Exception {
        System.out.println("--- 实验4：模拟 Web 应用用户上下文传递 ---");
        System.out.println("  模拟场景: 用户请求 → Controller → Service → DAO\n");

        // 模拟多个并发请求
        Thread request1 = new Thread(() -> {
            try {
                userContext.set("用户A（管理员）");
                handleRequest();
            } finally {
                userContext.remove(); // 最佳实践：finally 中清理
            }
        }, "request-1");

        Thread request2 = new Thread(() -> {
            try {
                userContext.set("用户B（普通用户）");
                handleRequest();
            } finally {
                userContext.remove();
            }
        }, "request-2");

        request1.start();
        request2.start();
        request1.join();
        request2.join();

        System.out.println("  ✅ 不同请求线程的用户上下文互不干扰\n");
    }

    // 模拟请求处理链路
    private static void handleRequest() {
        String threadName = Thread.currentThread().getName();
        System.out.println("  [" + threadName + "] Controller: 当前用户 = " + userContext.get());
        serviceLayer();
    }

    private static void serviceLayer() {
        String threadName = Thread.currentThread().getName();
        System.out.println("  [" + threadName + "] Service:    当前用户 = " + userContext.get());
        daoLayer();
    }

    private static void daoLayer() {
        String threadName = Thread.currentThread().getName();
        System.out.println("  [" + threadName + "] DAO:        当前用户 = " + userContext.get());
    }

    // ========================================================
    // 实验5：内存泄漏问题演示与最佳实践
    // ========================================================
    private static void demonstrateMemoryLeak() throws Exception {
        System.out.println("--- 实验5：内存泄漏问题与最佳实践 ---\n");

        System.out.println("  【内存泄漏原理】");
        System.out.println("  ThreadLocalMap 的 Entry 继承了 WeakReference<ThreadLocal<?>>");
        System.out.println("  Key（ThreadLocal 引用）是弱引用，Value 是强引用");
        System.out.println("  当 ThreadLocal 被 GC 回收后：");
        System.out.println("    - Key 变为 null（弱引用被回收）");
        System.out.println("    - Value 仍然被 Entry 强引用，无法回收");
        System.out.println("    - 这就是内存泄漏！\n");

        // 演示线程池 + ThreadLocal 的内存泄漏风险
        System.out.println("  【线程池中的风险】");
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // 反例：不清理 ThreadLocal
        System.out.println("  反例 - 不调用 remove():");
        for (int i = 0; i < 4; i++) {
            final int taskId = i;
            pool.submit(() -> {
                // 不清理！线程被复用时，上一个任务的数据还在
                String oldValue = userContext.get();
                System.out.println("    任务" + taskId + " 线程["
                        + Thread.currentThread().getName() + "] 旧值=" + oldValue);
                userContext.set("任务" + taskId + "的数据");
                // 没有 remove()！
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n  正例 - 始终调用 remove():");
        ExecutorService pool2 = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 4; i++) {
            final int taskId = i;
            pool2.submit(() -> {
                try {
                    String oldValue = userContext.get();
                    System.out.println("    任务" + taskId + " 线程["
                            + Thread.currentThread().getName() + "] 旧值=" + oldValue);
                    userContext.set("任务" + taskId + "的数据");
                } finally {
                    userContext.remove(); // ✅ 最佳实践：始终在 finally 中调用 remove()
                }
            });
        }

        pool2.shutdown();
        pool2.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n  【最佳实践总结】");
        System.out.println("  1. 使用完 ThreadLocal 后，始终在 finally 块中调用 remove()");
        System.out.println("  2. 将 ThreadLocal 声明为 private static final");
        System.out.println("  3. 在线程池环境中尤其要注意清理，因为线程会被复用");
        System.out.println("  4. 优先使用 ThreadLocal.withInitial() 设置初始值\n");
    }
}
