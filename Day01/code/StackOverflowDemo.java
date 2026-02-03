/**
 * 实验C：栈溢出（StackOverflowError）
 *
 * 运行建议：
 * -Xss256k  （把线程栈调小，更容易触发）
 */
public class StackOverflowDemo {
    private static long depth = 0;

    public static void main(String[] args) {
        System.out.println("PID=" + ProcessHandle.current().pid());
        try {
            recurse();
        } catch (StackOverflowError e) {
            System.out.println("Stack overflow at depth=" + depth);
            throw e;
        }
    }

    private static void recurse() {
        depth++;
        // 增加一点栈帧压力（局部变量）
        long a = depth;
        long b = a + 1;
        long c = b + 1;
        if ((c & 0xFFFF) == 0) {
            System.out.println("depth=" + depth);
        }
        recurse();
    }
}

