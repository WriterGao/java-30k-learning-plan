import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch 倒计时门闩演示
 *
 * 演示内容：
 * 1. 基本用法：主线程等待多个子任务完成
 * 2. 模拟多服务启动场景
 * 3. 模拟运动员赛跑（发令枪）
 * 4. 超时等待
 */
public class CountDownLatchDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== CountDownLatch 演示 ==========\n");

        demo1_BasicUsage();
        System.out.println();

        demo2_ServiceStartup();
        System.out.println();

        demo3_StartGun();
        System.out.println();

        demo4_TimeoutAwait();
    }

    /**
     * 演示1：基本用法 —— 主线程等待多个子任务完成
     */
    static void demo1_BasicUsage() throws InterruptedException {
        System.out.println("--- 演示1：基本用法（主线程等待子任务完成）---");

        int taskCount = 5;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    // 模拟耗时操作
                    Thread.sleep((long) (Math.random() * 2000));
                    System.out.println("  任务 " + taskId + " 完成, 当前剩余计数: " + (latch.getCount() - 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // 计数减1
                }
            }, "Task-" + i).start();
        }

        System.out.println("  主线程等待所有任务完成...");
        latch.await(); // 阻塞直到计数为0
        System.out.println("  ✅ 所有 " + taskCount + " 个任务已完成！主线程继续执行。");
    }

    /**
     * 演示2：模拟多服务启动
     * 场景：应用启动时需要多个服务（数据库、缓存、消息队列）全部就绪后才提供服务
     */
    static void demo2_ServiceStartup() throws InterruptedException {
        System.out.println("--- 演示2：模拟多服务启动 ---");

        String[] services = {"数据库连接池", "Redis缓存", "消息队列", "配置中心", "注册中心"};
        CountDownLatch latch = new CountDownLatch(services.length);
        ExecutorService executor = Executors.newFixedThreadPool(services.length);

        long start = System.currentTimeMillis();

        for (String service : services) {
            executor.submit(() -> {
                try {
                    // 模拟各服务不同的启动时间
                    long initTime = (long) (Math.random() * 3000 + 500);
                    Thread.sleep(initTime);
                    System.out.println("  ✓ " + service + " 启动完成（耗时 " + initTime + "ms）");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  ✅ 所有服务启动完成！总耗时 " + elapsed + "ms（并行启动）");
        System.out.println("  ⚡ 如果串行启动，耗时约为各服务启动时间之和，远大于 " + elapsed + "ms");

        executor.shutdown();
    }

    /**
     * 演示3：模拟运动员赛跑（发令枪场景）
     * 所有运动员先就位，裁判发令后同时起跑
     */
    static void demo3_StartGun() throws InterruptedException {
        System.out.println("--- 演示3：发令枪场景（CountDownLatch(1) 作为开关）---");

        int runnerCount = 4;
        CountDownLatch readyLatch = new CountDownLatch(runnerCount); // 等待所有运动员就位
        CountDownLatch startLatch = new CountDownLatch(1);           // 发令枪（开关）
        CountDownLatch finishLatch = new CountDownLatch(runnerCount); // 等待所有运动员跑完

        for (int i = 1; i <= runnerCount; i++) {
            final int runnerId = i;
            new Thread(() -> {
                try {
                    System.out.println("  🏃 运动员 " + runnerId + " 就位");
                    readyLatch.countDown(); // 就位
                    startLatch.await();     // 等待发令枪
                    // 模拟跑步
                    long runTime = (long) (Math.random() * 3000 + 1000);
                    Thread.sleep(runTime);
                    System.out.println("  🏁 运动员 " + runnerId + " 到达终点，用时 " + runTime + "ms");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            }, "Runner-" + i).start();
        }

        readyLatch.await(); // 等待所有运动员就位
        System.out.println("  📢 所有运动员已就位，裁判发令：跑！");
        startLatch.countDown(); // 发令

        finishLatch.await(); // 等待所有人跑完
        System.out.println("  ✅ 比赛结束！");
    }

    /**
     * 演示4：超时等待
     */
    static void demo4_TimeoutAwait() throws InterruptedException {
        System.out.println("--- 演示4：超时等待 ---");

        CountDownLatch latch = new CountDownLatch(3);

        // 只完成2个任务，第3个不完成
        for (int i = 1; i <= 2; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    System.out.println("  任务 " + taskId + " 完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 带超时的等待
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        if (completed) {
            System.out.println("  ✅ 所有任务在超时前完成");
        } else {
            System.out.println("  ⚠️ 等待超时！还有 " + latch.getCount() + " 个任务未完成");
        }
    }
}
