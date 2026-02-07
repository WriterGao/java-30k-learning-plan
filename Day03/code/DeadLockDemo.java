/**
 * 死锁演示
 *
 * 模拟两个线程互相等待对方释放锁的经典死锁场景：
 *   线程A: 先获取 lockA，再请求 lockB
 *   线程B: 先获取 lockB，再请求 lockA
 *
 * 运行方式：
 *   javac DeadLockDemo.java
 *   java DeadLockDemo
 *
 * 排查方法：
 *   1. jps -l               # 找到进程 PID
 *   2. jstack <pid>          # 打印线程堆栈，查看死锁信息
 *   3. jcmd <pid> Thread.print  # 同样可以查看线程信息
 *
 * 预期现象：
 *   - 程序启动后卡住不动（两个线程都在等待对方释放锁）
 *   - jstack 输出中会有 "Found one Java-level deadlock" 的提示
 *   - 会显示死锁的线程和它们持有/等待的锁
 *
 * @author Day03 JVM调优实战
 */
public class DeadLockDemo {

    // 两把锁
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║           死锁演示 (DeadLockDemo)             ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║ 排查方法：                                    ║");
        System.out.println("║   1. jps -l          (找到 PID)              ║");
        System.out.println("║   2. jstack <pid>    (查看死锁信息)           ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("主线程 PID 信息（方便排查）：");
        System.out.println("  进程名: " + getProcessName());
        System.out.println();

        // 线程A：先锁 A，再锁 B
        Thread threadA = new Thread(() -> {
            System.out.println("[线程A] 尝试获取 LOCK_A ...");
            synchronized (LOCK_A) {
                System.out.println("[线程A] 已获取 LOCK_A ✓");

                // 模拟业务处理
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("[线程A] 尝试获取 LOCK_B ...");
                synchronized (LOCK_B) {
                    System.out.println("[线程A] 已获取 LOCK_B ✓（不会执行到这里）");
                }
            }
        }, "Thread-A-持有lockA-等待lockB");

        // 线程B：先锁 B，再锁 A
        Thread threadB = new Thread(() -> {
            System.out.println("[线程B] 尝试获取 LOCK_B ...");
            synchronized (LOCK_B) {
                System.out.println("[线程B] 已获取 LOCK_B ✓");

                // 模拟业务处理
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("[线程B] 尝试获取 LOCK_A ...");
                synchronized (LOCK_A) {
                    System.out.println("[线程B] 已获取 LOCK_A ✓（不会执行到这里）");
                }
            }
        }, "Thread-B-持有lockB-等待lockA");

        // 启动两个线程
        threadA.start();
        threadB.start();

        // 等待一段时间后检测死锁
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("检测到程序可能发生了死锁！");
        System.out.println();
        System.out.println("请使用以下命令排查：");
        System.out.println("  1. jps -l");
        System.out.println("  2. jstack " + getProcessId());
        System.out.println();
        System.out.println("jstack 输出中应该能看到类似：");
        System.out.println("  Found one Java-level deadlock:");
        System.out.println("  =============================");
        System.out.println("  \"Thread-A-持有lockA-等待lockB\":");
        System.out.println("    waiting to lock monitor <0x...> (object ..., a java.lang.Object)");
        System.out.println("    which is held by \"Thread-B-持有lockB-等待lockA\"");
        System.out.println("  \"Thread-B-持有lockB-等待lockA\":");
        System.out.println("    waiting to lock monitor <0x...> (object ..., a java.lang.Object)");
        System.out.println("    which is held by \"Thread-A-持有lockA-等待lockB\"");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println();
        System.out.println("程序将持续运行（死锁状态），请使用 Ctrl+C 终止。");

        // 主线程等待（不会返回，因为子线程死锁了）
        try {
            threadA.join();
            threadB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取当前进程 ID
     */
    private static long getProcessId() {
        return ProcessHandle.current().pid();
    }

    /**
     * 获取当前进程名称
     */
    private static String getProcessName() {
        return ProcessHandle.current().info().command().orElse("unknown")
                + " (PID: " + getProcessId() + ")";
    }
}
