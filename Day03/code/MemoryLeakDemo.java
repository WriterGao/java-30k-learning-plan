import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存泄漏演示
 *
 * 模拟常见的内存泄漏场景：
 * 1. 静态集合持有对象引用，导致对象无法被 GC 回收
 * 2. HashMap 的 key 没有正确实现 hashCode/equals，导致无法移除
 *
 * 运行方式：
 *   javac MemoryLeakDemo.java
 *   java -Xms128m -Xmx128m \
 *        -XX:+HeapDumpOnOutOfMemoryError \
 *        -XX:HeapDumpPath=./heap_leak.hprof \
 *        -verbose:gc \
 *        MemoryLeakDemo
 *
 * 预期现象：
 *   - GC 频率越来越高
 *   - 每次 GC 后可用内存越来越少
 *   - 最终抛出 OutOfMemoryError: Java heap space
 *   - 生成堆转储文件 heap_leak.hprof，可用 MAT 分析
 *
 * @author Day03 JVM调优实战
 */
public class MemoryLeakDemo {

    // ========== 场景1：静态集合导致内存泄漏 ==========

    /**
     * 静态 List 持有所有创建的对象引用
     * 即使业务上不再需要这些对象，它们也无法被 GC 回收
     */
    private static final List<byte[]> LEAK_LIST = new ArrayList<>();

    /**
     * 模拟静态集合导致的内存泄漏
     * 不断向静态 List 中添加数据，且从不移除
     */
    public static void staticCollectionLeak() {
        System.out.println("===== 场景1：静态集合导致内存泄漏 =====");
        System.out.println("不断向静态 List 中添加 1MB 数据块...");
        System.out.println("观察 GC 日志：Full GC 频率增加，回收效果越来越差\n");

        int count = 0;
        try {
            while (true) {
                // 每次分配 1MB 数据
                byte[] data = new byte[1024 * 1024];
                LEAK_LIST.add(data);
                count++;

                if (count % 10 == 0) {
                    System.out.println("[静态集合泄漏] 已分配 " + count + " MB，"
                            + "List 大小: " + LEAK_LIST.size()
                            + "，当前堆使用: " + getHeapUsageMB() + " MB");
                }

                Thread.sleep(100); // 放慢速度，便于观察
            }
        } catch (OutOfMemoryError e) {
            System.out.println("\n[OOM] 内存溢出！共分配了 " + count + " MB");
            System.out.println("[OOM] 异常信息: " + e.getMessage());
            System.out.println("[OOM] 泄漏原因: 静态 List 持有所有 byte[] 引用，GC 无法回收");
            System.out.println("[OOM] 修复方案: ");
            System.out.println("  1. 避免使用静态集合长期持有对象引用");
            System.out.println("  2. 使用完毕后及时 remove");
            System.out.println("  3. 考虑使用 WeakReference 或缓存框架（如 Caffeine）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 场景2：HashMap key 未正确实现 hashCode/equals ==========

    /**
     * 自定义 Key 类：故意不重写 hashCode 和 equals
     * 导致相同逻辑的 key 在 HashMap 中被当作不同的 key
     */
    static class BadKey {
        private final String name;
        private final int id;

        public BadKey(String name, int id) {
            this.name = name;
            this.id = id;
        }

        // 故意不重写 hashCode 和 equals
        // 每个对象都使用 Object 默认的 hashCode（基于对象地址）
        // 导致：new BadKey("user", 1) 和 new BadKey("user", 1) 被视为不同的 key

        @Override
        public String toString() {
            return "BadKey{name='" + name + "', id=" + id + "}";
        }
    }

    /**
     * 模拟 HashMap key 不正确导致的内存泄漏
     */
    public static void hashMapKeyLeak() {
        System.out.println("\n===== 场景2：HashMap key 未正确实现 hashCode/equals =====");
        System.out.println("使用 BadKey（未重写 hashCode/equals）作为 HashMap 的 key");
        System.out.println("相同逻辑的 key 无法覆盖，导致 Map 无限增长\n");

        Map<BadKey, byte[]> cache = new HashMap<>();
        int count = 0;

        try {
            while (true) {
                // 使用"相同"的 key 放入 map
                // 但由于没有重写 hashCode/equals，每次都被当作新 key
                BadKey key = new BadKey("user", count % 100);
                byte[] value = new byte[10 * 1024]; // 10KB

                cache.put(key, value);
                count++;

                if (count % 1000 == 0) {
                    System.out.println("[HashMap泄漏] 已 put " + count + " 次，"
                            + "Map 大小: " + cache.size()
                            + "（预期最多 100，实际远超）"
                            + "，当前堆使用: " + getHeapUsageMB() + " MB");
                }

                if (count % 100 == 0) {
                    Thread.sleep(10);
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("\n[OOM] 内存溢出！共 put 了 " + count + " 次");
            System.out.println("[OOM] Map 实际大小: " + cache.size() + "（预期最多 100）");
            System.out.println("[OOM] 泄漏原因: BadKey 未重写 hashCode/equals，每次 put 都新增条目");
            System.out.println("[OOM] 修复方案: ");
            System.out.println("  1. 正确重写 hashCode() 和 equals() 方法");
            System.out.println("  2. 使用 IDE 自动生成或 Lombok 的 @EqualsAndHashCode");
            System.out.println("  3. 使用不可变类型作为 Map 的 key（如 String、Integer）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== 工具方法 ==========

    /**
     * 获取当前堆内存使用量（MB）
     */
    private static long getHeapUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    // ========== 主方法 ==========

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║         内存泄漏演示 (MemoryLeakDemo)         ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║ 建议 JVM 参数：                               ║");
        System.out.println("║   -Xms128m -Xmx128m                         ║");
        System.out.println("║   -XX:+HeapDumpOnOutOfMemoryError            ║");
        System.out.println("║   -verbose:gc                                ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        Runtime runtime = Runtime.getRuntime();
        System.out.println("堆内存信息：");
        System.out.println("  最大堆内存: " + runtime.maxMemory() / (1024 * 1024) + " MB");
        System.out.println("  初始堆内存: " + runtime.totalMemory() / (1024 * 1024) + " MB");
        System.out.println("  空闲堆内存: " + runtime.freeMemory() / (1024 * 1024) + " MB");
        System.out.println();

        // 选择场景运行
        String scenario = args.length > 0 ? args[0] : "1";

        switch (scenario) {
            case "1":
                staticCollectionLeak();
                break;
            case "2":
                hashMapKeyLeak();
                break;
            default:
                System.out.println("用法: java MemoryLeakDemo [1|2]");
                System.out.println("  1 - 静态集合泄漏（默认）");
                System.out.println("  2 - HashMap key 泄漏");
                break;
        }
    }
}
