# 📚 Day01 教学课件：JVM内存模型详解

> **导师定制课件** | 原创内容，图文并茂，深入浅出  
> 本课件完全替代《深入理解Java虚拟机》第2章，提供更详细、更实用的讲解

---

## 📋 目录

1. [JVM运行时数据区全景](#1-jvm运行时数据区全景)
2. [程序计数器（PC Register）](#2-程序计数器pc-register)
3. [Java虚拟机栈（JVM Stack）](#3-java虚拟机栈jvm-stack)
4. [本地方法栈（Native Method Stack）](#4-本地方法栈native-method-stack)
5. [Java堆（Java Heap）](#5-java堆java-heap)
6. [方法区（Method Area）与元空间](#6-方法区method-area与元空间)
7. [运行时常量池](#7-运行时常量池)
8. [直接内存（Direct Memory）](#8-直接内存direct-memory)
9. [对象创建全过程](#9-对象创建全过程)
10. [实战：内存溢出异常分析](#10-实战内存溢出异常分析)
11. [总结与检查清单](#11-总结与检查清单)

---

## 1. JVM运行时数据区全景

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    JVM运行时数据区                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  程序计数器   │  │  Java虚拟机栈 │  │  本地方法栈   │      │
│  │ (线程私有)    │  │ (线程私有)    │  │ (线程私有)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Java堆 (线程共享)                         │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │  │
│  │  │ 新生代   │  │ 老年代   │  │ 永久代/   │          │  │
│  │  │ Young    │  │ Old      │  │ 元空间    │          │  │
│  │  │ Gen      │  │ Gen      │  │ Metaspace│          │  │
│  │  └──────────┘  └──────────┘  └──────────┘          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           方法区/元空间 (线程共享)                      │  │
│  │  - 类信息、常量、静态变量                               │  │
│  │  - 运行时常量池                                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           直接内存 (堆外内存)                            │  │
│  │  - NIO使用的Native内存                                  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 线程私有 vs 线程共享

| 区域 | 线程属性 | 生命周期 | 异常类型 |
|------|---------|---------|---------|
| **程序计数器** | 线程私有 | 与线程同生共死 | 无异常（唯一不会OOM的区域） |
| **Java虚拟机栈** | 线程私有 | 与线程同生共死 | StackOverflowError / OutOfMemoryError |
| **本地方法栈** | 线程私有 | 与线程同生共死 | StackOverflowError / OutOfMemoryError |
| **Java堆** | 线程共享 | JVM启动时创建 | OutOfMemoryError |
| **方法区** | 线程共享 | JVM启动时创建 | OutOfMemoryError |
| **直接内存** | 线程共享 | 不受JVM管理 | OutOfMemoryError |

### 1.3 为什么需要区分线程私有和共享？

**线程私有区域**：
- 每个线程都有自己独立的副本
- 保证多线程环境下数据隔离，避免相互干扰
- 例如：每个线程都有自己的栈，方法调用不会混乱

**线程共享区域**：
- 所有线程共享同一块内存
- 需要同步机制保证线程安全
- 例如：堆中创建的对象可以被所有线程访问

---

## 2. 程序计数器（PC Register）

### 2.1 作用与特点

```
┌─────────────────────────────────────┐
│  线程1的程序计数器                    │
│  PC = 0x0001 (指向当前字节码地址)     │
├─────────────────────────────────────┤
│  线程2的程序计数器                    │
│  PC = 0x0005 (指向当前字节码地址)     │
└─────────────────────────────────────┘
```

**核心作用**：
- 记录当前线程正在执行的**字节码指令地址**
- 线程切换时，保存当前执行位置，恢复时继续执行

**关键特点**：
1. ✅ **唯一不会发生OutOfMemoryError的区域**
2. ✅ **线程私有**：每个线程独立拥有
3. ✅ **执行Native方法时值为空（Undefined）**：因为Native方法不是Java字节码

### 2.2 为什么需要程序计数器？

**场景演示**：

```java
// 假设有两个线程同时执行这段代码
public void method() {
    int a = 1;        // 字节码地址：0x0001
    int b = 2;        // 字节码地址：0x0002
    int c = a + b;    // 字节码地址：0x0003
    System.out.println(c); // 字节码地址：0x0004
}
```

**多线程执行流程**：

```
时间线：
T1: 线程1执行到 0x0002，CPU时间片用完，切换到线程2
T2: 线程2执行到 0x0004，CPU时间片用完，切换回线程1
T3: 线程1从 0x0002 继续执行（程序计数器保存了位置）
```

**如果没有程序计数器**：
- 线程切换后，不知道从哪里继续执行
- 程序会执行错误或崩溃

### 2.3 面试要点

**Q: 为什么程序计数器不会发生OOM？**  
A: 程序计数器只存储一个地址值，大小固定（32位JVM是4字节，64位JVM是8字节），不需要动态分配内存，所以不会溢出。

---

## 3. Java虚拟机栈（JVM Stack）

### 3.1 栈的结构

```
┌─────────────────────────────────────┐
│         Java虚拟机栈 (线程私有)        │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │  栈帧3 (method3)            │   │ ← 栈顶
│  │  - 局部变量表                │   │
│  │  - 操作数栈                  │   │
│  │  - 动态链接                  │   │
│  │  - 方法返回地址              │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  栈帧2 (method2)            │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  栈帧1 (method1)            │   │ ← 栈底
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

### 3.2 栈帧（Stack Frame）详解

每个方法调用都会创建一个栈帧，包含：

#### 3.2.1 局部变量表（Local Variables）

```java
public void method() {
    int a = 1;      // 局部变量表[0] = 1
    long b = 2L;    // 局部变量表[1] = 2L
    String c = "hello"; // 局部变量表[2] = 引用地址
    // 局部变量表存储：基本类型值 + 对象引用
}
```

**局部变量表结构**：
```
┌──────┬──────────┬────────────┐
│ 索引 │   类型    │    值      │
├──────┼──────────┼────────────┤
│  0   │   int    │     1      │
│  1   │   long   │     2L     │
│  2   │  String  │  引用地址   │
└──────┴──────────┴────────────┘
```

#### 3.2.2 操作数栈（Operand Stack）

用于计算过程中的临时数据存储：

```java
int result = a + b;  // 计算过程：
// 1. 将a压入操作数栈
// 2. 将b压入操作数栈
// 3. 执行加法指令，弹出两个数，计算结果压入栈
// 4. 将结果弹出，存入局部变量表
```

**操作数栈执行过程**：
```
步骤1: [a]          ← 栈顶
步骤2: [a, b]       ← 栈顶
步骤3: [result]     ← 栈顶（a+b的结果）
步骤4: []           ← 结果已存入局部变量表
```

#### 3.2.3 动态链接（Dynamic Linking）

- 指向运行时常量池中该方法的引用
- 用于支持多态（运行时确定调用哪个方法）

#### 3.2.4 方法返回地址（Return Address）

- 记录方法调用前的PC值
- 方法返回时，恢复PC值，继续执行

### 3.3 方法调用示例

```java
public class StackDemo {
    public static void main(String[] args) {
        int x = 10;
        int y = 20;
        int result = add(x, y);  // 调用add方法
        System.out.println(result);
    }
    
    public static int add(int a, int b) {
        return a + b;
    }
}
```

**栈帧变化过程**：

```
1. main方法调用：
   ┌─────────────────┐
   │ main栈帧        │
   │ x=10, y=20      │
   └─────────────────┘

2. 调用add方法：
   ┌─────────────────┐
   │ add栈帧         │ ← 栈顶
   │ a=10, b=20      │
   └─────────────────┘
   ┌─────────────────┐
   │ main栈帧        │
   │ x=10, y=20      │
   └─────────────────┘

3. add方法返回后：
   ┌─────────────────┐
   │ main栈帧        │ ← 栈顶（add栈帧已销毁）
   │ result=30       │
   └─────────────────┘
```

### 3.4 栈溢出异常

#### 3.4.1 StackOverflowError

**触发条件**：栈深度超过虚拟机允许的最大深度

```java
public class StackOverflowDemo {
    public static void main(String[] args) {
        recursiveMethod(0);
    }
    
    public static void recursiveMethod(int count) {
        System.out.println("深度: " + count);
        recursiveMethod(count + 1);  // 无限递归
    }
}
```

**执行结果**：
```
深度: 0
深度: 1
...
深度: 11404
Exception in thread "main" java.lang.StackOverflowError
```

**栈帧过多导致溢出**：
```
┌─────────┐
│ 栈帧N   │ ← 栈顶（超过最大深度）
├─────────┤
│ 栈帧N-1 │
├─────────┤
│ ...     │
├─────────┤
│ 栈帧1   │
└─────────┘
```

#### 3.4.2 OutOfMemoryError

**触发条件**：无法申请到足够内存扩展栈

```java
public class StackOOM {
    public static void main(String[] args) {
        while (true) {
            new Thread(() -> {
                while (true) {
                    // 每个线程占用栈空间
                }
            }).start();
        }
    }
}
```

**JVM参数**：
```bash
-Xss2m  # 设置每个线程的栈大小为2MB
```

---

## 4. 本地方法栈（Native Method Stack）

### 4.1 作用与特点

**作用**：为Native方法（C/C++实现的方法）服务

**示例**：
```java
public class NativeDemo {
    public native void nativeMethod();  // Native方法声明
    
    static {
        System.loadLibrary("nativeLib");  // 加载本地库
    }
}
```

**与Java虚拟机栈的区别**：
- Java虚拟机栈：为Java方法服务
- 本地方法栈：为Native方法服务

**HotSpot虚拟机实现**：
- HotSpot将本地方法栈和Java虚拟机栈**合二为一**
- 通过栈帧类型区分Java方法和Native方法

---

## 5. Java堆（Java Heap）

### 5.1 堆的核心特点

```
┌─────────────────────────────────────────────┐
│            Java堆 (线程共享)                  │
│         GC的主要区域                         │
├─────────────────────────────────────────────┤
│                                             │
│  ┌───────────────────────────────────────┐ │
│  │         新生代 (Young Generation)      │ │
│  │  ┌──────────┐  ┌──────────┐          │ │
│  │  │  Eden区  │  │ Survivor │          │ │
│  │  │          │  │  区      │          │ │
│  │  └──────────┘  └──────────┘          │ │
│  │     (8/10)        (1/10)              │ │
│  └───────────────────────────────────────┘ │
│                                             │
│  ┌───────────────────────────────────────┐ │
│  │         老年代 (Old Generation)        │ │
│  │                                         │ │
│  │                                         │ │
│  └───────────────────────────────────────┘ │
│                                             │
└─────────────────────────────────────────────┘
```

**核心特点**：
1. ✅ **线程共享**：所有线程共享同一块堆内存
2. ✅ **GC主战场**：大部分对象在这里分配和回收
3. ✅ **可扩展**：通过 `-Xmx` 和 `-Xms` 控制大小
4. ✅ **物理上可不连续**：逻辑上连续即可

### 5.2 堆内存分区详解

#### 5.2.1 新生代（Young Generation）

**分区比例**（默认）：
- **Eden区**：80%
- **Survivor区**：20%（From Survivor 10% + To Survivor 10%）

**对象分配流程**：

```
1. 新对象创建
   ↓
2. 优先在Eden区分配
   ↓
3. Eden区满 → Minor GC
   ↓
4. 存活对象 → Survivor区
   ↓
5. 经过多次GC仍存活 → 老年代
```

**详细流程图**：

```
┌─────────┐
│ 新对象   │
└────┬────┘
     │
     ▼
┌─────────┐
│ Eden区  │ ← 新对象优先分配在这里
│ 对象1   │
│ 对象2   │
│ ...     │
└────┬────┘
     │ Eden区满
     ▼
┌─────────┐
│Minor GC │ ← 垃圾回收
└────┬────┘
     │
     ▼
┌─────────┐      ┌─────────┐
│From     │ ←──→ │To       │
│Survivor │      │Survivor │ ← 存活对象在两个Survivor区之间复制
└─────────┘      └─────────┘
     │
     │ 经过多次GC仍存活
     ▼
┌─────────┐
│ 老年代   │ ← 长期存活的对象
└─────────┘
```

#### 5.2.2 老年代（Old Generation）

**特点**：
- 存放长期存活的对象
- 触发 **Major GC / Full GC**
- GC频率低，但耗时长

**进入老年代的条件**：
1. **年龄达到阈值**：对象在Survivor区经过多次GC，年龄达到15（默认）
2. **大对象直接进入**：超过 `-XX:PretenureSizeThreshold` 的大对象
3. **动态年龄判断**：Survivor区中相同年龄对象大小超过Survivor区一半

### 5.3 堆内存参数

```bash
# 堆内存大小
-Xms512m          # 初始堆大小
-Xmx1024m         # 最大堆大小

# 新生代大小
-Xmn256m          # 新生代大小（Eden + Survivor）

# 比例设置
-XX:NewRatio=2    # 老年代:新生代 = 2:1
-XX:SurvivorRatio=8  # Eden:Survivor = 8:1

# 大对象阈值
-XX:PretenureSizeThreshold=1m  # 超过1MB的对象直接进入老年代
```

### 5.4 堆内存溢出（OutOfMemoryError）

**触发条件**：不断创建对象，且对象无法被GC回收

**示例代码**：

```java
import java.util.ArrayList;
import java.util.List;

public class HeapOOM {
    public static void main(String[] args) {
        List<Object> list = new ArrayList<>();
        while (true) {
            list.add(new Object());  // 不断创建对象
        }
    }
}
```

**运行参数**：
```bash
java -Xms10m -Xmx10m -XX:+HeapDumpOnOutOfMemoryError HeapOOM
```

**异常信息**：
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

**内存变化图**：

```
时间线：
T1: [Eden: 20%] [Old: 10%]  ← 正常
T2: [Eden: 80%] [Old: 15%]  ← 对象增多
T3: [Eden: 100%] → Minor GC → [Eden: 30%] [Old: 20%]
T4: [Eden: 100%] → Minor GC → [Eden: 30%] [Old: 30%]
...
Tn: [Eden: 100%] [Old: 100%] → OutOfMemoryError
```

---

## 6. 方法区（Method Area）与元空间

### 6.1 JDK版本差异

```
JDK 7及以前：
┌─────────────────────────────────┐
│         Java堆                   │
│  ┌──────────┐  ┌──────────┐    │
│  │ 新生代   │  │ 老年代   │    │
│  └──────────┘  └──────────┘    │
│  ┌──────────────────────────┐  │
│  │   永久代 (PermGen)        │  │ ← 方法区在堆中
│  │   - 类信息                │  │
│  │   - 常量池                │  │
│  │   - 静态变量              │  │
│  └──────────────────────────┘  │
└─────────────────────────────────┘

JDK 8及以后：
┌─────────────────────────────────┐
│         Java堆                   │
│  ┌──────────┐  ┌──────────┐    │
│  │ 新生代   │  │ 老年代   │    │
│  └──────────┘  └──────────┘    │
└─────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│   元空间 (Metaspace)             │ ← 方法区在本地内存
│   - 类信息                      │
│   - 常量池                      │
│   - 静态变量（在堆中）           │
└─────────────────────────────────┘
```

### 6.2 方法区存储内容

**存储内容**：
1. **类信息**：类的版本、字段、方法、接口等
2. **常量池**：编译期生成的字面量和符号引用
3. **静态变量**：
   - JDK 7及以前：在永久代
   - JDK 8及以后：在堆中（与类信息分离）
4. **即时编译器编译后的代码**

### 6.3 永久代 vs 元空间

| 特性 | 永久代（PermGen） | 元空间（Metaspace） |
|------|------------------|-------------------|
| **位置** | Java堆中 | 本地内存（堆外） |
| **大小限制** | 受 `-XX:MaxPermSize` 限制 | 受系统可用内存限制 |
| **GC** | Full GC时回收 | 元空间满时触发Full GC |
| **优势** | - | 不会导致永久代溢出，更灵活 |

### 6.4 方法区溢出

**JDK 7示例**（永久代溢出）：

```java
import java.util.ArrayList;
import java.util.List;

public class PermGenOOM {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        int i = 0;
        while (true) {
            // 动态生成类（使用CGLib等）
            list.add(String.valueOf(i++).intern());
        }
    }
}
```

**运行参数（JDK 7）**：
```bash
-XX:PermSize=10m -XX:MaxPermSize=10m
```

**异常信息**：
```
Exception in thread "main" java.lang.OutOfMemoryError: PermGen space
```

**JDK 8示例**（元空间溢出）：

```java
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

public class MetaspaceOOM {
    public static void main(String[] args) {
        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(OOMObject.class);
            enhancer.setUseCache(false);
            enhancer.setCallback((MethodInterceptor) (obj, method, args1, proxy) -> 
                proxy.invokeSuper(obj, args1));
            enhancer.create();  // 动态创建类
        }
    }
    
    static class OOMObject {}
}
```

**运行参数（JDK 8+）**：
```bash
-XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m
```

**异常信息**：
```
Exception in thread "main" java.lang.OutOfMemoryError: Metaspace
```

---

## 7. 运行时常量池

### 7.1 常量池的作用

**存储内容**：
- **字面量**：字符串、数字等
- **符号引用**：类和接口的全限定名、字段名和描述符、方法名和描述符

**示例**：

```java
public class ConstantPoolDemo {
    public static void main(String[] args) {
        String s1 = "hello";           // 字面量
        String s2 = "hello";           // 从常量池获取
        String s3 = new String("hello"); // 新建对象
        
        System.out.println(s1 == s2);  // true（同一对象）
        System.out.println(s1 == s3);   // false（不同对象）
    }
}
```

### 7.2 String.intern()方法

```java
String s1 = new String("hello");
String s2 = s1.intern();  // 将字符串放入常量池
String s3 = "hello";

System.out.println(s2 == s3);  // true
```

**intern()方法的作用**：
- 如果常量池中已存在该字符串，返回常量池中的引用
- 如果不存在，将字符串放入常量池，返回引用

---

## 8. 直接内存（Direct Memory）

### 8.1 什么是直接内存？

**定义**：NIO使用Native函数库直接分配堆外内存

**特点**：
- 不受Java堆大小限制
- 受本机总内存和处理器寻址空间限制
- 读写性能高（减少一次数据拷贝）

### 8.2 NIO与直接内存

```java
import java.nio.ByteBuffer;

public class DirectMemoryDemo {
    public static void main(String[] args) {
        // 分配直接内存
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024 * 1024);
        
        // 分配堆内存
        ByteBuffer heapBuffer = ByteBuffer.allocate(1024 * 1024);
        
        System.out.println("直接内存: " + directBuffer.isDirect());  // true
        System.out.println("堆内存: " + heapBuffer.isDirect());     // false
    }
}
```

**内存对比图**：

```
传统IO（堆内存）：
┌──────────┐      ┌──────────┐      ┌──────────┐
│ Java堆   │ ──→  │  内核    │ ──→  │  磁盘    │
│ Buffer   │      │  Buffer  │      │          │
└──────────┘      └──────────┘      └──────────┘
    (需要拷贝)        (需要拷贝)

NIO（直接内存）：
┌──────────┐                    ┌──────────┐
│ 直接内存  │ ────────────────→  │  磁盘    │
│ Buffer   │   (零拷贝)          │          │
└──────────┘                    └──────────┘
```

### 8.3 直接内存溢出

```java
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DirectMemoryOOM {
    public static void main(String[] args) {
        List<ByteBuffer> list = new ArrayList<>();
        while (true) {
            list.add(ByteBuffer.allocateDirect(1024 * 1024));  // 每次分配1MB
        }
    }
}
```

**运行参数**：
```bash
-XX:MaxDirectMemorySize=10m
```

**异常信息**：
```
Exception in thread "main" java.lang.OutOfMemoryError: Direct buffer memory
```

---

## 9. 对象创建全过程

### 9.1 对象创建的5个步骤

```
┌─────────────────────────────────────────────┐
│         对象创建完整流程                      │
├─────────────────────────────────────────────┤
│                                             │
│  1. 检查类是否已加载                         │
│     ↓                                       │
│  2. 分配内存                                 │
│     ├─ 指针碰撞（Bump the Pointer）          │
│     └─ 空闲列表（Free List）                 │
│     ↓                                       │
│  3. 内存空间初始化（零值）                    │
│     ↓                                       │
│  4. 设置对象头                               │
│     ├─ Mark Word                            │
│     ├─ 类型指针                              │
│     └─ 数组长度（如果是数组）                │
│     ↓                                       │
│  5. 执行init方法（构造函数）                  │
│                                             │
└─────────────────────────────────────────────┘
```

### 9.2 详细步骤解析

#### 步骤1：检查类是否已加载

```java
Object obj = new Object();
// JVM检查：
// 1. 常量池中是否有Object类的符号引用
// 2. 该类是否已被加载、解析、初始化
// 3. 如果没有，先执行类加载过程
```

#### 步骤2：分配内存

**方式1：指针碰撞（Bump the Pointer）**

```
堆内存（规整）：
┌─────┬─────┬─────┬─────┬─────┐
│已用 │已用 │空闲 │空闲 │空闲 │
└─────┴─────┴─────┴─────┴─────┘
              ↑
           指针位置
              
分配新对象后：
┌─────┬─────┬─────┬─────┬─────┐
│已用 │已用 │新对象│空闲 │空闲 │
└─────┴─────┴─────┴─────┴─────┘
              ↑
           指针后移
```

**方式2：空闲列表（Free List）**

```
堆内存（不规整）：
┌─────┬─────┬─────┬─────┬─────┐
│已用 │空闲 │已用 │空闲 │空闲 │
└─────┴─────┴─────┴─────┴─────┘
  ↑     ↑     ↑     ↑     ↑
空闲列表记录所有空闲区域

分配新对象：
从空闲列表中找到合适大小的空闲区域
```

**并发安全问题**：

```java
// 问题：多线程同时分配内存，指针可能冲突
// 解决1：CAS + 失败重试
// 解决2：TLAB（Thread Local Allocation Buffer）

// TLAB：为每个线程预先分配一小块内存
┌─────────────────────────────────┐
│         Eden区                   │
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │TLAB1 │ │TLAB2 │ │TLAB3 │    │
│  │线程1 │ │线程2 │ │线程3 │    │
│  └──────┘ └──────┘ └──────┘    │
└─────────────────────────────────┘
```

#### 步骤3：内存空间初始化

```java
// 将分配的内存空间初始化为零值
// 基本类型：0, false, null等
// 引用类型：null

class Example {
    int a;        // 初始化为 0
    boolean b;    // 初始化为 false
    String c;     // 初始化为 null
}
```

#### 步骤4：设置对象头

**对象头结构**：

```
┌─────────────────────────────────┐
│          对象头 (Header)          │
├─────────────────────────────────┤
│  Mark Word (8字节，64位)         │
│  - 哈希码                        │
│  - GC分代年龄                    │
│  - 锁状态标志                    │
│  - 偏向线程ID                    │
├─────────────────────────────────┤
│  类型指针 (8字节，64位)           │
│  - 指向类元数据                  │
├─────────────────────────────────┤
│  数组长度 (4字节，仅数组对象)     │
└─────────────────────────────────┘
```

**Mark Word详解**（64位JVM）：

```
┌─────────────────────────────────────┐
│  Mark Word (64位)                   │
├─────────────────────────────────────┤
│  无锁状态：                          │
│  [25位未用][31位hash][1位未用][4位年龄][1位偏向][2位锁标志=01] │
├─────────────────────────────────────┤
│  偏向锁：                            │
│  [54位线程ID][2位Epoch][1位未用][4位年龄][1位偏向][2位锁标志=01] │
├─────────────────────────────────────┤
│  轻量级锁：                          │
│  [62位指向锁记录的指针][2位锁标志=00] │
├─────────────────────────────────────┤
│  重量级锁：                          │
│  [62位指向monitor的指针][2位锁标志=10] │
├─────────────────────────────────────┤
│  GC标记：                            │
│  [62位未用][2位锁标志=11]            │
└─────────────────────────────────────┘
```

#### 步骤5：执行init方法

```java
public class Person {
    private String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;  // init方法中的代码
        this.age = age;    // init方法中的代码
    }
}

// 对象创建流程：
// 1-4步：JVM自动完成
// 第5步：执行构造函数中的代码（init方法）
```

### 9.3 对象内存布局

```
┌─────────────────────────────────┐
│        对象在堆中的布局           │
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐ │
│  │      对象头 (Header)       │ │
│  │  - Mark Word              │ │
│  │  - 类型指针                │ │
│  │  - 数组长度（如果是数组）  │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌───────────────────────────┐ │
│  │    实例数据 (Instance Data)│ │
│  │  - 字段1                  │ │
│  │  - 字段2                  │ │
│  │  - ...                    │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌───────────────────────────┐ │
│  │    对齐填充 (Padding)       │ │
│  │  (保证对象大小为8字节倍数)  │ │
│  └───────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

**示例**：

```java
public class ObjectLayout {
    private int a;        // 4字节
    private long b;       // 8字节
    private String c;     // 8字节（引用）
    
    // 对象头：16字节（Mark Word 8 + 类型指针 8）
    // 实例数据：20字节（4 + 8 + 8）
    // 对齐填充：4字节（使总大小为8的倍数）
    // 总大小：40字节
}
```

### 9.4 对象的访问定位

**方式1：句柄访问**

```
┌──────────────┐
│  reference   │
│  (句柄地址)   │
└──────┬───────┘
       │
       ▼
┌─────────────────────────────┐
│      句柄池                  │
│  ┌─────────────────────┐   │
│  │ 对象实例数据指针     │───┼──→ 对象实例数据
│  └─────────────────────┘   │
│  ┌─────────────────────┐   │
│  │ 类型数据指针         │───┼──→ 类元数据
│  └─────────────────────┘   │
└─────────────────────────────┘

优点：对象移动时只需修改句柄，reference不变
缺点：多一次间接访问
```

**方式2：直接指针访问（HotSpot采用）**

```
┌──────────────┐
│  reference   │
│  (直接指向)   │
└──────┬───────┘
       │
       ▼
┌─────────────────────────────┐
│      对象实例数据             │
│  ┌─────────────────────┐   │
│  │ 对象头               │   │
│  │  - Mark Word        │   │
│  │  - 类型指针 ─────────┼───┼──→ 类元数据
│  └─────────────────────┘   │
│  ┌─────────────────────┐   │
│  │ 实例数据             │   │
│  └─────────────────────┘   │
└─────────────────────────────┘

优点：访问速度快（少一次间接访问）
缺点：对象移动时需要修改reference
```

---

## 10. 实战：内存溢出异常分析

### 10.1 堆内存溢出实战

**代码**：`code/HeapOomDemo.java`

```java
import java.util.ArrayList;
import java.util.List;

/**
 * 堆内存溢出演示
 * JVM参数：-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError
 */
public class HeapOomDemo {
    static class OOMObject {
        // 每个对象约64KB
        private byte[] bytes = new byte[64 * 1024];
    }
    
    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<>();
        while (true) {
            list.add(new OOMObject());
        }
    }
}
```

**运行与分析**：

```bash
# 1. 编译
javac HeapOomDemo.java

# 2. 运行（设置堆内存为20MB）
java -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError HeapOomDemo

# 3. 观察输出
# Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
# Dumping heap to java_pid12345.hprof ...
```

**使用jvisualvm分析**：

1. 打开jvisualvm：`jvisualvm`
2. 连接到运行中的Java进程
3. 观察堆内存变化：
   ```
   时间 → 堆内存使用率
   T1  → 10%
   T2  → 30%
   T3  → 50%
   T4  → 70%
   T5  → 90%
   T6  → 100% → OOM
   ```
4. 查看堆转储文件（.hprof），分析哪些对象占用内存最多

### 10.2 栈溢出实战

**代码**：`code/StackOverflowDemo.java`

```java
/**
 * 栈溢出演示
 * JVM参数：-Xss128k（减小栈大小，更快触发溢出）
 */
public class StackOverflowDemo {
    private int stackLength = 1;
    
    public void stackLeak() {
        stackLength++;
        stackLeak();  // 无限递归
    }
    
    public static void main(String[] args) {
        StackOverflowDemo demo = new StackOverflowDemo();
        try {
            demo.stackLeak();
        } catch (Throwable e) {
            System.out.println("栈深度: " + demo.stackLength);
            throw e;
        }
    }
}
```

**运行结果**：

```
栈深度: 11404
Exception in thread "main" java.lang.StackOverflowError
	at StackOverflowDemo.stackLeak(StackOverflowDemo.java:7)
	at StackOverflowDemo.stackLeak(StackOverflowDemo.java:7)
	...
```

### 10.3 使用jvisualvm观察内存

**步骤**：

1. **启动jvisualvm**：
   ```bash
   jvisualvm
   ```

2. **运行你的Java程序**：
   ```bash
   java -Xms512m -Xmx512m YourProgram
   ```

3. **在jvisualvm中**：
   - 左侧选择你的Java进程
   - 点击"监视"标签
   - 观察：
     - 堆内存使用情况
     - 非堆内存使用情况
     - 线程数
     - 类加载数

4. **查看GC活动**：
   - 点击"Visual GC"插件（需要安装）
   - 实时观察：
     - Eden区、Survivor区、老年代的使用情况
     - GC频率和耗时

5. **生成堆转储**：
   - 右键进程 → "堆转储"
   - 分析哪些对象占用内存最多

**jvisualvm界面示例**：

```
┌─────────────────────────────────────┐
│  jvisualvm - 进程监控                │
├─────────────────────────────────────┤
│                                     │
│  堆内存使用:                        │
│  ████████████░░░░░░░░  60%          │
│  300MB / 512MB                      │
│                                     │
│  非堆内存使用:                      │
│  ████░░░░░░░░░░░░░░░░  20%          │
│  50MB / 250MB                       │
│                                     │
│  线程: 15                           │
│  类: 2,345                          │
│                                     │
│  GC活动:                            │
│  Minor GC: 12次                     │
│  Full GC: 2次                       │
│                                     │
└─────────────────────────────────────┘
```

### 10.4 创建大量对象观察GC

**代码**：`code/HeapAllocationDemo.java`

```java
import java.util.ArrayList;
import java.util.List;

/**
 * 创建大量对象，观察GC情况
 * JVM参数：
 * -Xms100m -Xmx100m
 * -XX:+PrintGCDetails
 * -XX:+PrintGCDateStamps
 * -Xloggc:gc.log
 */
public class HeapAllocationDemo {
    public static void main(String[] args) throws InterruptedException {
        List<byte[]> list = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            // 每次分配1MB
            byte[] bytes = new byte[1024 * 1024];
            list.add(bytes);
            
            Thread.sleep(100);  // 暂停100ms，便于观察
            
            if (i % 100 == 0) {
                System.out.println("已分配: " + (i + 1) + " MB");
            }
        }
    }
}
```

**GC日志分析**：

```
2024-01-01T10:00:00.123+0800: [GC (Allocation Failure) 
[PSYoungGen: 25600K->1024K(29696K)] 25600K->1024K(98304K), 0.0012345 secs]
```

**日志解读**：
- `PSYoungGen`：使用Parallel Scavenge收集器的新生代
- `25600K->1024K`：GC前25600KB，GC后1024KB
- `(29696K)`：新生代总大小
- `25600K->1024K(98304K)`：整个堆GC前后大小
- `0.0012345 secs`：GC耗时

---

## 11. 总结与检查清单

### 11.1 核心知识点总结

**JVM运行时数据区**：

```
线程私有：
├─ 程序计数器：记录字节码指令地址（唯一不会OOM）
├─ Java虚拟机栈：存储方法调用的栈帧
└─ 本地方法栈：为Native方法服务

线程共享：
├─ Java堆：对象实例和数组（GC主战场）
│  ├─ 新生代：Eden + Survivor
│  └─ 老年代：长期存活的对象
├─ 方法区/元空间：类信息、常量池
└─ 直接内存：NIO使用的堆外内存
```

**对象创建流程**：

```
1. 检查类是否已加载
2. 分配内存（指针碰撞/空闲列表）
3. 初始化零值
4. 设置对象头
5. 执行init方法
```

**内存溢出类型**：

| 区域 | 异常类型 | 触发条件 |
|------|---------|---------|
| 堆 | OutOfMemoryError | 对象过多，无法分配 |
| 栈 | StackOverflowError | 递归过深或栈帧过多 |
| 方法区 | OutOfMemoryError | 类加载过多（JDK 7: PermGen, JDK 8+: Metaspace） |
| 直接内存 | OutOfMemoryError | 直接内存使用超过限制 |

### 11.2 学习检查清单

完成Day01学习后，请确认你能：

#### 基础理解
- [ ] 能画出JVM运行时数据区的完整结构图
- [ ] 能说出每个区域的作用、线程私有/共享属性、生命周期
- [ ] 能解释为什么程序计数器不会发生OOM
- [ ] 能区分StackOverflowError和OutOfMemoryError的触发场景

#### 深入理解
- [ ] 能解释堆内存分代的原因和GC的关系
- [ ] 能说出方法区在不同JDK版本中的实现差异（永久代 vs 元空间）
- [ ] 能解释对象创建的完整流程（从new到可用）
- [ ] 能说出对象内存布局（对象头、实例数据、对齐填充）

#### 实战能力
- [ ] 能通过代码复现堆内存溢出
- [ ] 能通过代码复现栈溢出
- [ ] 能使用jvisualvm观察内存使用情况
- [ ] 能分析GC日志，找出内存问题
- [ ] 能配置JVM参数（-Xms、-Xmx、-Xss等）

### 11.3 面试高频问题

**Q1: JVM内存区域有哪些？哪些是线程私有的？**

**A**: JVM内存区域包括：
- 线程私有：程序计数器、Java虚拟机栈、本地方法栈
- 线程共享：Java堆、方法区/元空间、直接内存

**Q2: 堆和栈的区别？**

**A**: 
- 堆：线程共享，存储对象实例，GC主要区域，生命周期与JVM相同
- 栈：线程私有，存储方法调用的栈帧，方法结束即销毁，生命周期与线程相同

**Q3: 对象创建的过程？**

**A**: 
1. 检查类是否已加载
2. 分配内存（指针碰撞或空闲列表）
3. 初始化零值
4. 设置对象头（Mark Word、类型指针等）
5. 执行init方法（构造函数）

**Q4: 什么情况下会发生堆内存溢出？**

**A**: 
- 不断创建对象，且对象无法被GC回收
- 常见原因：内存泄漏、对象生命周期过长、堆内存设置过小

**Q5: 永久代和元空间的区别？**

**A**: 
- 永久代（JDK 7及以前）：在Java堆中，受-XX:MaxPermSize限制
- 元空间（JDK 8及以后）：在本地内存中，受系统可用内存限制，更灵活

### 11.4 下一步学习建议

完成Day01后，建议：

1. **复习巩固**：重新画一遍JVM内存结构图，确保理解每个区域
2. **实战练习**：运行所有实验代码，观察内存变化
3. **工具熟练**：熟练掌握jvisualvm的使用
4. **准备Day02**：预习GC算法（标记-清除、标记-复制、标记-整理等）

---

## 📚 附录：常用JVM参数速查

```bash
# 堆内存
-Xms512m              # 初始堆大小
-Xmx1024m             # 最大堆大小
-Xmn256m              # 新生代大小

# 栈内存
-Xss1m                # 每个线程的栈大小

# 方法区/元空间
-XX:PermSize=256m     # 永久代初始大小（JDK 7）
-XX:MaxPermSize=512m  # 永久代最大大小（JDK 7）
-XX:MetaspaceSize=256m    # 元空间初始大小（JDK 8+）
-XX:MaxMetaspaceSize=512m # 元空间最大大小（JDK 8+）

# GC日志
-XX:+PrintGCDetails           # 打印GC详细信息
-XX:+PrintGCDateStamps        # 打印GC时间戳
-Xloggc:gc.log                # GC日志文件
-XX:+HeapDumpOnOutOfMemoryError # OOM时生成堆转储
-XX:HeapDumpPath=./heap.hprof  # 堆转储文件路径

# 直接内存
-XX:MaxDirectMemorySize=128m   # 直接内存最大大小
```

---

**🎉 恭喜完成Day01学习！**

本课件提供了JVM内存模型的全面讲解，结合实验代码和工具使用，帮助你深入理解JVM内存管理机制。继续加油，向30K目标前进！
