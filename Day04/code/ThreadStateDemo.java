/**
 * ThreadStateDemo.java - 线程状态转换观察实验
 *
 * 本程序演示 Java 线程的 6 种状态：
 * NEW → RUNNABLE → BLOCKED → WAITING → TIMED_WAITING → TERMINATED
 *
 * 运行方式：
 *   javac ThreadStateDemo.java
 *   java ThreadStateDemo
 *
 * 建议配合 jstack 观察线程状态：
 *   jps                 # 获取进程ID
 *   jstack <pid>        # 查看线程状态
 */
public class ThreadStateDemo {

    private static final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  线程状态转换观察实验");
        System.out.println("========================================\n");

        // 实验1：观察 NEW 和 TERMINATED 状态
        demonstrateNewAndTerminated();

        // 实验2：观察 RUNNABLE 状态
        demonstrateRunnable();

        // 实验3：观察 TIMED_WAITING 状态（sleep）
        demonstrateTimedWaiting();

        // 实验4：观察 WAITING 状态（wait）
        demonstrateWaiting();

        // 实验5：观察 BLOCKED 状态
        demonstrateBlocked();

        // 实验6：完整的状态转换流程
        demonstrateFullLifecycle();

        System.out.println("\n========================================");
        System.out.println("  所有实验完成！");
        System.out.println("========================================");
    }

    /**
     * 实验1：观察 NEW 和 TERMINATED 状态
     */
    private static void demonstrateNewAndTerminated() throws Exception {
        System.out.println("--- 实验1：NEW 和 TERMINATED 状态 ---");

        Thread thread = new Thread(() -> {
            System.out.println("  线程正在执行...");
        }, "demo-new-terminated");

        // NEW 状态：线程已创建但未启动
        System.out.println("  创建后未启动 → 状态: " + thread.getState()); // NEW

        thread.start();
        thread.join(); // 等待线程执行完毕

        // TERMINATED 状态：线程执行完毕
        System.out.println("  执行完毕后 → 状态: " + thread.getState()); // TERMINATED
        System.out.println();
    }

    /**
     * 实验2：观察 RUNNABLE 状态
     */
    private static void demonstrateRunnable() throws Exception {
        System.out.println("--- 实验2：RUNNABLE 状态 ---");

        Thread thread = new Thread(() -> {
            // 忙循环，保持 RUNNABLE 状态
            long sum = 0;
            for (long i = 0; i < 1_000_000_000L; i++) {
                sum += i;
            }
            // 防止编译器优化掉循环
            if (sum == -1) System.out.println(sum);
        }, "demo-runnable");

        thread.start();
        Thread.sleep(10); // 给线程一点时间启动

        // RUNNABLE 状态：线程正在运行或准备运行
        System.out.println("  忙循环中 → 状态: " + thread.getState()); // RUNNABLE

        thread.join();
        System.out.println();
    }

    /**
     * 实验3：观察 TIMED_WAITING 状态
     */
    private static void demonstrateTimedWaiting() throws Exception {
        System.out.println("--- 实验3：TIMED_WAITING 状态（sleep） ---");

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000); // 睡眠 2 秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "demo-timed-waiting");

        thread.start();
        Thread.sleep(100); // 确保线程已进入 sleep

        // TIMED_WAITING 状态：线程在有限时间内等待
        System.out.println("  sleep(2000) 中 → 状态: " + thread.getState()); // TIMED_WAITING

        thread.join();
        System.out.println();
    }

    /**
     * 实验4：观察 WAITING 状态
     */
    private static void demonstrateWaiting() throws Exception {
        System.out.println("--- 实验4：WAITING 状态（wait） ---");

        Thread thread = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(); // 无限期等待
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "demo-waiting");

        thread.start();
        Thread.sleep(100); // 确保线程已进入 wait

        // WAITING 状态：线程无限期等待
        System.out.println("  wait() 中 → 状态: " + thread.getState()); // WAITING

        // 唤醒线程
        synchronized (lock) {
            lock.notify();
        }

        thread.join();
        System.out.println();
    }

    /**
     * 实验5：观察 BLOCKED 状态
     */
    private static void demonstrateBlocked() throws Exception {
        System.out.println("--- 实验5：BLOCKED 状态 ---");

        final Object blockLock = new Object();

        // 线程A：持有锁并长时间占用
        Thread threadA = new Thread(() -> {
            synchronized (blockLock) {
                try {
                    Thread.sleep(2000); // 持有锁 2 秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "demo-holder");

        // 线程B：尝试获取锁，将被阻塞
        Thread threadB = new Thread(() -> {
            synchronized (blockLock) {
                System.out.println("  线程B获取到锁！");
            }
        }, "demo-blocked");

        threadA.start();
        Thread.sleep(100); // 确保线程A已持有锁

        threadB.start();
        Thread.sleep(100); // 确保线程B已尝试获取锁

        // BLOCKED 状态：线程等待获取监视器锁
        System.out.println("  线程A持有锁 → 线程A状态: " + threadA.getState()); // TIMED_WAITING
        System.out.println("  线程B等待锁 → 线程B状态: " + threadB.getState()); // BLOCKED

        threadA.join();
        threadB.join();
        System.out.println();
    }

    /**
     * 实验6：完整的状态转换流程
     */
    private static void demonstrateFullLifecycle() throws Exception {
        System.out.println("--- 实验6：完整状态转换流程 ---");

        final Object lifecycleLock = new Object();

        Thread thread = new Thread(() -> {
            // 阶段1：RUNNABLE（正在运行）
            long sum = 0;
            for (long i = 0; i < 100_000_000L; i++) {
                sum += i;
            }
            if (sum == -1) System.out.println(sum);

            // 阶段2：TIMED_WAITING（sleep）
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 阶段3：WAITING（wait）
            synchronized (lifecycleLock) {
                try {
                    lifecycleLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 阶段4：即将 TERMINATED
        }, "demo-lifecycle");

        // NEW
        System.out.println("  阶段0 NEW       → " + thread.getState());

        thread.start();

        // RUNNABLE（可能需要多次采样）
        Thread.sleep(5);
        System.out.println("  阶段1 RUNNABLE  → " + thread.getState());

        // TIMED_WAITING
        Thread.sleep(500);
        System.out.println("  阶段2 TIMED_WAITING → " + thread.getState());

        // WAITING
        Thread.sleep(1000);
        System.out.println("  阶段3 WAITING   → " + thread.getState());

        // 唤醒线程
        synchronized (lifecycleLock) {
            lifecycleLock.notify();
        }

        thread.join();

        // TERMINATED
        System.out.println("  阶段4 TERMINATED → " + thread.getState());
        System.out.println();
    }
}
