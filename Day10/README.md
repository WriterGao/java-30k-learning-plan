# Day10：其他集合框架源码分析

本目录用于存放「第 10 天」的学习资料、实验代码与当天笔记产出。

## 今日计划（来自总计划）

- [ ] 掌握 Java 集合框架全景图（Collection/Map 两大体系）
- [ ] 深入分析 ArrayList 源码（扩容机制、随机访问、fail-fast）
- [ ] 深入分析 LinkedList 源码（双向链表、Deque 接口）
- [ ] 全面对比 ArrayList vs LinkedList（时间复杂度、内存、缓存友好性）
- [ ] 分析 HashSet 源码（基于 HashMap 实现）
- [ ] 分析 LinkedHashSet / LinkedHashMap 源码（维护顺序的双向链表、LRU 缓存实现）
- [ ] 分析 TreeMap / TreeSet 源码（红黑树实现、Comparable vs Comparator）
- [ ] 分析 PriorityQueue 源码（二叉堆实现、上浮下沉操作）
- [ ] 分析 ArrayDeque 源码（循环数组实现）
- [ ] 掌握 Collections 工具类核心方法
- [ ] 理解 fail-fast vs fail-safe 机制
- [ ] 编写实验代码验证各知识点
- [ ] 手写 ArrayList 和 LinkedList
- [ ] 实现基于 LinkedHashMap 的 LRU 缓存
- [ ] 写学习笔记，总结集合框架源码核心要点

## 📚 推荐学习路径

**🎯 最佳学习方式**：直接使用 `00-教学课件_集合框架源码详解.md`（导师定制，图文并茂，Mermaid 流程图辅助理解）

1. **第一步**：阅读 `00-教学课件_集合框架源码详解.md`（核心课件，必读）
2. **第二步**：运行 `code/` 目录下的实验代码，观察各集合的行为和性能
3. **第三步**：完成 `03-实验与练习.md` 中的实验任务
4. **第四步**：在 `02-学习笔记.md` 中记录你的理解和思考

## 目录说明

### 核心学习资料（必读）
- **`00-教学课件_集合框架源码详解.md`**：⭐ **导师定制课件**，Mermaid 图表辅助，详细讲解集合框架源码（**推荐优先阅读**）
  - Java 集合框架全景图（完整类图）
  - ArrayList 源码深度分析（扩容、移除、fail-fast）
  - LinkedList 源码深度分析（双向链表、Deque）
  - ArrayList vs LinkedList 全面对比
  - HashSet 源码分析（基于 HashMap）
  - LinkedHashSet / LinkedHashMap 源码分析（LRU 缓存实现）
  - TreeMap / TreeSet 源码分析（红黑树）
  - PriorityQueue 源码分析（二叉堆）
  - ArrayDeque 源码分析（循环数组）
  - Collections 工具类
  - 集合选型指南（决策流程图）
  - fail-fast vs fail-safe 机制
  - 面试高频问题（12+ 题）

### 实践与产出
- `02-学习笔记.md`：学习笔记模板（与课件内容对齐）
- `03-实验与练习.md`：实验任务和验收标准
  - 实验A：ArrayList vs LinkedList 性能对比
  - 实验B：手写 ArrayList 和 LinkedList
  - 实验C：LinkedHashMap 实现 LRU 缓存
  - 实验D：fail-fast 机制验证
- `code/`：可运行的实验代码与运行命令
  - `ArrayListDemo.java`：ArrayList 核心操作与扩容演示
  - `LinkedListDemo.java`：LinkedList 操作与性能对比演示
  - `LRUCacheDemo.java`：基于 LinkedHashMap 的 LRU 缓存实现
  - `SimpleArrayList.java`：手写简化版 ArrayList
  - `README.md`：代码运行说明

## 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `README.md` | 学习目标、文件清单 | ✅ |
| `00-教学课件_集合框架源码详解.md` | 核心课件（Mermaid 图表） | ✅ |
| `02-学习笔记.md` | 学习笔记模板 | ✅ |
| `03-实验与练习.md` | 实验任务与验收标准 | ✅ |
| `code/README.md` | 代码运行说明 | ✅ |
| `code/ArrayListDemo.java` | ArrayList 核心操作与扩容演示 | ✅ |
| `code/LinkedListDemo.java` | LinkedList 操作与性能对比演示 | ✅ |
| `code/LRUCacheDemo.java` | 基于 LinkedHashMap 的 LRU 缓存实现 | ✅ |
| `code/SimpleArrayList.java` | 手写简化版 ArrayList | ✅ |
