# Day05：CAS 与 AQS 原理

本目录用于存放「第 5 天」的学习资料、实验代码与当天笔记产出。

## 今日计划（来自总计划）

- [ ] 学习 CAS 原理（Compare And Swap）及底层实现
- [ ] 理解 CAS 三大问题（ABA、循环开销、单变量原子性）
- [ ] 掌握 Atomic 原子类家族的使用与原理
- [ ] 深入学习 AQS（AbstractQueuedSynchronizer）核心设计
- [ ] 分析 ReentrantLock 源码（公平锁 vs 非公平锁）
- [ ] 了解 ReentrantReadWriteLock 读写锁原理
- [ ] 学习 CountDownLatch/Semaphore/CyclicBarrier 的 AQS 实现
- [ ] 了解 StampedLock 乐观读锁
- [ ] 编写实验代码验证各知识点
- [ ] 写学习笔记，总结 CAS 与 AQS 原理

## 📚 推荐学习路径

**🎯 最佳学习方式**：直接使用 `00-教学课件_CAS与AQS原理详解.md`（导师定制，图文并茂，Mermaid 流程图辅助理解）

1. **第一步**：阅读 `00-教学课件_CAS与AQS原理详解.md`（核心课件，必读）
2. **第二步**：运行 `code/` 目录下的实验代码，观察 CAS 和锁的行为
3. **第三步**：完成 `03-实验与练习.md` 中的 4 个实验
4. **第四步**：在 `02-学习笔记.md` 中记录你的理解和思考

## 目录说明

### 核心学习资料（必读）
- **`00-教学课件_CAS与AQS原理详解.md`**：⭐ **导师定制课件**，Mermaid 图表辅助，详细讲解 CAS 与 AQS 原理（**推荐优先阅读**）
  - CAS 原理与底层实现
  - Atomic 原子类家族
  - AQS 核心设计与源码分析
  - ReentrantLock / ReentrantReadWriteLock 源码剖析
  - CountDownLatch / Semaphore / CyclicBarrier 实现原理
  - StampedLock 乐观读锁
  - 面试高频问题（10+ 题）

### 实践与产出
- `02-学习笔记.md`：学习笔记模板（与课件内容对齐）
- `03-实验与练习.md`：实验任务和验收标准
  - 实验A：CAS 操作与 ABA 问题复现
  - 实验B：手写一个基于 AQS 的自定义锁
  - 实验C：ReentrantLock 公平锁 vs 非公平锁性能对比
  - 实验D：LongAdder vs AtomicLong 高并发性能对比
- `code/`：可运行的实验代码与运行命令
  - `CASDemo.java`：CAS 基本操作演示
  - `ABADemo.java`：ABA 问题复现与解决
  - `CustomLock.java`：基于 AQS 的自定义锁
  - `ReentrantLockDemo.java`：公平锁 vs 非公平锁对比
  - `LongAdderVsAtomicDemo.java`：LongAdder vs AtomicLong 性能对比
  - `README.md`：代码运行说明

## 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `README.md` | 学习目标、文件清单 | ✅ |
| `00-教学课件_CAS与AQS原理详解.md` | 核心课件（Mermaid 图表） | ✅ |
| `02-学习笔记.md` | 学习笔记模板 | ✅ |
| `03-实验与练习.md` | 实验任务与验收标准 | ✅ |
| `code/README.md` | 代码运行说明 | ✅ |
| `code/CASDemo.java` | CAS 基本操作演示 | ✅ |
| `code/ABADemo.java` | ABA 问题复现与解决 | ✅ |
| `code/CustomLock.java` | 基于 AQS 的自定义锁 | ✅ |
| `code/ReentrantLockDemo.java` | 公平锁 vs 非公平锁 | ✅ |
| `code/LongAdderVsAtomicDemo.java` | LongAdder vs AtomicLong | ✅ |
