import java.util.*;

/**
 * 手写简化版 ArrayList
 * 
 * 实现功能：
 * 1. 动态数组（Object[] 底层存储）
 * 2. add(E e) - 尾部追加 + 自动扩容
 * 3. add(int index, E e) - 指定位置插入
 * 4. remove(int index) - 按索引删除
 * 5. get(int index) - 随机访问
 * 6. set(int index, E e) - 修改元素
 * 7. size() / isEmpty() / contains()
 * 8. grow() - 1.5 倍扩容
 * 9. iterator() - 支持 fail-fast 的迭代器
 */
public class SimpleArrayList<E> implements Iterable<E> {

    // ================================================================
    // 核心字段
    // ================================================================

    /** 默认初始容量 */
    private static final int DEFAULT_CAPACITY = 10;

    /** 底层存储数组 */
    private Object[] elementData;

    /** 实际元素个数 */
    private int size;

    /** 结构修改计数器（用于 fail-fast） */
    private int modCount;

    // ================================================================
    // 构造方法
    // ================================================================

    /** 无参构造：初始容量为默认值 10 */
    public SimpleArrayList() {
        this.elementData = new Object[DEFAULT_CAPACITY];
        this.size = 0;
        this.modCount = 0;
    }

    /** 指定初始容量 */
    public SimpleArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        this.elementData = new Object[initialCapacity];
        this.size = 0;
        this.modCount = 0;
    }

    // ================================================================
    // 核心方法
    // ================================================================

    /**
     * 尾部追加元素
     * 
     * 流程：
     * 1. 检查是否需要扩容（ensureCapacity）
     * 2. 在 size 位置放入元素
     * 3. size++
     */
    public boolean add(E e) {
        ensureCapacity(size + 1);
        elementData[size++] = e;
        modCount++;
        return true;
    }

    /**
     * 在指定位置插入元素
     * 
     * 流程：
     * 1. 检查索引合法性
     * 2. 确保容量
     * 3. 使用 System.arraycopy 将 index 及之后的元素后移一位
     * 4. 在 index 位置放入元素
     * 5. size++
     */
    public void add(int index, E e) {
        rangeCheckForAdd(index);
        ensureCapacity(size + 1);

        // 将 index 及之后的元素向后移动一位
        System.arraycopy(elementData, index, elementData, index + 1, size - index);
        elementData[index] = e;
        size++;
        modCount++;
    }

    /**
     * 按索引删除元素
     * 
     * 流程：
     * 1. 检查索引合法性
     * 2. 保存旧值
     * 3. 计算需要移动的元素个数
     * 4. 使用 System.arraycopy 将 index+1 及之后的元素前移一位
     * 5. 最后一个位置置 null（帮助 GC）
     * 6. size--
     */
    @SuppressWarnings("unchecked")
    public E remove(int index) {
        rangeCheck(index);

        E oldValue = (E) elementData[index];

        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, numMoved);
        }
        elementData[--size] = null;  // 帮助 GC
        modCount++;

        return oldValue;
    }

    /**
     * 按对象删除（删除第一个匹配的元素）
     */
    public boolean remove(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++) {
                if (elementData[i] == null) {
                    fastRemove(i);
                    return true;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (o.equals(elementData[i])) {
                    fastRemove(i);
                    return true;
                }
            }
        }
        return false;
    }

    /** 内部快速删除（跳过边界检查） */
    private void fastRemove(int index) {
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, numMoved);
        }
        elementData[--size] = null;
        modCount++;
    }

    /**
     * 按索引获取元素 — O(1) 随机访问
     */
    @SuppressWarnings("unchecked")
    public E get(int index) {
        rangeCheck(index);
        return (E) elementData[index];
    }

    /**
     * 修改指定位置的元素
     */
    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        rangeCheck(index);
        E oldValue = (E) elementData[index];
        elementData[index] = element;
        return oldValue;
    }

    /**
     * 返回元素个数
     */
    public int size() {
        return size;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 是否包含指定元素
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * 返回元素第一次出现的索引，不存在返回 -1
     */
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++) {
                if (elementData[i] == null) return i;
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (o.equals(elementData[i])) return i;
            }
        }
        return -1;
    }

    /**
     * 清空列表
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            elementData[i] = null;
        }
        size = 0;
        modCount++;
    }

    // ================================================================
    // 扩容机制
    // ================================================================

    /**
     * 确保容量足够
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity > elementData.length) {
            grow(minCapacity);
        }
    }

    /**
     * 扩容核心方法
     * 
     * 扩容策略：新容量 = 旧容量 * 1.5（旧容量 + 旧容量 >> 1）
     * 如果 1.5 倍仍不够，则使用所需的最小容量
     */
    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        // 新容量 = 旧容量 + 旧容量/2（1.5 倍扩容）
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        // 如果 1.5 倍仍不够
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }

        // 防止容量为 0 时 1.5 倍还是 0
        if (newCapacity == 0) {
            newCapacity = DEFAULT_CAPACITY;
        }

        System.out.println("  [扩容] " + oldCapacity + " → " + newCapacity);

        // 使用 Arrays.copyOf 复制数组
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    // ================================================================
    // 迭代器（支持 fail-fast）
    // ================================================================

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * 内部迭代器类
     * 
     * fail-fast 机制：
     * - 创建迭代器时，记录当前的 modCount 为 expectedModCount
     * - 每次调用 next()/remove() 前，检查 modCount 是否发生变化
     * - 如果变化了，说明集合被外部修改，抛出 ConcurrentModificationException
     */
    private class Itr implements Iterator<E> {
        int cursor;             // 下一个要返回的元素索引
        int lastRet = -1;       // 上一个返回的元素索引
        int expectedModCount;   // 创建时的 modCount 快照

        Itr() {
            this.expectedModCount = modCount;
        }

        @Override
        public boolean hasNext() {
            return cursor != size;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            if (cursor >= size) {
                throw new NoSuchElementException();
            }
            lastRet = cursor;
            cursor++;
            return (E) elementData[lastRet];
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }
            checkForComodification();

            SimpleArrayList.this.remove(lastRet);
            cursor = lastRet;       // 因为删除了一个元素，cursor 回退
            lastRet = -1;
            expectedModCount = modCount;  // 同步 modCount
        }

        /** fail-fast 检测 */
        final void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    private void rangeCheck(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    @Override
    public String toString() {
        if (size == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(elementData[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取当前内部数组容量（仅用于演示）
     */
    public int capacity() {
        return elementData.length;
    }

    // ================================================================
    // 测试
    // ================================================================

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("  手写 SimpleArrayList 演示");
        System.out.println("====================================\n");

        testBasicOperations();
        testGrowMechanism();
        testIterator();
        testFailFast();
        testEdgeCases();
    }

    static void testBasicOperations() {
        System.out.println("【测试1】基本操作");
        System.out.println("----------------------------------");

        SimpleArrayList<String> list = new SimpleArrayList<>();
        System.out.println("创建空列表: " + list + " (size=" + list.size() + ", capacity=" + list.capacity() + ")");

        // 添加元素
        list.add("Apple");
        list.add("Banana");
        list.add("Cherry");
        list.add("Date");
        list.add("Elderberry");
        System.out.println("添加5个元素: " + list);

        // 指定位置插入
        list.add(2, "Blueberry");
        System.out.println("add(2, Blueberry): " + list);

        // 获取
        System.out.println("get(0) = " + list.get(0));
        System.out.println("get(3) = " + list.get(3));

        // 修改
        String old = list.set(1, "BlackBerry");
        System.out.println("set(1, BlackBerry)，旧值 = " + old);

        // 查找
        System.out.println("contains(Cherry) = " + list.contains("Cherry"));
        System.out.println("indexOf(Cherry) = " + list.indexOf("Cherry"));

        // 删除
        list.remove(0);
        System.out.println("remove(0): " + list);
        list.remove("Cherry");
        System.out.println("remove(Cherry): " + list);

        System.out.println("size = " + list.size() + ", isEmpty = " + list.isEmpty());
        System.out.println();
    }

    static void testGrowMechanism() {
        System.out.println("【测试2】扩容机制");
        System.out.println("----------------------------------");

        // 初始容量为 4
        SimpleArrayList<Integer> list = new SimpleArrayList<>(4);
        System.out.println("初始容量: " + list.capacity());

        for (int i = 1; i <= 20; i++) {
            list.add(i);
            if (i <= 4 || i == 5 || i == 7 || i == 10 || i == 14 || i == 20) {
                System.out.println("  添加第" + i + "个元素后: size=" + list.size() + ", capacity=" + list.capacity());
            }
        }

        System.out.println("最终: " + list);
        System.out.println();
    }

    static void testIterator() {
        System.out.println("【测试3】迭代器");
        System.out.println("----------------------------------");

        SimpleArrayList<String> list = new SimpleArrayList<>();
        list.add("A"); list.add("B"); list.add("C"); list.add("D"); list.add("E");

        // for-each 遍历（使用迭代器）
        System.out.print("for-each 遍历: ");
        for (String s : list) {
            System.out.print(s + " ");
        }
        System.out.println();

        // 使用迭代器安全删除
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if ("C".equals(s)) {
                it.remove();
            }
        }
        System.out.println("迭代器删除 C 后: " + list);
        System.out.println();
    }

    static void testFailFast() {
        System.out.println("【测试4】fail-fast 机制");
        System.out.println("----------------------------------");

        SimpleArrayList<String> list = new SimpleArrayList<>();
        list.add("A"); list.add("B"); list.add("C"); list.add("D");

        // 在迭代过程中修改集合 → 触发 ConcurrentModificationException
        System.out.print("在 for-each 中调用 list.add(): ");
        try {
            for (String s : list) {
                if ("B".equals(s)) {
                    list.add("X");  // 修改集合
                }
            }
            System.out.println("未抛异常");
        } catch (ConcurrentModificationException e) {
            System.out.println("✅ 捕获 ConcurrentModificationException");
        }

        // 在迭代过程中通过集合删除 → 触发 ConcurrentModificationException
        list = new SimpleArrayList<>();
        list.add("A"); list.add("B"); list.add("C"); list.add("D");
        System.out.print("在 for-each 中调用 list.remove(): ");
        try {
            for (String s : list) {
                if ("B".equals(s)) {
                    list.remove(1);  // 通过集合删除
                }
            }
            System.out.println("未抛异常");
        } catch (ConcurrentModificationException e) {
            System.out.println("✅ 捕获 ConcurrentModificationException");
        }

        System.out.println();
    }

    static void testEdgeCases() {
        System.out.println("【测试5】边界情况");
        System.out.println("----------------------------------");

        SimpleArrayList<Integer> list = new SimpleArrayList<>(0);
        System.out.println("容量为0的列表: capacity=" + list.capacity());
        list.add(1);
        System.out.println("添加1个元素后: size=" + list.size() + ", capacity=" + list.capacity());

        // 测试越界
        System.out.print("get(-1): ");
        try {
            list.get(-1);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("✅ " + e.getMessage());
        }

        System.out.print("get(100): ");
        try {
            list.get(100);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("✅ " + e.getMessage());
        }

        // 测试 null 元素
        SimpleArrayList<String> nullList = new SimpleArrayList<>();
        nullList.add(null);
        nullList.add("A");
        nullList.add(null);
        System.out.println("包含 null 的列表: " + nullList);
        System.out.println("contains(null) = " + nullList.contains(null));
        System.out.println("indexOf(null) = " + nullList.indexOf(null));
        nullList.remove(null);
        System.out.println("remove(null) 后: " + nullList);

        // 测试 clear
        list.clear();
        System.out.println("clear() 后: " + list + " (size=" + list.size() + ")");

        System.out.println("\n====================================");
        System.out.println("  所有测试通过！");
        System.out.println("====================================");
    }
}
