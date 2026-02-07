# Day02 实验代码说明

本目录提供第 2 天的可运行实验代码，用于验证"GC 日志分析、引用类型行为、收集器对比"等现象。

## 文件清单

| 文件 | 用途 |
|-----|------|
| `GCLogDemo.java` | GC 日志观察、Full GC 模拟、收集器性能对比 |
| `ReferenceDemo.java` | 四种引用类型（强/软/弱/虚）行为演示 |

## 编译方式

```bash
cd Day02/code
javac *.java
```

## 运行示例

### GC 日志观察（配合不同收集器）

```bash
# 使用 Serial 收集器
java -Xms64m -Xmx64m -Xmn32m -XX:+UseSerialGC -XX:+PrintGCDetails GCLogDemo

# 使用 Parallel 收集器
java -Xms64m -Xmx64m -Xmn32m -XX:+UseParallelGC -XX:+PrintGCDetails GCLogDemo

# 使用 G1 收集器
java -Xms64m -Xmx64m -XX:+UseG1GC -XX:+PrintGCDetails GCLogDemo

# 模拟 Full GC
java -Xms30m -Xmx30m -Xmn10m -XX:+UseSerialGC -XX:+PrintGCDetails GCLogDemo fullgc

# 性能对比测试
java -Xms128m -Xmx128m -XX:+UseG1GC GCLogDemo perf
```

### 引用类型演示

```bash
# 设置较小堆内存以便观察软引用回收
java -Xms20m -Xmx20m -XX:+PrintGCDetails ReferenceDemo
```

## 注意事项

- JDK 9+ 的 GC 日志参数有变化，请参考 `03-实验与练习.md` 中的双版本命令
- `-XX:+PrintGCDetails` 在 JDK 9+ 中已废弃，改用 `-Xlog:gc*`
- CMS 收集器在 JDK 14 中已移除
- ZGC 需要 JDK 11+（部分平台需要 JDK 14+）

## 观察建议

- **GC 日志**：重点关注 GC 类型、回收前后内存变化、暂停时间
- **jstat 监控**（命令行替代 GUI）：
  ```bash
  jstat -gc <pid> 1000      # 每秒打印 GC 信息
  jstat -gcutil <pid> 1000  # 每秒打印 GC 使用率
  ```
