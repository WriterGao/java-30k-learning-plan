# Day10 实验代码

## 文件说明

| 文件 | 说明 | 核心知识点 |
|------|------|-----------|
| `ArrayListDemo.java` | ArrayList 核心操作与 fail-fast 演示 | 扩容机制、随机访问、fail-fast、安全删除 |
| `LinkedListDemo.java` | LinkedList 操作与性能对比 | 双向链表、Deque、ArrayList vs LinkedList 性能测试 |
| `LRUCacheDemo.java` | 基于 LinkedHashMap 的 LRU 缓存 | accessOrder、removeEldestEntry、缓存淘汰 |
| `SimpleArrayList.java` | 手写简化版 ArrayList | 动态数组、扩容、System.arraycopy、fail-fast 迭代器 |

## 编译与运行

### 环境要求
- JDK 8 或以上版本

### 运行方式

```bash
# 进入代码目录
cd Day10/code

# 编译并运行 ArrayList 演示
javac ArrayListDemo.java && java ArrayListDemo

# 编译并运行 LinkedList 演示与性能对比
javac LinkedListDemo.java && java LinkedListDemo

# 编译并运行 LRU 缓存演示
javac LRUCacheDemo.java && java LRUCacheDemo

# 编译并运行手写 ArrayList
javac SimpleArrayList.java && java SimpleArrayList
```

## 预期输出说明

### ArrayListDemo
- 展示 ArrayList 的基本 CRUD 操作
- 展示扩容过程（通过反射观察内部数组长度）
- 展示 fail-fast 机制（触发 ConcurrentModificationException）
- 展示安全删除元素的多种方式

### LinkedListDemo
- 展示 LinkedList 的基本操作和 Deque 用法
- **性能对比测试**：ArrayList vs LinkedList 在尾部追加、头部插入、随机访问、遍历等场景的耗时比较

### LRUCacheDemo
- 展示 LinkedHashMap 的插入顺序和访问顺序
- 展示 LRU 缓存的淘汰行为
- 每步操作后打印缓存状态

### SimpleArrayList
- 展示手写 ArrayList 的所有核心功能
- add、remove、get、set 操作
- 自动扩容
- fail-fast 迭代器验证
