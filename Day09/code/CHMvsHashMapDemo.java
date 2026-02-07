import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrentHashMap vs HashMap 多线程安全性对比
 *
 * 演示内容：
 * 1. HashMap 多线程不安全（数据丢失 / 异常）
 * 2. Hashtable / synchronizedMap / ConcurrentHashMap 正确性对比
 * 3. 非原子复合操作 vs 原子操作
 * 4. 不同 Map 的迭代器行为对比（fail-fast vs 弱一致性）
 *
 * 运行方式：
 *   cd Day09/code
 *   javac CHMvsHashMapDemo.java
 *   java CHMvsHashMapDemo
 */
public class CHMvsHashMapDemo {

    // ============================================================
    // 测试1：HashMap 多线程不安全
    // ============================================================
    static void testHashMapUnsafe() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试1】HashMap 多线程不安全演示");
        System.out.println("=".repeat(70));

        final int THREAD_COUNT = 20;
        final int OPERATIONS = 10_000;
        Map<Integer, Integer> hashMap = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < OPERATIONS; j++) {
                        int key = threadId * OPERATIONS + j;
                        hashMap.put(key, key);
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    System.out.println("  [异常] " + Thread.currentThread().getName()
                            + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, "HashMap-Thread-" + i).start();
        }

        latch.await();
        int expected = THREAD_COUNT * OPERATIONS;
        int actual = hashMap.size();
        System.out.println("  期望大小: " + expected);
        System.out.println("  实际大小: " + actual);
        System.out.println("  数据丢失: " + (expected - actual) + " 条");
        System.out.println("  异常次数: " + exceptionCount.get());
        System.out.println("  结论: HashMap 在多线程下不安全, 可能丢数据或抛异常！");
        System.out.println();
    }

    // ============================================================
    // 测试2：四种 Map 正确性对比
    // ============================================================
    static void testMapCorrectness() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试2】四种 Map 正确性对比");
        System.out.println("=".repeat(70));

        final int THREAD_COUNT = 20;
        final int OPERATIONS = 10_000;

        // HashMap（不安全）
        Map<Integer, Integer> hashMap = new HashMap<>();
        runConcurrentPut(hashMap, "HashMap (不安全)", THREAD_COUNT, OPERATIONS);

        // Hashtable
        Map<Integer, Integer> hashtable = new Hashtable<>();
        runConcurrentPut(hashtable, "Hashtable", THREAD_COUNT, OPERATIONS);

        // synchronizedMap
        Map<Integer, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        runConcurrentPut(syncMap, "synchronizedMap", THREAD_COUNT, OPERATIONS);

        // ConcurrentHashMap
        Map<Integer, Integer> concurrentMap = new ConcurrentHashMap<>();
        runConcurrentPut(concurrentMap, "ConcurrentHashMap", THREAD_COUNT, OPERATIONS);

        System.out.println();
        System.out.println("  结论: HashMap 会丢数据, 其余三种线程安全 Map 保证数据完整");
        System.out.println();
    }

    static void runConcurrentPut(Map<Integer, Integer> map, String name,
                                  int threadCount, int operations) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        int key = threadId * operations + j;
                        map.put(key, key);
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        int expected = threadCount * operations;
        boolean correct = map.size() == expected;
        System.out.printf("  %-25s -> 期望: %d, 实际: %d, 正确: %s%s%n",
                name, expected, map.size(),
                correct ? "✅" : "❌",
                exceptionCount.get() > 0 ? " (异常: " + exceptionCount.get() + " 次)" : "");
    }

    // ============================================================
    // 测试3：非原子复合操作 vs 原子操作
    // ============================================================
    static void testAtomicVsNonAtomic() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试3】非原子复合操作 vs 原子操作");
        System.out.println("=".repeat(70));

        final int THREAD_COUNT = 20;
        final int INCREMENTS = 10_000;
        int expected = THREAD_COUNT * INCREMENTS;

        // ❌ 非原子：get + put
        System.out.println("  --- ❌ 非原子操作: get() + put() ---");
        ConcurrentHashMap<String, Integer> map1 = new ConcurrentHashMap<>();
        map1.put("count", 0);
        CountDownLatch latch1 = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    Integer val = map1.get("count");
                    map1.put("count", val + 1);
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();
        System.out.printf("    期望: %d, 实际: %d, 丢失: %d ❌%n",
                expected, map1.get("count"), expected - map1.get("count"));

        // ✅ 原子：compute
        System.out.println("  --- ✅ 原子操作: compute() ---");
        ConcurrentHashMap<String, Integer> map2 = new ConcurrentHashMap<>();
        map2.put("count", 0);
        CountDownLatch latch2 = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    map2.compute("count", (k, v) -> v + 1);
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        System.out.printf("    期望: %d, 实际: %d, 正确: %s ✅%n",
                expected, map2.get("count"),
                map2.get("count") == expected ? "是" : "否");

        // ✅ 原子：merge
        System.out.println("  --- ✅ 原子操作: merge() ---");
        ConcurrentHashMap<String, Integer> map3 = new ConcurrentHashMap<>();
        map3.put("count", 0);
        CountDownLatch latch3 = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    map3.merge("count", 1, Integer::sum);
                }
                latch3.countDown();
            }).start();
        }
        latch3.await();
        System.out.printf("    期望: %d, 实际: %d, 正确: %s ✅%n",
                expected, map3.get("count"),
                map3.get("count") == expected ? "是" : "否");

        System.out.println();
        System.out.println("  结论: ConcurrentHashMap 的单个方法是线程安全的,");
        System.out.println("        但 get + put 这样的复合操作不是原子的！");
        System.out.println("        应使用 compute / merge / putIfAbsent 等原子 API");
        System.out.println();
    }

    // ============================================================
    // 测试4：迭代器行为对比
    // ============================================================
    static void testIteratorBehavior() {
        System.out.println("=".repeat(70));
        System.out.println("【测试4】不同 Map 的迭代器行为对比");
        System.out.println("=".repeat(70));

        // ConcurrentHashMap: 弱一致性迭代器
        System.out.println("  ConcurrentHashMap（弱一致性迭代器）:");
        ConcurrentHashMap<Integer, String> cmap = new ConcurrentHashMap<>();
        for (int i = 0; i < 10; i++) cmap.put(i, "v" + i);
        try {
            for (Integer key : cmap.keySet()) {
                if (key == 5) cmap.put(100, "new");
            }
            System.out.println("    迭代时修改: 不抛异常 ✅ (弱一致性)");
        } catch (ConcurrentModificationException e) {
            System.out.println("    迭代时修改: 抛异常 ❌");
        }

        // HashMap: fail-fast 迭代器
        System.out.println("  HashMap（fail-fast 迭代器）:");
        HashMap<Integer, String> hmap = new HashMap<>();
        for (int i = 0; i < 10; i++) hmap.put(i, "v" + i);
        try {
            for (Integer key : hmap.keySet()) {
                if (key == 5) hmap.put(100, "new");
            }
            System.out.println("    迭代时修改: 不抛异常（偶尔发生）");
        } catch (ConcurrentModificationException e) {
            System.out.println("    迭代时修改: ConcurrentModificationException ✅ (fail-fast)");
        }

        // Hashtable: fail-fast 迭代器
        System.out.println("  Hashtable（fail-fast 迭代器）:");
        Hashtable<Integer, String> htable = new Hashtable<>();
        for (int i = 0; i < 10; i++) htable.put(i, "v" + i);
        try {
            for (Integer key : htable.keySet()) {
                if (key == 5) htable.put(100, "new");
            }
            System.out.println("    迭代时修改: 不抛异常（偶尔发生）");
        } catch (ConcurrentModificationException e) {
            System.out.println("    迭代时修改: ConcurrentModificationException ✅ (fail-fast)");
        }

        System.out.println();
        System.out.println("  说明: ConcurrentHashMap 使用弱一致性迭代器,");
        System.out.println("        迭代过程中允许其他线程修改, 不会抛异常。");
        System.out.println("        但可能看到部分修改, 也可能看不到 (弱一致性)。");
        System.out.println();
    }

    // ============================================================
    // 测试5：并发删除安全性
    // ============================================================
    static void testConcurrentRemove() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试5】并发读写删除安全性");
        System.out.println("=".repeat(70));

        final int SIZE = 100_000;
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
        for (int i = 0; i < SIZE; i++) {
            map.put(i, i);
        }
        System.out.println("  初始大小: " + map.size());

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(3);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);
        AtomicInteger removeCount = new AtomicInteger(0);

        // 读线程
        new Thread(() -> {
            try {
                startGate.await();
                for (int i = 0; i < SIZE; i++) {
                    map.get(i);
                    readCount.incrementAndGet();
                }
            } catch (Exception e) {
                System.out.println("  [读线程异常] " + e);
            } finally {
                endGate.countDown();
            }
        }).start();

        // 写线程
        new Thread(() -> {
            try {
                startGate.await();
                for (int i = SIZE; i < SIZE * 2; i++) {
                    map.put(i, i);
                    writeCount.incrementAndGet();
                }
            } catch (Exception e) {
                System.out.println("  [写线程异常] " + e);
            } finally {
                endGate.countDown();
            }
        }).start();

        // 删除线程
        new Thread(() -> {
            try {
                startGate.await();
                for (int i = 0; i < SIZE / 2; i++) {
                    map.remove(i);
                    removeCount.incrementAndGet();
                }
            } catch (Exception e) {
                System.out.println("  [删除线程异常] " + e);
            } finally {
                endGate.countDown();
            }
        }).start();

        startGate.countDown();
        endGate.await();

        System.out.println("  读操作: " + readCount.get() + " 次");
        System.out.println("  写操作: " + writeCount.get() + " 次");
        System.out.println("  删操作: " + removeCount.get() + " 次");
        System.out.println("  最终大小: " + map.size());
        System.out.println("  期望大小: " + (SIZE + writeCount.get() - removeCount.get()));
        System.out.println("  并发读写删除全程无异常 ✅");
        System.out.println();
    }

    // ============================================================
    // main
    // ============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║       ConcurrentHashMap vs HashMap 多线程安全性对比                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        testHashMapUnsafe();
        testMapCorrectness();
        testAtomicVsNonAtomic();
        testIteratorBehavior();
        testConcurrentRemove();

        System.out.println("=".repeat(70));
        System.out.println("所有测试完成！");
        System.out.println("=".repeat(70));
    }
}
