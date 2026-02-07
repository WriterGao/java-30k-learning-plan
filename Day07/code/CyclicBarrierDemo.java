import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CyclicBarrier 循环栅栏演示
 *
 * 演示内容：
 * 1. 基本用法：多线程互相等待
 * 2. barrierAction 回调
 * 3. 循环复用（多轮）
 * 4. 超时与 BrokenBarrierException
 */
public class CyclicBarrierDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== CyclicBarrier 演示 ==========\n");

        demo1_BasicBarrier();
        System.out.println();

        demo2_BarrierAction();
        System.out.println();

        demo3_CyclicReuse();
        System.out.println();

        demo4_BrokenBarrier();
    }

    /**
     * 演示1：基本用法 —— 多线程到达栅栏后互相等待
     */
    static void demo1_BasicBarrier() throws InterruptedException {
        System.out.println("--- 演示1：基本用法（多线程互相等待）---");

        int parties = 4;
        CyclicBarrier barrier = new CyclicBarrier(parties);

        for (int i = 1; i <= parties; i++) {
            final int playerId = i;
            new Thread(() -> {
                try {
                    long prepareTime = (long) (Math.random() * 2000 + 500);
                    Thread.sleep(prepareTime);
                    System.out.println("  玩家 " + playerId + " 已准备就绪（准备耗时 " + prepareTime + "ms）");

                    int arrivalIndex = barrier.await(); // 等待其他线程
                    System.out.println("  玩家 " + playerId + " 开始游戏（到达序号: " + arrivalIndex + "）");
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }, "Player-" + i).start();
        }

        Thread.sleep(5000); // 等待所有线程执行完
        System.out.println("  ✅ 所有玩家已进入游戏！");
    }

    /**
     * 演示2：barrierAction —— 所有线程到达后执行的回调任务
     */
    static void demo2_BarrierAction() throws InterruptedException {
        System.out.println("--- 演示2：barrierAction 回调 ---");

        int parties = 3;
        // 分段数据: 每个线程处理一段
        final int[][] data = new int[parties][];
        final int[] result = new int[1];

        CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
            // 所有线程到达后执行：汇总结果
            int total = 0;
            for (int[] segment : data) {
                if (segment != null) {
                    for (int v : segment) {
                        total += v;
                    }
                }
            }
            result[0] = total;
            System.out.println("  📊 [barrierAction] 数据汇总完成，总和 = " + total);
        });

        for (int i = 0; i < parties; i++) {
            final int segmentId = i;
            new Thread(() -> {
                try {
                    // 模拟每个线程计算一段数据
                    int[] segment = new int[10];
                    for (int j = 0; j < segment.length; j++) {
                        segment[j] = segmentId * 10 + j + 1;
                    }
                    data[segmentId] = segment;

                    int sum = 0;
                    for (int v : segment) sum += v;
                    System.out.println("  线程 " + segmentId + " 计算完成，段内和 = " + sum);

                    barrier.await(); // 等待其他线程
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }, "Worker-" + i).start();
        }

        Thread.sleep(3000);
        System.out.println("  ✅ 最终汇总结果 = " + result[0]);
    }

    /**
     * 演示3：CyclicBarrier 循环复用（多轮执行）
     */
    static void demo3_CyclicReuse() throws InterruptedException {
        System.out.println("--- 演示3：循环复用（多轮执行）---");

        int parties = 3;
        int rounds = 3;

        CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
            System.out.println("  ✅ 所有线程完成当前轮次！进入下一轮。\n");
        });

        for (int i = 1; i <= parties; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int round = 1; round <= rounds; round++) {
                        long workTime = (long) (Math.random() * 1000 + 200);
                        Thread.sleep(workTime);
                        System.out.println("  线程 " + threadId + " 完成第 " + round + " 轮（耗时 " + workTime + "ms）");
                        barrier.await(); // 每轮结束等待
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }, "T-" + i).start();
        }

        Thread.sleep(15000);
        System.out.println("  ✅ " + rounds + " 轮全部完成！CyclicBarrier 支持复用。");
    }

    /**
     * 演示4：BrokenBarrierException
     * 当某个线程在 await 时被中断或超时，栅栏会被打破
     */
    static void demo4_BrokenBarrier() throws InterruptedException {
        System.out.println("--- 演示4：BrokenBarrierException（栅栏损坏）---");

        CyclicBarrier barrier = new CyclicBarrier(3);

        // 线程1：正常等待
        Thread t1 = new Thread(() -> {
            try {
                System.out.println("  线程1 开始等待...");
                barrier.await(2, TimeUnit.SECONDS); // 带超时
                System.out.println("  线程1 通过栅栏");
            } catch (TimeoutException e) {
                System.out.println("  ⚠️ 线程1 超时，栅栏已损坏 (isBroken=" + barrier.isBroken() + ")");
            } catch (InterruptedException | BrokenBarrierException e) {
                System.out.println("  ⚠️ 线程1 捕获异常: " + e.getClass().getSimpleName());
            }
        }, "T1");

        // 线程2：正常等待，但因线程1超时而收到 BrokenBarrierException
        Thread t2 = new Thread(() -> {
            try {
                System.out.println("  线程2 开始等待...");
                barrier.await(); // 无超时
                System.out.println("  线程2 通过栅栏");
            } catch (InterruptedException | BrokenBarrierException e) {
                System.out.println("  ⚠️ 线程2 捕获 BrokenBarrierException (isBroken=" + barrier.isBroken() + ")");
            }
        }, "T2");

        t1.start();
        t2.start();

        // 不启动第3个线程，导致超时
        t1.join();
        t2.join();

        System.out.println("  栅栏状态: isBroken=" + barrier.isBroken()
                + ", numberWaiting=" + barrier.getNumberWaiting());

        // reset 可以重置栅栏
        barrier.reset();
        System.out.println("  执行 reset() 后: isBroken=" + barrier.isBroken());
    }
}
