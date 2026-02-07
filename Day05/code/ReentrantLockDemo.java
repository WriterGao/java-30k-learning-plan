import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Condition;
import java.util.ArrayList;
import java.util.List;

/**
 * ReentrantLock 公平锁 vs 非公平锁 演示
 *
 * 演示内容：
 * 1. 公平锁的 FIFO 获取顺序
 * 2. 非公平锁的插队现象
 * 3. 两种模式下的性能对比
 * 4. ReentrantLock 的可重入性
 * 5. Condition 精确唤醒
 * 6. ReentrantReadWriteLock 读写锁演示
 */
public class ReentrantLockDemo {

    // ==================== 1. 公平锁 vs 非公平锁 行为对比 ====================

    /**
     * 观察公平锁与非公平锁的获取顺序差异
     */
    static void fairnessComparisonDemo() throws InterruptedException {
        System.out.println("========== 1. 公平锁 vs 非公平锁 行为对比 ==========");

        System.out.println("\n--- 非公平锁（默认） ---");
        testFairness(new ReentrantLock(false));

        Thread.sleep(500);

        System.out.println("\n--- 公平锁 ---");
        testFairness(new ReentrantLock(true));

        System.out.println();
    }

    private static void testFairness(ReentrantLock lock) throws InterruptedException {
        List<String> acquireOrder = new ArrayList<>();

        // 先占住锁
        lock.lock();

        // 启动5个线程，它们都会排队等待
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int no = i;
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    acquireOrder.add("T" + no);
                } finally {
                    lock.unlock();
                }
            }, "T" + i);
            threads[i].start();
            Thread.sleep(50); // 确保线程按顺序排队
        }

        Thread.sleep(100);
        // 释放锁，让排队线程开始获取
        lock.unlock();

        for (Thread t : threads) t.join();

        System.out.println("获取顺序: " + acquireOrder);
        System.out.println("（公平锁应为 T0->T1->T2->T3->T4，非公平锁可能乱序）");
    }

    // ==================== 2. 性能对比 ====================

    /**
     * 公平锁 vs 非公平锁 性能对比
     * 非公平锁通常性能更好，因为减少了线程切换开销
     */
    static void performanceComparisonDemo() throws InterruptedException {
        System.out.println("========== 2. 公平锁 vs 非公平锁 性能对比 ==========");

        final int THREAD_COUNT = 10;
        final int LOOP_COUNT = 100_000;

        // 非公平锁性能测试
        long unfairTime = testPerformance(new ReentrantLock(false), THREAD_COUNT, LOOP_COUNT);
        System.out.println("非公平锁耗时: " + unfairTime + "ms");

        // 公平锁性能测试
        long fairTime = testPerformance(new ReentrantLock(true), THREAD_COUNT, LOOP_COUNT);
        System.out.println("公平锁耗时: " + fairTime + "ms");

        System.out.println("性能比: 公平锁/非公平锁 = " + String.format("%.2f", (double) fairTime / unfairTime));
        System.out.println("结论: 非公平锁通常更快，因为减少了线程上下文切换");
        System.out.println();
    }

    private static long testPerformance(ReentrantLock lock, int threadCount, int loopCount)
            throws InterruptedException {
        int[] counter = {0};
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long elapsed = System.currentTimeMillis() - start;

        // 验证正确性
        assert counter[0] == threadCount * loopCount : "计数器不正确!";
        return elapsed;
    }

    // ==================== 3. 可重入性演示 ====================

    /**
     * 演示 ReentrantLock 的可重入特性
     * 同一线程可以多次获取同一把锁
     */
    static void reentrantDemo() {
        System.out.println("========== 3. 可重入性演示 ==========");

        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        System.out.println("第1次获取锁, holdCount=" + lock.getHoldCount());

        lock.lock();
        System.out.println("第2次获取锁, holdCount=" + lock.getHoldCount());

        lock.lock();
        System.out.println("第3次获取锁, holdCount=" + lock.getHoldCount());

        lock.unlock();
        System.out.println("第1次释放锁, holdCount=" + lock.getHoldCount());

        lock.unlock();
        System.out.println("第2次释放锁, holdCount=" + lock.getHoldCount());

        lock.unlock();
        System.out.println("第3次释放锁, holdCount=" + lock.getHoldCount());

        System.out.println("注意: 获取了几次锁就必须释放几次！");
        System.out.println();
    }

    // ==================== 4. Condition 精确唤醒 ====================

    /**
     * 演示使用多个 Condition 实现精确唤醒
     * 实现 A -> B -> C 轮流打印
     */
    static void conditionDemo() throws InterruptedException {
        System.out.println("========== 4. Condition 精确唤醒（A->B->C 轮流打印） ==========");

        ReentrantLock lock = new ReentrantLock();
        Condition condA = lock.newCondition();
        Condition condB = lock.newCondition();
        Condition condC = lock.newCondition();
        int[] state = {1}; // 1=A, 2=B, 3=C

        Thread threadA = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                lock.lock();
                try {
                    while (state[0] != 1) condA.await();
                    System.out.print("A");
                    state[0] = 2;
                    condB.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });

        Thread threadB = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                lock.lock();
                try {
                    while (state[0] != 2) condB.await();
                    System.out.print("B");
                    state[0] = 3;
                    condC.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });

        Thread threadC = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                lock.lock();
                try {
                    while (state[0] != 3) condC.await();
                    System.out.print("C");
                    state[0] = 1;
                    condA.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });

        threadA.start();
        threadB.start();
        threadC.start();
        threadA.join();
        threadB.join();
        threadC.join();

        System.out.println(" (循环3次)");
        System.out.println("Condition 可以实现精确唤醒特定线程，这是 synchronized + notify 做不到的");
        System.out.println();
    }

    // ==================== 5. 读写锁演示 ====================

    /**
     * ReentrantReadWriteLock 演示
     * 读锁共享、写锁独占
     */
    static void readWriteLockDemo() throws InterruptedException {
        System.out.println("========== 5. ReentrantReadWriteLock 读写锁演示 ==========");

        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
        ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();
        String[] data = {"初始数据"};

        // 启动3个读线程
        for (int i = 0; i < 3; i++) {
            final int no = i;
            new Thread(() -> {
                readLock.lock();
                try {
                    System.out.println("[读线程" + no + "] 获取读锁，读取数据: " + data[0]
                            + " (读锁数量: " + rwLock.getReadLockCount() + ")");
                    Thread.sleep(500); // 模拟读操作耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    readLock.unlock();
                    System.out.println("[读线程" + no + "] 释放读锁");
                }
            }, "Reader-" + i).start();
        }

        Thread.sleep(100); // 确保读线程先获取锁

        // 启动1个写线程
        new Thread(() -> {
            System.out.println("[写线程] 尝试获取写锁（需要等待所有读锁释放）...");
            writeLock.lock();
            try {
                data[0] = "更新后的数据";
                System.out.println("[写线程] 获取写锁，写入数据: " + data[0]);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                writeLock.unlock();
                System.out.println("[写线程] 释放写锁");
            }
        }, "Writer").start();

        Thread.sleep(2000); // 等待所有操作完成
        System.out.println("最终数据: " + data[0]);
        System.out.println("读写锁特点: 读读共享、读写互斥、写写互斥");
        System.out.println();
    }

    // ==================== 6. synchronized vs ReentrantLock 对比 ====================

    static void comparisonSummary() {
        System.out.println("========== 6. synchronized vs ReentrantLock 对比 ==========");
        System.out.println();
        System.out.println("┌─────────────────┬──────────────────────┬──────────────────────┐");
        System.out.println("│ 特性             │ synchronized          │ ReentrantLock          │");
        System.out.println("├─────────────────┼──────────────────────┼──────────────────────┤");
        System.out.println("│ 实现层面         │ JVM 关键字             │ Java API (AQS)         │");
        System.out.println("│ 锁的获取/释放    │ 自动                   │ 手动 lock/unlock        │");
        System.out.println("│ 可中断           │ 不可中断               │ lockInterruptibly()    │");
        System.out.println("│ 超时获取         │ 不支持                 │ tryLock(timeout)       │");
        System.out.println("│ 公平性           │ 非公平                 │ 可选公平/非公平          │");
        System.out.println("│ 条件变量         │ 1个(wait/notify)      │ 多个(Condition)        │");
        System.out.println("│ 可重入           │ 是                    │ 是                      │");
        System.out.println("│ 读写锁           │ 不支持                 │ ReadWriteLock           │");
        System.out.println("│ 性能(JDK6+)      │ 相当                  │ 相当                    │");
        System.out.println("└─────────────────┴──────────────────────┴──────────────────────┘");
        System.out.println();
        System.out.println("建议: 简单场景用 synchronized，需要高级功能时用 ReentrantLock");
    }

    // ==================== main ====================

    public static void main(String[] args) throws InterruptedException {
        fairnessComparisonDemo();
        performanceComparisonDemo();
        reentrantDemo();
        conditionDemo();
        readWriteLockDemo();
        comparisonSummary();
    }
}
