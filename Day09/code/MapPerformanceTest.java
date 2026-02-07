import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HashMap vs Hashtable vs synchronizedMap vs ConcurrentHashMap 性能对比
 *
 * 测试维度：
 * 1. 纯写入性能（多线程 put）
 * 2. 纯读取性能（多线程 get）
 * 3. 混合读写性能（80% 读 + 20% 写）
 * 4. 不同线程数下的性能对比
 *
 * 运行方式：
 *   cd Day09/code
 *   javac MapPerformanceTest.java
 *   java MapPerformanceTest
 *
 * 建议：多次运行取平均值，JVM 需要预热
 */
public class MapPerformanceTest {

    // 预热数据量
    private static final int WARMUP_OPERATIONS = 100_000;
    // 正式测试数据量（每线程操作数）
    private static final int OPERATIONS_PER_THREAD = 500_000;
    // 测试轮次
    private static final int ROUNDS = 3;

    // ============================================================
    // 性能测试：纯写入（put）
    // ============================================================
    static long testPut(Map<Integer, Integer> map, int threadCount, int opsPerThread)
            throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        map.put(threadId * opsPerThread + j, j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        return (System.nanoTime() - start) / 1_000_000; // 转为毫秒
    }

    // ============================================================
    // 性能测试：纯读取（get）
    // ============================================================
    static long testGet(Map<Integer, Integer> map, int threadCount, int opsPerThread)
            throws InterruptedException {
        // 先填充数据
        int totalKeys = threadCount * opsPerThread;
        for (int i = 0; i < totalKeys; i++) {
            map.put(i, i);
        }

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        Random random = new Random(42);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startGate.await();
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int j = 0; j < opsPerThread; j++) {
                        map.get(rnd.nextInt(totalKeys));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        return (System.nanoTime() - start) / 1_000_000;
    }

