# Day07 实验代码说明

本目录提供第 7 天的可运行实验代码，用于验证 CountDownLatch、CyclicBarrier、Semaphore、CompletableFuture 以及 Fork/Join 框架等并发工具类。

## 运行方式（通用）

在仓库根目录执行：

```bash
cd Day07/code
javac *.java
```

## 各实验运行命令

### 1. CountDownLatch 倒计时门闩演示

```bash
java CountDownLatchDemo
```

**观察要点**：
- 多个子任务并行执行，主线程通过 `await()` 阻塞等待
- 子任务完成后调用 `countDown()` 减少计数
- 计数归零后主线程被唤醒，继续执行后续逻辑
- 模拟多服务启动、发令枪、超时等待等场景

### 2. CyclicBarrier 循环栅栏演示

```bash
java CyclicBarrierDemo
```

**观察要点**：
- 多线程到达栅栏点后互相等待（`await()`）
- 所有线程到齐后，先执行 barrierAction，再全部放行
- 栅栏可以复用（循环使用），适合分阶段并行计算
- 超时或中断会导致 BrokenBarrierException

### 3. Semaphore 信号量/限流演示

```bash
java SemaphoreDemo
```

**观察要点**：
- 限制同时访问某资源的线程数量
- `acquire()` 获取许可（可能阻塞），`release()` 释放许可
- 公平/非公平模式的差异
- `tryAcquire` 非阻塞获取

### 4. CompletableFuture 异步编程演示

```bash
java CompletableFutureDemo
```

**观察要点**：
- `supplyAsync` / `runAsync` 创建异步任务
- `thenApply` / `thenCompose` / `thenCombine` 链式调用
- `exceptionally` / `handle` / `whenComplete` 异常处理
- `allOf` / `anyOf` 多任务组合
- 模拟并行调用多个微服务接口

### 5. Fork/Join 框架大数组求和演示

```bash
java ForkJoinDemo
```

**观察要点**：
- RecursiveTask 有返回值的分治计算（数组求和）
- 串行 vs 并行的性能对比
- 不同数组规模下的加速比
- 工作窃取次数（stealCount）

### 6. Fork/Join 框架归并排序演示

```bash
java ForkJoinSortDemo
```

**观察要点**：
- RecursiveAction 无返回值的分治计算（归并排序）
- RecursiveTask 有返回值的分治计算（数组求和）
- Fork/Join 归并排序与 Arrays.sort 的性能对比
- 工作窃取算法：空闲线程从其他线程的双端队列尾部窃取任务

## 文件说明

| 文件 | 实验 | 说明 |
|------|------|------|
| `CountDownLatchDemo.java` | 实验A | CountDownLatch 多服务启动 / 发令枪 / 超时等待 |
| `CyclicBarrierDemo.java` | 实验A | CyclicBarrier 互相等待 / barrierAction / 循环复用 |
| `SemaphoreDemo.java` | 实验B | Semaphore 限流 / 停车场 / 公平vs非公平 |
| `CompletableFutureDemo.java` | 实验C | CompletableFuture 全面演示 |
| `ForkJoinDemo.java` | 实验D | Fork/Join 大数组并行求和 |
| `ForkJoinSortDemo.java` | 附加 | Fork/Join 归并排序 + 性能对比 |
