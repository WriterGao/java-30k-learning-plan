import java.util.*;

/**
 * 基于 LinkedHashMap 的 LRU 缓存演示
 * 
 * 演示内容：
 * 1. LinkedHashMap 插入顺序 vs 访问顺序
 * 2. LRU 缓存实现
 * 3. LRU 缓存淘汰行为详细跟踪
 * 4. 实际应用场景模拟
 */
public class LRUCacheDemo {

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("  LinkedHashMap 与 LRU 缓存演示");
        System.out.println("====================================\n");

        demo1_InsertionOrder();
        demo2_AccessOrder();
        demo3_LRUCache();
        demo4_LRUCacheDetailedTrace();
        demo5_PracticalLRU();
    }

    /**
     * 演示1：LinkedHashMap 插入顺序（默认）
     */
    static void demo1_InsertionOrder() {
        System.out.println("【演示1】LinkedHashMap 插入顺序（accessOrder=false，默认）");
        System.out.println("----------------------------------------------------------");

        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("Cherry", 3);
        map.put("Apple", 1);
        map.put("Banana", 2);
        map.put("Date", 4);

        System.out.println("插入顺序: Cherry → Apple → Banana → Date");
        System.out.print("遍历顺序: ");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.print(entry.getKey() + "=" + entry.getValue() + " → ");
        }
        System.out.println("END");

        // 访问 Apple 不影响顺序
        map.get("Apple");
        System.out.print("访问 Apple 后遍历: ");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.print(entry.getKey() + " → ");
        }
        System.out.println("END（顺序不变）");

        // 对比 HashMap（无序）
        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("Cherry", 3);
        hashMap.put("Apple", 1);
        hashMap.put("Banana", 2);
        hashMap.put("Date", 4);
        System.out.print("HashMap 遍历:     ");
        for (String key : hashMap.keySet()) {
            System.out.print(key + " → ");
        }
        System.out.println("END（无序）");

        System.out.println();
    }

    /**
     * 演示2：LinkedHashMap 访问顺序
     */
    static void demo2_AccessOrder() {
        System.out.println("【演示2】LinkedHashMap 访问顺序（accessOrder=true）");
        System.out.println("----------------------------------------------------------");

        // accessOrder = true
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>(16, 0.75f, true);
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        map.put("D", 4);

        printLinkedHashMap("初始状态", map);

        map.get("A");
        printLinkedHashMap("get(A) 后", map);

        map.get("C");
        printLinkedHashMap("get(C) 后", map);

        map.put("B", 20);  // 更新已有 key
        printLinkedHashMap("put(B,20) 后", map);

        map.put("E", 5);   // 插入新 key
        printLinkedHashMap("put(E,5) 后", map);

        System.out.println("→ 规律：最近被访问（get/put）的元素移到尾部");
        System.out.println("→ 链表头部就是「最近最少使用」的元素\n");
    }

    /**
     * 演示3：LRU 缓存基本使用
     */
    static void demo3_LRUCache() {
        System.out.println("【演示3】LRU 缓存（容量=3）");
        System.out.println("----------------------------------------------------------");

        LRUCache<String, Integer> cache = new LRUCache<>(3);

        System.out.println("操作序列：put(A,1) → put(B,2) → put(C,3) → get(A) → put(D,4)\n");

        cache.put("A", 1);
        System.out.println("put(A, 1) → 缓存: " + cache);

        cache.put("B", 2);
        System.out.println("put(B, 2) → 缓存: " + cache);

        cache.put("C", 3);
        System.out.println("put(C, 3) → 缓存: " + cache + " [已满]");

        Integer val = cache.get("A");
        System.out.println("get(A)=" + val + "  → 缓存: " + cache + " [A 移到尾部]");

        cache.put("D", 4);
        System.out.println("put(D, 4) → 缓存: " + cache + " [B 被淘汰！]");

        System.out.println("\nget(B) = " + cache.get("B") + " ← B 已被淘汰");
        System.out.println("get(C) = " + cache.get("C"));
        System.out.println("get(A) = " + cache.get("A"));

        System.out.println();
    }

    /**
     * 演示4：LRU 缓存淘汰行为详细跟踪
     */
    static void demo4_LRUCacheDetailedTrace() {
        System.out.println("【演示4】LRU 缓存淘汰详细跟踪（容量=4）");
        System.out.println("----------------------------------------------------------");

        LRUCache<Integer, String> cache = new LRUCache<>(4);

        String[][] operations = {
            {"put", "1", "One"},
            {"put", "2", "Two"},
            {"put", "3", "Three"},
            {"put", "4", "Four"},
            {"get", "2", null},
            {"get", "1", null},
            {"put", "5", "Five"},    // 淘汰 3（最久未使用）
            {"get", "3", null},      // null，已被淘汰
            {"put", "6", "Six"},     // 淘汰 4
            {"get", "4", null},      // null，已被淘汰
            {"put", "2", "TwoNew"}, // 更新已有 key
            {"put", "7", "Seven"},   // 淘汰 1 或 5（取决于访问顺序）
        };

        System.out.println(String.format("%-25s %-30s %-10s", "操作", "缓存状态 (头→尾 = 老→新)", "返回值"));
        System.out.println("-".repeat(70));

        for (String[] op : operations) {
            String result;
            if ("put".equals(op[0])) {
                cache.put(Integer.parseInt(op[1]), op[2]);
                result = "-";
            } else {
                String val = cache.get(Integer.parseInt(op[1]));
                result = String.valueOf(val);
            }

            String operation = op[0] + "(" + op[1] + (op[2] != null ? ", " + op[2] : "") + ")";
            System.out.println(String.format("%-25s %-30s %-10s", operation, cache, result));
        }

        System.out.println();
    }

    /**
     * 演示5：实际应用场景 - 网页缓存
     */
    static void demo5_PracticalLRU() {
        System.out.println("【演示5】实际应用场景 - 简单网页缓存");
        System.out.println("----------------------------------------------------------");

        // 模拟一个简单的网页缓存
        LRUCache<String, String> pageCache = new LRUCache<>(3);

        // 模拟请求序列
        String[] requests = {
            "/index.html", "/about.html", "/contact.html",
            "/index.html",  // 再次访问首页
            "/products.html",  // 新页面，触发淘汰
            "/about.html",  // 已被淘汰
        };

        System.out.println("缓存容量: 3 个页面\n");

        for (String url : requests) {
            String content = pageCache.get(url);
            if (content != null) {
                System.out.println("✅ 缓存命中: " + url + " → " + content);
            } else {
                // 模拟从"服务器"加载
                content = "Content of " + url;
                pageCache.put(url, content);
                System.out.println("❌ 缓存未命中: " + url + " → 从服务器加载并缓存");
            }
            System.out.println("   当前缓存: " + pageCache.keySet());
        }

        System.out.println();
    }

    // ================================================================
    // LRU 缓存实现
    // ================================================================

    /**
     * 基于 LinkedHashMap 的 LRU 缓存实现
     * 
     * 核心原理：
     * 1. accessOrder=true：每次 get/put 都会将节点移到链表尾部
     * 2. removeEldestEntry：当元素个数超过容量时，删除链表头节点（最久未使用）
     */
    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LRUCache(int capacity) {
            // initialCapacity=capacity, loadFactor=0.75, accessOrder=true
            super(capacity, 0.75f, true);
            this.capacity = capacity;
        }

        /**
         * 当 size > capacity 时，返回 true，表示需要删除最老的元素
         * 这个方法在每次 put 之后被调用（afterNodeInsertion）
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    static void printLinkedHashMap(String label, LinkedHashMap<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(": [");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(" → ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("]");
        System.out.println(sb.toString());
    }
}