    // ============================================================
    // 性能测试：混合读写（80% 读 + 20% 写）
    // ============================================================
    static long testMixed(Map<Integer, Integer> map, int threadCount, int opsPerThread)
            throws InterruptedException {
        // 先填充数据
        int totalKeys = threadCount * opsPerThread;
        for (int i = 0; i < Math.min(totalKeys, 100_000); i++) {
            map.put(i, i);
        }

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startGate.await();
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int j = 0; j < opsPerThread; j++) {
                        if (rnd.nextInt(100) < 80) {
                            // 80% 读操作
                            map.get(rnd.nextInt(100_000));
                        } else {
                            // 20% 写操作
                            map.put(rnd.nextInt(100_000), j);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        return (System.nanoTime() - start) / 1_000_000;
    }

    // ============================================================
    // 创建各种 Map 实例
    // ============================================================
    @SuppressWarnings("deprecation")
    static Map<Integer, Integer> createMap(String type) {
        switch (type) {
            case "Hashtable":
                return new Hashtable<>();
            case "synchronizedMap":
                return Collections.synchronizedMap(new HashMap<>());
            case "ConcurrentHashMap":
                return new ConcurrentHashMap<>();
            default:
                throw new IllegalArgumentException("Unknown map type: " + type);
        }
    }

    // ============================================================
    // 运行完整性能对比
    // ============================================================
    static void runBenchmark() throws InterruptedException {
        String[] mapTypes = {"Hashtable", "synchronizedMap", "ConcurrentHashMap"};
        int[] threadCounts = {1, 2, 4, 8, 16, 32};

        // JVM 预热
        System.out.println("JVM 预热中...");
        for (String type : mapTypes) {
            Map<Integer, Integer> map = createMap(type);
            testPut(map, 4, WARMUP_OPERATIONS);
        }
        System.out.println("预热完成！");
        System.out.println();

        // ========== 测试1: 纯写入 ==========
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    测试1: 纯写入性能 (put)                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.printf("%-6s", "线程数");
        for (String type : mapTypes) {
            System.out.printf("  %-20s", type + "(ms)");
        }
        System.out.println("  ConcurrentHashMap加速比");
        System.out.println("-".repeat(90));

        for (int tc : threadCounts) {
            System.out.printf("%-6d", tc);
            long[] times = new long[mapTypes.length];
            for (int t = 0; t < mapTypes.length; t++) {
                long total = 0;
                for (int r = 0; r < ROUNDS; r++) {
                    Map<Integer, Integer> map = createMap(mapTypes[t]);
                    total += testPut(map, tc, OPERATIONS_PER_THREAD);
                }
                times[t] = total / ROUNDS;
                System.out.printf("  %-20d", times[t]);
            }
            // 加速比 = Hashtable / ConcurrentHashMap
            if (times[2] > 0) {
                System.out.printf("  %.2fx vs Hashtable", (double) times[0] / times[2]);
            }
            System.out.println();
        }
        System.out.println();

        // ========== 测试2: 纯读取 ==========
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    测试2: 纯读取性能 (get)                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.printf("%-6s", "线程数");
        for (String type : mapTypes) {
            System.out.printf("  %-20s", type + "(ms)");
        }
        System.out.println("  ConcurrentHashMap加速比");
        System.out.println("-".repeat(90));

        for (int tc : threadCounts) {
            System.out.printf("%-6d", tc);
            long[] times = new long[mapTypes.length];
            for (int t = 0; t < mapTypes.length; t++) {
                long total = 0;
                for (int r = 0; r < ROUNDS; r++) {
                    Map<Integer, Integer> map = createMap(mapTypes[t]);
                    total += testGet(map, tc, OPERATIONS_PER_THREAD);
                }
                times[t] = total / ROUNDS;
                System.out.printf("  %-20d", times[t]);
            }
            if (times[2] > 0) {
                System.out.printf("  %.2fx vs Hashtable", (double) times[0] / times[2]);
            }
            System.out.println();
        }
        System.out.println();

        // ========== 测试3: 混合读写 ==========
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                测试3: 混合读写性能 (80%读 + 20%写)                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.printf("%-6s", "线程数");
        for (String type : mapTypes) {
            System.out.printf("  %-20s", type + "(ms)");
        }
        System.out.println("  ConcurrentHashMap加速比");
        System.out.println("-".repeat(90));

        for (int tc : threadCounts) {
            System.out.printf("%-6d", tc);
            long[] times = new long[mapTypes.length];
            for (int t = 0; t < mapTypes.length; t++) {
                long total = 0;
                for (int r = 0; r < ROUNDS; r++) {
                    Map<Integer, Integer> map = createMap(mapTypes[t]);
                    total += testMixed(map, tc, OPERATIONS_PER_THREAD);
                }
                times[t] = total / ROUNDS;
                System.out.printf("  %-20d", times[t]);
            }
            if (times[2] > 0) {
                System.out.printf("  %.2fx vs Hashtable", (double) times[0] / times[2]);
            }
            System.out.println();
        }
        System.out.println();
    }

    // ============================================================
    // 分析说明
    // ============================================================
    static void printAnalysis() {
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         性能分析说明                                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("1. Hashtable 使用 synchronized 方法锁住整个表，所有操作串行化");
        System.out.println("   - 多线程并发时，锁竞争严重，性能随线程数增加而下降");
        System.out.println();
        System.out.println("2. synchronizedMap 使用 synchronized(mutex) 包装每个方法，同样是粗粒度锁");
        System.out.println("   - 性能与 Hashtable 相当，锁粒度相同");
        System.out.println();
        System.out.println("3. ConcurrentHashMap (JDK 8) 使用 CAS + synchronized(头节点) 细粒度锁");
        System.out.println("   - 写入: 只锁单个桶的头节点，不同桶可并发写入");
        System.out.println("   - 读取: 完全无锁（volatile + Node.val/next 保证可见性）");
        System.out.println("   - 线程数越多，相对优势越明显");
        System.out.println();
        System.out.println("4. 关键结论:");
        System.out.println("   - 单线程下，ConcurrentHashMap 略慢于 HashMap（额外 CAS 开销）");
        System.out.println("   - 多线程下，ConcurrentHashMap 远快于 Hashtable/synchronizedMap");
        System.out.println("   - 读取性能: ConcurrentHashMap >> Hashtable（无锁 vs 有锁）");
        System.out.println("   - 混合读写: ConcurrentHashMap 优势最明显（读无锁 + 写细粒度锁）");
        System.out.println();
    }

    // ============================================================
    // main
    // ============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║     HashMap vs Hashtable vs synchronizedMap vs ConcurrentHashMap     ║");
        System.out.println("║                         性能对比测试                                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("配置: 每线程操作数=" + OPERATIONS_PER_THREAD
                + ", 测试轮次=" + ROUNDS);
        System.out.println("CPU 核心数: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        runBenchmark();
        printAnalysis();

        System.out.println("测试完成！");
    }
}
