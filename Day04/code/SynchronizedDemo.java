import java.util.concurrent.CountDownLatch;

/**
 * SynchronizedDemo.java - synchronized 锁机制演示
 *
 * 本程序演示：
 * 1. 线程安全问题复现（不加锁）
 * 2. synchronized 修复线程安全问题
 * 3. 对象锁 vs 类锁
 * 4. synchronized 可重入性
 * 5. 死锁演示与排查
 *
 * 运行方式：
 *   javac SynchronizedDemo.java
 *   java SynchronizedDemo
 *
 * 死锁排查：
 *   jps                 # 获取进程ID
 *   jstack <pid>        # 查看是否有 "Found one Java-level deadlock"
 */
public class SynchronizedDemo {

    // ======== 共享变量 ========
    private static int unsafeCount = 0;
    private static int safeCount = 0;
    private static final Object countLock = new Object();

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  synchronized 锁机制演示");
        System.out.println("========================================\n");

        // 实验1：不加锁的线程安全问题
        demonstrateUnsafe();

        // 实验2：synchronized 修复
        demonstrateSafe();

        // 实验3：对象锁 vs 类锁
        demonstrateLockTypes();

        // 实验4：synchronized 可重入性
        demonstrateReentrant();

        // 实验5：死锁演示
        demonstrateDeadlock();

