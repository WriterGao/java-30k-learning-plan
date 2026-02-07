import java.util.*;

/**
 * LinkedList 操作演示与 ArrayList vs LinkedList 性能对比
 * 
 * 演示内容：
 * 1. LinkedList 基本操作
 * 2. LinkedList 作为 Deque（双端队列）和 Stack（栈）使用
 * 3. ArrayList vs LinkedList 性能对比测试
 *    - 尾部追加
 *    - 头部插入
 *    - 随机访问
 *    - 遍历
 *    - 迭代器中间删除
 */
public class LinkedListDemo {

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("  LinkedList 操作与性能对比演示");
        System.out.println("====================================\n");

        demo1_BasicOperations();
        demo2_AsDeque();
        demo3_AsStack();
        demo4_PerformanceComparison();
    }

    /**
     * 演示1：LinkedList 基本操作
     */
    static void demo1_BasicOperations() {
        System.out.println("【演示1】LinkedList 基本操作");
        System.out.println("----------------------------------");

        LinkedList<String> list = new LinkedList<>();

        // 添加元素
        list.add("B");           // 尾部添加
        list.addFirst("A");      // 头部添加
        list.addLast("D");       // 尾部添加
        list.add(2, "C");        // 指定位置添加
        System.out.println("添加后: " + list);

        // 获取元素
        System.out.println("getFirst() = " + list.getFirst());
        System.out.println("getLast() = " + list.getLast());
        System.out.println("get(2) = " + list.get(2));

        // 删除元素
        list.removeFirst();
        System.out.println("removeFirst(): " + list);
        list.removeLast();
        System.out.println("removeLast(): " + list);

        System.out.println("size = " + list.size());
        System.out.println();
    }

    /**
     * 演示2：LinkedList 作为 Deque（双端队列）
     */
    static void demo2_AsDeque() {
        System.out.println("【演示2】LinkedList 作为 Deque（双端队列）");
        System.out.println("----------------------------------");

        Deque<String> deque = new LinkedList<>();

        // 队列操作（FIFO）
        System.out.println("--- 队列模式（FIFO）---");
        deque.offer("第一个");     // 尾部入队
        deque.offer("第二个");
        deque.offer("第三个");
        System.out.println("入队后: " + deque);
        System.out.println("poll(): " + deque.poll());     // 头部出队
        System.out.println("poll(): " + deque.poll());
        System.out.println("剩余: " + deque);

        // 双端队列操作
        System.out.println("\n--- 双端队列模式 ---");
        deque.clear();
        deque.offerFirst("B");
        deque.offerFirst("A");
        deque.offerLast("C");
        deque.offerLast("D");
        System.out.println("双端添加后: " + deque);
        System.out.println("peekFirst() = " + deque.peekFirst());
        System.out.println("peekLast() = " + deque.peekLast());
        System.out.println("pollFirst() = " + deque.pollFirst());
        System.out.println("pollLast() = " + deque.pollLast());
        System.out.println("剩余: " + deque);

        System.out.println();
    }

    /**
     * 演示3：LinkedList 作为 Stack（栈）
     */
    static void demo3_AsStack() {
        System.out.println("【演示3】LinkedList 作为 Stack（栈）");
        System.out.println("----------------------------------");

        Deque<String> stack = new LinkedList<>();

        // 入栈
        stack.push("底部");
        stack.push("中间");
        stack.push("顶部");
        System.out.println("入栈后: " + stack);

        // 查看栈顶
        System.out.println("peek() = " + stack.peek());

        // 出栈
        System.out.println("pop() = " + stack.pop());
        System.out.println("pop() = " + stack.pop());
        System.out.println("剩余: " + stack);

        System.out.println("\n⚠️  注意：官方推荐使用 ArrayDeque 代替 LinkedList 作为栈和队列");
        System.out.println();
    }

    /**
     * 演示4：ArrayList vs LinkedList 性能对比
     */
    static void demo4_PerformanceComparison() {
        System.out.println("【演示4】ArrayList vs LinkedList 性能对比");
        System.out.println("==========================================\n");

        testTailAppend();
        testHeadInsert();
        testRandomAccess();
        testIteration();
        testIteratorRemove();

        System.out.println("==========================================");
        System.out.println("总结：");
        System.out.println("  - 尾部追加：ArrayList ≈ LinkedList（ArrayList 略快，缓存友好）");
        System.out.println("  - 头部插入：LinkedList >> ArrayList（ArrayList 需要移动所有元素）");
        System.out.println("  - 随机访问：ArrayList >> LinkedList（O(1) vs O(n)）");
        System.out.println("  - 顺序遍历：ArrayList ≥ LinkedList（缓存友好性优势）");
        System.out.println("  - 迭代器删除：LinkedList > ArrayList（O(1) vs O(n)）");
        System.out.println("  - 绝大多数场景推荐使用 ArrayList！");
        System.out.println("==========================================\n");
    }

    /**
     * 测试1：尾部追加
     */
    static void testTailAppend() {
        System.out.println("--- 测试1：尾部追加 100 万个元素 ---");
        int count = 1_000_000;

        // ArrayList
        ArrayList<Integer> arrayList = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            arrayList.add(i);
        }
        long alTime = System.nanoTime() - start;

        // LinkedList
        LinkedList<Integer> linkedList = new LinkedList<>();
        start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            linkedList.add(i);
        }
        long llTime = System.nanoTime() - start;

        System.out.printf("  ArrayList:  %,d ns (%.2f ms)%n", alTime, alTime / 1_000_000.0);
        System.out.printf("  LinkedList: %,d ns (%.2f ms)%n", llTime, llTime / 1_000_000.0);
        System.out.printf("  胜者: %s (快 %.1f 倍)%n%n",
                alTime < llTime ? "ArrayList" : "LinkedList",
                alTime < llTime ? (double) llTime / alTime : (double) alTime / llTime);
    }

    /**
     * 测试2：头部插入
     */
    static void testHeadInsert() {
        System.out.println("--- 测试2：头部插入 10 万个元素 ---");
        int count = 100_000;

        // ArrayList
        ArrayList<Integer> arrayList = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            arrayList.add(0, i);
        }
        long alTime = System.nanoTime() - start;

        // LinkedList
        LinkedList<Integer> linkedList = new LinkedList<>();
        start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            linkedList.addFirst(i);
        }
        long llTime = System.nanoTime() - start;

        System.out.printf("  ArrayList:  %,d ns (%.2f ms)%n", alTime, alTime / 1_000_000.0);
        System.out.printf("  LinkedList: %,d ns (%.2f ms)%n", llTime, llTime / 1_000_000.0);
        System.out.printf("  胜者: %s (快 %.1f 倍)%n%n",
                alTime < llTime ? "ArrayList" : "LinkedList",
                alTime < llTime ? (double) llTime / alTime : (double) alTime / llTime);
    }

    /**
     * 测试3：随机访问
     */
    static void testRandomAccess() {
        System.out.println("--- 测试3：随机访问 10 万次（列表大小 10 万） ---");
        int size = 100_000;
        int accessCount = 100_000;
        Random random = new Random(42);

        // 准备数据
        ArrayList<Integer> arrayList = new ArrayList<>();
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        // 预生成随机索引
        int[] indices = new int[accessCount];
        for (int i = 0; i < accessCount; i++) {
            indices[i] = random.nextInt(size);
        }

        // ArrayList 随机访问
        long start = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < accessCount; i++) {
            sum1 += arrayList.get(indices[i]);
        }
        long alTime = System.nanoTime() - start;

        // LinkedList 随机访问（非常慢！）
        // 为了避免等太久，只测试 1000 次，然后推算
        int llAccessCount = 1_000;
        start = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < llAccessCount; i++) {
            sum2 += linkedList.get(indices[i]);
        }
        long llTimePartial = System.nanoTime() - start;
        long llTimeEstimated = llTimePartial * (accessCount / llAccessCount);

        System.out.printf("  ArrayList:  %,d ns (%.2f ms) [%d次访问]%n", alTime, alTime / 1_000_000.0, accessCount);
        System.out.printf("  LinkedList: %,d ns (%.2f ms) [%d次实测，推算%d次]%n",
                llTimeEstimated, llTimeEstimated / 1_000_000.0, llAccessCount, accessCount);
        System.out.printf("  胜者: ArrayList (约快 %,.0f 倍)%n%n", (double) llTimeEstimated / alTime);
    }

    /**
     * 测试4：顺序遍历
     */
    static void testIteration() {
        System.out.println("--- 测试4：for-each 遍历 100 万个元素 ---");
        int size = 1_000_000;

        ArrayList<Integer> arrayList = new ArrayList<>();
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        // ArrayList 遍历
        long start = System.nanoTime();
        long sum1 = 0;
        for (int val : arrayList) {
            sum1 += val;
        }
        long alTime = System.nanoTime() - start;

        // LinkedList 遍历
        start = System.nanoTime();
        long sum2 = 0;
        for (int val : linkedList) {
            sum2 += val;
        }
        long llTime = System.nanoTime() - start;

        System.out.printf("  ArrayList:  %,d ns (%.2f ms)%n", alTime, alTime / 1_000_000.0);
        System.out.printf("  LinkedList: %,d ns (%.2f ms)%n", llTime, llTime / 1_000_000.0);
        System.out.printf("  胜者: %s (快 %.1f 倍)%n%n",
                alTime < llTime ? "ArrayList" : "LinkedList",
                alTime < llTime ? (double) llTime / alTime : (double) alTime / llTime);
    }

    /**
     * 测试5：使用迭代器删除元素
     */
    static void testIteratorRemove() {
        System.out.println("--- 测试5：迭代器删除每第3个元素（列表大小 10 万） ---");
        int size = 100_000;

        // 准备 ArrayList
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < size; i++) arrayList.add(i);

        long start = System.nanoTime();
        Iterator<Integer> it1 = arrayList.iterator();
        int count1 = 0;
        while (it1.hasNext()) {
            it1.next();
            if (++count1 % 3 == 0) {
                it1.remove();
            }
        }
        long alTime = System.nanoTime() - start;

        // 准备 LinkedList
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) linkedList.add(i);

        start = System.nanoTime();
        Iterator<Integer> it2 = linkedList.iterator();
        int count2 = 0;
        while (it2.hasNext()) {
            it2.next();
            if (++count2 % 3 == 0) {
                it2.remove();
            }
        }
        long llTime = System.nanoTime() - start;

        System.out.printf("  ArrayList:  %,d ns (%.2f ms)%n", alTime, alTime / 1_000_000.0);
        System.out.printf("  LinkedList: %,d ns (%.2f ms)%n", llTime, llTime / 1_000_000.0);
        System.out.printf("  胜者: %s (快 %.1f 倍)%n%n",
                alTime < llTime ? "ArrayList" : "LinkedList",
                alTime < llTime ? (double) llTime / alTime : (double) alTime / llTime);
    }
}
