import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * ABA 问题演示与解决方案
 *
 * 演示内容：
 * 1. 复现 ABA 问题
 * 2. 使用 AtomicStampedReference 解决 ABA（版本号方案）
 * 3. 使用 AtomicMarkableReference 解决 ABA（标记位方案）
 */
public class ABADemo {

    // ==================== 1. ABA 问题复现 ====================

    /**
     * 演示 ABA 问题：
     * 线程1 读取值 A，准备将其更新为 C
     * 线程2 在此期间将 A -> B -> A
     * 线程1 的 CAS 操作仍然成功（因为值仍然是 A），但实际上值已经被改过了
     */
    static void abaProblemDemo() throws InterruptedException {
        System.out.println("========== 1. ABA 问题复现 ==========");

        AtomicReference<String> atomicRef = new AtomicReference<>("A");

        // 线程1：读取值 A，休眠一段时间后尝试 CAS(A -> C)
        Thread thread1 = new Thread(() -> {
            String value = atomicRef.get(); // 读取到 "A"
            System.out.println("[线程1] 读取到值: " + value);

            try {
                // 模拟业务处理耗时
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 尝试 CAS：A -> C
            boolean success = atomicRef.compareAndSet("A", "C");
            System.out.println("[线程1] CAS(A -> C): " + success + ", 当前值: " + atomicRef.get());
            System.out.println("[线程1] ⚠️ CAS 成功了！但线程1并不知道值曾经被改为 B 又改回 A");
        }, "线程1");

        // 线程2：快速执行 A -> B -> A
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(100); // 确保线程1先读取
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // A -> B
            boolean success1 = atomicRef.compareAndSet("A", "B");
            System.out.println("[线程2] CAS(A -> B): " + success1 + ", 当前值: " + atomicRef.get());

            // B -> A（把值改回去）
            boolean success2 = atomicRef.compareAndSet("B", "A");
            System.out.println("[线程2] CAS(B -> A): " + success2 + ", 当前值: " + atomicRef.get());
            System.out.println("[线程2] 已完成 A -> B -> A 的修改");
        }, "线程2");

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println();
    }

    // ==================== 2. AtomicStampedReference 解决 ABA ====================

    /**
     * 使用 AtomicStampedReference 的版本号（stamp）来检测 ABA
     * 每次修改都会更新版本号，即使值相同，版本号也不同
     */
    static void stampedReferenceDemo() throws InterruptedException {
        System.out.println("========== 2. AtomicStampedReference 解决 ABA ==========");

        // 初始值 "A"，初始版本号 1
        AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>("A", 1);

        // 线程1：读取值和版本号，休眠后尝试 CAS
        Thread thread1 = new Thread(() -> {
            String value = stampedRef.getReference();
            int stamp = stampedRef.getStamp();
            System.out.println("[线程1] 读取到值: " + value + ", 版本号: " + stamp);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 尝试 CAS：期望值=A, 期望版本号=1, 新值=C, 新版本号=2
            boolean success = stampedRef.compareAndSet("A", "C", stamp, stamp + 1);
            System.out.println("[线程1] CAS(A->C, stamp " + stamp + "->" + (stamp + 1) + "): " + success);
            System.out.println("[线程1] 当前值: " + stampedRef.getReference()
                    + ", 当前版本号: " + stampedRef.getStamp());
            if (!success) {
                System.out.println("[线程1] ✅ CAS 失败！因为版本号已经变化，成功检测到 ABA 问题");
            }
        }, "线程1");

        // 线程2：快速执行 A -> B -> A，但版本号会递增
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int stamp = stampedRef.getStamp();

            // A -> B (版本号 1 -> 2)
            boolean success1 = stampedRef.compareAndSet("A", "B", stamp, stamp + 1);
            System.out.println("[线程2] CAS(A->B, stamp " + stamp + "->" + (stamp + 1) + "): " + success1);

            stamp = stampedRef.getStamp();

            // B -> A (版本号 2 -> 3)
            boolean success2 = stampedRef.compareAndSet("B", "A", stamp, stamp + 1);
            System.out.println("[线程2] CAS(B->A, stamp " + stamp + "->" + (stamp + 1) + "): " + success2);
            System.out.println("[线程2] 已完成 A->B->A，当前版本号: " + stampedRef.getStamp());
        }, "线程2");

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println();
    }

    // ==================== 3. AtomicMarkableReference 解决 ABA ====================

    /**
     * 使用 AtomicMarkableReference 的标记位来检测是否被修改过
     * 与 AtomicStampedReference 的区别：只关心"是否被修改过"，不关心修改了几次
     */
    static void markableReferenceDemo() throws InterruptedException {
        System.out.println("========== 3. AtomicMarkableReference 解决 ABA ==========");

        // 初始值 "A"，初始标记 false（未被修改）
        AtomicMarkableReference<String> markableRef = new AtomicMarkableReference<>("A", false);

        // 线程1：读取值和标记，休眠后尝试 CAS
        Thread thread1 = new Thread(() -> {
            String value = markableRef.getReference();
            boolean marked = markableRef.isMarked();
            System.out.println("[线程1] 读取到值: " + value + ", 标记: " + marked);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 尝试 CAS：期望标记=false
            boolean success = markableRef.compareAndSet("A", "C", false, true);
            System.out.println("[线程1] CAS(A->C, mark false->true): " + success);
            System.out.println("[线程1] 当前值: " + markableRef.getReference()
                    + ", 当前标记: " + markableRef.isMarked());
            if (!success) {
                System.out.println("[线程1] ✅ CAS 失败！因为标记已变化，检测到值曾被修改");
            }
        }, "线程1");

        // 线程2：修改值 A -> B -> A，同时改变标记
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // A -> B (标记 false -> true)
            boolean success1 = markableRef.compareAndSet("A", "B", false, true);
            System.out.println("[线程2] CAS(A->B, mark false->true): " + success1);

            // B -> A (标记保持 true)
            boolean success2 = markableRef.compareAndSet("B", "A", true, true);
            System.out.println("[线程2] CAS(B->A, mark true->true): " + success2);
            System.out.println("[线程2] 已完成 A->B->A");
        }, "线程2");

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println();
    }

    // ==================== 4. 总结对比 ====================

    static void summaryDemo() {
        System.out.println("========== 4. 总结对比 ==========");
        System.out.println();
        System.out.println("┌───────────────────────────┬──────────────────────────────────────────┐");
        System.out.println("│ 解决方案                   │ 特点                                      │");
        System.out.println("├───────────────────────────┼──────────────────────────────────────────┤");
        System.out.println("│ AtomicReference            │ 无法检测 ABA 问题                          │");
        System.out.println("│ AtomicStampedReference     │ 版本号方案，能检测修改次数                    │");
        System.out.println("│ AtomicMarkableReference    │ 标记位方案，只关心是否被修改过                 │");
        System.out.println("└───────────────────────────┴──────────────────────────────────────────┘");
        System.out.println();
        System.out.println("适用场景：");
        System.out.println("  - AtomicStampedReference：需要精确追踪修改次数（如库存管理、账户余额）");
        System.out.println("  - AtomicMarkableReference：只需知道是否被修改过（如节点标记删除）");
    }

    // ==================== main ====================

    public static void main(String[] args) throws InterruptedException {
        abaProblemDemo();
        stampedReferenceDemo();
        markableReferenceDemo();
        summaryDemo();
    }
}
