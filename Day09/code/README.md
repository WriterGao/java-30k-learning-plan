# Day09 实验代码说明

本目录提供第 9 天的可运行实验代码，用于验证 ConcurrentHashMap 的线程安全性、并发操作 API、扩容过程观察以及与其他 Map 实现的性能对比。

## 运行方式（通用）

在仓库根目录执行：

```bash
cd Day09/code
javac *.java
```

## 各实验运行命令

### 1. ConcurrentHashMap 核心操作演示

```bash
java ConcurrentHashMapDemo
```

**观察要点**：
- 扩容过程观察（sizeCtl 变化、容量变化）
- ConcurrentHashMap 弱一致性迭代器（不抛 ConcurrentModificationException）
- JDK 8 原子操作 API（compute / merge / computeIfAbsent 等）
- size() 与 mappingCount() 在并发写入过程中的行为
- 复合操作陷阱演示（非原子 vs 原子操作）
- null 限制演示

### 2. ConcurrentHashMap vs HashMap 多线程安全性对比

```bash
java CHMvsHashMapDemo
```

**观察要点**：
- HashMap 多线程不安全（数据丢失或异常）
- Hashtable / synchronizedMap / ConcurrentHashMap 的正确性对比
- 非原子复合操作（get + put）vs 原子操作（compute）的差异
- 不同 Map 的迭代器行为对比（fail-fast vs 弱一致性）

### 3. 性能对比测试

```bash
java CHMPerformanceDemo
```

**观察要点**：
- 纯写入性能：ConcurrentHashMap vs Hashtable vs synchronizedMap
- 纯读取性能：ConcurrentHashMap 无锁读取的优势
- 混合读写性能（80% 读 + 20% 写）：最接近真实场景
- 不同线程数（1/4/8/16/32）下的性能变化趋势
- ConcurrentHashMap 在高并发下的加速比

> **注意**：性能测试结果受机器配置、JVM 版本、CPU 核心数等影响。建议多次运行取平均值。

## 文件说明

| 文件 | 实验 | 说明 |
|------|------|------|
| `ConcurrentHashMapDemo.java` | 实验B/D | ConcurrentHashMap 扩容、弱一致性、原子 API、陷阱演示 |
| `CHMvsHashMapDemo.java` | 实验A | HashMap vs ConcurrentHashMap 多线程安全性对比 |
| `CHMPerformanceDemo.java` | 实验C | Hashtable vs synchronizedMap vs ConcurrentHashMap 多维度性能对比 |
