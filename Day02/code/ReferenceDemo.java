import java.lang.ref.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Day02 实验：四种引用类型演示
 *
 * 演示 Java 四种引用类型的行为差异：
 * 1. 强引用（Strong Reference）    - GC 时不会被回收
 * 2. 软引用（Soft Reference）      - 内存不足时被回收
 * 3. 弱引用（Weak Reference）      - 下一次 GC 时被回收
 * 4. 虚引用（Phantom Reference）   - 随时可能被回收，get() 永远返回 null
 *
 * 使用方式：
 *   javac ReferenceDemo.java
 *   java -Xms20m -Xmx20m -XX:+PrintGCDetails ReferenceDemo
 *
 * JDK 9+ 请将 -XX:+PrintGCDetails 替换为 -Xlog:gc*
 */
public class ReferenceDemo {

    /** 用于演示的对象，在 finalize 时打印信息 */
    static class BigObject {
        private String name;
        private byte[] data; // 占用一定内存空间

        public BigObject(String name, int sizeKB) {
            this.name = name;
            this.data = new byte[sizeKB * 1024];
            System.out.println("  [创建] " + name + " (" + sizeKB + "KB)");
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        @SuppressWarnings("deprecation")
        protected void finalize() throws Throwable {
            System.out.println("  [回收] " + name + " 被垃圾回收器回收！");
            super.finalize();
        }
    }

    // ========== 1. 强引用演示 ==========

    public static void strongReferenceDemo() {
        System.out.println("\n====== 1. 强引用（Strong Reference）======");
        System.out.println("特点：只要强引用存在，GC 永远不会回收该对象。\n");

        // 创建强引用对象
        BigObject obj = new BigObject("StrongObj", 100);

        System.out.println("  GC 前: obj = " + obj);

        // 手动触发 GC
        System.gc();
        sleep(500);

        System.out.println("  GC 后: obj = " + obj + " (强引用，不会被回收)");

        // 断开强引用
        obj = null;
        System.out.println("  设置 obj = null 后触发 GC...");
        System.gc();
        sleep(500);

        System.out.println("  结论：只有断开强引用后，对象才能被回收。\n");
    }

    // ========== 2. 软引用演示 ==========

    public static void softReferenceDemo() {
        System.out.println("====== 2. 软引用（Soft Reference）======");
        System.out.println("特点：内存充足时不回收，内存不足时回收。常用于缓存。\n");

        // 创建软引用
        BigObject obj = new BigObject("SoftObj", 100);
        SoftReference<BigObject> softRef = new SoftReference<>(obj);
        obj = null; // 断开强引用，只保留软引用

        System.out.println("  GC 前: softRef.get() = " + softRef.get());

        // 内存充足时 GC
        System.gc();
        sleep(500);
        System.out.println("  GC 后（内存充足）: softRef.get() = " + softRef.get());

        // 尝试消耗内存，使软引用被回收
        System.out.println("  尝试消耗内存，触发软引用回收...");
        try {
            List<byte[]> memoryHog = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                memoryHog.add(new byte[1024 * 1024]); // 每次 1MB
                if (softRef.get() == null) {
                    System.out.println("  内存压力下: softRef.get() = null (已被回收!)");
                    break;
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("  内存溢出! softRef.get() = " + softRef.get());
        }

        System.out.println("  结论：软引用在内存不足时会被回收，适合做缓存。\n");
    }

    // ========== 3. 弱引用演示 ==========

    public static void weakReferenceDemo() {
        System.out.println("====== 3. 弱引用（Weak Reference）======");
        System.out.println("特点：无论内存是否充足，GC 时一定被回收。\n");

        // 创建弱引用
        BigObject obj = new BigObject("WeakObj", 100);
        WeakReference<BigObject> weakRef = new WeakReference<>(obj);
        obj = null; // 断开强引用，只保留弱引用

        System.out.println("  GC 前: weakRef.get() = " + weakRef.get());

        // 触发 GC
        System.gc();
        sleep(500);

        System.out.println("  GC 后: weakRef.get() = " + weakRef.get() + " (应为 null)");
        System.out.println("  结论：弱引用在下一次 GC 时就会被回收，适合做临时缓存（如 WeakHashMap）。\n");
    }

    // ========== 4. 虚引用演示 ==========

    public static void phantomReferenceDemo() {
        System.out.println("====== 4. 虚引用（Phantom Reference）======");
        System.out.println("特点：get() 永远返回 null，对象被回收后通知放入引用队列。\n");

        // 虚引用必须搭配引用队列使用
        ReferenceQueue<BigObject> refQueue = new ReferenceQueue<>();

        BigObject obj = new BigObject("PhantomObj", 100);
        PhantomReference<BigObject> phantomRef = new PhantomReference<>(obj, refQueue);

        System.out.println("  phantomRef.get() = " + phantomRef.get() + " (虚引用永远返回 null)");

        // 断开强引用，触发 GC
        obj = null;
        System.gc();
        sleep(500);

        // 检查引用队列
        Reference<? extends BigObject> polled = refQueue.poll();
        if (polled != null) {
            System.out.println("  引用队列中收到通知！对象已被回收。");
            System.out.println("  polled == phantomRef ? " + (polled == phantomRef));
        } else {
            System.out.println("  引用队列暂时为空（GC 可能尚未完成）");
            // 再等一下
            System.gc();
            sleep(1000);
            polled = refQueue.poll();
            if (polled != null) {
                System.out.println("  第二次检查：引用队列中收到通知！对象已被回收。");
            } else {
                System.out.println("  第二次检查：引用队列仍为空（部分 JVM 实现可能有延迟）");
            }
        }

        System.out.println("  结论：虚引用用于跟踪对象被回收的时机（如资源清理、堆外内存管理）。\n");
    }

    // ========== 辅助方法 ==========

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 主入口 ==========

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Day02 四种引用类型演示");
        System.out.println("========================================");

        // 打印 JVM 信息
        System.out.println("[JVM] " + System.getProperty("java.vm.name")
                + " " + System.getProperty("java.vm.version"));
        System.out.println("[JDK] " + System.getProperty("java.version"));
        System.out.println("[堆]  Max=" + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");

        // 依次演示四种引用
        strongReferenceDemo();
        weakReferenceDemo();
        softReferenceDemo();
        phantomReferenceDemo();

        System.out.println("========================================");
        System.out.println("  引用类型总结");
        System.out.println("========================================");
        System.out.println("| 引用类型 | 回收时机             | 典型用途           |");
        System.out.println("|---------|---------------------|--------------------|");
        System.out.println("| 强引用   | 永不回收（只要可达）   | 普通对象引用          |");
        System.out.println("| 软引用   | 内存不足时回收        | 内存敏感缓存          |");
        System.out.println("| 弱引用   | 下次 GC 时回收       | WeakHashMap、临时缓存 |");
        System.out.println("| 虚引用   | 随时回收，get()=null  | 跟踪回收时机、堆外内存  |");
    }
}
