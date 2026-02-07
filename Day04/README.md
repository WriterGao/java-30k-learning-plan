# Day04：多线程基础

本目录用于存放「第 4 天」的学习资料、实验代码与当天笔记产出。

## 今日计划（来自总计划）

- [ ] 理解进程与线程的区别
- [ ] 掌握线程创建的 4 种方式（Thread/Runnable/Callable/线程池）
- [ ] 理解线程的 6 种状态与生命周期
- [ ] 掌握线程核心方法（start/run/sleep/yield/join/interrupt）
- [ ] 深入理解 synchronized 原理（对象头、Monitor、锁升级）
- [ ] 深入理解 volatile 原理（可见性、有序性、内存屏障）
- [ ] 理解 Java 内存模型 JMM
- [ ] 掌握 ThreadLocal 原理与使用
- [ ] 理解线程安全的实现方式
- [ ] 理解死锁的条件与排查方法
- [ ] 编写代码验证线程状态转换、synchronized、volatile、生产者消费者
- [ ] 写学习笔记，总结多线程基础

## 📚 推荐学习路径

**🎯 最佳学习方式**：直接使用 `00-教学课件_多线程基础详解.md`（导师定制，图文并茂，包含 Mermaid 图表）

1. **第一步**：阅读 `00-教学课件_多线程基础详解.md`（核心课件，必读）
2. **第二步**：运行 `code/` 目录下的实验代码，观察线程行为
3. **第三步**：完成 `03-实验与练习.md` 中的实验任务
4. **第四步**：在 `02-学习笔记.md` 中记录你的理解和思考

## 目录说明

### 核心学习资料（必读）
- **`00-教学课件_多线程基础详解.md`**：⭐ **导师定制课件**，图文并茂，详细讲解多线程基础（**推荐优先阅读**）
  - 包含 11 个章节的完整讲解
  - 包含 Mermaid 图表、代码示例、原理分析
  - 包含面试高频问题和检查清单

### 实践与产出
- `02-学习笔记.md`：学习笔记模板（与课件内容对齐）
- `03-实验与练习.md`：实验任务和验收标准（与课件实验一致）
- `code/`：可运行的实验代码与运行命令
  - `ThreadStateDemo.java`：线程状态转换观察
  - `SynchronizedDemo.java`：synchronized 锁机制演示
  - `VolatileDemo.java`：volatile 可见性验证
  - `ProducerConsumerDemo.java`：生产者消费者模式实现
  - `ThreadLocalDemo.java`：ThreadLocal 原理演示
  - `README.md`：代码运行说明

## 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `README.md` | 本文件，学习目标与文件清单 | ✅ |
| `00-教学课件_多线程基础详解.md` | 核心课件，多线程全面讲解 | ✅ |
| `02-学习笔记.md` | 学习笔记模板 | ✅ |
| `03-实验与练习.md` | 实验任务与验收标准 | ✅ |
| `code/README.md` | 代码运行说明 | ✅ |
| `code/ThreadStateDemo.java` | 线程状态转换演示 | ✅ |
| `code/SynchronizedDemo.java` | synchronized 锁机制演示 | ✅ |
| `code/VolatileDemo.java` | volatile 可见性验证 | ✅ |
| `code/ProducerConsumerDemo.java` | 生产者消费者模式 | ✅ |
| `code/ThreadLocalDemo.java` | ThreadLocal 演示 | ✅ |
