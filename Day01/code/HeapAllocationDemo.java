import java.util.ArrayList;
import java.util.List;

/**
 * 实验A：堆分配与增长
 *
 * 目的：
 * - 通过不断分配对象并“持有引用”，让堆使用量持续上升
 * - 配合较小的 -Xmx，更容易观察到 GC 行为
 */
public class HeapAllocationDemo {
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws Exception {
        List<byte[]> holder = new ArrayList<>();

        System.out.println("PID=" + ProcessHandle.current().pid());
        printMem("start");

        // 每轮分配 4MB，默认 30 轮 = 120MB（你可通过 -Xmx 控制是否触发 GC/接近 OOM）
        int rounds = 30;
        int chunkMb = 4;

        for (int i = 1; i <= rounds; i++) {
            holder.add(new byte[chunkMb * _1MB]);
            printMem("after alloc round " + i + " (+" + chunkMb + "MB)");
            Thread.sleep(300);
        }

        System.out.println("Allocated and held " + (rounds * chunkMb) + "MB.");
        System.out.println("Now releasing half of references to allow GC...");

        for (int i = 0; i < holder.size() / 2; i++) {
            holder.set(i, null);
        }

        printMem("after nulling half");
        System.out.println("You may trigger GC in VisualVM/jvisualvm and observe heap change.");

        // 保持进程存活，方便观察
        Thread.sleep(60_000);
        printMem("end");
    }

    private static void printMem(String tag) {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;
        System.out.printf("[%s] used=%dMB, total=%dMB, free=%dMB%n",
                tag, used / _1MB, total / _1MB, free / _1MB);
    }
}

