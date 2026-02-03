import java.util.ArrayList;
import java.util.List;

/**
 * 实验B：制造 Java 堆 OOM
 *
 * 运行建议：
 * -Xms64m -Xmx64m
 *
 * 说明：
 * 通过不断分配并持有 byte[]，让对象无法被回收，最终触发：
 * java.lang.OutOfMemoryError: Java heap space
 */
public class HeapOomDemo {
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        System.out.println("PID=" + ProcessHandle.current().pid());

        List<byte[]> holder = new ArrayList<>();
        int chunkMb = 2;
        long count = 0;

        try {
            while (true) {
                holder.add(new byte[chunkMb * _1MB]);
                count++;

                if (count % 10 == 0) {
                    System.out.println("Allocated ~" + (count * chunkMb) + "MB so far, holder.size=" + holder.size());
                }
            }
        } catch (OutOfMemoryError oom) {
            System.out.println("OOM after allocated ~" + (count * chunkMb) + "MB, holder.size=" + holder.size());
            throw oom;
        }
    }
}

