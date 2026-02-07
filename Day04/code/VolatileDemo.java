import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VolatileDemo.java - volatile 可见性与有序性验证
 *
 * 本程序演示：
 * 1. 不使用 volatile 时的可见性问题
 * 2. 使用 volatile 解决可见性问题
 * 3. volatile 不保证原子性
 * 4. volatile 的正确使用场景（状态标志、双重检查锁定）
 *
 * 运行方式：
 *   javac VolatileDemo.java
 *   java VolatileDemo
 *
 * 注意：可见性问题可能不是每次都能复现，
 *       因为 JVM 的即时编译优化和 CPU 缓存行为具有不确定性。
 *       多运行几次或使用 -server 参数增加复现概率。
 */
public class VolatileDemo {

    // ======== 不使用 volatile ========
    private static boolean running = true; // 无 volatile

    // ======== 使用 volatile ========
    private static volatile boolean volatileRunning = true; // 有 volatile

    // ======== 原子性测试 ========
    private static volatile int volatileCount = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  volatile 可见性与有序性验证");
        System.out.println("========================================\n");

        // 实验1：不使用 volatile 的可见性问题
        demonstrateVisibilityProblem();

        // 实验2：volatile 解决可见性问题
        demonstrateVolatileFix();

        // 实验3：volatile 不保证原子性
        demonstrateNonAtomicity();

        // 实验4：volatile 的正确使用场景
        demonstrateCorrectUsage();

        System.out.println("\n========================================");
        System.out.println("  所有实验完成！");
        System.out.println("========================================");
    }

    // ========================================================
    // 实验1：可见性问题复现
    // ========================================================
    private static void demonstrateVisibilityProblem() throws Exception {
        System.out.println("--- 实验1：不使用 volatile 的可见性问题 ---");
        System.out.println("  说明: 主线程修改 running=false，工作线程可能看不到更新");
        System.out.println("  （注意: 此问题不一定每次复现，取决于 JVM 和 CPU）\n");

        running = true;

        Thread worker = new Thread(() -> {
            int count = 0;
            while (running) {
                count++;
            }
            System.out.println("  [工作线程] 退出循环，循环次数: " + count);
        }, "no-volatile-worker");

        worker.start();
        Thread.sleep(100); // 让工作线程运行一会儿

        System.out.println("  [主线程] 设置 running = false");
        running = false;

        // 等待最多 2 秒
        worker.join(2000);

        if (worker.isAlive()) {
            System.out.println("  ❌ 可见性问题复现！工作线程没有看到 running 的更新");
            System.out.println("     工作线程仍在运行，强制停止...");
            worker.interrupt();
            // 设置一个标志让循环也检查 interrupt
            running = false;
            worker.join(1000);
        } else {
            System.out.println("  ⚠️ 本次未复现可见性问题（JVM 可能刷新了缓存）");
            System.out.println("     尝试使用 java -server VolatileDemo 增加复现概率");
        }
        System.out.println();
    }

    // ========================================================
    // 实验2：volatile 解决可见性问题
    // ========================================================
    private static void demonstrateVolatileFix() throws Exception {
        System.out.println("--- 实验2：使用 volatile 解决可见性问题 ---");

        volatileRunning = true;

        Thread worker = new Thread(() -> {
            int count = 0;
            while (volatileRunning) {
                count++;
            }
            System.out.println("  [工作线程] 退出循环，循环次数: " + count);
        }, "volatile-worker");

        worker.start();
        Thread.sleep(100);

        System.out.println("  [主线程] 设置 volatileRunning = false");
        volatileRunning = false;

        worker.join(2000);

        if (!worker.isAlive()) {
            System.out.println("  ✅ volatile 生效！工作线程成功看到更新并退出");
        } else {
            System.out.println("  异常：工作线程仍在运行");
            worker.interrupt();
        }
        System.out.println();
    }

    // ========================================================
    // 实验3：volatile 不保证原子性
    // ========================================================
    private static void demonstrateNonAtomicity() throws Exception {
        System.out.println("--- 实验3：volatile 不保证原子性 ---");
        System.out.println("  说明: 即使使用 volatile，count++ 仍然不是原子操作\n");

        volatileCount = 0;
        int threadCount = 10;
        int loopCount = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    volatileCount++; // volatile 不保证原子性！
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        int expected = threadCount * loopCount;
        System.out.println("  期望值: " + expected);
        System.out.println("  实际值: " + volatileCount);
        System.out.println("  结论: " + (volatileCount == expected
                ? "偶然正确（不代表原子性）"
                : "❌ volatile 不保证原子性！差值 = " + (expected - volatileCount)));

        // 使用 AtomicInteger 对比
        AtomicInteger atomicCount = new AtomicInteger(0);
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    atomicCount.incrementAndGet();
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();

        System.out.println("\n  AtomicInteger 对比:");
        System.out.println("  期望值: " + expected);
        System.out.println("  实际值: " + atomicCount.get());
        System.out.println("  结论: ✅ AtomicInteger（CAS）保证了原子性");
        System.out.println();
    }

    // ========================================================
    // 实验4：volatile 的正确使用场景
    // ========================================================
    private static void demonstrateCorrectUsage() throws Exception {
        System.out.println("--- 实验4：volatile 的正确使用场景 ---");

        // 场景A：状态标志（volatile 最典型用法）
        System.out.println("  【场景A】状态标志（推荐使用 volatile）");
        GracefulShutdown server = new GracefulShutdown();
        Thread serverThread = new Thread(server::run, "server");
        serverThread.start();
        Thread.sleep(200);
        server.shutdown();
        serverThread.join(2000);
        System.out.println("  ✅ 服务优雅停机完成\n");

        // 场景B：双重检查锁定（DCL）单例模式
        System.out.println("  【场景B】双重检查锁定（DCL）单例模式");
        System.out.println("  Singleton 实例: " + Singleton.getInstance());
        System.out.println("  再次获取: " + Singleton.getInstance());
        System.out.println("  是否同一实例: " + (Singleton.getInstance() == Singleton.getInstance()));
        System.out.println("  ✅ volatile 防止指令重排序，确保 DCL 正确性\n");
    }

    /**
     * 场景A：优雅停机 - volatile 用作状态标志
     */
    static class GracefulShutdown {
        private volatile boolean stopped = false;

        public void run() {
            System.out.println("    服务启动...");
            int taskCount = 0;
            while (!stopped) {
                // 模拟处理任务
                taskCount++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("    服务停止，共处理 " + taskCount + " 个任务");
        }

        public void shutdown() {
            System.out.println("    发送停机信号...");
            stopped = true;
        }
    }

    /**
     * 场景B：DCL 单例模式 - volatile 防止指令重排序
     *
     * 为什么需要 volatile？
     * instance = new Singleton() 实际分三步：
     *   1. 分配内存空间
     *   2. 初始化对象
     *   3. 将 instance 指向内存空间
     * 如果没有 volatile，步骤 2 和 3 可能重排序，
     * 导致其他线程看到一个未初始化完成的对象。
     */
    static class Singleton {
        private static volatile Singleton instance;

        private Singleton() {
            System.out.println("    Singleton 构造函数执行");
        }

        public static Singleton getInstance() {
            if (instance == null) {               // 第一次检查（无锁）
                synchronized (Singleton.class) {
                    if (instance == null) {        // 第二次检查（有锁）
                        instance = new Singleton();
                    }
                }
            }
            return instance;
        }

        @Override
        public String toString() {
            return "Singleton@" + Integer.toHexString(hashCode());
        }
    }
}
