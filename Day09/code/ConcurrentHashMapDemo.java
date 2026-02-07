import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrentHashMap 核心操作演示
 *
 * 演示内容：
 * 1. 扩容过程观察
 * 2. 弱一致性迭代器
 * 3. JDK 8 原子操作 API（compute / merge / computeIfAbsent 等）
 * 4. size() 与 mappingCount() 行为
 * 5. 复合操作陷阱演示
 * 6. null 限制演示
 *
 * 运行方式：
 *   cd Day09/code
 *   javac ConcurrentHashMapDemo.java
 *   java ConcurrentHashMapDemo
 */
public class ConcurrentHashMapDemo {

    // ============================================================
    // 测试1：扩容过程观察
    // ============================================================
    static void testResizeObservation() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试1】ConcurrentHashMap 扩容过程观察");
        System.out.println("=".repeat(70));

        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>(16);
        System.out.println("  初始容量: 16 (默认)");
        System.out.println("  默认负载因子: 0.75");
        System.out.println("  扩容阈值: 16 * 0.75 = 12");
        System.out.println();

        // 逐步插入，观察扩容
        for (int i = 0; i < 50; i++) {
            map.put(i, i);
            if (i == 11 || i == 12 || i == 13 || i == 24 || i == 25 || i == 49) {
                System.out.printf("  插入第 %d 个元素后: size=%d, mappingCount=%d%n",
                        i + 1, map.size(), map.mappingCount());
            }
        }

        System.out.println();
        System.out.println("  说明: ConcurrentHashMap 在 size > threshold 时触发扩容");
        System.out.println("  扩容时容量翻倍, 阈值也翻倍");
        System.out.println("  多线程场景下, 其他线程会协助扩容 (helpTransfer)");
        System.out.println();

