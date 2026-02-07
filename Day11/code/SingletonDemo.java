import java.io.*;
import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day11 - 单例模式演示
 * 
 * 包含 6 种单例实现方式 + 枚举单例 + 反射/序列化攻击与防御
 */
public class SingletonDemo {

    // ======================== 1. 饿汉式 - 静态常量 ========================
    static class EagerSingleton1 {
        private EagerSingleton1() {}

        private static final EagerSingleton1 INSTANCE = new EagerSingleton1();

        public static EagerSingleton1 getInstance() {
            return INSTANCE;
        }
    }

    // ======================== 2. 饿汉式 - 静态代码块 ========================
    static class EagerSingleton2 {
        private EagerSingleton2() {}

        private static final EagerSingleton2 INSTANCE;

        static {
            // 可以在这里做初始化操作，如读取配置
            INSTANCE = new EagerSingleton2();
        }

        public static EagerSingleton2 getInstance() {
            return INSTANCE;
        }
    }

    // ======================== 3. 懒汉式 - 线程不安全 ========================
    static class LazySingletonUnsafe {
        private LazySingletonUnsafe() {}

        private static LazySingletonUnsafe instance;

        public static LazySingletonUnsafe getInstance() {
            if (instance == null) {
                // 模拟耗时操作，增加线程安全问题出现的概率
                try { Thread.sleep(1); } catch (InterruptedException e) {}
                instance = new LazySingletonUnsafe();
            }
            return instance;
        }
    }

    // ======================== 4. 懒汉式 - synchronized ========================
    static class LazySingletonSync {
        private LazySingletonSync() {}

        private static LazySingletonSync instance;

        public static synchronized LazySingletonSync getInstance() {
            if (instance == null) {
                instance = new LazySingletonSync();
            }
            return instance;
        }
    }

    // ======================== 5. DCL 双重检查锁 ========================
    static class DCLSingleton {
        private DCLSingleton() {}

        // volatile 防止指令重排序
        private static volatile DCLSingleton instance;

        public static DCLSingleton getInstance() {
            if (instance == null) {                      // 第一次检查（无锁）
                synchronized (DCLSingleton.class) {       // 加锁
                    if (instance == null) {                // 第二次检查（有锁）
                        instance = new DCLSingleton();
                    }
                }
            }
            return instance;
        }
    }

    // ======================== 6. 静态内部类 ========================
    static class InnerClassSingleton implements Serializable {
        private static final long serialVersionUID = 1L;

        private InnerClassSingleton() {}

        private static class SingletonHolder {
            private static final InnerClassSingleton INSTANCE = new InnerClassSingleton();
        }

        public static InnerClassSingleton getInstance() {
            return SingletonHolder.INSTANCE;
        }

        // readResolve 防止序列化破坏单例
        private Object readResolve() {
            return SingletonHolder.INSTANCE;
        }
    }

    // ======================== 7. 枚举单例 ========================
    enum EnumSingleton {
        INSTANCE;

        private int count = 0;

        public void doSomething() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    // ======================== 8. 用于演示反射攻击的单例（无防御） ========================
    static class VulnerableSingleton {
        private VulnerableSingleton() {}

        private static final VulnerableSingleton INSTANCE = new VulnerableSingleton();

        public static VulnerableSingleton getInstance() {
            return INSTANCE;
        }
    }

    // ======================== 9. 用于演示反射攻击的单例（有防御） ========================
    static class DefendedSingleton {
        private static boolean created = false;

        private DefendedSingleton() {
            synchronized (DefendedSingleton.class) {
                if (created) {
                    throw new RuntimeException("禁止反射创建单例实例！已存在实例。");
                }
                created = true;
            }
        }

        private static class SingletonHolder {
            private static final DefendedSingleton INSTANCE = new DefendedSingleton();
        }

        public static DefendedSingleton getInstance() {
            return SingletonHolder.INSTANCE;
        }
    }

    // ======================== 10. 用于演示序列化破坏的单例（无防御） ========================
    static class SerializableVulnerable implements Serializable {
        private static final long serialVersionUID = 1L;

        private SerializableVulnerable() {}

        private static final SerializableVulnerable INSTANCE = new SerializableVulnerable();

        public static SerializableVulnerable getInstance() {
            return INSTANCE;
        }
        // 注意：没有 readResolve() 方法
    }

    // ======================== 11. 用于演示序列化破坏的单例（有防御） ========================
    static class SerializableDefended implements Serializable {
        private static final long serialVersionUID = 1L;

        private SerializableDefended() {}

        private static final SerializableDefended INSTANCE = new SerializableDefended();

        public static SerializableDefended getInstance() {
            return INSTANCE;
        }

        // readResolve() 防止序列化破坏单例
        private Object readResolve() {
            return INSTANCE;
        }
    }

    // ======================== 测试方法 ========================

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  Day11 - 单例模式演示");
        System.out.println("========================================\n");

