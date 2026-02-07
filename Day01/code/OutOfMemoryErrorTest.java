import java.util.ArrayList;
import java.util.List;

public class OutOfMemoryErrorTest {
    public static void main(String[] args) {
        while (true) {
            new Thread(() -> {
                while (true) {
                    // 每个线程占用栈空间
                }
            }).start();
        }
    }

    
}