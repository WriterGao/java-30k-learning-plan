# Day04 实验代码说明

本目录提供第 4 天的可运行实验代码，用于验证"线程状态、synchronized、volatile、生产者消费者、ThreadLocal"等多线程核心知识点。

## 运行方式（通用）

在仓库根目录执行：

```bash
cd Day04/code
javac *.java
```

运行示例：

```bash
# 实验A：观察线程状态转换
java ThreadStateDemo

# 实验B：synchronized 锁机制演示
java SynchronizedDemo

# 实验C：volatile 可见性验证
java VolatileDemo

# 实验D：生产者消费者模式
java ProducerConsumerDemo

# 实验E：ThreadLocal 演示
java ThreadLocalDemo
```

## 代码文件说明

| 文件 | 说明 | 关键知识点 |
|------|------|-----------|
| `ThreadStateDemo.java` | 观察线程 6 种状态的转换 | NEW/RUNNABLE/BLOCKED/WAITING/TIMED_WAITING/TERMINATED |
| `SynchronizedDemo.java` | synchronized 锁机制演示 | 对象锁、类锁、可重入性、死锁演示与检测 |
| `VolatileDemo.java` | volatile 可见性与有序性验证 | 可见性问题复现、volatile 修复、非原子性验证 |
| `ProducerConsumerDemo.java` | 经典生产者消费者模式 | wait/notify、BlockingQueue 两种实现 |
| `ThreadLocalDemo.java` | ThreadLocal 原理与内存泄漏 | ThreadLocal 使用、内存泄漏复现、最佳实践 |

## 观察建议

- **jstack**：查看线程状态和堆栈信息
  ```bash
  # 获取进程ID
  jps
  # 查看线程状态
  jstack <pid>
  ```
- **jconsole**：图形化观察线程数量和状态
- **VisualVM**：监控线程、观察死锁
- **命令行替代**（若不方便用 GUI）：
  - `jcmd <pid> Thread.print`：打印线程信息
  - `jcmd <pid> VM.flags`：查看 VM 参数

## 注意事项

1. 部分实验（如死锁演示）会导致程序挂起，需要手动终止（Ctrl+C）
2. volatile 可见性实验可能因 JVM 优化而表现不同，多运行几次观察
3. 建议在实验过程中使用 `jstack` 实时观察线程状态