        // 并发插入，触发多线程扩容
        System.out.println("  --- 并发插入触发扩容 ---");
        ConcurrentHashMap<Integer, Integer> concurrentMap = new ConcurrentHashMap<>(16);
        final int THREAD_COUNT = 8;
        final int OPS = 10000;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(THREAD_COUNT);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < OPS; j++) {
                        concurrentMap.put(threadId * OPS + j, j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            }, "Writer-" + t).start();
        }

        startGate.countDown();
        // 在写入过程中观察
        for (int i = 0; i < 3; i++) {
            Thread.sleep(5);
            System.out.printf("  并发写入中: size=%d%n", concurrentMap.size());
        }
        endGate.await();
        System.out.printf("  写入完成: 期望=%d, 实际=%d, 正确=%s%n",
                THREAD_COUNT * OPS, concurrentMap.size(),
                concurrentMap.size() == THREAD_COUNT * OPS ? "✅" : "❌");
        System.out.println();
    }

    // ============================================================
    // 测试2：弱一致性迭代器
    // ============================================================
    static void testWeakConsistency() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试2】ConcurrentHashMap 弱一致性迭代器演示");
        System.out.println("=".repeat(70));

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put("key-" + i, i);
        }

        System.out.println("  初始大小: " + map.size());

        // 在迭代过程中修改 Map
        Thread modifier = new Thread(() -> {
            try {
                Thread.sleep(10);
                map.put("key-new-1", 100);
                map.put("key-new-2", 200);
                map.remove("key-0");
                System.out.println("  [修改线程] 添加了 key-new-1, key-new-2, 移除了 key-0");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        modifier.start();

        // 迭代器不会抛 ConcurrentModificationException
        int count = 0;
        try {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                count++;
                Thread.sleep(5);
            }
            System.out.println("  [迭代线程] 遍历了 " + count + " 个元素");
            System.out.println("  ConcurrentHashMap 迭代期间没有抛出异常 ✅ (弱一致性)");
        } catch (ConcurrentModificationException e) {
            System.out.println("  意外：抛出了 ConcurrentModificationException ❌");
        }

        modifier.join();

        // 对比 HashMap
        System.out.println();
        System.out.println("  对比: HashMap 在迭代时修改会抛出 ConcurrentModificationException:");
        HashMap<String, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            hashMap.put("key-" + i, i);
        }
        try {
            for (String key : hashMap.keySet()) {
                if ("key-5".equals(key)) {
                    hashMap.put("key-new", 999);
                }
            }
            System.out.println("  HashMap 迭代时修改没有抛异常（偶尔发生，不保证）");
        } catch (ConcurrentModificationException e) {
            System.out.println("  HashMap 抛出 ConcurrentModificationException ✅ (fail-fast)");
        }
        System.out.println();
    }

    // ============================================================
    // 测试3：JDK 8 原子操作 API
    // ============================================================
    static void testAtomicOperations() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试3】ConcurrentHashMap JDK 8 原子操作 API 演示");
        System.out.println("=".repeat(70));

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // 1. putIfAbsent
        map.putIfAbsent("count", 0);
        map.putIfAbsent("count", 999);
        System.out.println("  putIfAbsent(\"count\", 0) → putIfAbsent(\"count\", 999)");
        System.out.println("  结果: count = " + map.get("count") + " (第二次插入被忽略)");

        // 2. compute 原子更新
        System.out.println();
        System.out.println("  compute 原子更新演示:");
        final int THREAD_COUNT = 10;
        final int INCREMENTS = 10_000;
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    map.compute("count", (k, v) -> (v == null ? 0 : v) + 1);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("  " + THREAD_COUNT + " 线程各 compute +1 " + INCREMENTS + " 次");
        System.out.println("  期望: " + (THREAD_COUNT * INCREMENTS) + ", 实际: " + map.get("count"));
        System.out.println("  结果: " + (map.get("count") == THREAD_COUNT * INCREMENTS ? "✅ 原子操作正确" : "❌ 不正确"));

        // 3. merge 词频统计
        System.out.println();
        System.out.println("  merge 原子合并演示 (词频统计):");
        ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<>();
        String[] words = {"hello", "world", "hello", "java", "hello", "world", "concurrent"};
        for (String word : words) {
            wordCount.merge(word, 1, Integer::sum);
        }
        System.out.println("  词频统计: " + wordCount);

        // 4. computeIfAbsent 懒初始化
        System.out.println();
        System.out.println("  computeIfAbsent 演示 (懒初始化列表):");
        ConcurrentHashMap<String, List<String>> multiMap = new ConcurrentHashMap<>();
        multiMap.computeIfAbsent("fruits", k -> new CopyOnWriteArrayList<>()).add("apple");
        multiMap.computeIfAbsent("fruits", k -> new CopyOnWriteArrayList<>()).add("banana");
        multiMap.computeIfAbsent("vegs", k -> new CopyOnWriteArrayList<>()).add("carrot");
        System.out.println("  multiMap = " + multiMap);

        // 5. getOrDefault
        System.out.println();
        System.out.println("  getOrDefault 演示:");
        System.out.println("  map.getOrDefault(\"missing\", -1) = " + map.getOrDefault("missing", -1));

        // 6. forEach / search / reduce
        System.out.println();
        System.out.println("  并行批量操作演示:");
        ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        scores.put("Charlie", 92);
        scores.put("Diana", 88);

        System.out.print("  forEach: ");
        scores.forEach(1, (k, v) ->
                System.out.print(k + "=" + v + " "));
        System.out.println();

        String found = scores.search(1, (k, v) -> v > 90 ? k : null);
        System.out.println("  search(分数>90): " + found);

        int total = scores.reduce(1, (k, v) -> v, Integer::sum);
        System.out.println("  reduce(总分): " + total);
        System.out.println();
    }

    // ============================================================
    // 测试4：size() 与 mappingCount() 行为
    // ============================================================
    static void testSizeBehavior() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试4】ConcurrentHashMap size() 与 mappingCount() 演示");
        System.out.println("=".repeat(70));

        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
        final int THREAD_COUNT = 10;
        final int OPERATIONS = 100_000;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < OPERATIONS; j++) {
                        map.put(threadId * OPERATIONS + j, j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        startGate.countDown();

        System.out.println("  并发写入过程中多次读取 size():");
        for (int i = 0; i < 5; i++) {
            Thread.sleep(10);
            System.out.printf("    size()=%d, mappingCount()=%d%n",
                    map.size(), map.mappingCount());
        }

        endGate.await();
        System.out.println("  写入完成后:");
        System.out.println("    最终 size() = " + map.size());
        System.out.println("    最终 mappingCount() = " + map.mappingCount());
        System.out.println("    期望大小 = " + (THREAD_COUNT * OPERATIONS));
        System.out.println();
        System.out.println("  说明: size() 返回 int (最大 Integer.MAX_VALUE)");
        System.out.println("        mappingCount() 返回 long (JDK 8 新增)");
        System.out.println("        两者都基于 baseCount + counterCells, 非精确瞬时值");
        System.out.println();
    }

    // ============================================================
    // 测试5：复合操作陷阱演示
    // ============================================================
    static void testCompoundOperationTrap() throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("【测试5】复合操作陷阱演示");
        System.out.println("=".repeat(70));

        final int THREAD_COUNT = 20;
        final int INCREMENTS = 10_000;

        // ❌ 非原子复合操作：get + put
        System.out.println("  --- 非原子复合操作 (get + put) ---");
        ConcurrentHashMap<String, Integer> map1 = new ConcurrentHashMap<>();
        map1.put("count", 0);
        CountDownLatch latch1 = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    // ❌ 这不是原子操作！get 和 put 之间其他线程可能修改
                    Integer val = map1.get("count");
                    map1.put("count", val + 1);
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();
        int expected = THREAD_COUNT * INCREMENTS;
        System.out.println("  期望: " + expected);
        System.out.println("  实际: " + map1.get("count"));
        System.out.println("  丢失: " + (expected - map1.get("count")));
        System.out.println("  结论: get + put 不是原子操作, 存在竞态条件 ❌");

        // ✅ 原子操作：compute
        System.out.println();
        System.out.println("  --- 原子操作 (compute) ---");
        ConcurrentHashMap<String, Integer> map2 = new ConcurrentHashMap<>();
        map2.put("count", 0);
        CountDownLatch latch2 = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    // ✅ compute 是原子操作
                    map2.compute("count", (k, v) -> v + 1);
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        System.out.println("  期望: " + expected);
        System.out.println("  实际: " + map2.get("count"));
        System.out.println("  结论: compute 是原子操作, 结果正确 ✅");

        // ✅ 原子操作：merge
        System.out.println();
        System.out.println("  --- 原子操作 (merge) ---");
        ConcurrentHashMap<String, Integer> map3 = new ConcurrentHashMap<>();
        map3.put("count", 0);
        CountDownLatch latch3 = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    // ✅ merge 也是原子操作
                    map3.merge("count", 1, Integer::sum);
                }
                latch3.countDown();
            }).start();
        }
        latch3.await();
        System.out.println("  期望: " + expected);
        System.out.println("  实际: " + map3.get("count"));
        System.out.println("  结论: merge 是原子操作, 结果正确 ✅");
        System.out.println();
    }

    // ============================================================
    // 测试6：null 限制演示
    // ============================================================
    static void testNullRestriction() {
        System.out.println("=".repeat(70));
        System.out.println("【测试6】ConcurrentHashMap null 限制演示");
        System.out.println("=".repeat(70));

        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

        // null key
        try {
            map.put(null, "value");
            System.out.println("  put(null, \"value\"): 成功（意外）");
        } catch (NullPointerException e) {
            System.out.println("  put(null, \"value\"): NullPointerException ✅");
        }

        // null value
        try {
            map.put("key", null);
            System.out.println("  put(\"key\", null): 成功（意外）");
        } catch (NullPointerException e) {
            System.out.println("  put(\"key\", null): NullPointerException ✅");
        }

        // putIfAbsent null value
        try {
            map.putIfAbsent("key", null);
            System.out.println("  putIfAbsent(\"key\", null): 成功（意外）");
        } catch (NullPointerException e) {
            System.out.println("  putIfAbsent(\"key\", null): NullPointerException ✅");
        }

        System.out.println();
        System.out.println("  原因: 并发环境下 get() 返回 null 有二义性");
        System.out.println("        无法区分 '不存在' 和 'value 就是 null'");
        System.out.println("        HashMap 可以用 containsKey 区分,");
        System.out.println("        但 ConcurrentHashMap 中 containsKey 和 get 之间可能有修改");
        System.out.println();
    }

    // ============================================================
    // main
    // ============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║           ConcurrentHashMap 核心操作演示                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        testResizeObservation();
        testWeakConsistency();
        testAtomicOperations();
        testSizeBehavior();
        testCompoundOperationTrap();
        testNullRestriction();

        System.out.println("=".repeat(70));
        System.out.println("所有测试完成！");
        System.out.println("=".repeat(70));
    }
}
