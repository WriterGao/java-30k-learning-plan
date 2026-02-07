# Day03 实验代码说明

本目录提供第 3 天的可运行实验代码，用于复现和排查"内存泄漏/死锁/CPU飙高"等常见线上问题。

## 代码清单

| 文件 | 说明 | 对应实验 |
|------|------|---------|
| `MemoryLeakDemo.java` | 模拟内存泄漏场景 | 实验B：jmap + MAT 分析 |
| `DeadLockDemo.java` | 模拟线程死锁场景 | 实验C：jstack 排查死锁 |
| `CPUHighDemo.java` | 模拟 CPU 飙高场景 | 实验A：jstat 监控 + top 定位 |

## 编译方式

```bash
cd Day03/code
javac *.java
```

## 运行方式

### 1. 内存泄漏演示

```bash
# 设置较小的堆内存，加速泄漏过程
java -Xms128m -Xmx128m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=./heap_leak.hprof \
     -verbose:gc \
     MemoryLeakDemo
```

**观察要点**：
- Full GC 频率逐渐增加
- 每次 GC 后可用内存越来越少
- 最终抛出 `OutOfMemoryError: Java heap space`
- 生成的 `heap_leak.hprof` 可用 MAT 分析

### 2. 死锁演示

```bash
java DeadLockDemo
```

**排查方法**：
```bash
# 找到进程 PID
jps -l

# 打印线程堆栈，查看死锁信息
jstack <pid>

# 或者使用 jcmd
jcmd <pid> Thread.print
```

### 3. CPU 飙高演示

```bash
java CPUHighDemo
```

**排查方法**：
```bash
# 1. 找到 CPU 占用高的 Java 进程
top -c

# 2. 找到 CPU 占用高的线程（macOS 使用 ps）
ps -M -p <pid>    # macOS
ps -mp <pid> -o THREAD,tid,time   # Linux

# 3. 将线程 ID 转为 16 进制
printf "%x\n" <tid>

# 4. 在 jstack 输出中查找该线程
jstack <pid> | grep -A 30 "nid=0x<hex_tid>"
```

## 配合工具使用

- **jstat**：实时监控 GC 情况
- **jmap**：导出堆转储文件
- **jstack**：打印线程堆栈
- **MAT**：分析堆转储，定位内存泄漏
- **Arthas**：在线诊断神器

## 注意事项

- 运行内存泄漏和 CPU 飙高的代码时，注意及时终止（Ctrl+C）
- 堆转储文件可能较大，分析完毕后可删除
- 建议在测试/学习环境中运行，不要在生产环境操作
