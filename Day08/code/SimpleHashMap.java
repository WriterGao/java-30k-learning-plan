/**
 * 手写简化版 HashMap
 *
 * 实现特性：
 * 1. 数组 + 链表（不含红黑树）
 * 2. hash 扰动函数
 * 3. put / get / remove / size 基本操作
 * 4. 自动扩容（size > threshold 时）
 * 5. 支持 null key
 *
 * 与 JDK HashMap 的差异：
 * - 没有红黑树（链表过长时不会转换）
 * - 没有实现 Map 接口
 * - 没有 fail-fast 机制（modCount）
 * - 没有实现序列化
 * - 扩容策略简化
 */
public class SimpleHashMap<K, V> {

    // ==================== 内部节点类 ====================

    /**
     * 链表节点，对应 JDK HashMap 的 Node<K,V>
     */
    static class Node<K, V> {
        final int hash;    // key 的 hash 值（缓存，避免重复计算）
        final K key;       // 键
        V value;           // 值
        Node<K, V> next;   // 下一个节点（链表指针）

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    // ==================== 常量 ====================

    /** 默认初始容量：16（必须是 2 的幂） */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16

    /** 最大容量 */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /** 默认负载因子：0.75 */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // ==================== 字段 ====================

    /** 存储桶的数组 */
    Node<K, V>[] table;

    /** 当前元素数量 */
    int size;

    /** 扩容阈值 = capacity * loadFactor */
    int threshold;

    /** 负载因子 */
    final float loadFactor;

    // ==================== 构造方法 ====================

    /**
     * 默认构造（初始容量16，负载因子0.75）
     */
    public SimpleHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    /**
     * 指定初始容量
     */
    public SimpleHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 指定初始容量和负载因子
     */
    @SuppressWarnings("unchecked")
    public SimpleHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        this.loadFactor = loadFactor;
        // 将初始容量调整为 >= initialCapacity 的最小 2 的幂
        this.threshold = tableSizeFor(initialCapacity);
    }

    // ==================== 核心方法 ====================

    /**
     * hash 扰动函数
     * 与 JDK 8 HashMap.hash() 完全一致：
     * 高16位异或低16位，让高位也参与桶定位运算
     */
    static int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 返回 >= cap 的最小 2 的幂
     * 算法：通过位运算将最高位以下的所有位都设为1，然后+1
     */
    static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * 放入键值对
     *
     * 流程（对应 JDK 8 putVal）：
     * 1. table 为空则初始化（resize）
     * 2. 计算桶位置 (n-1) & hash
     * 3. 桶为空 → 直接放入新节点
     * 4. 桶不为空 → 遍历链表
     *    4a. 找到相同 key → 覆盖 value
     *    4b. 没有相同 key → 尾插法添加新节点
     * 5. size + 1，超过 threshold 则扩容
     *
     * @return 旧值（如果 key 已存在），否则 null
     */
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        // 1. table 为空则初始化
        if (table == null || table.length == 0) {
            resize();
        }

        int hash = hash(key);
        int n = table.length;
        int index = (n - 1) & hash; // 计算桶位置

        // 2. 桶为空 → 直接放入新节点
        Node<K, V> first = table[index];
        if (first == null) {
            table[index] = new Node<>(hash, key, value, null);
        } else {
            // 3. 桶不为空 → 遍历链表
            Node<K, V> e = first;
            Node<K, V> prev = null;

            while (e != null) {
                // 找到相同 key → 覆盖 value
                if (e.hash == hash && (e.key == key || (key != null && key.equals(e.key)))) {
                    V oldValue = e.value;
                    e.value = value;
                    return oldValue; // 返回旧值
                }
                prev = e;
                e = e.next;
            }

            // 没找到相同 key → 尾插法添加新节点
            prev.next = new Node<>(hash, key, value, null);
        }

        // 4. size + 1，检查是否需要扩容
        if (++size > threshold) {
            resize();
        }

