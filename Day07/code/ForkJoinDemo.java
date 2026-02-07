import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fork/Join 框架演示 —— 大数组并行求和
 *
 * 演示内容：
 * 1. RecursiveTask 并行求和（有返回值）
 * 2. 串行 vs 并行性能对比
 * 3. 不同数组规模下的加速比
 * 4. 工作窃取算法观察
 * 5. 阈值对性能的影响
 */
public class ForkJoinDemo {

    // 阈值：子任务数组长度小于此值时，直接顺序计算
    private static final int THRESHOLD = 10_000;

    public static void main(String[] args) {
        System.out.println("========== Fork/Join 大数组并行求和演示 ==========\n");
        System.out.println("CPU 核心数: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        demo1_BasicParallelSum();
        System.out.println();

        demo2_PerformanceComparison();
        System.out.println();

        demo3_ThresholdImpact();
        System.out.println();

        demo4_WorkStealingObservation();
    }

    // =============================================
    // 演示1：基本并行求和
    // =============================================

    static void demo1_BasicParallelSum() {
        System.out.println("--- 演示1：基本并行求和 ---");

        int size = 10_000_000;
        long[] array = generateArray(size);

        // 串行求和
        long start = System.currentTimeMillis();
        long serialSum = serialSum(array);
        long serialTime = System.currentTimeMillis() - start;

        // Fork/Join 并行求和
        ForkJoinPool pool = new ForkJoinPool();
        start = System.currentTimeMillis();
        SumTask task = new SumTask(array, 0, array.length, THRESHOLD);
        long parallelSum = pool.invoke(task);
        long parallelTime = System.currentTimeMillis() - start;

        System.out.println("  数组大小: " + formatNumber(size));
        System.out.println("  串行求和: sum=" + serialSum + ", 耗时=" + serialTime + "ms");
        System.out.println("  并行求和: sum=" + parallelSum + ", 耗时=" + parallelTime + "ms");
        System.out.println("  结果一致: " + (serialSum == parallelSum));
        System.out.println("  加速比:   " + String.format("%.2f", (double) serialTime / Math.max(parallelTime, 1)) + "x");
        System.out.println("  池信息:   parallelism=" + pool.getParallelism()
                + ", stealCount=" + pool.getStealCount());

        pool.shutdown();
    }

    // =============================================
    // 演示2：不同数据规模的性能对比
    // =============================================

    static void demo2_PerformanceComparison() {
        System.out.println("--- 演示2：不同数据规模的性能对比 ---");

        int[] sizes = {100_000, 1_000_000, 10_000_000, 50_000_000, 100_000_000};

        System.out.printf("  %-15s %-12s %-12s %-10s %-12s%n",
                "数组大小", "串行(ms)", "并行(ms)", "加速比", "窃取次数");
        System.out.println("  " + "-".repeat(65));

        ForkJoinPool pool = new ForkJoinPool();

        for (int size : sizes) {
            long[] array = generateArray(size);

            // 预热（避免 JIT 影响首次测量）
            if (size == sizes[0]) {
                pool.invoke(new SumTask(array, 0, array.length, THRESHOLD));
                serialSum(array);
            }

            // 串行
            long start = System.currentTimeMillis();
            long serialSum = serialSum(array);
            long serialTime = System.currentTimeMillis() - start;

            // 并行
            long stealBefore = pool.getStealCount();
            start = System.currentTimeMillis();
            long parallelSum = pool.invoke(new SumTask(array, 0, array.length, THRESHOLD));
            long parallelTime = System.currentTimeMillis() - start;
            long steals = pool.getStealCount() - stealBefore;

            double speedup = parallelTime == 0 ? 0 : (double) serialTime / parallelTime;

            System.out.printf("  %-15s %-12s %-12s %-10s %-12d%n",
                    formatNumber(size),
                    serialTime + "ms",
                    parallelTime + "ms",
                    String.format("%.2fx", speedup),
                    steals);

            // 验证正确性
            if (serialSum != parallelSum) {
                System.out.println("  ⚠️ 结果不一致！serial=" + serialSum + " parallel=" + parallelSum);
            }
        }

        pool.shutdown();
        System.out.println("\n  💡 数据量越大，Fork/Join 的加速效果越明显");
        System.out.println("  💡 小数据量时，任务拆分和调度的开销可能大于并行带来的收益");
    }

    // =============================================
    // 演示3：阈值对性能的影响
    // =============================================

    static void demo3_ThresholdImpact() {
        System.out.println("--- 演示3：阈值(THRESHOLD)对性能的影响 ---");

        int size = 10_000_000;
        long[] array = generateArray(size);
        int[] thresholds = {100, 1_000, 5_000, 10_000, 50_000, 100_000, 500_000, 1_000_000};

        // 先计算串行时间作为基准
        long start = System.currentTimeMillis();
        long serialSum = serialSum(array);
        long serialTime = System.currentTimeMillis() - start;
        System.out.println("  数组大小: " + formatNumber(size));
        System.out.println("  串行耗时: " + serialTime + "ms（基准）");
        System.out.println();

        System.out.printf("  %-15s %-12s %-10s %-15s%n",
                "阈值", "并行(ms)", "加速比", "子任务数(约)");
        System.out.println("  " + "-".repeat(55));

        ForkJoinPool pool = new ForkJoinPool();

        for (int threshold : thresholds) {
            start = System.currentTimeMillis();
            long parallelSum = pool.invoke(new SumTask(array, 0, array.length, threshold));
            long parallelTime = System.currentTimeMillis() - start;

            double speedup = parallelTime == 0 ? 0 : (double) serialTime / parallelTime;
            int approxTasks = size / threshold; // 大约的子任务数

            System.out.printf("  %-15s %-12s %-10s %-15s%n",
                    formatNumber(threshold),
                    parallelTime + "ms",
                    String.format("%.2fx", speedup),
                    "~" + formatNumber(approxTasks));

            if (serialSum != parallelSum) {
                System.out.println("  ⚠️ 结果不一致！");
            }
        }

        pool.shutdown();
        System.out.println("\n  💡 阈值太小 → 子任务过多，调度开销大");
        System.out.println("  💡 阈值太大 → 拆分不够，并行度不足");
        System.out.println("  💡 最佳阈值需根据实际数据量和 CPU 核心数调整");
    }

    // =============================================
    // 演示4：工作窃取观察
    // =============================================

    static void demo4_WorkStealingObservation() {
        System.out.println("--- 演示4：工作窃取算法观察 ---");

        int size = 20_000_000;
        long[] array = generateArray(size);

        // 使用不同的并行度
        int[] parallelisms = {1, 2, 4, Runtime.getRuntime().availableProcessors()};

        System.out.printf("  %-12s %-12s %-12s %-10s%n",
                "并行度", "耗时(ms)", "窃取次数", "加速比");
        System.out.println("  " + "-".repeat(50));

        // 先测串行
        long start = System.currentTimeMillis();
        serialSum(array);
        long serialTime = System.currentTimeMillis() - start;

        for (int p : parallelisms) {
            ForkJoinPool pool = new ForkJoinPool(p);
            start = System.currentTimeMillis();
            pool.invoke(new SumTask(array, 0, array.length, THRESHOLD));
            long time = System.currentTimeMillis() - start;

            double speedup = time == 0 ? 0 : (double) serialTime / time;

            System.out.printf("  %-12d %-12s %-12d %-10s%n",
                    p, time + "ms", pool.getStealCount(),
                    String.format("%.2fx", speedup));

            pool.shutdown();
        }

        System.out.println("\n  💡 并行度越高，窃取次数通常越多");
        System.out.println("  💡 工作窃取使得空闲线程可以帮助繁忙线程，提高 CPU 利用率");
        System.out.println("  💡 理想的并行度通常等于 CPU 核心数");
    }

    // =============================================
    // RecursiveTask: 并行求和任务
    // =============================================

    /**
     * RecursiveTask: 有返回值的 Fork/Join 任务
     *
     * 分治策略：
     * 1. 如果数组段长度 <= 阈值，直接顺序计算
     * 2. 否则将数组一分为二：
     *    - 左半部分 fork（放入队列，可能被其他线程窃取）
     *    - 右半部分 compute（当前线程直接计算）
     *    - join 等待左半部分完成
     *    - 合并两部分结果
     */
    static class SumTask extends RecursiveTask<Long> {
        private final long[] array;
        private final int start;
        private final int end;
        private final int threshold;

        SumTask(long[] array, int start, int end, int threshold) {
            this.array = array;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected Long compute() {
            int length = end - start;

            // 小于阈值，直接顺序计算
            if (length <= threshold) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }

            // 拆分为两个子任务
            int mid = start + length / 2;
            SumTask leftTask = new SumTask(array, start, mid, threshold);
            SumTask rightTask = new SumTask(array, mid, end, threshold);

            // ① fork: 将左半部分推入当前线程的 Deque
            leftTask.fork();

            // ② compute: 当前线程直接计算右半部分（不浪费当前线程）
            Long rightResult = rightTask.compute();

            // ③ join: 等待左半部分完成并获取结果
            Long leftResult = leftTask.join();

            // ④ 合并结果
            return leftResult + rightResult;
        }
    }

    // =============================================
    // 辅助方法
    // =============================================

    /**
     * 串行求和
     */
    static long serialSum(long[] array) {
        long sum = 0;
        for (long v : array) {
            sum += v;
        }
        return sum;
    }

    /**
     * 生成随机数组
     */
    static long[] generateArray(int size) {
        long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = ThreadLocalRandom.current().nextLong(100);
        }
        return array;
    }

    /**
     * 格式化数字（添加逗号分隔）
     */
    static String formatNumber(int n) {
        return String.format("%,d", n);
    }
}
