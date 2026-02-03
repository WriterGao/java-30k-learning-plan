# Day01 实验代码说明

本目录提供第 1 天的可运行实验代码，用于验证“堆分配/GC/堆 OOM/栈溢出”等现象。

## 运行方式（通用）

在仓库根目录执行：

```bash
cd Day01/code
javac *.java
```

运行示例（按需二选一/多选）：

```bash
# 实验A：堆分配与增长（建议配合较小的 -Xmx 观察 GC 更明显）
java -Xms128m -Xmx128m HeapAllocationDemo

# 实验B：堆 OOM（注意：会抛异常退出）
java -Xms64m -Xmx64m HeapOomDemo

# 实验C：栈溢出（注意：会抛异常退出）
java -Xss256k StackOverflowDemo
```

## 观察建议

- **VisualVM/jvisualvm**：连接进程后观察 Heap 曲线（必要时手动触发 GC 对比）
- **命令行替代**（若你不方便用 GUI）：
  - `jcmd <pid> GC.heap_info`
  - `jcmd <pid> VM.flags`
  - `jcmd <pid> GC.class_histogram`