        System.out.println("\n========================================");
        System.out.println("  所有实验完成！");
        System.out.println("========================================");
    }

    // ========================================================
    // 实验1：不加锁的线程安全问题
    // ========================================================
    private static void demonstrateUnsafe() throws Exception {
        System.out.println("--- 实验1：不加锁的线程安全问题 ---");

        unsafeCount = 0;
        int threadCount = 10;
        int loopCount = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    unsafeCount++; // 非原子操作：读-改-写
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        int expected = threadCount * loopCount;
        System.out.println("  期望值: " + expected);
        System.out.println("  实际值: " + unsafeCount);
        System.out.println("  结论: " + (unsafeCount == expected
                ? "偶然正确（不代表线程安全）"
                : "❌ 数据丢失！差值 = " + (expected - unsafeCount)));
        System.out.println("  原因: count++ 不是原子操作（读-改-写三步）\n");
    }

    // ========================================================
    // 实验2：synchronized 修复线程安全问题
    // ========================================================
    private static void demonstrateSafe() throws Exception {
        System.out.println("--- 实验2：synchronized 修复线程安全问题 ---");

        safeCount = 0;
        int threadCount = 10;
        int loopCount = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    synchronized (countLock) {
                        safeCount++; // 在同步块内操作
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        int expected = threadCount * loopCount;
        System.out.println("  期望值: " + expected);
        System.out.println("  实际值: " + safeCount);
        System.out.println("  结论: " + (safeCount == expected
                ? "✅ 线程安全！synchronized 保证了原子性"
                : "异常情况"));
        System.out.println();
    }

    // ========================================================
    // 实验3：对象锁 vs 类锁
    // ========================================================
    private static void demonstrateLockTypes() throws Exception {
        System.out.println("--- 实验3：对象锁 vs 类锁 ---");

        LockTypeDemo demo1 = new LockTypeDemo();
        LockTypeDemo demo2 = new LockTypeDemo();

        System.out.println("  【测试A】同一对象的两个同步方法 → 互斥");
        Thread t1 = new Thread(() -> demo1.syncMethodA(), "objLock-A");
        Thread t2 = new Thread(() -> demo1.syncMethodB(), "objLock-B");
        t1.start();
        Thread.sleep(50);
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\n  【测试B】不同对象的同步方法 → 不互斥");
        Thread t3 = new Thread(() -> demo1.syncMethodA(), "obj1-A");
        Thread t4 = new Thread(() -> demo2.syncMethodA(), "obj2-A");
        t3.start();
        t4.start();
        t3.join();
        t4.join();

        System.out.println("\n  【测试C】类锁（static synchronized）→ 不同对象也互斥");
        Thread t5 = new Thread(() -> LockTypeDemo.staticSyncMethod(), "classLock-1");
        Thread t6 = new Thread(() -> LockTypeDemo.staticSyncMethod(), "classLock-2");
        t5.start();
        Thread.sleep(50);
        t6.start();
        t5.join();
        t6.join();

        System.out.println();
    }

    /**
     * 辅助类：演示对象锁和类锁
     */
    static class LockTypeDemo {
        // 对象锁：synchronized 修饰实例方法
        public synchronized void syncMethodA() {
            String name = Thread.currentThread().getName();
            System.out.println("    [" + name + "] 进入 syncMethodA，持有对象锁");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("    [" + name + "] 离开 syncMethodA，释放对象锁");
        }

        public synchronized void syncMethodB() {
            String name = Thread.currentThread().getName();
            System.out.println("    [" + name + "] 进入 syncMethodB，持有对象锁");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("    [" + name + "] 离开 syncMethodB，释放对象锁");
        }

        // 类锁：synchronized 修饰静态方法
        public static synchronized void staticSyncMethod() {
            String name = Thread.currentThread().getName();
            System.out.println("    [" + name + "] 进入 staticSyncMethod，持有类锁");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("    [" + name + "] 离开 staticSyncMethod，释放类锁");
        }
    }

    // ========================================================
    // 实验4：synchronized 可重入性
    // ========================================================
    private static void demonstrateReentrant() throws Exception {
        System.out.println("--- 实验4：synchronized 可重入性 ---");

        ReentrantDemo demo = new ReentrantDemo();
        Thread thread = new Thread(() -> {
            demo.outer();
        }, "reentrant-thread");

        thread.start();
        thread.join();

        System.out.println("  结论: synchronized 是可重入锁，同一线程可多次获取同一把锁\n");
    }

    static class ReentrantDemo {
        public synchronized void outer() {
            System.out.println("  进入 outer 方法（第一次获取锁）");
            inner(); // 在同步方法内调用另一个同步方法
        }

        public synchronized void inner() {
            System.out.println("  进入 inner 方法（第二次获取同一把锁 → 可重入）");
            deepInner();
        }

        public synchronized void deepInner() {
            System.out.println("  进入 deepInner 方法（第三次获取同一把锁 → 可重入）");
        }
    }

    // ========================================================
    // 实验5：死锁演示
    // ========================================================
    private static void demonstrateDeadlock() throws Exception {
        System.out.println("--- 实验5：死锁演示 ---");
        System.out.println("  ⚠️ 以下将制造一个死锁，程序会在5秒后检测并退出");

        final Object lockA = new Object();
        final Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("  [线程1] 持有 lockA，等待 lockB...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lockB) {
                    System.out.println("  [线程1] 同时持有 lockA 和 lockB");
                }
            }
        }, "deadlock-thread-1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("  [线程2] 持有 lockB，等待 lockA...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                synchronized (lockA) {
                    System.out.println("  [线程2] 同时持有 lockA 和 lockB");
                }
            }
        }, "deadlock-thread-2");

        t1.start();
        t2.start();

        // 等待 3 秒检测死锁
        Thread.sleep(3000);

        System.out.println("\n  检测线程状态:");
        System.out.println("  [线程1] 状态: " + t1.getState());
        System.out.println("  [线程2] 状态: " + t2.getState());

        if (t1.getState() == Thread.State.BLOCKED && t2.getState() == Thread.State.BLOCKED) {
            System.out.println("  ❌ 检测到死锁！两个线程都处于 BLOCKED 状态");
            System.out.println("  排查方法: jstack <pid> 可以看到 'Found one Java-level deadlock'");
            System.out.println("  解决方案: 统一锁的获取顺序（例如都先获取 lockA 再获取 lockB）");

            // 强制中断死锁线程以便程序继续
            t1.interrupt();
            t2.interrupt();
        }

        // 给线程时间退出（interrupt 不一定能打断 synchronized 阻塞）
        t1.join(2000);
        t2.join(2000);

        // 如果线程仍然存活（死锁），设为守护线程不阻塞程序退出
        if (t1.isAlive() || t2.isAlive()) {
            System.out.println("  (死锁线程仍在运行，程序将继续执行后续实验)");
        }
        System.out.println();
    }
}
