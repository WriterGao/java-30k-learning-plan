# Day06 实验代码说明

本目录提供第 6 天的可运行实验代码，用于验证线程池工作流程、拒绝策略、手写简易线程池以及 ForkJoinPool 工作窃取算法等。

## 运行方式（通用）

在仓库根目录执行：

```bash
cd Day06/code
javac *.java
```

## 各实验运行命令

### 1. 线程池基本使用和参数演示

```bash
java ThreadPoolDemo
```

**观察要点**：
- 核心线程、非核心线程的创建时机
- 任务进入队列的行为
- 4 种拒绝策略的触发效果
- execute vs submit 的异常处理区别
- 自定义 ThreadFactory 的线程命名
- 线程池状态变化（shutdown/shutdownNow）

### 2. 拒绝策略专项演示

```bash
java RejectPolicyDemo
```

**观察要点**：
- 4 种内置拒绝策略的行为对比（AbortPolicy/CallerRunsPolicy/DiscardPolicy/DiscardOldestPolicy）
- CallerRunsPolicy 的反压（Back Pressure）效果
- 自定义拒绝策略：日志记录 + 统计、带超时重试入队
- 不同队列类型（有界/无界/SynchronousQueue）对拒绝策略触发时机的影响

### 3. 手写简易线程池

```bash
java SimpleThreadPool
```

**观察要点**：
- 线程池初始化时 Worker 线程的创建
- 任务提交到阻塞队列的过程
- Worker 线程从队列取任务执行的循环
- 线程池关闭时 Worker 的优雅退出
- 拒绝策略的触发（AbortPolicy 和 CallerRunsPolicy）

### 4. ForkJoinPool 工作窃取算法

```bash
java ForkJoinDemo
```

**观察要点**：
- 大任务的递归拆分过程
- fork/join 的调用时机
- 工作窃取的执行效率
- RecursiveTask（有返回值）vs RecursiveAction（无返回值）的用法
- ForkJoinPool 与单线程的性能对比
- 工作窃取次数统计

## 文件说明

| 文件 | 对应实验 | 说明 |
|------|---------|------|
| `ThreadPoolDemo.java` | 实验A | 线程池工作流程、参数演示、execute vs submit、shutdown 对比 |
| `RejectPolicyDemo.java` | 实验B | 4 种拒绝策略对比、自定义拒绝策略、队列类型影响 |
| `SimpleThreadPool.java` | 实验C | 手写简易线程池（核心线程 + 阻塞队列 + 拒绝策略） |
| `ForkJoinDemo.java` | 实验D | ForkJoin 递归拆分、工作窃取算法、性能对比 |

## 注意事项

- 所有代码使用 JDK 8+ 编译和运行
- ForkJoinDemo 的性能对比示例会分配较大数组（1 亿元素），需要充足的堆内存
- 如遇到内存不足，可以使用 `java -Xmx512m ForkJoinDemo` 运行