        // 测试1：多线程下各单例实现的安全性
        testThreadSafety();

        // 测试2：反射攻击
        testReflectionAttack();

        // 测试3：枚举防御反射
        testEnumReflectionDefense();

        // 测试4：序列化攻击与防御
        testSerializationAttack();

        // 测试5：枚举单例演示
        testEnumSingleton();
    }

    /**
     * 测试1：多线程下各单例实现的线程安全性
     */
    static void testThreadSafety() throws InterruptedException {
        System.out.println("=== 测试1：多线程下的线程安全性 ===\n");

        int threadCount = 50;

        // 测试线程不安全的懒汉式
        System.out.println("--- 懒汉式（线程不安全）---");
        ConcurrentHashMap<Integer, Integer> unsafeResults = new ConcurrentHashMap<>();
        CountDownLatch latch1 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                LazySingletonUnsafe instance = LazySingletonUnsafe.getInstance();
                unsafeResults.put(System.identityHashCode(instance), 1);
                latch1.countDown();
            }).start();
        }
        latch1.await();
        System.out.println("  创建了 " + unsafeResults.size() + " 个不同的实例" +
                (unsafeResults.size() > 1 ? " ⚠️ 单例被破坏！" : " ✅ 单例正常"));

        // 测试 DCL
        System.out.println("\n--- DCL 双重检查锁 ---");
        ConcurrentHashMap<Integer, Integer> dclResults = new ConcurrentHashMap<>();
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                DCLSingleton instance = DCLSingleton.getInstance();
                dclResults.put(System.identityHashCode(instance), 1);
                latch2.countDown();
            }).start();
        }
        latch2.await();
        System.out.println("  创建了 " + dclResults.size() + " 个不同的实例" +
                (dclResults.size() > 1 ? " ⚠️ 单例被破坏！" : " ✅ 单例正常"));

        // 测试静态内部类
        System.out.println("\n--- 静态内部类 ---");
        ConcurrentHashMap<Integer, Integer> innerResults = new ConcurrentHashMap<>();
        CountDownLatch latch3 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                InnerClassSingleton instance = InnerClassSingleton.getInstance();
                innerResults.put(System.identityHashCode(instance), 1);
                latch3.countDown();
            }).start();
        }
        latch3.await();
        System.out.println("  创建了 " + innerResults.size() + " 个不同的实例" +
                (innerResults.size() > 1 ? " ⚠️ 单例被破坏！" : " ✅ 单例正常"));

        // 测试枚举
        System.out.println("\n--- 枚举单例 ---");
        ConcurrentHashMap<Integer, Integer> enumResults = new ConcurrentHashMap<>();
        CountDownLatch latch4 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                EnumSingleton instance = EnumSingleton.INSTANCE;
                enumResults.put(System.identityHashCode(instance), 1);
                latch4.countDown();
            }).start();
        }
        latch4.await();
        System.out.println("  创建了 " + enumResults.size() + " 个不同的实例" +
                (enumResults.size() > 1 ? " ⚠️ 单例被破坏！" : " ✅ 单例正常"));

        System.out.println();
    }

    /**
     * 测试2：反射攻击普通单例
     */
    static void testReflectionAttack() {
        System.out.println("=== 测试2：反射攻击单例 ===\n");

        try {
            // 正常获取单例
            VulnerableSingleton s1 = VulnerableSingleton.getInstance();

            // 通过反射获取私有构造器
            Constructor<VulnerableSingleton> constructor =
                    VulnerableSingleton.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            VulnerableSingleton s2 = constructor.newInstance();

            System.out.println("  正常实例 hashCode: " + System.identityHashCode(s1));
            System.out.println("  反射实例 hashCode: " + System.identityHashCode(s2));
            System.out.println("  s1 == s2 ? " + (s1 == s2) + " ⚠️ 单例被反射破坏！");
        } catch (Exception e) {
            System.out.println("  反射攻击失败: " + e.getMessage());
        }

        // 测试有防御的单例
        System.out.println("\n--- 有防御的单例 ---");
        try {
            DefendedSingleton s1 = DefendedSingleton.getInstance();
            Constructor<DefendedSingleton> constructor =
                    DefendedSingleton.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            DefendedSingleton s2 = constructor.newInstance(); // 应该抛异常
            System.out.println("  防御失败！创建了新实例");
        } catch (Exception e) {
            System.out.println("  ✅ 防御成功！反射被拦截: " + e.getCause().getMessage());
        }

        System.out.println();
    }

    /**
     * 测试3：枚举防御反射攻击
     */
    static void testEnumReflectionDefense() {
        System.out.println("=== 测试3：枚举防御反射攻击 ===\n");

        try {
            // 尝试用反射创建枚举实例
            Constructor<EnumSingleton> constructor =
                    EnumSingleton.class.getDeclaredConstructor(String.class, int.class);
            constructor.setAccessible(true);
            EnumSingleton s2 = constructor.newInstance("INSTANCE2", 1);
            System.out.println("  枚举防御失败！");
        } catch (Exception e) {
            System.out.println("  ✅ 枚举防御成功！JVM 层面禁止反射创建枚举实例");
            String msg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            System.out.println("  异常信息: " + msg);
        }

        System.out.println();
    }

    /**
     * 测试4：序列化攻击与防御
     */
    static void testSerializationAttack() throws Exception {
        System.out.println("=== 测试4：序列化攻击与防御 ===\n");

        // 4.1 无防御的单例 - 序列化破坏
        System.out.println("--- 无 readResolve() 防御 ---");
        SerializableVulnerable s1 = SerializableVulnerable.getInstance();

        // 序列化
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(s1);
        oos.close();

        // 反序列化
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        SerializableVulnerable s2 = (SerializableVulnerable) ois.readObject();
        ois.close();

        System.out.println("  原始实例 hashCode: " + System.identityHashCode(s1));
        System.out.println("  反序列化 hashCode: " + System.identityHashCode(s2));
        System.out.println("  s1 == s2 ? " + (s1 == s2) +
                (s1 == s2 ? " ✅" : " ⚠️ 序列化破坏了单例！"));

        // 4.2 有防御的单例 - readResolve()
        System.out.println("\n--- 有 readResolve() 防御 ---");
        SerializableDefended s3 = SerializableDefended.getInstance();

        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(s3);
        oos.close();

        bis = new ByteArrayInputStream(bos.toByteArray());
        ois = new ObjectInputStream(bis);
        SerializableDefended s4 = (SerializableDefended) ois.readObject();
        ois.close();

        System.out.println("  原始实例 hashCode: " + System.identityHashCode(s3));
        System.out.println("  反序列化 hashCode: " + System.identityHashCode(s4));
        System.out.println("  s3 == s4 ? " + (s3 == s4) +
                (s3 == s4 ? " ✅ readResolve() 防御成功！" : " ⚠️"));

        // 4.3 枚举序列化
        System.out.println("\n--- 枚举序列化（天然防御）---");
        EnumSingleton e1 = EnumSingleton.INSTANCE;
        e1.doSomething();

        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(e1);
        oos.close();

        bis = new ByteArrayInputStream(bos.toByteArray());
        ois = new ObjectInputStream(bis);
        EnumSingleton e2 = (EnumSingleton) ois.readObject();
        ois.close();

        System.out.println("  原始实例 hashCode: " + System.identityHashCode(e1));
        System.out.println("  反序列化 hashCode: " + System.identityHashCode(e2));
        System.out.println("  e1 == e2 ? " + (e1 == e2) +
                (e1 == e2 ? " ✅ 枚举天然防御序列化破坏！" : " ⚠️"));
        System.out.println("  反序列化后 count = " + e2.getCount() + " (状态保持)");

        System.out.println();
    }

    /**
     * 测试5：枚举单例功能演示
     */
    static void testEnumSingleton() {
        System.out.println("=== 测试5：枚举单例功能演示 ===\n");

        EnumSingleton s1 = EnumSingleton.INSTANCE;
        EnumSingleton s2 = EnumSingleton.INSTANCE;

        System.out.println("  s1 == s2 ? " + (s1 == s2) + " ✅");
        s1.doSomething();
        s1.doSomething();
        s1.doSomething();
        System.out.println("  调用 3 次后 count = " + s2.getCount() +
                " (s1 和 s2 是同一实例)");

        System.out.println();
        System.out.println("========================================");
        System.out.println("  各种单例实现方式对比总结");
        System.out.println("========================================");
        System.out.println("  | 实现方式         | 线程安全 | 懒加载 | 防反射 | 防序列化 |");
        System.out.println("  |-----------------|---------|--------|--------|---------|");
        System.out.println("  | 饿汉式(静态常量)  |   ✅    |   ❌   |   ❌   |   ❌    |");
        System.out.println("  | 饿汉式(静态代码块) |   ✅    |   ❌   |   ❌   |   ❌    |");
        System.out.println("  | 懒汉式(不安全)    |   ❌    |   ✅   |   ❌   |   ❌    |");
        System.out.println("  | 懒汉式(sync)     |   ✅    |   ✅   |   ❌   |   ❌    |");
        System.out.println("  | DCL 双重检查锁    |   ✅    |   ✅   |   ❌   |   ❌    |");
        System.out.println("  | 静态内部类        |   ✅    |   ✅   |   ❌   |   ❌*   |");
        System.out.println("  | 枚举 ⭐          |   ✅    |   ❌   |   ✅   |   ✅    |");
        System.out.println("  * 需手动添加 readResolve()");
        System.out.println("\n  推荐：枚举 > 静态内部类 > DCL > 饿汉式");
    }
}
