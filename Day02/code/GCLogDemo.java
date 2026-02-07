import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Day02 实验：GC 日志观察与收集器对比
 *
 * 本程序提供三种模式：
 * 1. 默认模式：持续分配对象，触发多次 Minor GC，观察 GC 日志
 * 2. fullgc 模式：分配大量持久对象，触发 Full GC
 * 3. perf 模式：高负载性能测试，对比不同收集器
 *
 * 使用方式：
 *   javac GCLogDemo.java
 *
 *   # 默认模式（观察 GC 日志）
 *   java -Xms64m -Xmx64m -Xmn32m -XX:+UseSerialGC -XX:+PrintGCDetails GCLogDemo
 *
 *   # Full GC 模式
 *   java -Xms30m -Xmx30m -Xmn10m -XX:+UseSerialGC -XX:+PrintGCDetails GCLogDemo fullgc
 *
 *   # 性能对比模式
 *   java -Xms128m -Xmx128m -XX:+UseG1GC GCLogDemo perf
 *
 * JDK 9+ 请将 -XX:+PrintGCDetails 替换为 -Xlog:gc*
 */
public class GCLogDemo {

    // ========== 默认模式：观察 Minor GC ==========

    /**
     * 持续分配短生命周期对象，触发多次 Minor GC
     * 部分对象会被保留（晋升到老年代），其余被回收
     */
    public static void minorGCDemo() {
        System.out.println("===== Minor GC 观察模式 =====");
        System.out.println("堆内存: -Xms64m -Xmx64m, 新生代: -Xmn32m");
        System.out.println("持续分配对象，观察 Minor GC 日志...\n");

        List<byte[]> retainedObjects = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 500; i++) {
            // 分配 64KB ~ 256KB 的对象
            int size = (random.nextInt(4) + 1) * 64 * 1024;
            byte[] obj = new byte[size];

            // 约 10% 的对象会被长期持有（模拟晋升到老年代的情况）
            if (random.nextInt(10) == 0) {
                retainedObjects.add(obj);
            }

            // 每 100 次输出一次进度
            if (i % 100 == 0) {
                System.out.println("[进度] 已分配 " + (i + 1) + " 个对象, "
                        + "长期持有: " + retainedObjects.size() + " 个");
            }
        }

        System.out.println("\n[完成] 共创建 500 个对象，长期持有 "
                + retainedObjects.size() + " 个");
        System.out.println("请查看 GC 日志，观察 Minor GC 的发生过程。");
    }

    // ========== Full GC 模式 ==========

    /**
     * 持续分配对象并持有引用，撑满老年代，触发 Full GC
     * 设置较小堆内存可以更快触发
     */
    public static void fullGCDemo() {
        System.out.println("===== Full GC 模拟模式 =====");
        System.out.println("堆内存: -Xms30m -Xmx30m, 新生代: -Xmn10m");
        System.out.println("持续分配并持有对象，触发 Full GC...\n");

        List<byte[]> list = new ArrayList<>();
        int count = 0;

        try {
            while (true) {
                // 每次分配 512KB
                list.add(new byte[512 * 1024]);
                count++;

                if (count % 10 == 0) {
                    System.out.println("[进度] 已分配 " + count + " 个对象 ("
                            + (count * 512 / 1024) + " MB)");
                }

                // 短暂停，便于观察
                Thread.sleep(50);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("\n[OOM] 内存溢出！共分配了 " + count + " 个对象 ("
                    + (count * 512 / 1024) + " MB)");
            System.out.println("[提示] 请查看 GC 日志中的 Full GC 记录。");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 性能对比模式 ==========

    /**
     * 高负载测试：大量短生命周期对象分配和回收
     * 用于对比不同收集器的性能表现
     */
    public static void performanceTest() {
        System.out.println("===== 性能对比模式 =====");
        System.out.println("高负载分配测试，对比不同收集器的表现...\n");

        long startTime = System.currentTimeMillis();
        Random random = new Random(42); // 固定种子，保证可重复

        List<byte[]> shortLived = new ArrayList<>();
        List<byte[]> longLived = new ArrayList<>();
        int totalAllocations = 0;
        int gcTriggered = 0;

        for (int round = 0; round < 50; round++) {
            // 每轮分配大量短生命周期对象
            for (int i = 0; i < 200; i++) {
                int size = (random.nextInt(8) + 1) * 1024; // 1KB ~ 8KB
                shortLived.add(new byte[size]);
                totalAllocations++;
            }

            // 每轮保留少量长生命周期对象
            longLived.add(new byte[32 * 1024]); // 32KB

            // 清理短生命周期对象（模拟业务处理完毕）
            shortLived.clear();

            if (round % 10 == 0) {
                System.out.println("[轮次 " + (round + 1) + "/50] "
                        + "累计分配: " + totalAllocations + " 个对象");
            }
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;

        System.out.println("\n===== 测试结果 =====");
        System.out.println("执行时间: " + elapsed + " ms");
        System.out.println("总分配次数: " + totalAllocations);
        System.out.println("长期持有对象: " + longLived.size());
        System.out.println("吞吐量: " + String.format("%.2f", totalAllocations * 1000.0 / elapsed) + " 次/秒");
        System.out.println("\n[提示] 使用不同 GC 收集器多次运行本程序，对比执行时间。");
    }

    // ========== 主入口 ==========

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Day02 GC 日志观察与收集器对比实验");
        System.out.println("========================================\n");

        // 打印 JVM 信息
        System.out.println("[JVM] " + System.getProperty("java.vm.name")
                + " " + System.getProperty("java.vm.version"));
        System.out.println("[JDK] " + System.getProperty("java.version"));
        System.out.println("[堆]  Max=" + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB"
                + ", Total=" + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB"
                + ", Free=" + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");
        System.out.println();

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "fullgc":
                    fullGCDemo();
                    break;
                case "perf":
                    performanceTest();
                    break;
                default:
                    System.out.println("未知模式: " + args[0]);
                    System.out.println("可用模式: (无参数)=MinorGC观察, fullgc=FullGC模拟, perf=性能对比");
                    break;
            }
        } else {
            minorGCDemo();
        }
    }
}