        return null;
    }

    /**
     * 根据 key 获取 value
     *
     * 流程（对应 JDK 8 getNode）：
     * 1. table 不为空 && 桶不为空
     * 2. 检查第一个节点（大部分情况命中）
     * 3. 遍历链表查找
     *
     * @return 对应的 value，不存在返回 null
     */
    public V get(K key) {
        if (table == null || table.length == 0) {
            return null;
        }

        int hash = hash(key);
        int index = (table.length - 1) & hash;
        Node<K, V> e = table[index];

        // 遍历链表查找
        while (e != null) {
            if (e.hash == hash && (e.key == key || (key != null && key.equals(e.key)))) {
                return e.value;
            }
            e = e.next;
        }

        return null;
    }

    /**
     * 删除指定 key 的节点
     *
     * @return 被删除的 value，不存在返回 null
     */
    public V remove(K key) {
        if (table == null || table.length == 0) {
            return null;
        }

        int hash = hash(key);
        int index = (table.length - 1) & hash;
        Node<K, V> e = table[index];
        Node<K, V> prev = null;

        while (e != null) {
            if (e.hash == hash && (e.key == key || (key != null && key.equals(e.key)))) {
                // 找到目标节点
                if (prev == null) {
                    // 目标是桶的第一个节点
                    table[index] = e.next;
                } else {
                    // 目标在链表中间或末尾
                    prev.next = e.next;
                }
                size--;
                return e.value;
            }
            prev = e;
            e = e.next;
        }

        return null;
    }

    /**
     * 扩容
     *
     * 流程（简化版 JDK 8 resize）：
     * 1. 计算新容量（旧容量 * 2）
     * 2. 创建新数组
     * 3. 遍历旧数组，将每个桶的链表拆分到新数组
     *    - 使用 e.hash & oldCap 判断高低位
     *    - == 0 → 留在原位置（低位链表）
     *    - != 0 → 移到 原位置 + oldCap（高位链表）
     */
    @SuppressWarnings("unchecked")
    void resize() {
        Node<K, V>[] oldTable = table;
        int oldCap = (oldTable == null) ? 0 : oldTable.length;
        int oldThr = threshold;
        int newCap, newThr;

        if (oldCap > 0) {
            // 正常扩容：容量翻倍
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }
            newCap = oldCap << 1; // 容量 * 2
            newThr = (int) (newCap * loadFactor);
        } else if (oldThr > 0) {
            // 首次 put，用 threshold 作为初始容量
            newCap = oldThr;
            newThr = (int) (newCap * loadFactor);
        } else {
            // 默认初始化
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        }

        threshold = newThr;
        Node<K, V>[] newTable = (Node<K, V>[]) new Node[newCap];
        table = newTable;

        // 迁移旧数据
        if (oldTable != null) {
            for (int j = 0; j < oldCap; j++) {
                Node<K, V> e = oldTable[j];
                if (e == null) continue;

                oldTable[j] = null; // 帮助 GC

                if (e.next == null) {
                    // 桶中只有一个节点，直接放到新位置
                    newTable[e.hash & (newCap - 1)] = e;
                } else {
                    // JDK 8 优化：高低位链表拆分
                    Node<K, V> loHead = null, loTail = null; // 低位链表
                    Node<K, V> hiHead = null, hiTail = null; // 高位链表

                    Node<K, V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            // 低位：留在原位置
                            if (loTail == null) loHead = e;
                            else loTail.next = e;
                            loTail = e;
                        } else {
                            // 高位：移到 原位置 + oldCap
                            if (hiTail == null) hiHead = e;
                            else hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);

                    if (loTail != null) {
                        loTail.next = null;
                        newTable[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTable[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(K key) {
        return get(key) != null || (key == null && containsNullKey());
    }

    private boolean containsNullKey() {
        if (table == null) return false;
        Node<K, V> e = table[0]; // null key 总是在桶 0
        while (e != null) {
            if (e.key == null) return true;
            e = e.next;
        }
        return false;
    }

    /**
     * 打印 HashMap 内部结构（调试用）
     */
    public void printStructure() {
        if (table == null) {
            System.out.println("table = null (未初始化)");
            return;
        }
        System.out.println("容量=" + table.length + ", size=" + size + ", threshold=" + threshold);
        for (int i = 0; i < table.length; i++) {
            Node<K, V> e = table[i];
            if (e != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("  桶[").append(String.format("%2d", i)).append("]: ");
                while (e != null) {
                    sb.append("[").append(e.key).append("=").append(e.value).append("]");
                    if (e.next != null) sb.append(" → ");
                    e = e.next;
                }
                System.out.println(sb);
            }
        }
    }

    @Override
    public String toString() {
        if (size == 0) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (table != null) {
            for (Node<K, V> node : table) {
                Node<K, V> e = node;
                while (e != null) {
                    if (!first) sb.append(", ");
                    sb.append(e.key).append("=").append(e.value);
                    first = false;
                    e = e.next;
                }
            }
        }
        return sb.append("}").toString();
    }

    // ==================== 测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 手写 SimpleHashMap 测试 ==========");
        System.out.println();

        // --- 测试1：基本 put & get ---
        System.out.println("--- 测试1：基本 put & get ---");
        SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
        map.put("Java", 1);
        map.put("Python", 2);
        map.put("Go", 3);
        map.put("Rust", 4);
        map.put("C++", 5);

        System.out.println("map: " + map);
        System.out.println("get(\"Java\"): " + map.get("Java"));      // 1
        System.out.println("get(\"Go\"): " + map.get("Go"));          // 3
        System.out.println("get(\"Ruby\"): " + map.get("Ruby"));      // null
        System.out.println("size: " + map.size());                      // 5
        System.out.println("containsKey(\"Rust\"): " + map.containsKey("Rust")); // true
        System.out.println();

        // --- 测试2：key 覆盖 ---
        System.out.println("--- 测试2：key 覆盖 ---");
        Integer oldVal = map.put("Java", 100);
        System.out.println("put覆盖 Java, 旧值: " + oldVal);           // 1
        System.out.println("get(\"Java\"): " + map.get("Java"));      // 100
        System.out.println("size（不变）: " + map.size());              // 5
        System.out.println();

        // --- 测试3：remove ---
        System.out.println("--- 测试3：remove ---");
        Integer removed = map.remove("Go");
        System.out.println("remove(\"Go\"): " + removed);              // 3
        System.out.println("get(\"Go\"): " + map.get("Go"));          // null
        System.out.println("size: " + map.size());                      // 4
        System.out.println();

        // --- 测试4：null key ---
        System.out.println("--- 测试4：null key ---");
        map.put(null, 999);
        System.out.println("put(null, 999)");
        System.out.println("get(null): " + map.get(null));              // 999
        System.out.println("containsKey(null): " + map.containsKey(null)); // true
        System.out.println("size: " + map.size());                      // 5
        System.out.println();

        // --- 测试5：扩容测试 ---
        System.out.println("--- 测试5：扩容测试 ---");
        SimpleHashMap<Integer, String> bigMap = new SimpleHashMap<>();
        System.out.println("插入前：");
        bigMap.printStructure();

        for (int i = 0; i < 30; i++) {
            bigMap.put(i, "val" + i);
        }
        System.out.println("\n插入30个元素后：");
        bigMap.printStructure();
        System.out.println("size: " + bigMap.size()); // 30
        System.out.println();

        // 验证数据完整性
        boolean allCorrect = true;
        for (int i = 0; i < 30; i++) {
            if (!("val" + i).equals(bigMap.get(i))) {
                System.out.println("数据丢失! key=" + i);
                allCorrect = false;
            }
        }
        System.out.println("扩容后数据完整性: " + (allCorrect ? "✓ 全部正确" : "✗ 有数据丢失"));
        System.out.println();

        // --- 测试6：hash 冲突（链表） ---
        System.out.println("--- 测试6：hash 冲突处理 ---");
        SimpleHashMap<String, Integer> collisionMap = new SimpleHashMap<>(4); // 小容量，容易冲突
        collisionMap.put("Aa", 1);   // "Aa" 和 "BB" 在 Java 中 hashCode 相同！
        collisionMap.put("BB", 2);
        collisionMap.put("C", 3);
        collisionMap.put("D", 4);

        System.out.println("\"Aa\".hashCode() = " + "Aa".hashCode());
        System.out.println("\"BB\".hashCode() = " + "BB".hashCode());
        System.out.println("Aa 和 BB 的 hashCode " + ("Aa".hashCode() == "BB".hashCode() ? "相同!" : "不同"));
        System.out.println("get(\"Aa\"): " + collisionMap.get("Aa")); // 1
        System.out.println("get(\"BB\"): " + collisionMap.get("BB")); // 2
        collisionMap.printStructure();
        System.out.println();

        // --- 测试7：大量数据测试 ---
        System.out.println("--- 测试7：大量数据测试 ---");
        SimpleHashMap<Integer, Integer> largeMap = new SimpleHashMap<>();
        int testSize = 10000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < testSize; i++) {
            largeMap.put(i, i * 10);
        }

        long putTime = System.currentTimeMillis() - startTime;
        System.out.println("插入 " + testSize + " 个元素耗时: " + putTime + "ms");

        // 验证
        startTime = System.currentTimeMillis();
        int errorCount = 0;
        for (int i = 0; i < testSize; i++) {
            Integer val = largeMap.get(i);
            if (val == null || val != i * 10) {
                errorCount++;
            }
        }
        long getTime = System.currentTimeMillis() - startTime;
        System.out.println("查询 " + testSize + " 个元素耗时: " + getTime + "ms");
        System.out.println("错误数: " + errorCount);
        System.out.println("最终 size: " + largeMap.size());

        System.out.println();
        System.out.println("========== 所有测试完成 ==========");
    }
}
