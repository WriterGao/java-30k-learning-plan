import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HashMap 线程不安全场景演示
 *
 * 演示内容：
 * 1. 多线程并发 put 导致数据丢失
 * 2. 多线程并发 put + get 导致数据不一致
 * 3. 并发修改导致 ConcurrentModificationException
 * 4. 多线程环境下 size 不准确
 * 5. 对比三种线程安全方案的正确性和性能
 *
 * 运行方式：
 *   javac HashMapThreadUnsafeDemo.java
 *   java HashMapThreadUnsafeDemo
 */
public class HashMapThreadUnsafeDemo {

    // ==================== 实验1：并发 put 数据丢失 ====================

    /**
     * 实验1：多线程并发 put 导致数据丢失
     *
     * 原理：多个线程同时执行 putVal()，可能同时判断桶为空并写入，
     * 导致后写入的覆盖先写入的，造成数据丢失。
     *
     * 具体场景：
     * - 线程A和线程B同时计算到桶 index=5
     * - 线程A判断 table[5] == null，准备写入
     * - 线程B也判断 table[5] == null（还没写入），也准备写入
     * - 线程A写入 table[5] = nodeA
     * - 线程B写入 table[5] = nodeB → nodeA 丢失！
     */
    static void experiment1_concurrentPutDataLoss() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验1：并发 put 数据丢失");
        System.out.println("=".repeat(70));
        System.out.println();

        final int THREAD_COUNT = 20;
        final int PER_THREAD_COUNT = 5000;
        final int EXPECTED = THREAD_COUNT * PER_THREAD_COUNT;

        System.out.printf("线程数=%d, 每线程插入=%d, 期望总量=%d%n%n",
                THREAD_COUNT, PER_THREAD_COUNT, EXPECTED);

        // 多次测试，观察数据丢失的概率
        int testRounds = 5;
        System.out.printf("%-8s %-12s %-12s %-12s%n", "轮次", "期望size", "实际size", "丢失数据");
        System.out.println("-".repeat(50));

        for (int round = 1; round <= testRounds; round++) {
            Map<String, Integer> map = new HashMap<>();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

            for (int t = 0; t < THREAD_COUNT; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        startLatch.await(); // 所有线程同时开始
                        for (int i = 0; i < PER_THREAD_COUNT; i++) {
                            map.put("t" + threadId + "_" + i, i);
                        }
                    } catch (Exception e) {
                        // 可能出现异常
                    } finally {
                        endLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown(); // 发令枪
            endLatch.await();

            int actualSize = map.size();
            int lostCount = EXPECTED - actualSize;
            System.out.printf("%-8d %-12d %-12d %-12s%n",
                    round, EXPECTED, actualSize,
                    lostCount > 0 ? lostCount + " ✗" : "0 ✓");
        }

        System.out.println();
        System.out.println("【分析】HashMap 并发 put 会导致：");
        System.out.println("  1. 两个线程同时判断桶为空，后写入的覆盖先写入的 → 数据丢失");
        System.out.println("  2. 并发链表操作可能丢失节点");
        System.out.println("  3. size++ 非原子操作，可能计数不准");
        System.out.println();
    }

    // ==================== 实验2：并发 put + get 数据不一致 ====================

    /**
     * 实验2：一边 put 一边 get，可能读到不一致的数据
     */
    static void experiment2_concurrentPutGet() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验2：并发 put + get 数据不一致");
        System.out.println("=".repeat(70));
        System.out.println();

        final Map<Integer, Integer> map = new HashMap<>();
        final AtomicInteger nullReadCount = new AtomicInteger(0);
        final AtomicInteger totalReadCount = new AtomicInteger(0);
        final int DATA_SIZE = 100000;
        final int READER_COUNT = 5;

        // 先放入一些初始数据
        for (int i = 0; i < DATA_SIZE; i++) {
            map.put(i, i);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1 + READER_COUNT);

