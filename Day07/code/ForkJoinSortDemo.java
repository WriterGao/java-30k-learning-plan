import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fork/Join 框架演示 —— 归并排序 + 求和
 *
 * 演示内容：
 * 1. RecursiveTask（有返回值）—— 并行数组求和
 * 2. RecursiveAction（无返回值）—— 并行归并排序
 * 3. 工作窃取算法说明
 * 4. 性能对比：Fork/Join vs 普通排序
 */
public class ForkJoinSortDemo {

    // 阈值：子数组长度小于此值时，直接使用普通排序
    private static final int THRESHOLD = 10000;

    public static void main(String[] args) {
        System.out.println("========== Fork/Join 框架演示 ==========\n");

        demo1_RecursiveTaskSum();
        System.out.println();

        demo2_RecursiveActionSort();
        System.out.println();

        demo3_Performance();
    }

    // =============================================
    // 演示1：RecursiveTask —— 并行数组求和（有返回值）
    // =============================================

    static void demo1_RecursiveTaskSum() {
        System.out.println("--- 演示1：RecursiveTask 并行数组求和 ---");

        int size = 10_000_000;
        long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = ThreadLocalRandom.current().nextLong(100);
        }

        // 串行求和
        long start = System.currentTimeMillis();
        long serialSum = 0;
        for (long v : array) serialSum += v;
        long serialTime = System.currentTimeMillis() - start;

        // Fork/Join 并行求和
        ForkJoinPool pool = new ForkJoinPool();
        start = System.currentTimeMillis();
        SumTask task = new SumTask(array, 0, array.length);
        long parallelSum = pool.invoke(task);
        long parallelTime = System.currentTimeMillis() - start;

        System.out.println("  数组大小: " + size);
        System.out.println("  串行求和: sum=" + serialSum + ", 耗时=" + serialTime + "ms");
        System.out.println("  并行求和: sum=" + parallelSum + ", 耗时=" + parallelTime + "ms");
        System.out.println("  结果一致: " + (serialSum == parallelSum));
        System.out.println("  池信息: parallelism=" + pool.getParallelism()
                + ", stealCount=" + pool.getStealCount());

        pool.shutdown();
    }

    /**
     * RecursiveTask: 有返回值的 Fork/Join 任务
     */
    static class SumTask extends RecursiveTask<Long> {
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

            // 小于阈值，直接计算
            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }

            // 拆分为两个子任务
            int mid = start + length / 2;
            SumTask leftTask = new SumTask(array, start, mid);
            SumTask rightTask = new SumTask(array, mid, end);

            // fork: 将子任务提交到工作队列
            leftTask.fork();
            // 当前线程直接计算右半部分
            Long rightResult = rightTask.compute();
            // join: 等待左半部分完成
            Long leftResult = leftTask.join();

            return leftResult + rightResult;
        }
    }

    // =============================================
    // 演示2：RecursiveAction —— 并行归并排序（无返回值）
    // =============================================

    static void demo2_RecursiveActionSort() {
        System.out.println("--- 演示2：RecursiveAction 并行归并排序 ---");

        int size = 1_000_000;
        int[] original = new int[size];
        for (int i = 0; i < size; i++) {
            original[i] = ThreadLocalRandom.current().nextInt(10_000_000);
        }

        // 展示小数组排序效果
        int[] small = Arrays.copyOfRange(original, 0, 20);
        System.out.println("  排序前(前20个): " + Arrays.toString(small));

        ForkJoinPool pool = new ForkJoinPool();
        int[] arr = Arrays.copyOf(original, original.length);
        int[] temp = new int[arr.length];

        long start = System.currentTimeMillis();
        MergeSortAction action = new MergeSortAction(arr, temp, 0, arr.length - 1);
        pool.invoke(action);
        long elapsed = System.currentTimeMillis() - start;

        int[] sortedSmall = Arrays.copyOfRange(arr, 0, 20);
        System.out.println("  排序后(前20个): " + Arrays.toString(sortedSmall));
        System.out.println("  数组大小: " + size + ", Fork/Join 排序耗时: " + elapsed + "ms");
        System.out.println("  排序正确: " + isSorted(arr));
        System.out.println("  窃取次数: " + pool.getStealCount());

        pool.shutdown();
    }

    /**
     * RecursiveAction: 无返回值的 Fork/Join 任务 —— 归并排序
     */
    static class MergeSortAction extends RecursiveAction {
        private final int[] array;
        private final int[] temp;
        private final int left;
        private final int right;

        MergeSortAction(int[] array, int[] temp, int left, int right) {
            this.array = array;
            this.temp = temp;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            if (right - left < THRESHOLD) {
                // 小于阈值，使用 Arrays.sort（TimSort）
                Arrays.sort(array, left, right + 1);
                return;
            }

            int mid = left + (right - left) / 2;

            MergeSortAction leftAction = new MergeSortAction(array, temp, left, mid);
            MergeSortAction rightAction = new MergeSortAction(array, temp, mid + 1, right);

            // 并行排序左右两部分
            invokeAll(leftAction, rightAction);

            // 合并两个有序部分
            merge(array, temp, left, mid, right);
        }

        private void merge(int[] arr, int[] tmp, int left, int mid, int right) {
            System.arraycopy(arr, left, tmp, left, right - left + 1);

            int i = left;
            int j = mid + 1;
            int k = left;

            while (i <= mid && j <= right) {
                if (tmp[i] <= tmp[j]) {
                    arr[k++] = tmp[i++];
                } else {
                    arr[k++] = tmp[j++];
                }
            }
            while (i <= mid) arr[k++] = tmp[i++];
            while (j <= right) arr[k++] = tmp[j++];
        }
    }

    // =============================================
    // 演示3：性能对比
    // =============================================

    static void demo3_Performance() {
        System.out.println("--- 演示3：Fork/Join vs 普通排序 性能对比 ---");

        int[] sizes = {100_000, 500_000, 1_000_000, 5_000_000};

        System.out.printf("  %-12s %-15s %-15s %-10s%n", "数组大小", "Arrays.sort", "Fork/Join排序", "加速比");
        System.out.println("  " + "-".repeat(55));

        ForkJoinPool pool = new ForkJoinPool();

        for (int size : sizes) {
            int[] original = new int[size];
            for (int i = 0; i < size; i++) {
                original[i] = ThreadLocalRandom.current().nextInt(10_000_000);
            }

            // Arrays.sort
            int[] arr1 = Arrays.copyOf(original, original.length);
            long start = System.currentTimeMillis();
            Arrays.sort(arr1);
            long sortTime = System.currentTimeMillis() - start;

            // Fork/Join
            int[] arr2 = Arrays.copyOf(original, original.length);
            int[] temp = new int[arr2.length];
            start = System.currentTimeMillis();
            pool.invoke(new MergeSortAction(arr2, temp, 0, arr2.length - 1));
            long forkJoinTime = System.currentTimeMillis() - start;

            double speedup = sortTime == 0 ? 0 : (double) sortTime / forkJoinTime;
            System.out.printf("  %-12d %-15s %-15s %-10s%n",
                    size,
                    sortTime + "ms",
                    forkJoinTime + "ms",
                    String.format("%.2fx", speedup));
        }

        pool.shutdown();
        System.out.println("\n  💡 提示: Fork/Join 在大数据量时优势更明显，小数据量开销可能大于收益");
        System.out.println("  💡 工作窃取(Work-Stealing): 空闲线程从繁忙线程的双端队列尾部窃取任务，提高CPU利用率");
    }

    // ========== 辅助方法 ==========

    static boolean isSorted(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < arr[i - 1]) return false;
        }
        return true;
    }
}
