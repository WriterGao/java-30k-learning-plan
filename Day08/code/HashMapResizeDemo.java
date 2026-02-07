import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * HashMap 扩容过程详细观察
 *
 * 演示内容：
 * 1. 通过反射观察 HashMap 内部 table 数组的变化
 * 2. 观察扩容触发的精确时机（size > threshold）
 * 3. 验证 JDK 8 高低位链表拆分优化（e.hash & oldCap）
 * 4. 观察扩容前后元素在桶中的位置变化
 * 5. 观察指定初始容量对扩容行为的影响
 *
 * 运行方式：
 *   javac HashMapResizeDemo.java
 *   java HashMapResizeDemo
 */
public class HashMapResizeDemo {

    // ==================== 反射工具方法 ====================

    /**
     * 通过反射获取 HashMap 内部 table 数组
     */
    @SuppressWarnings("unchecked")
    static Object[] getTable(HashMap<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        return (Object[]) tableField.get(map);
    }

    /**
     * 获取 HashMap 容量（table.length）
     */
    static int getCapacity(HashMap<?, ?> map) throws Exception {
        Object[] table = getTable(map);
        return table == null ? 0 : table.length;
    }

    /**
     * 获取 HashMap 的 threshold
     */
    static int getThreshold(HashMap<?, ?> map) throws Exception {
        Field f = HashMap.class.getDeclaredField("threshold");
        f.setAccessible(true);
        return f.getInt(map);
    }

    /**
     * 获取 HashMap 的 loadFactor
     */
    static float getLoadFactor(HashMap<?, ?> map) throws Exception {
        Field f = HashMap.class.getDeclaredField("loadFactor");
        f.setAccessible(true);
        return f.getFloat(map);
    }

    /**
     * 获取 Node 的 hash 字段
     */
    static int getNodeHash(Object node) throws Exception {
        Field f = node.getClass().getDeclaredField("hash");
        f.setAccessible(true);
        return f.getInt(node);
    }

    /**
     * 获取 Node 的 key 字段
     */
    static Object getNodeKey(Object node) throws Exception {
        Field f = node.getClass().getDeclaredField("key");
        f.setAccessible(true);
        return f.get(node);
    }

    /**
     * 获取 Node 的 next 字段
     */
    static Object getNodeNext(Object node) throws Exception {
        Field f = node.getClass().getDeclaredField("next");
        f.setAccessible(true);
        return f.get(node);
    }

    // ==================== 实验1：观察扩容触发时机 ====================

    /**
     * 实验1：精确观察每一次扩容的触发时机
     *
     * 默认初始容量 16，负载因子 0.75
     * threshold = 16 * 0.75 = 12
     * 当 size > 12 时（即插入第13个元素后），触发第一次扩容
     */
    static void experiment1_resizeTiming() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验1：观察 HashMap 扩容触发时机");
        System.out.println("=".repeat(70));
        System.out.println();

        HashMap<Integer, String> map = new HashMap<>();

        System.out.printf("%-10s %-8s %-10s %-12s %-8s%n",
                "操作", "size", "capacity", "threshold", "扩容?");
        System.out.println("-".repeat(55));

        // 初始状态
        System.out.printf("%-10s %-8d %-10d %-12d %-8s%n",
                "初始", map.size(), getCapacity(map), getThreshold(map), "-");

        // 逐个插入，观察每一次扩容
        for (int i = 1; i <= 100; i++) {
            int oldCap = getCapacity(map);
            map.put(i, "val" + i);
            int newCap = getCapacity(map);

            boolean resized = (oldCap != newCap);

            // 只打印关键节点和扩容点
            if (resized || i <= 2 || i == 12 || i == 13 || i == 24
                    || i == 25 || i == 48 || i == 49 || i == 96 || i == 97) {
                System.out.printf("%-10s %-8d %-10d %-12d %-8s%n",
                        "put(" + i + ")",
                        map.size(),
                        newCap,
                        getThreshold(map),
                        resized ? "★ 扩容! " + oldCap + "→" + newCap : "");
            }
        }

