# Day08：HashMap 源码分析

本目录用于存放「第 8 天」的学习资料、实验代码与当天笔记产出。

## 今日计划（来自总计划）

- [ ] 深入理解 HashMap 底层数据结构（数组 + 链表 + 红黑树）
- [ ] 掌握 hash() 扰动函数的设计原理
- [ ] 逐行分析 put() 方法源码及完整流程
- [ ] 分析 get() 方法源码
- [ ] 理解 resize() 扩容机制（JDK 8 高低位链表拆分优化）
- [ ] 掌握链表转红黑树的条件与过程（treeifyBin）
- [ ] 理解红黑树基础（性质、旋转、插入调整）
- [ ] 对比 JDK 7 与 JDK 8 HashMap 的差异
- [ ] 分析 JDK 7 HashMap 多线程死循环问题
- [ ] 手写简化版 HashMap
- [ ] 编写实验代码验证各知识点
- [ ] 写学习笔记，总结 HashMap 源码核心要点

## 📚 推荐学习路径

**🎯 最佳学习方式**：直接使用 `00-教学课件_HashMap源码深度分析.md`（导师定制，图文并茂，Mermaid 流程图辅助理解）

1. **第一步**：阅读 `00-教学课件_HashMap源码深度分析.md`（核心课件，必读）
2. **第二步**：运行 `code/` 目录下的实验代码，观察 HashMap 的行为
3. **第三步**：完成 `03-实验与练习.md` 中的实验任务
4. **第四步**：在 `02-学习笔记.md` 中记录你的理解和思考

## 目录说明

### 核心学习资料（必读）
- **`00-教学课件_HashMap源码深度分析.md`**：⭐ **导师定制课件**，Mermaid 图表辅助，详细讲解 HashMap 源码（**推荐优先阅读**）
  - HashMap 数据结构（数组 + 链表 + 红黑树）
  - 核心常量与字段分析
  - hash() 扰动函数设计原理
  - put() / get() / remove() 方法源码逐行分析
  - resize() 扩容机制详解（JDK 8 优化）
  - 链表转红黑树过程（treeifyBin）
  - 红黑树基础与性质
  - JDK 7 vs JDK 8 对比
  - JDK 7 多线程死循环问题（详细图解）
  - HashMap 与其他 Map 对比
  - 手写简化版 HashMap
  - 面试高频问题（15+ 题）

### 实践与产出
- `02-学习笔记.md`：学习笔记模板（与课件内容对齐）
- `03-实验与练习.md`：实验任务和验收标准
  - 实验A：观察 HashMap 扩容过程
  - 实验B：验证红黑树转换
  - 实验C：手写简化版 HashMap
  - 实验D：HashMap 线程不安全复现
- `code/`：可运行的实验代码与运行命令
  - `HashMapDemo.java`：HashMap 核心操作演示
  - `HashMapResizeDemo.java`：扩容过程观察
  - `SimpleHashMap.java`：手写简化版 HashMap
  - `HashMapThreadUnsafeDemo.java`：线程不安全演示
  - `README.md`：代码运行说明

## 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `README.md` | 学习目标、文件清单 | ✅ |
| `00-教学课件_HashMap源码深度分析.md` | 核心课件（Mermaid 图表，1500+ 行） | ✅ |
| `02-学习笔记.md` | 学习笔记模板 | ✅ |
| `03-实验与练习.md` | 实验任务与验收标准 | ✅ |
| `code/README.md` | 代码运行说明 | ✅ |
| `code/HashMapDemo.java` | HashMap 核心操作演示 | ✅ |
| `code/HashMapResizeDemo.java` | 扩容过程观察 | ✅ |
| `code/SimpleHashMap.java` | 手写简化版 HashMap | ✅ |
| `code/HashMapThreadUnsafeDemo.java` | 线程不安全演示 | ✅ |
