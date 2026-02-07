import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CAS（Compare And Swap）基本操作演示
 *
 * 演示内容：
 * 1. AtomicInteger 的 CAS 操作
 * 2. compareAndSet 成功与失败
 * 3. getAndIncrement 底层 CAS 自旋
 * 4. 多线程环境下的 CAS 原子操作
 */
public class CASDemo {

    // ==================== 1. CAS 基本操作 ====================

    /**
     * 演示 compareAndSet 的基本用法
     */
    static void basicCASDemo() {
        System.out.println("========== 1. CAS 基本操作 ==========");

        AtomicInteger atomicInt = new AtomicInteger(100);
        System.out.println("初始值: " + atomicInt.get());

        // CAS 操作：期望值=100, 新值=200
        boolean success1 = atomicInt.compareAndSet(100, 200);
        System.out.println("CAS(100 -> 200): " + success1 + ", 当前值: " + atomicInt.get());

        // CAS 操作：期望值=100（已经是200了），新值=300
        boolean success2 = atomicInt.compareAndSet(100, 300);
        System.out.println("CAS(100 -> 300): " + success2 + ", 当前值: " + atomicInt.get());

        // CAS 操作：期望值=200, 新值=300
        boolean success3 = atomicInt.compareAndSet(200, 300);
        System.out.println("CAS(200 -> 300): " + success3 + ", 当前值: " + atomicInt.get());

        System.out.println();
    }

    // ==================== 2. 常用原子操作 ====================

    /**
     * 演示 AtomicInteger 的常用方法
     */
    static void atomicOperationsDemo() {
        System.out.println("========== 2. 常用原子操作 ==========");

        AtomicInteger counter = new AtomicInteger(0);

        // getAndIncrement: 先获取，再+1（相当于 i++）
        int old1 = counter.getAndIncrement();
        System.out.println("getAndIncrement: 旧值=" + old1 + ", 当前值=" + counter.get());

        // incrementAndGet: 先+1，再获取（相当于 ++i）
        int new1 = counter.incrementAndGet();
        System.out.println("incrementAndGet: 新值=" + new1 + ", 当前值=" + counter.get());

        // getAndAdd: 先获取，再加指定值
        int old2 = counter.getAndAdd(10);
        System.out.println("getAndAdd(10): 旧值=" + old2 + ", 当前值=" + counter.get());

        // addAndGet: 先加指定值，再获取
        int new2 = counter.addAndGet(10);
        System.out.println("addAndGet(10): 新值=" + new2 + ", 当前值=" + counter.get());

        // getAndUpdate (JDK 8+): 使用 lambda 更新
        int old3 = counter.getAndUpdate(x -> x * 2);
        System.out.println("getAndUpdate(x*2): 旧值=" + old3 + ", 当前值=" + counter.get());

        // accumulateAndGet (JDK 8+): 带累加器的更新
        int new3 = counter.accumulateAndGet(5, Integer::max);
        System.out.println("accumulateAndGet(5, max): 新值=" + new3 + ", 当前值=" + counter.get());

        System.out.println();
    }

    // ==================== 3. AtomicReference 演示 ====================

    /**
     * 演示 AtomicReference 对引用类型的 CAS 操作
     */
    static void atomicReferenceDemo() {
        System.out.println("========== 3. AtomicReference 演示 ==========");

        AtomicReference<String> atomicRef = new AtomicReference<>("Hello");
        System.out.println("初始值: " + atomicRef.get());

        boolean success1 = atomicRef.compareAndSet("Hello", "World");
        System.out.println("CAS(Hello -> World): " + success1 + ", 当前值: " + atomicRef.get());

        boolean success2 = atomicRef.compareAndSet("Hello", "Java");
        System.out.println("CAS(Hello -> Java): " + success2 + ", 当前值: " + atomicRef.get());

        System.out.println();
    }

    // ==================== 4. 多线程 CAS 计数器 ====================

    /**
     * 多线程环境下使用 CAS 保证原子性
     * 对比：普通 int 变量 vs AtomicInteger
     */
    static void multiThreadCASDemo() throws InterruptedException {
        System.out.println("========== 4. 多线程 CAS 计数器 ==========");

        final int THREAD_COUNT = 20;
        final int INCREMENT_COUNT = 10000;

        // 普通变量（线程不安全）
        final int[] unsafeCounter = {0};
        // 原子变量（线程安全）
        AtomicInteger safeCounter = new AtomicInteger(0);

        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENT_COUNT; j++) {
                    unsafeCounter[0]++;          // 非原子操作
                    safeCounter.incrementAndGet(); // CAS 原子操作
                }
            });
        }

        // 启动所有线程
        for (Thread t : threads) t.start();
        // 等待所有线程结束
        for (Thread t : threads) t.join();

        int expected = THREAD_COUNT * INCREMENT_COUNT;
        System.out.println("期望值: " + expected);
        System.out.println("普通 int 计数器: " + unsafeCounter[0] + " (可能丢失更新)");
        System.out.println("AtomicInteger 计数器: " + safeCounter.get() + " (始终正确)");
        System.out.println("差值: " + (expected - unsafeCounter[0]));

        System.out.println();
    }

    // ==================== 5. CAS 自旋演示 ====================

    /**
     * 手动实现 CAS 自旋操作
     * 模拟 getAndIncrement 的底层实现
     */
    static void casSpinDemo() {
        System.out.println("========== 5. CAS 自旋演示 ==========");

        AtomicInteger value = new AtomicInteger(0);

        // 手动实现 CAS 自旋（模拟 getAndIncrement 底层）
        int spinCount = 0;
        int oldValue;
        do {
            oldValue = value.get();
            spinCount++;
            // 如果 CAS 失败（被其他线程修改），则重试
        } while (!value.compareAndSet(oldValue, oldValue + 1));

        System.out.println("CAS 自旋完成, 自旋次数: " + spinCount);
        System.out.println("旧值: " + oldValue + ", 新值: " + value.get());

        System.out.println();
        System.out.println("【原理说明】");
        System.out.println("CAS 自旋是乐观锁的核心思想：");
        System.out.println("1. 读取当前值（期望值）");
        System.out.println("2. 计算新值");
        System.out.println("3. 调用 CAS 尝试更新");
        System.out.println("4. 如果失败（被其他线程修改），回到步骤1重试");
        System.out.println("5. 直到 CAS 成功");
    }

    // ==================== main ====================

    public static void main(String[] args) throws InterruptedException {
        basicCASDemo();
        atomicOperationsDemo();
        atomicReferenceDemo();
        multiThreadCASDemo();
        casSpinDemo();
    }
}
