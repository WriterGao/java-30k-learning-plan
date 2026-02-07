import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * ProducerConsumerDemo.java - 生产者消费者模式实现
 *
 * 本程序演示两种经典的生产者消费者实现：
 * 1. 使用 wait/notify 实现（手动同步）
 * 2. 使用 BlockingQueue 实现（推荐方式）
 *
 * 运行方式：
 *   javac ProducerConsumerDemo.java
 *   java ProducerConsumerDemo
 */
public class ProducerConsumerDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  生产者消费者模式实现");
        System.out.println("========================================\n");

        // 实现1：wait/notify 方式
        demonstrateWaitNotify();

        // 实现2：BlockingQueue 方式
        demonstrateBlockingQueue();

        System.out.println("\n========================================");
        System.out.println("  所有实验完成！");
        System.out.println("========================================");
    }

    // ========================================================
    // 实现1：使用 wait/notify
    // ========================================================
    private static void demonstrateWaitNotify() throws Exception {
        System.out.println("--- 实现1：使用 wait/notify ---\n");

        MessageQueue queue = new MessageQueue(5); // 缓冲区大小为 5
        final int TOTAL_MESSAGES = 10;

        // 生产者
        Thread producer1 = new Thread(() -> {
            for (int i = 1; i <= TOTAL_MESSAGES / 2; i++) {
                try {
                    String msg = "P1-消息" + i;
                    queue.put(msg);
                    System.out.println("  [生产者1] 生产: " + msg
                            + " | 队列大小: " + queue.size());
                    Thread.sleep(100); // 模拟生产耗时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "producer-1");

        Thread producer2 = new Thread(() -> {
            for (int i = 1; i <= TOTAL_MESSAGES / 2; i++) {
                try {
                    String msg = "P2-消息" + i;
                    queue.put(msg);
                    System.out.println("  [生产者2] 生产: " + msg
                            + " | 队列大小: " + queue.size());
                    Thread.sleep(150); // 模拟生产耗时（比P1慢）
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "producer-2");

        // 消费者
        Thread consumer = new Thread(() -> {
            int consumed = 0;
            while (consumed < TOTAL_MESSAGES) {
                try {
                    String msg = queue.take();
                    System.out.println("  [消费者] 消费: " + msg
                            + " | 队列大小: " + queue.size());
                    consumed++;
                    Thread.sleep(200); // 模拟消费耗时（比生产慢）
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "consumer");

        producer1.start();
        producer2.start();
        consumer.start();

        producer1.join();
        producer2.join();
        consumer.join();

        System.out.println("\n  ✅ wait/notify 实现完成\n");
    }

    /**
     * 基于 wait/notify 的消息队列
     * 核心要点：
     * 1. wait() 必须在 synchronized 块中调用
     * 2. wait() 会释放锁，notify() 不会立即释放锁
     * 3. 使用 while 而非 if 检查条件（防止虚假唤醒）
     * 4. 使用 notifyAll() 而非 notify()（防止信号丢失）
     */
    static class MessageQueue {
        private final Queue<String> queue = new LinkedList<>();
        private final int capacity;

        public MessageQueue(int capacity) {
            this.capacity = capacity;
        }

        /**
         * 生产消息（队列满时阻塞）
         */
        public synchronized void put(String message) throws InterruptedException {
            // 使用 while 而非 if（防止虚假唤醒 spurious wakeup）
            while (queue.size() >= capacity) {
                System.out.println("  [" + Thread.currentThread().getName()
                        + "] 队列已满，等待消费...");
                wait(); // 释放锁，进入 WAITING 状态
            }
            queue.offer(message);
            notifyAll(); // 唤醒等待的消费者
        }

        /**
         * 消费消息（队列空时阻塞）
         */
        public synchronized String take() throws InterruptedException {
            while (queue.isEmpty()) {
                System.out.println("  [" + Thread.currentThread().getName()
                        + "] 队列为空，等待生产...");
                wait(); // 释放锁，进入 WAITING 状态
            }
            String message = queue.poll();
            notifyAll(); // 唤醒等待的生产者
            return message;
        }

        public synchronized int size() {
            return queue.size();
        }
    }

    // ========================================================
    // 实现2：使用 BlockingQueue（推荐方式）
    // ========================================================
    private static void demonstrateBlockingQueue() throws Exception {
        System.out.println("--- 实现2：使用 BlockingQueue（推荐方式） ---\n");

        // ArrayBlockingQueue：有界阻塞队列
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
        final int TOTAL_MESSAGES = 10;
        final String POISON_PILL = "STOP"; // 毒丸模式，用于通知消费者停止

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= TOTAL_MESSAGES; i++) {
                    String msg = "BQ-消息" + i;
                    queue.put(msg); // 队列满时自动阻塞
                    System.out.println("  [BQ-生产者] 生产: " + msg
                            + " | 队列大小: " + queue.size());
                    Thread.sleep(100);
                }
                queue.put(POISON_PILL); // 放入毒丸
                System.out.println("  [BQ-生产者] 发送停止信号");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "bq-producer");

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String msg = queue.take(); // 队列空时自动阻塞
                    if (POISON_PILL.equals(msg)) {
                        System.out.println("  [BQ-消费者] 收到停止信号，退出");
                        break;
                    }
                    System.out.println("  [BQ-消费者] 消费: " + msg
                            + " | 队列大小: " + queue.size());
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "bq-consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("\n  ✅ BlockingQueue 实现完成");
        System.out.println("  优势: 代码更简洁，不需要手动 synchronized/wait/notify\n");
    }
}
