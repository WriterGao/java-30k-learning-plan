# Day06：线程池深入

本目录用于存放「第 6 天」的学习资料、实验代码与当天笔记产出。

## 今日计划（来自总计划）

- [ ] 理解线程池的设计思想（资源复用、控制并发、管理生命周期）
- [ ] 掌握 ThreadPoolExecutor 的 7 个核心参数
- [ ] 理解线程池的工作流程（任务提交→核心线程→队列→最大线程→拒绝）
- [ ] 掌握 4 种拒绝策略的原理与适用场景
- [ ] 了解常用工作队列（ArrayBlockingQueue/LinkedBlockingQueue/SynchronousQueue/PriorityBlockingQueue/DelayQueue）
- [ ] 理解 Executors 工厂方法的陷阱（阿里规范禁止使用的原因）
- [ ] 掌握线程池的 5 种状态与生命周期转换
- [ ] 理解 execute vs submit 的区别
- [ ] 深入分析线程池核心源码（ctl 字段、execute/addWorker/runWorker/getTask）
- [ ] 学习线程池最佳实践（IO 密集 vs CPU 密集场景的线程数设置）
- [ ] 了解 ScheduledThreadPoolExecutor 定时线程池
- [ ] 理解 ForkJoinPool 工作窃取算法
- [ ] 手写简易线程池，理解核心原理
- [ ] 编写实验代码验证各知识点
- [ ] 写学习笔记，总结线程池原理

## 📚 推荐学习路径

**🎯 最佳学习方式**：直接使用 `00-教学课件_线程池原理与实战详解.md`（导师定制，图文并茂，Mermaid 流程图辅助理解）

1. **第一步**：阅读 `00-教学课件_线程池原理与实战详解.md`（核心课件，必读）
2. **第二步**：运行 `code/` 目录下的实验代码，观察线程池的工作行为
3. **第三步**：完成 `03-实验与练习.md` 中的 4 个实验
4. **第四步**：在 `02-学习笔记.md` 中记录你的理解和思考

## 目录说明

### 核心学习资料（必读）
- **`00-教学课件_线程池原理与实战详解.md`**：⭐ **导师定制课件**，Mermaid 图表辅助，详细讲解线程池原理（**推荐优先阅读**）
  - 为什么需要线程池
  - ThreadPoolExecutor 7 个核心参数详解
  - 线程池工作流程（Mermaid 流程图）
  - 4 种拒绝策略详解 + 自定义策略
  - 5 种常用阻塞队列对比
  - Executors 工厂方法与阿里规范禁用原因
  - ThreadPoolExecutor 源码深度分析（ctl 字段、5 种状态、execute/addWorker/runWorker/getTask）
  - ScheduledThreadPoolExecutor 定时任务线程池
  - ForkJoinPool 工作窃取算法
  - 线程池最佳实践（线程数配置、监控、优雅关闭、异常处理）
  - 手写简易线程池（完整实现）
  - 面试高频问题（12+ 题详细解答）

### 实践与产出
- `02-学习笔记.md`：学习笔记模板（与课件内容对齐）
- `03-实验与练习.md`：实验任务和验收标准
  - 实验A：观察不同参数配置的线程池行为
  - 实验B：4 种拒绝策略效果对比
  - 实验C：手写简易线程池
  - 实验D：ForkJoinPool 并行计算
- `code/`：可运行的实验代码与运行命令
  - `ThreadPoolDemo.java`：线程池基本使用和参数演示
  - `RejectPolicyDemo.java`：拒绝策略演示
  - `SimpleThreadPool.java`：手写简易线程池
  - `ForkJoinDemo.java`：ForkJoin 框架演示
  - `README.md`：代码运行说明

## 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `README.md` | 学习目标、文件清单 | ✅ |
| `00-教学课件_线程池原理与实战详解.md` | 核心课件（Mermaid 图表） | ✅ |
| `02-学习笔记.md` | 学习笔记模板 | ✅ |
| `03-实验与练习.md` | 实验任务与验收标准 | ✅ |
| `code/README.md` | 代码运行说明 | ✅ |
| `code/ThreadPoolDemo.java` | 线程池基本使用和参数演示 | ✅ |
| `code/RejectPolicyDemo.java` | 拒绝策略演示 | ✅ |
| `code/SimpleThreadPool.java` | 手写简易线程池 | ✅ |
| `code/ForkJoinDemo.java` | ForkJoin 框架演示 | ✅ |
