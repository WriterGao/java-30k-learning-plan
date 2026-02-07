import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * HashMap 源码分析 - 综合演示
 *
 * 演示内容：
 * 1. HashMap 基本操作（put/get/remove/遍历）
 * 2. hash() 扰动函数效果验证
 * 3. 扩容过程观察
 * 4. hash 冲突演示
 * 5. 多线程安全性测试
 * 6. null key/value 处理
 */
public class HashMapDemo {

    // ==================== 1. 基本操作 ====================

    /**
     * 演示 HashMap 的基本 CRUD 操作
     */
    static void basicOperationDemo() {
        System.out.println("========== 1. HashMap 基本操作 ==========");

        HashMap<String, Integer> map = new HashMap<>();

        // --- put ---
        map.put("Java", 1);
        map.put("Python", 2);
        map.put("Go", 3);
        map.put("Rust", 4);
        map.put("C++", 5);
        System.out.println("put 5个元素后: " + map);

        // --- get ---
        System.out.println("get(\"Java\"): " + map.get("Java"));
        System.out.println("get(\"Ruby\"): " + map.get("Ruby")); // null

        // --- containsKey / containsValue ---
        System.out.println("containsKey(\"Go\"): " + map.containsKey("Go"));
        System.out.println("containsValue(3): " + map.containsValue(3));

        // --- put 覆盖 ---
        Integer oldVal = map.put("Java", 100);
        System.out.println("put覆盖 Java, 旧值: " + oldVal + ", 新值: " + map.get("Java"));

        // --- remove ---
        Integer removed = map.remove("Rust");
        System.out.println("remove(\"Rust\"): " + removed + ", map: " + map);

        // --- size / isEmpty ---
        System.out.println("size: " + map.size() + ", isEmpty: " + map.isEmpty());

        // --- getOrDefault (JDK 8) ---
        System.out.println("getOrDefault(\"Ruby\", -1): " + map.getOrDefault("Ruby", -1));

        // --- putIfAbsent (JDK 8) ---
        map.putIfAbsent("Java", 999); // Java 已存在，不会覆盖
        map.putIfAbsent("Ruby", 6);   // Ruby 不存在，会插入
        System.out.println("putIfAbsent后: " + map);

        System.out.println();
    }

    // ==================== 2. 遍历方式 ====================

    /**
     * 演示 HashMap 的多种遍历方式
     */
    static void iterationDemo() {
        System.out.println("========== 2. HashMap 遍历方式 ==========");

        HashMap<String, Integer> map = new HashMap<>();
        map.put("Apple", 1);
        map.put("Banana", 2);
        map.put("Cherry", 3);
        map.put("Date", 4);

        // 方式1: entrySet（推荐，效率最高）
        System.out.println("--- entrySet 遍历 ---");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }

        // 方式2: keySet
        System.out.println("--- keySet 遍历 ---");
        for (String key : map.keySet()) {
            System.out.println("  " + key + " = " + map.get(key));
        }

        // 方式3: values
        System.out.println("--- values 遍历 ---");
        for (Integer value : map.values()) {
            System.out.println("  value = " + value);
        }

        // 方式4: forEach (JDK 8)
        System.out.println("--- forEach 遍历 ---");
        map.forEach((key, value) ->
            System.out.println("  " + key + " -> " + value));

