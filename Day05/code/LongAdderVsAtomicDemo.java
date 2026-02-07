import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.CountDownLatch;

/**
 * LongAdder vs AtomicLong 高并发性能对比
 *
 * 演示内容：
 * 1. AtomicLong 在高并发下的性能瓶颈
 * 2. LongAdder 的分段累加优势
 * 3. LongAccumulator 的自定义累加
 * 4. 不同线程数下的性能对比
 *
 * 原理说明：
 * - AtomicLong: 所有线程竞争同一个 value 变量的 CAS，高并发下大量 CAS 失败重试
 * - LongAdder: 采用分段思想（Cell 数组），不同线程操作不同 Cell，最后求和
 *   类似 ConcurrentHashMap 的分段锁思想，减少竞争，提高吞吐量
 */
public class LongAdderVsAtomicDemo {

    // ==================== 1. 基本用法对比 ====================

    static void basicUsageDemo() {
        System.out.println("========== 1. 基本用法对比 ==========");

        // AtomicLong 用法
        AtomicLong atomicLong = new AtomicLong(0);
        atomicLong.incrementAndGet();
        atomicLong.addAndGet(10);
        System.out.println("AtomicLong: " + atomicLong.get());

        // LongAdder 用法
        LongAdder longAdder = new LongAdder();
        longAdder.increment();
        longAdder.add(10);
        System.out.println("LongAdder: " + longAdder.sum());

        // LongAccumulator 用法（自定义累加函数）
        LongAccumulator accumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);
        accumulator.accumulate(10);
        accumulator.accumulate(5);
        accumulator.accumulate(20);
        accumulator.accumulate(8);
        System.out.println("LongAccumulator(max): " + accumulator.get());

        // LongAccumulator 实现求和
        LongAccumulator sumAccumulator = new LongAccumulator(Long::sum, 0);
        sumAccumulator.accumulate(10);
        sumAccumulator.accumulate(20);
        sumAccumulator.accumulate(30);
        System.out.println("LongAccumulator(sum): " + sumAccumulator.get());

        System.out.println();
    }

    // ==================== 2. 高并发性能对比 ====================

    /**
     * 在不同线程数下对比 AtomicLong 和 LongAdder 的性能
     */
    static void performanceBenchmark() throws InterruptedException {
        System.out.println("========== 2. 高并发性能对比 ==========");
        System.out.println();

        int[] threadCounts = {1, 2, 4, 8, 16, 32};
        int operationsPerThread = 1_000_000;

        System.out.printf("%-10s | %-15s | %-15s | %-10s%n",
                "线程数", "AtomicLong(ms)", "LongAdder(ms)", "加速比");
        System.out.println("-----------|-----------------|-----------------|----------");

        for (int threadCount : threadCounts) {
            long atomicTime = benchmarkAtomicLong(threadCount, operationsPerThread);
            long adderTime = benchmarkLongAdder(threadCount, operationsPerThread);

            double speedup = (double) atomicTime / adderTime;
            System.out.printf("%-10d | %-15d | %-15d | %-10.2f%n",
                    threadCount, atomicTime, adderTime, speedup);
        }

        System.out.println();
        System.out.println("结论：");
        System.out.println("  - 低并发时，两者性能差异不大");
        System.out.println("  - 高并发时，LongAdder 明显优于 AtomicLong");
        System.out.println("  - 原因：LongAdder 采用分段（Cell[]）减少 CAS 竞争");
        System.out.println();
    }

    /**
     * AtomicLong 性能测试
     */
    private static long benchmarkAtomicLong(int threadCount, int opsPerThread)
            throws InterruptedException {
        AtomicLong counter = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    counter.incrementAndGet();
                }
                latch.countDown();
            });
        }

        long start = System.nanoTime();
        for (Thread t : threads) t.start();
        latch.await();
        long elapsed = (System.nanoTime() - start) / 1_000_000; // 转为毫秒

        // 验证正确性
        long expected = (long) threadCount * opsPerThread;
        assert counter.get() == expected : "AtomicLong 计数不正确!";

        return elapsed;
    }

    /**
     * LongAdder 性能测试
     */
    private static long benchmarkLongAdder(int threadCount, int opsPerThread)
            throws InterruptedException {
        LongAdder counter = new LongAdder();
        CountDownLatch latch = new CountDownLatch(threadCount);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    counter.increment();
                }
                latch.countDown();
            });
        }

        long start = System.nanoTime();
        for (Thread t : threads) t.start();
        latch.await();
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        long expected = (long) threadCount * opsPerThread;
        assert counter.sum() == expected : "LongAdder 计数不正确!";

        return elapsed;
    }

    // ==================== 3. LongAdder 原理图解 ====================

    static void principleExplanation() {
        System.out.println("========== 3. LongAdder 原理图解 ==========");
        System.out.println();
        System.out.println("AtomicLong 结构（所有线程竞争同一个值）：");
        System.out.println("┌─────────────────────────────────────────────┐");
        System.out.println("│  Thread-1 ─┐                                │");
        System.out.println("│  Thread-2 ─┤── CAS ──→ [ value ]           │");
        System.out.println("│  Thread-3 ─┤         (高并发=大量CAS失败)    │");
        System.out.println("│  Thread-N ─┘                                │");
        System.out.println("└─────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("LongAdder 结构（分段减少竞争）：");
        System.out.println("┌─────────────────────────────────────────────┐");
        System.out.println("│  Thread-1 ──→ [ Cell[0] ]                   │");
        System.out.println("│  Thread-2 ──→ [ Cell[1] ]   sum() 时求和    │");
        System.out.println("│  Thread-3 ──→ [ Cell[2] ]  ═══════════════  │");
        System.out.println("│  Thread-N ──→ [ base    ]   base + ΣCell[i] │");
        System.out.println("└─────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("LongAdder 核心思想：");
        System.out.println("  1. 初始时只操作 base 变量");
        System.out.println("  2. 出现竞争时，创建 Cell 数组");
        System.out.println("  3. 不同线程通过 hash 映射到不同 Cell");
        System.out.println("  4. sum() 时汇总 base + 所有 Cell 的值");
        System.out.println("  5. Cell 数组会动态扩容（最大为 CPU 核数）");
        System.out.println();
        System.out.println("适用场景：");
        System.out.println("  - AtomicLong: 需要精确读取当前值的场景");
        System.out.println("  - LongAdder:  写多读少的统计计数场景（如 QPS 统计）");
        System.out.println("  - 注意: LongAdder 的 sum() 不是原子操作，可能不精确");
    }

    // ==================== main ====================

    public static void main(String[] args) throws InterruptedException {
        basicUsageDemo();
        performanceBenchmark();
        principleExplanation();
    }
}
