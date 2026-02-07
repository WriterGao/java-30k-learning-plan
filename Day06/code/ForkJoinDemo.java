import java.util.concurrent.*;
import java.util.Arrays;

/**
 * Day06 - ForkJoinPool 工作窃取算法演示
 *
 * 演示内容：
 * 1. RecursiveTask（有返回值）- 大数组求和
 * 2. RecursiveAction（无返回值）- 大数组排序
 * 3. ForkJoinPool vs 普通线程池性能对比
 * 4. 工作窃取算法的效果
 */
public class ForkJoinDemo {

    // ==================== 示例1：RecursiveTask - 大数组求和 ====================

    /**
     * 使用 ForkJoin 递归拆分任务，计算大数组的求和
     *
     * 思路：
     *   - 如果数组长度 <= THRESHOLD，直接计算
     *   - 否则拆成两半，分别 fork 到子任务，最后 join 合并结果
     */
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 10000; // 拆分阈值
        private final long[] array;
        private final int start;
        private final int end;

        SumTask(long[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;

            // 如果任务足够小，直接计算
            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }

            // 拆分任务
            int mid = start + length / 2;
            SumTask leftTask = new SumTask(array, start, mid);
            SumTask rightTask = new SumTask(array, mid, end);

            // fork 左任务到其他线程执行
            leftTask.fork();

            // 当前线程直接计算右任务（减少 fork 开销）
            long rightResult = rightTask.compute();

            // join 等待左任务结果
            long leftResult = leftTask.join();

            return leftResult + rightResult;
        }
    }

    // ==================== 示例2：RecursiveAction - 并行排序 ====================

    /**
     * 使用 ForkJoin 实现并行归并排序
     */
    static class ParallelMergeSort extends RecursiveAction {
        private static final int THRESHOLD = 8192;
        private final int[] array;
        private final int start;
        private final int end;
        private final int[] temp;

        ParallelMergeSort(int[] array, int start, int end, int[] temp) {
            this.array = array;
            this.start = start;
            this.end = end;
            this.temp = temp;
        }

        @Override
        protected void compute() {
            int length = end - start;

            // 如果任务足够小，使用 Arrays.sort 直接排序
            if (length <= THRESHOLD) {
                Arrays.sort(array, start, end);
                return;
            }

            // 拆分
            int mid = start + length / 2;
            ParallelMergeSort leftSort = new ParallelMergeSort(array, start, mid, temp);
            ParallelMergeSort rightSort = new ParallelMergeSort(array, mid, end, temp);

            // 并行执行
            invokeAll(leftSort, rightSort);

            // 合并两个有序数组
            merge(array, start, mid, end, temp);
        }

        private void merge(int[] arr, int left, int mid, int right, int[] tmp) {
            System.arraycopy(arr, left, tmp, left, right - left);
            int i = left, j = mid, k = left;
            while (i < mid && j < right) {
                if (tmp[i] <= tmp[j]) {
                    arr[k++] = tmp[i++];
                } else {
                    arr[k++] = tmp[j++];
                }
            }
            while (i < mid) arr[k++] = tmp[i++];
            while (j < right) arr[k++] = tmp[j++];
        }
    }

    // ==================== 示例3：性能对比 ====================

    /**
     * 对比 ForkJoinPool 与单线程的求和性能
     */
    static void performanceComparison() {
        System.out.println("========== 示例3：ForkJoinPool vs 单线程性能对比 ==========\n");

        int size = 100_000_000; // 1亿个元素
        long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = i + 1;
        }

        // --- 单线程求和 ---
        long startTime = System.currentTimeMillis();
        long sum1 = 0;
        for (int i = 0; i < size; i++) {
            sum1 += array[i];
        }
        long singleTime = System.currentTimeMillis() - startTime;
        System.out.printf("单线程求和: %d, 耗时: %dms%n", sum1, singleTime);

        // --- ForkJoinPool 求和 ---
        ForkJoinPool forkJoinPool = new ForkJoinPool(); // 默认使用 CPU 核心数
        System.out.printf("ForkJoinPool 并行度: %d%n", forkJoinPool.getParallelism());

        startTime = System.currentTimeMillis();
        SumTask task = new SumTask(array, 0, size);
        long sum2 = forkJoinPool.invoke(task);
        long forkJoinTime = System.currentTimeMillis() - startTime;
        System.out.printf("ForkJoin求和: %d, 耗时: %dms%n", sum2, forkJoinTime);

        // --- 结果验证 ---
        System.out.printf("%n结果一致: %s%n", sum1 == sum2);
        if (singleTime > 0 && forkJoinTime > 0) {
            System.out.printf("加速比: %.2fx%n", (double) singleTime / forkJoinTime);
        }

        forkJoinPool.shutdown();
        System.out.println();
    }

    // ==================== 示例4：工作窃取观察 ====================

    /**
     * 通过不均匀的任务分配，观察工作窃取行为
     */
    static class UnevenTask extends RecursiveTask<String> {
        private final String name;
        private final int workload;

        UnevenTask(String name, int workload) {
            this.name = name;
            this.workload = workload;
        }

        @Override
        protected String compute() {
            if (workload <= 1) {
                // 模拟工作
                long start = System.currentTimeMillis();
                try { Thread.sleep(100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                long elapsed = System.currentTimeMillis() - start;
                String result = String.format("  任务[%s] 在 [%s] 上执行, 耗时=%dms",
                        name, Thread.currentThread().getName(), elapsed);
                System.out.println(result);
                return result;
            }

            // 拆分任务
            UnevenTask left = new UnevenTask(name + "-L", workload / 2);
            UnevenTask right = new UnevenTask(name + "-R", workload - workload / 2);

            left.fork();
            String rightResult = right.compute();
            String leftResult = left.join();

            return leftResult + "\n" + rightResult;
        }
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {

        // --- 示例1：RecursiveTask 大数组求和 ---
        System.out.println("========== 示例1：RecursiveTask - 大数组求和 ==========\n");

        long[] array = new long[1_000_000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }

        ForkJoinPool pool = new ForkJoinPool();
        SumTask sumTask = new SumTask(array, 0, array.length);
        long result = pool.invoke(sumTask);
        long expected = (long) array.length * (array.length + 1) / 2;

        System.out.printf("ForkJoin 求和结果: %d%n", result);
        System.out.printf("数学公式验证:     %d%n", expected);
        System.out.printf("结果正确: %s%n%n", result == expected);

        // --- 示例2：RecursiveAction 并行排序 ---
        System.out.println("========== 示例2：RecursiveAction - 并行排序 ==========\n");

        int[] sortArray = new int[500_000];
        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < sortArray.length; i++) {
            sortArray[i] = random.nextInt(1_000_000);
        }

        int[] temp = new int[sortArray.length];

        // ForkJoin 排序
        long startTime = System.currentTimeMillis();
        ParallelMergeSort sortTask = new ParallelMergeSort(sortArray, 0, sortArray.length, temp);
        pool.invoke(sortTask);
        long forkJoinSortTime = System.currentTimeMillis() - startTime;
        System.out.printf("ForkJoin 排序耗时: %dms%n", forkJoinSortTime);

        // 验证排序正确性
        boolean sorted = true;
        for (int i = 1; i < sortArray.length; i++) {
            if (sortArray[i] < sortArray[i - 1]) {
                sorted = false;
                break;
            }
        }
        System.out.printf("排序正确: %s%n%n", sorted);

        // --- 示例3：性能对比 ---
        performanceComparison();

        // --- 示例4：工作窃取观察 ---
        System.out.println("========== 示例4：工作窃取行为观察 ==========\n");

        ForkJoinPool observePool = new ForkJoinPool(4); // 4 个线程
        System.out.printf("ForkJoinPool 线程数: %d%n%n", observePool.getParallelism());

        UnevenTask unevenTask = new UnevenTask("ROOT", 8);
        observePool.invoke(unevenTask);

        System.out.printf("%n窃取次数: %d%n", observePool.getStealCount());

        pool.shutdown();
        observePool.shutdown();

        System.out.println("\n========== 所有示例完成 ==========");
    }
}
