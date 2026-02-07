import java.util.*;
import java.lang.reflect.Field;

/**
 * ArrayList 核心操作演示
 * 
 * 演示内容：
 * 1. 基本 CRUD 操作
 * 2. 扩容机制观察（通过反射）
 * 3. fail-fast 机制
 * 4. 安全删除元素的方式
 * 5. subList 的坑
 */
public class ArrayListDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("====================================");
        System.out.println("  ArrayList 核心操作演示");
        System.out.println("====================================\n");

        demo1_BasicOperations();
        demo2_GrowMechanism();
        demo3_FailFast();
        demo4_SafeRemove();
        demo5_SubListPitfall();
        demo6_RandomAccess();
    }

    /**
     * 演示1：基本 CRUD 操作
     */
    static void demo1_BasicOperations() {
        System.out.println("【演示1】ArrayList 基本操作");
        System.out.println("----------------------------------");

        // 创建
        ArrayList<String> list = new ArrayList<>();
        System.out.println("创建空 ArrayList，size = " + list.size());

        // 添加
        list.add("Apple");
        list.add("Banana");
        list.add("Cherry");
        list.add("Date");
        list.add("Elderberry");
        System.out.println("添加5个元素: " + list);

        // 指定位置插入
        list.add(2, "Blueberry");
        System.out.println("在索引2插入 Blueberry: " + list);

        // 获取
        System.out.println("get(0) = " + list.get(0));
        System.out.println("get(3) = " + list.get(3));

        // 修改
        String old = list.set(1, "BlackBerry");
        System.out.println("set(1, BlackBerry)，旧值 = " + old);
        System.out.println("修改后: " + list);

        // 查找
        System.out.println("contains(Cherry) = " + list.contains("Cherry"));
        System.out.println("indexOf(Cherry) = " + list.indexOf("Cherry"));

        // 删除
        list.remove("Cherry");
        System.out.println("remove(Cherry): " + list);
        list.remove(0);
        System.out.println("remove(0): " + list);

        System.out.println("最终 size = " + list.size());
        System.out.println();
    }

    /**
     * 演示2：扩容机制观察（通过反射获取内部数组长度）
     * 
     * 注意：JDK 16+ 默认禁止反射访问内部字段。
     * 如果反射失败，改为使用理论值说明扩容过程。
     */
    static void demo2_GrowMechanism() throws Exception {
        System.out.println("【演示2】ArrayList 扩容机制");
        System.out.println("----------------------------------");

        boolean reflectionWorks = canReflect();

        if (reflectionWorks) {
            ArrayList<Integer> list = new ArrayList<>();
            System.out.println("无参构造后，内部数组长度 = " + getCapacity(list));

            list.add(1);
            System.out.println("添加第1个元素后，内部数组长度 = " + getCapacity(list));

            for (int i = 2; i <= 10; i++) list.add(i);
            System.out.println("添加到第10个元素，内部数组长度 = " + getCapacity(list));

            list.add(11);
            System.out.println("添加第11个元素（触发扩容），内部数组长度 = " + getCapacity(list));
            System.out.println("  → 扩容计算：10 + 10>>1 = 10 + 5 = 15");

            for (int i = 12; i <= 15; i++) list.add(i);
            System.out.println("添加到第15个元素，内部数组长度 = " + getCapacity(list));

            list.add(16);
            System.out.println("添加第16个元素（触发扩容），内部数组长度 = " + getCapacity(list));
            System.out.println("  → 扩容计算：15 + 15>>1 = 15 + 7 = 22");

            ArrayList<Integer> list2 = new ArrayList<>(100);
            System.out.println("\nnew ArrayList(100)，内部数组长度 = " + getCapacity(list2));

            list.trimToSize();
            System.out.println("trimToSize() 后，list 内部数组长度 = " + getCapacity(list)
                    + " (size=" + list.size() + ")");
        } else {
            System.out.println("（JDK 16+ 限制反射访问，使用理论值演示扩容过程）\n");

            System.out.println("ArrayList 扩容过程（无参构造，默认容量 10）：");
            System.out.println("┌──────────┬───────────┬──────────┬────────────────────────┐");
            System.out.println("│ 添加第N个 │ 扩容前容量 │ 扩容后容量 │ 计算方式                │");
            System.out.println("├──────────┼───────────┼──────────┼────────────────────────┤");
            System.out.println("│    1     │    0      │    10    │ 首次 add，默认容量 10    │");
            System.out.println("│   11     │   10      │    15    │ 10 + 10>>1 = 15        │");
            System.out.println("│   16     │   15      │    22    │ 15 + 15>>1 = 22        │");
            System.out.println("│   23     │   22      │    33    │ 22 + 22>>1 = 33        │");
            System.out.println("│   34     │   33      │    49    │ 33 + 33>>1 = 49        │");
            System.out.println("│   50     │   49      │    73    │ 49 + 49>>1 = 73        │");
            System.out.println("└──────────┴───────────┴──────────┴────────────────────────┘");

            // 验证扩容逻辑
            System.out.println("\n手动验证 1.5 倍扩容：");
            int capacity = 10;
            for (int i = 0; i < 6; i++) {
                int newCapacity = capacity + (capacity >> 1);
                System.out.println("  " + capacity + " → " + newCapacity
                        + " (=" + capacity + " + " + (capacity >> 1) + ")");
                capacity = newCapacity;
            }

            // new ArrayList(100)
            System.out.println("\nnew ArrayList(100) → 内部数组长度 = 100");
            System.out.println("new ArrayList(0)   → 内部数组长度 = 0（第一次 add 时扩容到 10）");
            System.out.println("new ArrayList()    → 内部数组长度 = 0（懒初始化，第一次 add 时扩容到 10）");
        }

        System.out.println();
    }

    /** 检测反射是否可用 */
    static boolean canReflect() {
        try {
            Field field = ArrayList.class.getDeclaredField("elementData");
            field.setAccessible(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 演示3：fail-fast 机制
     */
    static void demo3_FailFast() {
        System.out.println("【演示3】fail-fast 机制");
        System.out.println("----------------------------------");

        // 场景1：for-each 中调用 list.remove() — 触发 ConcurrentModificationException
        List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        System.out.println("原始列表: " + list);

        System.out.print("在 for-each 中调用 list.remove(): ");
        try {
            for (String s : list) {
                if ("C".equals(s)) {
                    list.remove(s);
                }
            }
            System.out.println("未抛异常（不可靠！）");
        } catch (ConcurrentModificationException e) {
            System.out.println("✅ 抛出 ConcurrentModificationException");
        }

        // 场景2：Iterator 遍历中调用 list.add()
        list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        System.out.print("在 Iterator 遍历中调用 list.add(): ");
        try {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                String s = it.next();
                if ("B".equals(s)) {
                    list.add("X");
                }
            }
            System.out.println("未抛异常");
        } catch (ConcurrentModificationException e) {
            System.out.println("✅ 抛出 ConcurrentModificationException");
        }

        System.out.println();
    }

    /**
     * 演示4：安全删除元素的多种方式
     */
    static void demo4_SafeRemove() {
        System.out.println("【演示4】安全删除元素的方式");
        System.out.println("----------------------------------");

        // 方式1：使用 Iterator.remove()
        List<String> list1 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        Iterator<String> it = list1.iterator();
        while (it.hasNext()) {
            if ("C".equals(it.next())) {
                it.remove();
            }
        }
        System.out.println("方式1 Iterator.remove(): " + list1);

        // 方式2：使用 removeIf()（JDK 8+）
        List<String> list2 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        list2.removeIf(s -> "C".equals(s));
        System.out.println("方式2 removeIf(): " + list2);

        // 方式3：倒序遍历删除
        List<String> list3 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        for (int i = list3.size() - 1; i >= 0; i--) {
            if ("C".equals(list3.get(i))) {
                list3.remove(i);
            }
        }
        System.out.println("方式3 倒序遍历删除: " + list3);

        // 方式4：正序遍历但手动调整索引
        List<String> list4 = new ArrayList<>(Arrays.asList("A", "B", "C", "C", "D", "E"));
        for (int i = 0; i < list4.size(); i++) {
            if ("C".equals(list4.get(i))) {
                list4.remove(i);
                i--;  // 索引回退
            }
        }
        System.out.println("方式4 正序遍历+调整索引: " + list4);

        // 方式5：使用 Stream（创建新列表）
        List<String> list5 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        List<String> filtered = new ArrayList<>();
        for (String s : list5) {
            if (!"C".equals(s)) {
                filtered.add(s);
            }
        }
        System.out.println("方式5 创建新列表过滤: " + filtered);

        System.out.println();
    }

    /**
     * 演示5：subList 的坑
     */
    static void demo5_SubListPitfall() {
        System.out.println("【演示5】subList 的坑");
        System.out.println("----------------------------------");

        // 坑1：修改 subList 影响原始 List
        List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        List<String> sub = list.subList(1, 4);
        System.out.println("原始列表: " + list);
        System.out.println("subList(1,4): " + sub);

        sub.set(0, "X");
        System.out.println("sub.set(0, X) 后:");
        System.out.println("  原始列表: " + list + " ← 也被修改了！");
        System.out.println("  subList: " + sub);

        // 坑2：修改原始 List 后操作 subList
        System.out.print("修改原始 List 后操作 subList: ");
        list.add("F");
        try {
            sub.get(0);
            System.out.println("未抛异常");
        } catch (ConcurrentModificationException e) {
            System.out.println("✅ 抛出 ConcurrentModificationException");
        }

        // 正确方式：创建独立副本
        List<String> list2 = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        List<String> independent = new ArrayList<>(list2.subList(1, 4));
        list2.add("F");
        System.out.println("独立副本方式: independent = " + independent + " （不受原始 List 修改影响）");

        System.out.println();
    }

    /**
     * 演示6：RandomAccess 标记接口
     */
    static void demo6_RandomAccess() {
        System.out.println("【演示6】RandomAccess 标记接口");
        System.out.println("----------------------------------");

        ArrayList<Integer> arrayList = new ArrayList<>();
        LinkedList<Integer> linkedList = new LinkedList<>();

        System.out.println("ArrayList implements RandomAccess: "
                + (arrayList instanceof java.util.RandomAccess));
        System.out.println("LinkedList implements RandomAccess: "
                + (linkedList instanceof java.util.RandomAccess));

        // Collections.binarySearch 会根据 RandomAccess 选择不同的算法
        for (int i = 0; i < 100; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        // ArrayList: 使用索引随机访问的二分查找
        int idx1 = Collections.binarySearch(arrayList, 42);
        // LinkedList: 使用迭代器的二分查找
        int idx2 = Collections.binarySearch(linkedList, 42);
        System.out.println("binarySearch(42): ArrayList=" + idx1 + ", LinkedList=" + idx2);

        System.out.println();
    }

    /**
     * 通过反射获取 ArrayList 内部数组的长度（容量）
     */
    static int getCapacity(ArrayList<?> list) throws Exception {
        Field field = ArrayList.class.getDeclaredField("elementData");
        field.setAccessible(true);
        Object[] elementData = (Object[]) field.get(list);
        return elementData.length;
    }
}
