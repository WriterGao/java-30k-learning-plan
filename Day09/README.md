# Day09：ConcurrentHashMap 源码分析

本目录用于存放「第 9 天」的学习资料、实验代码与当天笔记产出。

## 今日计划（来自总计划）

- [ ] 理解 HashMap 线程不安全的根因（扩容死链、数据覆盖）
- [ ] 了解 Hashtable 和 Collections.synchronizedMap 的局限性
- [ ] 掌握 JDK 7 ConcurrentHashMap 的 Segment 分段锁设计
- [ ] 深入分析 JDK 8 ConcurrentHashMap 的 CAS + synchronized 实现
- [ ] 逐行分析 put()、get()、transfer() 等核心方法源码
- [ ] 理解 sizeCtl 的多重含义（未初始化/初始化中/扩容阈值/扩容标识）
- [ ] 理解多线程协助扩容机制（ForwardingNode + stride 分段迁移）
- [ ] 掌握 size() / sumCount() 的 LongAdder 分段计数思想
- [ ] 对比 HashMap vs Hashtable vs ConcurrentHashMap
- [ ] 了解常见使用陷阱（复合操作非原子、size 不精确、null 不允许）
- [ ] 编写实验代码验证各知识点
- [ ] 写学习笔记，总结 ConcurrentHashMap 源码分析

## 推荐学习路径

**最佳学习方式**：直接使用 `00-教学课件_ConcurrentHashMap源码深度分析.md`（导师定制，图文并茂，Mermaid 流程图辅助理解）

1. **第一步**：阅读 `00-教学课件_ConcurrentHashMap源码深度分析.md`（核心课件，必读）
2. **第二步**：运行 `code/` 目录下的实验代码，观察并发 Map 的行为和性能
3. **第三步**：完成 `03-实验与练习.md` 中的实验
4. **第四步**：在 `02-学习笔记.md` 中记录你的理解和思考

## 目录说明

### 核心学习资料（必读）
- **`00-教学课件_ConcurrentHashMap源码深度分析.md`**：⭐ **导师定制课件**，Mermaid 图表辅助，详细讲解 ConcurrentHashMap 源码（**推荐优先阅读**）
  - 为什么需要 ConcurrentHashMap
  - JDK 7 Segment 分段锁实现（数据结构 + put/get/size 流程）
  - JDK 8 CAS + synchronized 实现（重点最详细）
    - 核心字段：table/sizeCtl/baseCount/counterCells
    - sizeCtl 的多重含义
    - initTable() 源码分析
    - put() / putVal() 完整源码逐行分析 + 流程图
    - get() 源码分析（无锁读原理）
    - transfer() 多线程协助扩容机制
    - size() / sumCount() 的 LongAdder 思想
    - 红黑树操作（TreeBin/TreeNode）
  - JDK 7 vs JDK 8 全面对比
  - ConcurrentHashMap vs HashMap vs Hashtable 对比
  - 常见使用陷阱
  - 面试高频问题（15+ 题）

### 实践与产出
- `02-学习笔记.md`：学习笔记模板（与课件内容对齐）
- `03-实验与练习.md`：实验任务和验收标准
  - 实验A：ConcurrentHashMap vs HashMap 多线程安全性对比
  - 实验B：观察 ConcurrentHashMap 扩容过程
  - 实验C：性能对比测试
  - 实验D：复合操作陷阱演示
- `code/`：可运行的实验代码与运行命令
  - `ConcurrentHashMapDemo.java`：ConcurrentHashMap 核心操作演示
  - `CHMvsHashMapDemo.java`：ConcurrentHashMap vs HashMap 多线程安全性对比
  - `CHMPerformanceDemo.java`：性能对比测试
  - `README.md`：代码运行说明

## 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `README.md` | 学习目标、文件清单 | ✅ |
| `00-教学课件_ConcurrentHashMap源码深度分析.md` | 核心课件（Mermaid 图表，源码逐行分析） | ✅ |
| `02-学习笔记.md` | 学习笔记模板 | ✅ |
| `03-实验与练习.md` | 实验任务与验收标准 | ✅ |
| `code/README.md` | 代码运行说明 | ✅ |
| `code/ConcurrentHashMapDemo.java` | ConcurrentHashMap 核心操作演示 | ✅ |
| `code/CHMvsHashMapDemo.java` | ConcurrentHashMap vs HashMap 多线程安全性对比 | ✅ |
| `code/CHMPerformanceDemo.java` | 性能对比测试 | ✅ |
