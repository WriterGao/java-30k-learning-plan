# Day05 实验代码说明

本目录提供第 5 天的可运行实验代码，用于验证 CAS 原理、ABA 问题、AQS 自定义锁、ReentrantLock 公平/非公平锁对比以及 LongAdder 高并发性能等。

## 运行方式（通用）

在仓库根目录执行：

```bash
cd Day05/code
javac *.java
```

## 各实验运行命令

### 1. CAS 基本操作演示

```bash
java CASDemo
```

**观察要点**：
- CAS 成功/失败的返回值
- AtomicInteger 的 compareAndSet/getAndIncrement 操作
- 多线程下 CAS 自旋的行为

### 2. ABA 问题复现与解决

```bash
java ABADemo
```

**观察要点**：
- 普通 AtomicReference 无法检测 ABA 问题
- AtomicStampedReference 通过版本号检测 ABA
- AtomicMarkableReference 通过标记位检测

### 3. 基于 AQS 的自定义锁

```bash
java CustomLock
```

**观察要点**：
- 自定义独占锁的 lock/unlock 行为
- 多线程竞争时的排队等待
- tryLock 的超时机制

### 4. ReentrantLock 公平锁 vs 非公平锁

```bash
java ReentrantLockDemo
```

**观察要点**：
- 公平锁的 FIFO 获取顺序
- 非公平锁的插队现象
- 两种模式下的吞吐量差异

### 5. LongAdder vs AtomicLong 性能对比

```bash
java LongAdderVsAtomicDemo
```

**观察要点**：
- 低并发下两者性能差异不大
- 高并发下 LongAdder 的明显优势
- 线程数与性能差异的关系

## 文件说明

| 文件 | 实验 | 说明 |
|------|------|------|
| `CASDemo.java` | 实验A | CAS 基本操作、AtomicInteger 使用 |
| `ABADemo.java` | 实验A | ABA 问题复现与 AtomicStampedReference 解决方案 |
| `CustomLock.java` | 实验B | 基于 AQS 的自定义独占锁 |
| `ReentrantLockDemo.java` | 实验C | 公平锁 vs 非公平锁性能与行为对比 |
| `LongAdderVsAtomicDemo.java` | 实验D | LongAdder vs AtomicLong 高并发性能对比 |
