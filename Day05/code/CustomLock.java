import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 基于 AQS 的自定义独占锁（不可重入）
 *
 * 演示内容：
 * 1. 继承 AQS 实现自定义同步器
 * 2. 实现 Lock 接口
 * 3. 独占模式的获取/释放逻辑
 * 4. tryLock 超时机制
 * 5. Condition 条件等待
 *
 * 设计说明：
 * - state = 0 表示锁空闲
 * - state = 1 表示锁被占用
 * - 不支持重入（同一线程再次获取会阻塞）
 */
public class CustomLock implements Lock {

    /**
     * 内部同步器，继承 AQS
     * AQS 的核心：state 状态变量 + CLH 等待队列
     */
    private static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 判断锁是否被当前线程独占
         */
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1 && getExclusiveOwnerThread() == Thread.currentThread();
        }

        /**
         * 尝试获取锁（独占模式）
         * 被 AQS 的 acquire() 方法调用
         *
         * @param acquires 获取数量（这里固定为1）
         * @return true 表示获取成功
         */
        @Override
        protected boolean tryAcquire(int acquires) {
            // CAS 将 state 从 0 改为 1
            if (compareAndSetState(0, 1)) {
                // 设置当前线程为独占线程
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * 尝试释放锁（独占模式）
         * 被 AQS 的 release() 方法调用
         *
         * @param releases 释放数量（这里固定为1）
         * @return true 表示释放成功（完全释放）
         */
        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException("锁未被持有，无法释放");
            }
            // 清除独占线程
            setExclusiveOwnerThread(null);
            // 设置 state 为 0（释放锁）
            // 注意：这里不需要 CAS，因为只有持有锁的线程才能释放
            setState(0);
            return true;
        }

        /**
         * 创建 Condition 对象
         */
        Condition newCondition() {
            return new ConditionObject();
        }
    }

    // 同步器实例
    private final Sync sync = new Sync();

    // ==================== Lock 接口实现 ====================

    /**
     * 获取锁（阻塞式）
     * 内部调用 AQS 的 acquire()，流程：
     * 1. 先 tryAcquire() 尝试获取
     * 2. 失败则加入 CLH 队列等待
     * 3. 在队列中自旋/阻塞，直到获取成功
     */
    @Override
    public void lock() {
        sync.acquire(1);
    }

    /**
     * 获取锁（可中断）
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 尝试获取锁（非阻塞）
     * @return true 表示获取成功，false 表示获取失败
     */
    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    /**
     * 尝试获取锁（带超时）
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    /**
     * 释放锁
     * 内部调用 AQS 的 release()，流程：
     * 1. 先 tryRelease() 释放状态
     * 2. 唤醒 CLH 队列中的后继节点
     */
    @Override
    public void unlock() {
        sync.release(1);
    }

    /**
     * 获取条件变量
     */
    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询锁是否被持有
     */
    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询是否有线程在等待获取锁
     */
    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    // ==================== 测试代码 ====================

    /**
     * 测试1：基本的 lock/unlock
     */
    static void testBasicLockUnlock() throws InterruptedException {
        System.out.println("========== 测试1：基本 lock/unlock ==========");

        CustomLock lock = new CustomLock();
        int[] sharedCounter = {0};
        final int THREAD_COUNT = 10;
        final int INCREMENT_COUNT = 10000;

        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENT_COUNT; j++) {
                    lock.lock();
                    try {
                        sharedCounter[0]++;
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

        System.out.println("期望值: " + (THREAD_COUNT * INCREMENT_COUNT));
        System.out.println("实际值: " + sharedCounter[0]);
        System.out.println("耗时: " + elapsed + "ms");
        System.out.println("结果: " + (sharedCounter[0] == THREAD_COUNT * INCREMENT_COUNT ? "✅ 正确" : "❌ 错误"));
        System.out.println();
    }

    /**
     * 测试2：tryLock 非阻塞获取
     */
    static void testTryLock() throws InterruptedException {
        System.out.println("========== 测试2：tryLock 非阻塞获取 ==========");

        CustomLock lock = new CustomLock();

        // 主线程先获取锁
        lock.lock();
        System.out.println("[主线程] 已获取锁");

        Thread t = new Thread(() -> {
            // tryLock 立即返回，不阻塞
            boolean acquired = lock.tryLock();
            System.out.println("[子线程] tryLock 结果: " + acquired);

            if (!acquired) {
                System.out.println("[子线程] 未获取到锁，尝试带超时的 tryLock...");
                try {
                    acquired = lock.tryLock(500, TimeUnit.MILLISECONDS);
                    System.out.println("[子线程] tryLock(500ms) 结果: " + acquired);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
        Thread.sleep(200);

        // 主线程释放锁
        lock.unlock();
        System.out.println("[主线程] 已释放锁");

        t.join();
        System.out.println();
    }

    /**
     * 测试3：Condition 条件等待
     */
    static void testCondition() throws InterruptedException {
        System.out.println("========== 测试3：Condition 条件等待 ==========");

        CustomLock lock = new CustomLock();
        Condition condition = lock.newCondition();
        boolean[] dataReady = {false};

        // 消费者线程：等待数据准备好
        Thread consumer = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("[消费者] 等待数据...");
                while (!dataReady[0]) {
                    condition.await(); // 释放锁并等待
                }
                System.out.println("[消费者] 数据已就绪，开始消费！");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "消费者");

        // 生产者线程：准备数据后通知
        Thread producer = new Thread(() -> {
            try {
                Thread.sleep(500); // 模拟数据准备
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            lock.lock();
            try {
                dataReady[0] = true;
                System.out.println("[生产者] 数据已准备好，通知消费者");
                condition.signal(); // 唤醒等待的消费者
            } finally {
                lock.unlock();
            }
        }, "生产者");

        consumer.start();
        producer.start();
        consumer.join();
        producer.join();

        System.out.println();
    }

    /**
     * 测试4：多线程竞争排队
     */
    static void testQueuedThreads() throws InterruptedException {
        System.out.println("========== 测试4：多线程竞争排队 ==========");

        CustomLock lock = new CustomLock();

        for (int i = 0; i < 5; i++) {
            final int threadNo = i;
            new Thread(() -> {
                System.out.println("[线程" + threadNo + "] 尝试获取锁...");
                lock.lock();
                try {
                    System.out.println("[线程" + threadNo + "] 获得锁，执行任务（还有等待线程: "
                            + lock.hasQueuedThreads() + "）");
                    Thread.sleep(200); // 模拟任务
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("[线程" + threadNo + "] 释放锁");
                    lock.unlock();
                }
            }, "Worker-" + i).start();

            Thread.sleep(50); // 错开启动，观察排队效果
        }

        Thread.sleep(2000); // 等待所有线程完成
    }

    public static void main(String[] args) throws InterruptedException {
        testBasicLockUnlock();
        testTryLock();
        testCondition();
        testQueuedThreads();
    }
}
