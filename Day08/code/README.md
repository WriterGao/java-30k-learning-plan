# Day08 实验代码说明

本目录提供第 8 天的可运行实验代码，用于验证 HashMap 源码核心原理，包括基本操作、hash 分布、扩容行为、红黑树转换、线程安全性以及手写简化版 HashMap。

## 运行方式（通用）

在仓库根目录执行：

```bash
cd Day08/code
javac *.java
```

## 各实验运行命令

### 1. HashMap 核心操作演示

```bash
java HashMapDemo
```

**演示内容**：
- HashMap 基本操作（put/get/remove/遍历）
- null key 和 null value 的处理
- hash() 扰动函数效果验证
- 扩容过程观察（通过反射查看内部状态）
- hash 冲突演示（自定义 hashCode 制造冲突）
- 多线程安全性测试（HashMap vs ConcurrentHashMap）
- equals/hashCode 契约演示

**对应实验**：实验A、实验B

### 2. HashMap 扩容过程详细观察

```bash
java HashMapResizeDemo
```

**演示内容**：
- 扩容触发时机（精确到每一个put操作）
- JDK 8 高低位链表拆分验证（e.hash & oldCap）
- 扩容前后桶内结构对比
- 初始容量对扩容次数的影响
- tableSizeFor 实际容量验证
- 扩容对性能的影响（预设容量 vs 默认容量）

**对应实验**：实验A

### 3. 手写简化版 HashMap

```bash
java SimpleHashMap
```

**演示内容**：
- 自实现的 HashMap（数组 + 链表）
- put/get/remove 基本操作
- hash 冲突处理（链地址法，尾插法）
- JDK 8 高低位链表拆分扩容机制
- null key 支持
- 大量数据正确性和性能测试

**对应实验**：实验C

### 4. HashMap 线程不安全演示

```bash
java HashMapThreadUnsafeDemo
```

**演示内容**：
- 并发 put 数据丢失
- 并发 put + get 数据不一致
- ConcurrentModificationException（fail-fast 机制）
- 并发操作导致 size 不准确
- 线程安全方案对比（HashMap / Hashtable / synchronizedMap / ConcurrentHashMap）
- JDK 7 头插法死循环原理讲解

**对应实验**：实验D

## 文件说明

| 文件 | 对应实验 | 说明 |
|------|---------|------|
| `HashMapDemo.java` | 实验A/B | HashMap 核心操作、hash 分布、扩容观察、冲突演示 |
| `HashMapResizeDemo.java` | 实验A | 扩容过程详细观察（6个子实验） |
| `SimpleHashMap.java` | 实验C | 手写简化版 HashMap（数组 + 链表 + 高低位拆分扩容） |
| `HashMapThreadUnsafeDemo.java` | 实验D | 线程不安全场景演示（6个子实验） |

## 建议运行顺序

1. `HashMapDemo` → 熟悉 HashMap 基本操作和 hash 原理
2. `HashMapResizeDemo` → 深入理解扩容机制
3. `SimpleHashMap` → 通过手写加深理解
4. `HashMapThreadUnsafeDemo` → 理解线程安全问题

## 注意事项

- 多线程实验（`HashMapThreadUnsafeDemo`）的结果具有**不确定性**，每次运行可能不同
- 建议多次运行多线程实验，观察数据丢失的变化
- 反射操作依赖 JDK 内部实现，建议使用 JDK 8~17
- 如果使用 JDK 16+，可能需要添加 `--add-opens java.base/java.util=ALL-UNNAMED` 参数