        // 方式5: Iterator
        System.out.println("--- Iterator 遍历 ---");
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            System.out.println("  " + entry.getKey() + " => " + entry.getValue());
        }

        System.out.println();
    }

    // ==================== 3. null key/value ====================

    /**
     * 演示 HashMap 对 null key 和 null value 的处理
     */
    static void nullKeyValueDemo() {
        System.out.println("========== 3. null key/value 处理 ==========");

        HashMap<String, String> map = new HashMap<>();

        // null key
        map.put(null, "nullKeyValue");
        System.out.println("put(null, \"nullKeyValue\"): " + map.get(null));

        // null value
        map.put("key1", null);
        System.out.println("put(\"key1\", null): " + map.get("key1"));

        // null key 覆盖
        map.put(null, "newNullKeyValue");
        System.out.println("覆盖 null key: " + map.get(null));

        // 注意：containsKey 和 get 的区别
        System.out.println("containsKey(\"key1\"): " + map.containsKey("key1")); // true
        System.out.println("get(\"key1\"): " + map.get("key1")); // null
        System.out.println("get(\"key2\"): " + map.get("key2")); // null（key不存在）
        System.out.println("【注意】get返回null不能区分'value为null'和'key不存在'，用containsKey判断");

        System.out.println();
    }

    // ==================== 4. hash() 扰动函数验证 ====================

    /**
     * 演示 hash() 扰动函数的效果
     * JDK 8: (h = key.hashCode()) ^ (h >>> 16)
     */
    static void hashFunctionDemo() {
        System.out.println("========== 4. hash() 扰动函数验证 ==========");

        System.out.println("--- JDK 8 hash() 扰动函数 ---");
        System.out.println("公式: (h = key.hashCode()) ^ (h >>> 16)");
        System.out.println();

        // 几个示例 key
        String[] keys = {"Java", "Python", "Go", "Rust", "C++", "JavaScript", "Kotlin", "Swift"};

        System.out.printf("%-12s %-12s %-12s %-12s %-6s %-6s%n",
                "Key", "hashCode", "h>>>16", "hash()", "桶(16)", "桶(32)");
        System.out.println("-".repeat(70));

        for (String key : keys) {
            int h = key.hashCode();
            int hash = h ^ (h >>> 16); // JDK 8 的扰动函数
            int bucket16 = hash & (16 - 1);  // 容量16时的桶位置
            int bucket32 = hash & (32 - 1);  // 容量32时的桶位置

            System.out.printf("%-12s %-12s %-12s %-12s %-6d %-6d%n",
                    key,
                    toBinaryString(h),
                    toBinaryString(h >>> 16),
                    toBinaryString(hash),
                    bucket16,
                    bucket32);
        }

        System.out.println();

        // 对比：不使用扰动函数
        System.out.println("--- 对比：不使用扰动函数直接取模 ---");
        System.out.println("如果直接用 hashCode & (n-1)，高位信息全部丢失：");

        // 构造高位不同、低位相同的 hashCode
        int h1 = 0b00000000_00000001_00000000_00010000; // 高位 1
        int h2 = 0b00000000_00000010_00000000_00010000; // 高位 2
        int h3 = 0b00000000_00000100_00000000_00010000; // 高位 4

        System.out.printf("h1 = %s, 直接取模桶=%d, 扰动后桶=%d%n",
                toBinaryString(h1), h1 & 15, (h1 ^ (h1 >>> 16)) & 15);
        System.out.printf("h2 = %s, 直接取模桶=%d, 扰动后桶=%d%n",
                toBinaryString(h2), h2 & 15, (h2 ^ (h2 >>> 16)) & 15);
        System.out.printf("h3 = %s, 直接取模桶=%d, 扰动后桶=%d%n",
                toBinaryString(h3), h3 & 15, (h3 ^ (h3 >>> 16)) & 15);

        System.out.println("可见：不做扰动时三个 hash 落入同一个桶，扰动后分散到不同桶！");
        System.out.println();
    }

    /**
     * 将 int 转为 32 位二进制字符串（高位补零）
     */
    static String toBinaryString(int value) {
        String binary = Integer.toBinaryString(value);
        // 只取低16位方便展示
        if (binary.length() > 16) {
            return "..." + binary.substring(binary.length() - 16);
        }
        return String.format("%16s", binary).replace(' ', '0');
    }

    // ==================== 5. 扩容过程观察 ====================

    /**
     * 通过反射观察 HashMap 的内部状态变化
     */
    static void resizeDemo() throws Exception {
        System.out.println("========== 5. 扩容过程观察 ==========");

        HashMap<Integer, String> map = new HashMap<>();

        System.out.printf("%-8s %-8s %-10s %-12s%n", "操作", "size", "capacity", "threshold");
        System.out.println("-".repeat(42));
        printMapInfo("初始", map);

        // 依次插入，观察扩容点
        for (int i = 1; i <= 50; i++) {
            int oldCapacity = getCapacity(map);
            map.put(i, "val" + i);
            int newCapacity = getCapacity(map);

            if (oldCapacity != newCapacity || i <= 3 || i == 12 || i == 13
                    || i == 24 || i == 25 || i == 48 || i == 49) {
                printMapInfo("put(" + i + ")", map);
                if (oldCapacity != newCapacity && oldCapacity > 0) {
                    System.out.println("  ↑↑↑ 触发扩容！容量 " + oldCapacity + " → " + newCapacity);
                }
            }
        }

        System.out.println();

        // 演示扩容后元素位置变化
        System.out.println("--- 扩容后元素位置变化（e.hash & oldCap 判断高低位） ---");
        System.out.printf("%-6s %-12s %-12s %-12s %-8s%n",
                "Key", "hash", "桶(cap=16)", "桶(cap=32)", "高/低位");
        System.out.println("-".repeat(55));

        for (int key = 0; key < 32; key++) {
            int h = Integer.hashCode(key);
            int hash = h ^ (h >>> 16);
            int bucket16 = hash & (16 - 1);
            int bucket32 = hash & (32 - 1);
            // 只展示在桶0和桶5的元素
            if (bucket16 == 0 || bucket16 == 5) {
                String highLow = (hash & 16) == 0 ? "低位(不动)" : "高位(+16)";
                System.out.printf("%-6d %-12s %-12d %-12d %-8s%n",
                        key, toBinaryString(hash), bucket16, bucket32, highLow);
            }
        }

        System.out.println();
    }

    /**
     * 通过反射获取 HashMap 的容量
     */
    static int getCapacity(HashMap<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
        return table == null ? 0 : table.length;
    }

    /**
     * 通过反射获取 HashMap 的 threshold
     */
    static int getThreshold(HashMap<?, ?> map) throws Exception {
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        return thresholdField.getInt(map);
    }

    /**
     * 打印 HashMap 内部状态
     */
    static void printMapInfo(String label, HashMap<?, ?> map) throws Exception {
        System.out.printf("%-8s %-8d %-10d %-12d%n",
                label, map.size(), getCapacity(map), getThreshold(map));
    }

    // ==================== 6. hash 冲突演示 ====================

    /**
     * 构造 hash 冲突，演示链表的形成
     */
    static void hashCollisionDemo() {
        System.out.println("========== 6. hash 冲突演示 ==========");

        // 方式1：自定义类，强制相同 hashCode
        System.out.println("--- 使用自定义 hashCode 制造冲突 ---");

        HashMap<CollidingKey, String> map = new HashMap<>(16);
        for (int i = 0; i < 12; i++) {
            map.put(new CollidingKey(i), "value" + i);
        }

        System.out.println("插入12个相同 hash 的 key，size = " + map.size());
        System.out.println("所有 key 都在同一个桶中，形成链表（>= 8 个可能转为红黑树）");
        System.out.println();

        // 方式2：Integer key 的冲突
        System.out.println("--- Integer key 的桶分布（容量16） ---");
        int[] bucketCount = new int[16];
        for (int i = 0; i < 100; i++) {
            int h = Integer.hashCode(i);
            int hash = h ^ (h >>> 16);
            int bucket = hash & 15;
            bucketCount[bucket]++;
        }

        System.out.println("key 0~99 在各桶中的分布：");
        for (int i = 0; i < 16; i++) {
            String bar = "█".repeat(bucketCount[i]);
            System.out.printf("  桶[%2d]: %2d 个 %s%n", i, bucketCount[i], bar);
        }

        System.out.println();
    }

    /**
     * 自定义 key 类，所有实例返回相同 hashCode（制造冲突）
     */
    static class CollidingKey {
        final int id;

        CollidingKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 1; // 所有 key 的 hashCode 都相同
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CollidingKey)) return false;
            return this.id == ((CollidingKey) obj).id;
        }

        @Override
        public String toString() {
            return "Key(" + id + ")";
        }
    }

    // ==================== 7. 多线程安全性测试 ====================

    /**
     * 演示 HashMap 在多线程下的不安全行为
     * 对比 HashMap / synchronizedMap / ConcurrentHashMap
     */
    static void threadSafetyDemo() throws Exception {
        System.out.println("========== 7. 多线程安全性测试 ==========");

        final int THREAD_COUNT = 10;
        final int PUT_COUNT = 10000;
        final int EXPECTED_SIZE = THREAD_COUNT * PUT_COUNT;

        // --- HashMap（线程不安全）---
        Map<String, Integer> hashMap = new HashMap<>();
        concurrentPut(hashMap, THREAD_COUNT, PUT_COUNT);
        System.out.printf("HashMap:            期望 size=%d, 实际 size=%d %s%n",
                EXPECTED_SIZE, hashMap.size(),
                hashMap.size() == EXPECTED_SIZE ? "✓" : "✗ (数据丢失!)");

        // --- Collections.synchronizedMap ---
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        concurrentPut(syncMap, THREAD_COUNT, PUT_COUNT);
        System.out.printf("synchronizedMap:    期望 size=%d, 实际 size=%d %s%n",
                EXPECTED_SIZE, syncMap.size(),
                syncMap.size() == EXPECTED_SIZE ? "✓" : "✗");

        // --- ConcurrentHashMap ---
        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        concurrentPut(concurrentMap, THREAD_COUNT, PUT_COUNT);
        System.out.printf("ConcurrentHashMap:  期望 size=%d, 实际 size=%d %s%n",
                EXPECTED_SIZE, concurrentMap.size(),
                concurrentMap.size() == EXPECTED_SIZE ? "✓" : "✗");

        System.out.println();
        System.out.println("【结论】HashMap 多线程 put 会导致数据丢失，应使用 ConcurrentHashMap");

        System.out.println();
    }

    /**
     * 多线程并发 put
     */
    static void concurrentPut(Map<String, Integer> map, int threadCount, int putCount)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threadCount);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < putCount; j++) {
                    // 每个线程使用不同的 key 前缀，确保 key 不重复
                    map.put("t" + threadId + "_k" + j, j);
                }
                latch.countDown();
            });
        }

        for (Thread t : threads) t.start();
        latch.await();
    }

    // ==================== 8. HashMap 与 equals/hashCode 的关系 ====================

    /**
     * 演示不正确的 equals/hashCode 导致的问题
     */
    static void equalsHashCodeDemo() {
        System.out.println("========== 8. equals/hashCode 契约 ==========");

        // 正确示例：重写了 equals 和 hashCode
        HashMap<Person, String> map = new HashMap<>();
        map.put(new Person("张三", 25), "工程师");
        map.put(new Person("李四", 30), "设计师");

        // 用相同属性的新对象查找
        Person query = new Person("张三", 25);
        System.out.println("查找 new Person(\"张三\", 25): " + map.get(query));

        // 演示不重写 hashCode 的后果
        HashMap<BadPerson, String> badMap = new HashMap<>();
        badMap.put(new BadPerson("张三", 25), "工程师");

        BadPerson badQuery = new BadPerson("张三", 25);
        System.out.println("不重写hashCode, 查找结果: " + badMap.get(badQuery) + " (null! 因为hashCode不同)");

        System.out.println();
        System.out.println("【重要】作为 HashMap 的 key 的对象，必须同时重写 equals() 和 hashCode()");
        System.out.println("约定：equals 相等 → hashCode 必须相等；hashCode 相等 → equals 不一定相等");

        System.out.println();
    }

    /**
     * 正确重写了 equals 和 hashCode 的类
     */
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    /**
     * 只重写了 equals 没有重写 hashCode 的类（反面教材）
     */
    static class BadPerson {
        String name;
        int age;

        BadPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BadPerson)) return false;
            BadPerson person = (BadPerson) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        // 故意不重写 hashCode！使用默认的 Object.hashCode()（基于内存地址）
    }

    // ==================== main ====================

    public static void main(String[] args) throws Exception {
        basicOperationDemo();
        iterationDemo();
        nullKeyValueDemo();
        hashFunctionDemo();
        resizeDemo();
        hashCollisionDemo();
        threadSafetyDemo();
        equalsHashCodeDemo();
    }
}
