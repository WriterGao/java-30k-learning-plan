import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Semaphore 信号量/限流演示
 *
 * 演示内容：
 * 1. 基本用法：限制同时访问的线程数
 * 2. 模拟停车场（限流场景）
 * 3. 公平模式 vs 非公平模式
 * 4. tryAcquire 非阻塞获取
 */
public class SemaphoreDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== Semaphore 演示 ==========\n");

        demo1_BasicUsage();
        System.out.println();

        demo2_ParkingLot();
        System.out.println();

        demo3_FairVsUnfair();
        System.out.println();

        demo4_TryAcquire();
    }

    /**
     * 演示1：基本用法 —— 限制同时访问某资源的线程数
     */
    static void demo1_BasicUsage() throws InterruptedException {
        System.out.println("--- 演示1：基本用法（限制并发数为3）---");

        Semaphore semaphore = new Semaphore(3); // 允许3个线程同时访问

        for (int i = 1; i <= 8; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    semaphore.acquire(); // 获取许可
                    System.out.println("  线程 " + threadId + " 获取许可，当前可用许可: "
                            + semaphore.availablePermits()
                            + ", 等待线程数: " + semaphore.getQueueLength());
                    Thread.sleep((long) (Math.random() * 2000 + 500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release(); // 释放许可
                    System.out.println("  线程 " + threadId + " 释放许可");
                }
            }, "T-" + i).start();
        }

        Thread.sleep(8000);
        System.out.println("  ✅ 所有线程执行完毕，最终可用许可: " + semaphore.availablePermits());
    }

    /**
     * 演示2：模拟停车场
     * 停车场有固定车位（许可数），车满时后续车辆需排队等待
     */
    static void demo2_ParkingLot() throws InterruptedException {
        System.out.println("--- 演示2：模拟停车场（5个车位，10辆车）---");

        final int PARKING_SPACES = 5;
        final int CAR_COUNT = 10;
        Semaphore parking = new Semaphore(PARKING_SPACES, true); // 公平模式

        ExecutorService executor = Executors.newFixedThreadPool(CAR_COUNT);

        for (int i = 1; i <= CAR_COUNT; i++) {
            final int carId = i;
            executor.submit(() -> {
                try {
                    System.out.println("  🚗 车辆 " + carId + " 到达停车场，等待车位...");
                    long waitStart = System.currentTimeMillis();
                    parking.acquire();
                    long waitTime = System.currentTimeMillis() - waitStart;

                    System.out.println("  🅿️ 车辆 " + carId + " 停入车位（等待 " + waitTime
                            + "ms），剩余车位: " + parking.availablePermits());

                    // 模拟停车时间
                    Thread.sleep((long) (Math.random() * 3000 + 1000));

                    System.out.println("  🚗 车辆 " + carId + " 离开停车场");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    parking.release();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("  ✅ 所有车辆已离开，剩余车位: " + parking.availablePermits());
    }

    /**
     * 演示3：公平模式 vs 非公平模式
     */
    static void demo3_FairVsUnfair() throws InterruptedException {
        System.out.println("--- 演示3：公平 vs 非公平模式 ---");

        System.out.println("  [非公平模式]");
        testFairness(new Semaphore(1, false)); // 非公平

        Thread.sleep(1000);
        System.out.println();

        System.out.println("  [公平模式]");
        testFairness(new Semaphore(1, true));  // 公平
    }

    static void testFairness(Semaphore semaphore) throws InterruptedException {
        // 先占住许可
        semaphore.acquire();

        // 启动多个线程排队等待
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println("    线程 " + id + " 获取许可");
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            }, "Fair-" + i).start();
            Thread.sleep(50); // 保证按顺序排队
        }

        Thread.sleep(100);
        semaphore.release(); // 释放初始许可
        Thread.sleep(2000);  // 等待所有线程完成
    }

    /**
     * 演示4：tryAcquire 非阻塞获取
     */
    static void demo4_TryAcquire() throws InterruptedException {
        System.out.println("--- 演示4：tryAcquire 非阻塞获取 ---");

        Semaphore semaphore = new Semaphore(2);

        // 先获取全部许可
        semaphore.acquire(2);
        System.out.println("  许可已全部被占用 (available=" + semaphore.availablePermits() + ")");

        // 尝试非阻塞获取
        boolean acquired1 = semaphore.tryAcquire();
        System.out.println("  tryAcquire() 立即尝试: " + (acquired1 ? "成功" : "失败"));

        // 尝试带超时的获取
        boolean acquired2 = semaphore.tryAcquire(1, TimeUnit.SECONDS);
        System.out.println("  tryAcquire(1s) 超时尝试: " + (acquired2 ? "成功" : "失败（超时）"));

        // 释放后再尝试
        semaphore.release(2);
        boolean acquired3 = semaphore.tryAcquire(2);
        System.out.println("  释放后 tryAcquire(2): " + (acquired3 ? "成功" : "失败"));

        if (acquired3) {
            semaphore.release(2);
        }
    }
}