        System.out.println();
        System.out.println("【结论】");
        System.out.println("  - 默认容量 16，threshold=12，插入第13个元素后扩容到 32");
        System.out.println("  - 每次扩容：容量翻倍，threshold 也翻倍");
        System.out.println("  - 扩容序列：16 → 32 → 64 → 128");
        System.out.println();
    }

    // ==================== 实验2：高低位链表拆分验证 ====================

    /**
     * 实验2：验证 JDK 8 扩容时的高低位链表拆分
     *
     * 核心原理：
     *   扩容时不需要重新计算 hash，只需要看 hash & oldCap 的结果
     *   - == 0 → 留在原位（低位链表）
     *   - != 0 → 移到 原位置 + oldCap（高位链表）
     */
    static void experiment2_highLowBitSplit() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验2：JDK 8 高低位链表拆分验证");
        System.out.println("=".repeat(70));
        System.out.println();

        System.out.println("原理：扩容时用 hash & oldCap 判断元素新位置");
        System.out.println("  - (hash & oldCap) == 0 → 留在原位（低位）");
        System.out.println("  - (hash & oldCap) != 0 → 移到 原位 + oldCap（高位）");
        System.out.println();

        // 用容量16演示扩容到32
        int oldCap = 16;
        int newCap = 32;

        System.out.printf("%-8s %-12s %-20s %-10s %-10s %-12s%n",
                "Key", "hashCode", "hash(扰动后)", "桶(cap=16)", "桶(cap=32)", "hash&oldCap");
        System.out.println("-".repeat(80));

        // 找出在同一个桶中的key（桶0和桶5为例）
        for (int key = 0; key < 64; key++) {
            int h = Integer.hashCode(key);
            int hash = h ^ (h >>> 16); // JDK 8 扰动函数

            int bucket16 = hash & (oldCap - 1);  // 旧桶位置
            int bucket32 = hash & (newCap - 1);   // 新桶位置
            int highBit = hash & oldCap;          // 高位判断

            // 只展示桶0和桶5的元素
            if (bucket16 == 0 || bucket16 == 5) {
                System.out.printf("%-8d %-12s %-20s %-10d %-10d %-12s%n",
                        key,
                        toBinary(h, 8),
                        toBinary(hash, 8),
                        bucket16,
                        bucket32,
                        highBit == 0 ? "0 (留原位)" : oldCap + " (移动)");
            }
        }

        System.out.println();
        System.out.println("【验证结论】");
        System.out.println("  - 桶0的元素扩容后：hash&16==0 的留在桶0，hash&16!=0 的移到桶16");
        System.out.println("  - 桶5的元素扩容后：hash&16==0 的留在桶5，hash&16!=0 的移到桶21");
        System.out.println("  - 这个优化避免了重新计算 hash，只需要一次位与运算！");
        System.out.println();
    }

    // ==================== 实验3：观察桶内链表结构 ====================

    /**
     * 实验3：通过反射观察扩容前后桶内的链表结构
     */
    static void experiment3_bucketStructure() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验3：扩容前后桶内结构对比");
        System.out.println("=".repeat(70));
        System.out.println();

        // 使用较小的容量，方便观察冲突
        HashMap<Integer, String> map = new HashMap<>(8, 0.75f);

        // 插入一些会产生冲突的key
        for (int i = 0; i < 6; i++) {
            map.put(i, "v" + i);
        }

        System.out.println("--- 扩容前（容量=8，已插入6个元素）---");
        printBucketDetail(map);

        // 继续插入触发扩容 (threshold = 8 * 0.75 = 6)
        map.put(6, "v6");

        System.out.println("--- 扩容后（插入第7个元素后）---");
        printBucketDetail(map);
    }

    /**
     * 打印桶的详细结构
     */
    static void printBucketDetail(HashMap<?, ?> map) throws Exception {
        Object[] table = getTable(map);
        if (table == null) {
            System.out.println("  table = null (未初始化)");
            return;
        }

        System.out.printf("  容量=%d, size=%d, threshold=%d%n",
                table.length, map.size(), getThreshold(map));

        for (int i = 0; i < table.length; i++) {
            Object node = table[i];
            if (node != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  桶[%2d]: ", i));

                while (node != null) {
                    Object key = getNodeKey(node);
                    int hash = getNodeHash(node);
                    sb.append(String.format("[key=%s, hash=%s]", key, toBinary(hash, 8)));

                    node = getNodeNext(node);
                    if (node != null) sb.append(" → ");
                }
                System.out.println(sb);
            }
        }
        System.out.println();
    }

    // ==================== 实验4：指定初始容量的影响 ====================

    /**
     * 实验4：对比不同初始容量对扩容次数的影响
     */
    static void experiment4_initialCapacity() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验4：初始容量对扩容次数的影响");
        System.out.println("=".repeat(70));
        System.out.println();

        int dataSize = 1000;

        // 场景1：默认容量（16）
        int resizeCount1 = countResizes(new HashMap<>(), dataSize);

        // 场景2：指定合适的容量
        int optimalCap = (int) (dataSize / 0.75f) + 1;
        int resizeCount2 = countResizes(new HashMap<>(optimalCap), dataSize);

        // 场景3：指定过小的容量
        int resizeCount3 = countResizes(new HashMap<>(4), dataSize);

        // 场景4：指定恰好的容量
        int resizeCount4 = countResizes(new HashMap<>(1024), dataSize);

        System.out.printf("插入 %d 个元素的扩容次数对比：%n", dataSize);
        System.out.println("-".repeat(50));
        System.out.printf("  默认容量(16):         扩容 %d 次%n", resizeCount1);
        System.out.printf("  最优容量(%d):       扩容 %d 次%n", optimalCap, resizeCount2);
        System.out.printf("  过小容量(4):          扩容 %d 次%n", resizeCount3);
        System.out.printf("  恰好容量(1024):       扩容 %d 次%n", resizeCount4);

        System.out.println();
        System.out.println("【建议】");
        System.out.println("  已知数据量 n 时，推荐初始容量 = (int)(n / 0.75) + 1");
        System.out.println("  例如：存1000个元素，initialCapacity = 1334 → 实际容量 2048");
        System.out.println("  这样可以完全避免扩容，提升性能！");
        System.out.println();
    }

    /**
     * 统计插入 n 个元素时 HashMap 的扩容次数
     */
    static int countResizes(HashMap<Integer, Integer> map, int n) throws Exception {
        int resizeCount = 0;
        int lastCap = getCapacity(map);

        for (int i = 0; i < n; i++) {
            map.put(i, i);
            int newCap = getCapacity(map);
            if (newCap != lastCap) {
                resizeCount++;
                lastCap = newCap;
            }
        }
        return resizeCount;
    }

    // ==================== 实验5：tableSizeFor 验证 ====================

    /**
     * 实验5：验证 tableSizeFor 方法
     * 传入任意容量，实际分配的是 >= 该值的最小 2 的幂
     */
    static void experiment5_tableSizeFor() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验5：tableSizeFor 实际容量验证");
        System.out.println("=".repeat(70));
        System.out.println();

        System.out.println("传入 new HashMap<>(n) 时，实际分配的容量：");
        System.out.printf("%-20s %-20s%n", "指定容量(n)", "实际容量(2的幂)");
        System.out.println("-".repeat(40));

        int[] testCaps = {1, 2, 3, 4, 5, 7, 8, 9, 10, 15, 16, 17, 30, 32, 33, 100, 1000};

        for (int cap : testCaps) {
            HashMap<Integer, Integer> map = new HashMap<>(cap);
            // put一个元素触发table初始化
            map.put(0, 0);
            int actualCap = getCapacity(map);
            System.out.printf("%-20d %-20d%n", cap, actualCap);
        }

        System.out.println();
        System.out.println("【结论】HashMap 容量总是 2 的幂（1, 2, 4, 8, 16, 32, 64, 128, ...）");
        System.out.println("  tableSizeFor() 通过位运算快速找到 >= n 的最小 2 的幂");
        System.out.println();
    }

    // ==================== 实验6：扩容性能对比 ====================

    /**
     * 实验6：扩容对性能的影响
     */
    static void experiment6_resizePerformance() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("实验6：扩容对性能的影响");
        System.out.println("=".repeat(70));
        System.out.println();

        int dataSize = 1_000_000;
        int warmup = 3;
        int rounds = 5;

        // 预热
        for (int i = 0; i < warmup; i++) {
            HashMap<Integer, Integer> m1 = new HashMap<>();
            HashMap<Integer, Integer> m2 = new HashMap<>(dataSize * 2);
            for (int j = 0; j < dataSize; j++) {
                m1.put(j, j);
                m2.put(j, j);
            }
        }

        long totalDefault = 0;
        long totalOptimal = 0;

        for (int r = 0; r < rounds; r++) {
            // 默认容量
            long start = System.nanoTime();
            HashMap<Integer, Integer> map1 = new HashMap<>();
            for (int i = 0; i < dataSize; i++) {
                map1.put(i, i);
            }
            totalDefault += System.nanoTime() - start;

            // 预设容量
            start = System.nanoTime();
            HashMap<Integer, Integer> map2 = new HashMap<>((int) (dataSize / 0.75f) + 1);
            for (int i = 0; i < dataSize; i++) {
                map2.put(i, i);
            }
            totalOptimal += System.nanoTime() - start;
        }

        long avgDefault = totalDefault / rounds / 1_000_000;
        long avgOptimal = totalOptimal / rounds / 1_000_000;

        System.out.printf("插入 %,d 个元素（%d轮平均）：%n", dataSize, rounds);
        System.out.printf("  默认容量(16):    %d ms%n", avgDefault);
        System.out.printf("  预设最优容量:    %d ms%n", avgOptimal);
        System.out.printf("  性能提升:        约 %.1f%%%n",
                (1.0 - (double) avgOptimal / avgDefault) * 100);

        System.out.println();
        System.out.println("【结论】预设合理的初始容量可以避免多次扩容，显著提升大量插入的性能");
        System.out.println();
    }

    // ==================== 工具方法 ====================

    /**
     * 将整数转为指定位数的二进制字符串
     */
    static String toBinary(int value, int bits) {
        String binary = Integer.toBinaryString(value);
        if (binary.length() > bits) {
            return "..." + binary.substring(binary.length() - bits);
        }
        return String.format("%" + bits + "s", binary).replace(' ', '0');
    }

    // ==================== main ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          HashMap 扩容过程详细观察实验                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        experiment1_resizeTiming();
        experiment2_highLowBitSplit();
        experiment3_bucketStructure();
        experiment4_initialCapacity();
        experiment5_tableSizeFor();
        experiment6_resizePerformance();

        System.out.println("========== 所有扩容实验完成 ==========");
    }
}
