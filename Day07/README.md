# Day07：并发工具类

本目录用于存放「第 7 天」的学习资料、实验代码与当天笔记产出。

## 今日计划（来自总计划）

- [ ] 学习 CountDownLatch 原理与使用（AQS 共享模式）
- [ ] 学习 CyclicBarrier 原理与使用（循环栅栏）
- [ ] 学习 Semaphore 原理与使用（信号量、限流）
- [ ] 学习 Exchanger 原理与使用
- [ ] 学习 Phaser 原理与使用（JDK 7+）
- [ ] 掌握 CompletableFuture 异步编程（链式调用、异常处理、多任务组合）
- [ ] 学习 Fork/Join 框架（分治思想、工作窃取算法）
- [ ] 理解 BlockingQueue 家族（6 种实现及适用场景）
- [ ] 理解 CopyOnWriteArrayList / CopyOnWriteArraySet 原理
- [ ] 完成第 1 周知识总结与面试高频问题
- [ ] 编写实验代码验证各知识点
- [ ] 写学习笔记，总结并发工具类原理

## 📚 推荐学习路径

**🎯 最佳学习方式**：直接使用 `00-教学课件_并发工具类详解.md`（导师定制，图文并茂，Mermaid 流程图辅助理解）

1. **第一步**：阅读 `00-教学课件_并发工具类详解.md`（核心课件，必读）
2. **第二步**：运行 `code/` 目录下的实验代码，观察并发工具类的行为
3. **第三步**：完成 `03-实验与练习.md` 中的 4 个实验
4. **第四步**：在 `02-学习笔记.md` 中记录你的理解和思考

## 目录说明

### 核心学习资料（必读）
- **`00-教学课件_并发工具类详解.md`**：⭐ **导师定制课件**，Mermaid 图表辅助，详细讲解并发工具类（**推荐优先阅读**）
  - CountDownLatch 原理与使用（AQS 共享模式）
  - CyclicBarrier 原理与使用（循环栅栏）
  - Semaphore 原理与使用（信号量、限流）
  - Exchanger / Phaser 原理与使用
  - CompletableFuture 异步编程详解（重点章节）
  - Fork/Join 框架（分治思想、工作窃取）
  - BlockingQueue 家族（6 种实现）
  - CopyOnWriteArrayList / CopyOnWriteArraySet
  - 并发工具选型指南
  - 第 1 周总结（JVM + 并发知识体系回顾）
  - 面试高频问题（12+ 题详细答案）

### 实践与产出
- `02-学习笔记.md`：学习笔记模板（与课件内容对齐）
- `03-实验与练习.md`：实验任务和验收标准
  - 实验A：CountDownLatch + CyclicBarrier 对比实验
  - 实验B：Semaphore 实现连接池
  - 实验C：CompletableFuture 并行调用
  - 实验D：Fork/Join 大数组求和
- `code/`：可运行的实验代码与运行命令
  - `CountDownLatchDemo.java`：CountDownLatch 倒计时门闩演示
  - `CyclicBarrierDemo.java`：CyclicBarrier 循环栅栏演示
  - `SemaphoreDemo.java`：Semaphore 信号量/限流演示
  - `CompletableFutureDemo.java`：CompletableFuture 异步编程演示
  - `ForkJoinDemo.java`：Fork/Join 框架大数组并行求和演示
  - `ForkJoinSortDemo.java`：Fork/Join 框架归并排序演示
  - `README.md`：代码运行说明

## 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `README.md` | 学习目标、文件清单 | ✅ |
| `00-教学课件_并发工具类详解.md` | 核心课件（Mermaid 图表） | ✅ |
| `02-学习笔记.md` | 学习笔记模板 | ✅ |
| `03-实验与练习.md` | 实验任务与验收标准 | ✅ |
| `code/README.md` | 代码运行说明 | ✅ |
| `code/CountDownLatchDemo.java` | CountDownLatch 演示 | ✅ |
| `code/CyclicBarrierDemo.java` | CyclicBarrier 演示 | ✅ |
| `code/SemaphoreDemo.java` | Semaphore 演示 | ✅ |
| `code/CompletableFutureDemo.java` | CompletableFuture 演示 | ✅ |
| `code/ForkJoinDemo.java` | Fork/Join 大数组求和 | ✅ |
| `code/ForkJoinSortDemo.java` | Fork/Join 归并排序 | ✅ |