        // 写线程：不断更新数据
        new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < DATA_SIZE; i++) {
                    map.put(i, i * 10);          // 更新已有key
                    map.put(DATA_SIZE + i, i);    // 添加新key（可能触发扩容）
                }
            } catch (Exception e) {
                System.out.println("  写线程异常: " + e.getClass().getSimpleName());
            } finally {
                endLatch.countDown();
            }
        }).start();

        // 读线程：不断读取数据
        for (int r = 0; r < READER_COUNT; r++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < DATA_SIZE; i++) {
                        Integer val = map.get(i);
                        totalReadCount.incrementAndGet();
                        if (val == null) {
                            nullReadCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("  读线程异常: " + e.getClass().getSimpleName());
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        endLatch.await();

        System.out.printf("总读取次数: %d%n", totalReadCount.get());
        System.out.printf("读到 null 的次数: %d (这些 key 明明存在!)%n", nullReadCount.get());

        if (nullReadCount.get() > 0) {
            System.out.println("  → 说明扩容期间，已存在的 key 可能暂时 \"消失\"！");
        } else {
            System.out.println("  → 本次运行未观察到 null 读取（但不代表线程安全，多运行几次试试）");
        }

        System.out.println();
        System.out.println("【分析】扩容期间，数据从旧 table 迁移到新 table，");
        System.out.println("  读线程可能访问到尚未迁移完成的新 table，导致读到 null。");
        System.out.println();
    }

    // ==================== 实验3：ConcurrentModificationException ====================

    /**
     * 实验3：遍历时修改 HashMap 触发 ConcurrentModificationException
     * 这是 fail-fast 机制（modCount 检测）
     */
    static void experiment3_failFast() {
        System.out.println("=".repeat(70));
        System.out.println("实验3：遍历时修改触发 ConcurrentModificationException");
        System.out.println("=".repeat(70));
        System.out.println();

        // 场景1：单线程 - for-each 中修改
        System.out.println("--- 场景1：for-each 中 put ---");
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        try {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if ("B".equals(entry.getKey())) {
                    map.put("D", 4); // 遍历中修改
                }
            }
            System.out.println("  未抛出异常（不常见）");
        } catch (ConcurrentModificationException e) {
            System.out.println("  ✓ 捕获到 ConcurrentModificationException!");
            System.out.println("  原因：for-each 内部用 Iterator，检测到 modCount 变化");
        }

        // 场景2：单线程 - for-each 中 remove
        System.out.println();
        System.out.println("--- 场景2：for-each 中 remove ---");
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("A", 1);
        map2.put("B", 2);
        map2.put("C", 3);

        try {
            for (String key : map2.keySet()) {
                if ("B".equals(key)) {
                    map2.remove(key); // 遍历中删除
                }
            }
            System.out.println("  未抛出异常");
        } catch (ConcurrentModificationException e) {
            System.out.println("  ✓ 捕获到 ConcurrentModificationException!");
        }

        // 正确做法：使用 Iterator.remove()
        System.out.println();
        System.out.println("--- 正确做法：使用 Iterator.remove() ---");
        Map<String, Integer> map3 = new HashMap<>();
        map3.put("A", 1);
        map3.put("B", 2);
        map3.put("C", 3);

        Iterator<Map.Entry<String, Integer>> it = map3.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            if ("B".equals(entry.getKey())) {
                it.remove(); // 安全删除
            }
        }
        System.out.println("  使用 Iterator.remove() 安全删除后: " + map3);

        // 正确做法2：使用 removeIf (JDK 8)
        Map<String, Integer> map4 = new HashMap<>();
        map4.put("A", 1);
        map4.put("B", 2);
        map4.put("C", 3);
        map4.entrySet().removeIf(entry -> "B".equals(entry.getKey()));
        System.out.println("  使用 removeIf 安全删除后: " + map4);

        System.out.println();
    }

    // ==================== 实验4：size 不准确 ====================

    /**
     * 实验4：并发操作导致 size 不准确
     *
     * size++ 对应字节码：
     *   getfield size
     *   iconst_1
     *   iadd
     *   putfield size
     *
     * 这不是原子操作！两个线程可能同时读到 size=10，
     * 各自+1后写回 11，实际插入了2个元素但 size 只增加了1。
     */
    static void experiment4_inaccurateSize() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验4：并发操作导致 size 不准确");
        System.out.println("=".repeat(70));
        System.out.println();

        final int THREAD_COUNT = 10;
        final int OPS_PER_THREAD = 10000;

        // HashMap - size 会不准确
        Map<String, Integer> hashMap = new HashMap<>();
        CountDownLatch latch1 = new CountDownLatch(THREAD_COUNT);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            new Thread(() -> {
                for (int i = 0; i < OPS_PER_THREAD; i++) {
                    hashMap.put("t" + threadId + "_" + i, 1);
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();

        // 用 entrySet 实际遍历计数
        int actualCount = 0;
        try {
            for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
                actualCount++;
            }
        } catch (Exception e) {
            System.out.println("遍历异常: " + e.getClass().getSimpleName());
        }

        int expected = THREAD_COUNT * OPS_PER_THREAD;
        System.out.printf("期望元素数: %d%n", expected);
        System.out.printf("map.size(): %d%n", hashMap.size());
        System.out.printf("实际遍历数: %d%n", actualCount);

        if (hashMap.size() != expected || actualCount != expected) {
            System.out.println("→ size 或实际元素数与期望不符！");
        }

        System.out.println();
        System.out.println("【分析】size++ 不是原子操作（read → add → write），");
        System.out.println("  多线程同时执行时会产生'丢失更新'，导致 size < 实际元素数");
        System.out.println();
    }

    // ==================== 实验5：三种线程安全方案对比 ====================

    /**
     * 实验5：对比 HashMap / synchronizedMap / Hashtable / ConcurrentHashMap
     */
    static void experiment5_threadSafeSolutions() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验5：线程安全方案对比");
        System.out.println("=".repeat(70));
        System.out.println();

        final int THREAD_COUNT = 10;
        final int OPS_PER_THREAD = 50000;
        final int EXPECTED = THREAD_COUNT * OPS_PER_THREAD;

        // HashMap（线程不安全）
        Map<String, Integer> hashMap = new HashMap<>();
        long t1 = benchmarkConcurrentPut(hashMap, THREAD_COUNT, OPS_PER_THREAD);

        // Hashtable（全表锁）
        Map<String, Integer> hashtable = new Hashtable<>();
        long t2 = benchmarkConcurrentPut(hashtable, THREAD_COUNT, OPS_PER_THREAD);

        // Collections.synchronizedMap（全表锁）
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        long t3 = benchmarkConcurrentPut(syncMap, THREAD_COUNT, OPS_PER_THREAD);

        // ConcurrentHashMap（分段锁 / CAS）
        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        long t4 = benchmarkConcurrentPut(concurrentMap, THREAD_COUNT, OPS_PER_THREAD);

        System.out.printf("%-30s %-12s %-12s %-10s%n", "方案", "期望size", "实际size", "耗时(ms)");
        System.out.println("-".repeat(70));

        printResult("HashMap (不安全)", hashMap, EXPECTED, t1);
        printResult("Hashtable", hashtable, EXPECTED, t2);
        printResult("synchronizedMap", syncMap, EXPECTED, t3);
        printResult("ConcurrentHashMap", concurrentMap, EXPECTED, t4);

        System.out.println();
        System.out.println("【对比总结】");
        System.out.println("  ┌───────────────────────┬────────────┬────────────┬──────────────┐");
        System.out.println("  │ 方案                  │ 线程安全   │ 性能       │ 适用场景     │");
        System.out.println("  ├───────────────────────┼────────────┼────────────┼──────────────┤");
        System.out.println("  │ HashMap               │ ✗ 不安全   │ ★★★★★ 最快 │ 单线程       │");
        System.out.println("  │ Hashtable             │ ✓ 全表锁   │ ★★☆☆☆ 慢  │ 不推荐(遗留) │");
        System.out.println("  │ synchronizedMap       │ ✓ 全表锁   │ ★★☆☆☆ 慢  │ 简单兼容场景 │");
        System.out.println("  │ ConcurrentHashMap     │ ✓ 分段锁   │ ★★★★☆ 快  │ 高并发推荐   │");
        System.out.println("  └───────────────────────┴────────────┴────────────┴──────────────┘");
        System.out.println();
    }

    /**
     * 并发 put 基准测试
     */
    static long benchmarkConcurrentPut(Map<String, Integer> map, int threadCount, int opsPerThread)
            throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        map.put("t" + threadId + "_" + i, i);
                    }
                } catch (Exception ignored) {
                }
                endLatch.countDown();
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await();
        return System.currentTimeMillis() - start;
    }

    static void printResult(String name, Map<?, ?> map, int expected, long time) {
        int actual = map.size();
        String status = (actual == expected) ? "✓" : "✗ 丢失" + (expected - actual);
        System.out.printf("%-30s %-12d %-12s %-10d%n", name, expected, actual + " " + status, time);
    }

    // ==================== 实验6：模拟 JDK 7 死循环原理 ====================

    /**
     * 实验6：讲解 JDK 7 头插法导致的环形链表问题
     *
     * 注意：JDK 8 已修复此问题（改为尾插法），此处仅讲解原理，不实际复现。
     * 在 JDK 8+ 中运行，HashMap 不会出现死循环，但仍然会数据丢失。
     */
    static void experiment6_jdk7DeadLoopExplanation() {
        System.out.println("=".repeat(70));
        System.out.println("实验6：JDK 7 头插法死循环原理讲解");
        System.out.println("=".repeat(70));
        System.out.println();

        System.out.println("【JDK 7 HashMap 扩容的 transfer 方法（头插法）】");
        System.out.println();
        System.out.println("  void transfer(Entry[] newTable) {");
        System.out.println("      for (Entry<K,V> e : table) {");
        System.out.println("          while (e != null) {");
        System.out.println("              Entry<K,V> next = e.next;  // ① 记录 next");
        System.out.println("              int i = indexFor(e.hash, newTable.length);");
        System.out.println("              e.next = newTable[i];      // ② 头插：e 指向桶头");
        System.out.println("              newTable[i] = e;           // ③ e 成为新桶头");
        System.out.println("              e = next;                  // ④ 移到下一个");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println();

        System.out.println("【死循环形成过程】");
        System.out.println();
        System.out.println("假设：桶中有链表 A → B → null，两个线程同时扩容");
        System.out.println();
        System.out.println("  初始状态:  桶[i]: A → B → null");
        System.out.println();
        System.out.println("  === 线程1 执行到 ① 后挂起 ===");
        System.out.println("  线程1: e=A, next=B (暂停)");
        System.out.println();
        System.out.println("  === 线程2 完整执行扩容 ===");
        System.out.println("  头插法结果: 新桶[j]: B → A → null  (顺序反转!)");
        System.out.println();
        System.out.println("  === 线程1 恢复执行 ===");
        System.out.println("  此时: e=A, next=B");
        System.out.println();
        System.out.println("  第1轮: 头插 A 到新桶");
        System.out.println("    新桶[j]: A → null");
        System.out.println("    e = next = B");
        System.out.println();
        System.out.println("  第2轮: 头插 B 到新桶");
        System.out.println("    但此时 B.next 已被线程2改为指向 A!");
        System.out.println("    新桶[j]: B → A → null");
        System.out.println("    e = B.next = A  (不是null!)");
        System.out.println();
        System.out.println("  第3轮: 头插 A 到新桶");
        System.out.println("    新桶[j]: A → B → A → B → ... (环形链表!)");
        System.out.println("    此后 get() 遍历到此桶就会死循环!");
        System.out.println();

        System.out.println("  图解:");
        System.out.println("    正常: A → B → null");
        System.out.println("    死循环: A ⇄ B (A.next=B, B.next=A)");
        System.out.println();

        System.out.println("【JDK 8 的修复】");
        System.out.println("  - 改用尾插法 + 高低位链表拆分");
        System.out.println("  - 不会产生链表反转，所以不会形成环");
        System.out.println("  - 但 JDK 8 HashMap 仍然不是线程安全的！只是不会死循环了");
        System.out.println();
    }

    // ==================== main ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          HashMap 线程不安全场景演示                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        experiment1_concurrentPutDataLoss();
        experiment2_concurrentPutGet();
        experiment3_failFast();
        experiment4_inaccurateSize();
        experiment5_threadSafeSolutions();
        experiment6_jdk7DeadLoopExplanation();

        System.out.println("========== 所有线程安全实验完成 ==========");
    }
}
