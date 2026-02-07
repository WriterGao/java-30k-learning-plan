import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CPU 飙高演示
 *
 * 模拟常见的 CPU 飙高场景：
 * 1. 死循环（最常见）
 * 2. 频繁 Full GC 导致 CPU 飙高
 * 3. 大量线程自旋/竞争
 *
 * 运行方式：
 *   javac CPUHighDemo.java
 *   java CPUHighDemo [1|2|3]
 *
 * 排查流程（Linux/macOS）：
 *   1. top -c                         # 找到 CPU 占用高的 Java 进程 PID
 *   2. ps -mp <pid> -o THREAD,tid,time  # (Linux) 找到 CPU 占用高的线程 TID
 *      ps -M -p <pid>                  # (macOS) 找到 CPU 占用高的线程
 *   3. printf "%x\n" <tid>            # 将线程 ID 转为 16 进制
 *   4. jstack <pid> | grep -A 30 "nid=0x<hex>"  # 在线程栈中定位
 *
 * @author Day03 JVM调优实战
 */
public class CPUHighDemo {

    // ========== 场景1：死循环 ==========

    /**
     * 模拟业务代码中的死循环
     * 这是最常见的 CPU 飙高原因
     */
    public static void infiniteLoop() {
        System.out.println("===== 场景1：死循环导致 CPU 飙高 =====");
        System.out.println("启动一个死循环线程，CPU 单核占用将接近 100%");
        System.out.println("请打开另一个终端，按照排查流程定位问题线程\n");

        Thread cpuKiller = new Thread(() -> {
            System.out.println("[死循环线程] 开始运行，线程名: " + Thread.currentThread().getName());

            // 模拟业务中的死循环 Bug
            // 例如：while 条件写错、缺少 break、状态未正确更新
            long count = 0;
            while (true) {
                count++;
                // 空循环，疯狂消耗 CPU
                if (count % 1_000_000_000L == 0) {
                    // 偶尔打印一下，证明在运行
                    System.out.println("[死循环线程] 已循环 " + (count / 1_000_000_000L) + " 十亿次");
                }
            }
        }, "cpu-killer-infinite-loop");

        cpuKiller.start();

        // 主线程休眠，保持进程存活
        holdMainThread();
    }

    // ========== 场景2：频繁 Full GC ==========

    /**
     * 模拟频繁 Full GC 导致的 CPU 飙高
     * 不断创建对象使内存接近上限，触发频繁 GC
     */
    public static void frequentFullGC() {
        System.out.println("===== 场景2：频繁 Full GC 导致 CPU 飙高 =====");
        System.out.println("不断分配对象，使堆内存接近上限，触发频繁 Full GC");
        System.out.println("建议使用: java -Xms64m -Xmx64m -verbose:gc CPUHighDemo 2\n");

        List<byte[]> holdList = new ArrayList<>();
        Random random = new Random();
        int count = 0;

        while (true) {
            try {
                // 不断分配对象
                byte[] data = new byte[1024 * random.nextInt(64)]; // 0~64KB
                holdList.add(data);
                count++;

                // 随机释放一部分（模拟业务中有对象回收，但总量在增长）
                if (holdList.size() > 500 && random.nextInt(10) < 3) {
                    // 释放前半部分
                    int removeCount = holdList.size() / 4;
                    for (int i = 0; i < removeCount; i++) {
                        holdList.remove(0);
                    }
                }

                if (count % 1000 == 0) {
                    Runtime rt = Runtime.getRuntime();
                    long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                    long maxMB = rt.maxMemory() / (1024 * 1024);
                    System.out.println("[Full GC 场景] 分配次数: " + count
                            + "，List 大小: " + holdList.size()
                            + "，堆使用: " + usedMB + "/" + maxMB + " MB");
                }

                Thread.sleep(1); // 稍微放慢
            } catch (OutOfMemoryError e) {
                System.out.println("\n[OOM] 内存不足，清理部分数据后继续...");
                // 清理一半数据，模拟"挣扎"状态
                int half = holdList.size() / 2;
                for (int i = 0; i < half; i++) {
                    holdList.remove(0);
                }
                // 继续循环，制造频繁 GC
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ========== 场景3：大量线程自旋竞争 ==========

    /**
     * 模拟大量线程竞争同一把锁
     * 每个线程都在自旋等待，导致 CPU 飙高
     */
    public static void threadContention() {
        System.out.println("===== 场景3：大量线程自旋竞争导致 CPU 飙高 =====");
        System.out.println("启动 50 个线程竞争同一把锁，同时有自旋等待逻辑");
        System.out.println("观察 CPU 使用率和线程状态\n");

        final Object lock = new Object();
        final int threadCount = 50;

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread t = new Thread(() -> {
                while (true) {
                    // 自旋等待（模拟不当的忙等待）
                    long spinCount = 0;
                    while (spinCount < 1_000_000) {
                        spinCount++;
                    }

                    synchronized (lock) {
                        // 模拟临界区操作
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }, "spin-thread-" + threadId);
            t.setDaemon(true);
            t.start();
        }

        System.out.println("[线程竞争] 已启动 " + threadCount + " 个竞争线程");
        holdMainThread();
    }

    // ========== 工具方法 ==========

    /**
     * 保持主线程存活，便于使用工具排查
     */
    private static void holdMainThread() {
        try {
            System.out.println();
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("程序正在运行，请在另一个终端执行排查命令：");
            System.out.println();
            long pid = ProcessHandle.current().pid();
            System.out.println("  PID: " + pid);
            System.out.println();
            System.out.println("  排查步骤：");
            System.out.println("  1. top -c                     (确认 CPU 占用)");
            System.out.println("  2. jstack " + pid + "               (查看线程栈)");
            System.out.println("  3. jstat -gc " + pid + " 1000      (监控 GC)");
            System.out.println();
            System.out.println("按 Ctrl+C 终止程序");
            System.out.println("═══════════════════════════════════════════════");

            // 主线程持续等待
            while (true) {
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 主方法 ==========

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║         CPU 飙高演示 (CPUHighDemo)            ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║ 场景：                                       ║");
        System.out.println("║   1 - 死循环（默认）                          ║");
        System.out.println("║   2 - 频繁 Full GC                           ║");
        System.out.println("║   3 - 大量线程自旋竞争                        ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        String scenario = args.length > 0 ? args[0] : "1";

        switch (scenario) {
            case "1":
                infiniteLoop();
                break;
            case "2":
                frequentFullGC();
                break;
            case "3":
                threadContention();
                break;
            default:
                System.out.println("用法: java CPUHighDemo [1|2|3]");
                System.out.println("  1 - 死循环（默认）");
                System.out.println("  2 - 频繁 Full GC");
                System.out.println("  3 - 大量线程自旋竞争");
                break;
        }
    }
}
