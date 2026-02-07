# JVM 内存模型详细对比图（JDK 7 及以前 vs JDK 8 及以后）

---

## 一、JDK 7 及以前（永久代时代）

```mermaid
flowchart TB
  subgraph jvm["JVM 运行时数据区（JDK 7 及以前）"]
    subgraph private["线程私有（每个线程一份）"]
      PC["程序计数器<br/>PC Register<br/>当前字节码地址 · 唯一不会 OOM"]
      Stack["Java 虚拟机栈<br/>栈帧: 局部变量表、操作数栈<br/>动态链接、方法返回地址"]
      Native["本地方法栈<br/>Native 方法 C/C++<br/>HotSpot 中与 Java 栈合一"]
    end

    subgraph shared["线程共享"]
      subgraph heap["Java 堆 Heap · -Xms/-Xmx · OOM: heap space"]
        subgraph young["新生代 Young Gen · -Xmn/NewRatio/SurvivorRatio"]
          Eden["Eden 约80%<br/>新对象优先分配"]
          S0["From Survivor 约10%"]
          S1["To Survivor 约10%<br/>Minor GC 复制"]
        end
        Old["老年代 Old Gen<br/>长期存活/大对象/年龄15<br/>Major·Full GC"]
        subgraph perm["永久代 PermGen（方法区在堆内）"]
          PermContent["-XX:PermSize/MaxPermSize<br/>类元数据 · 运行时常量池<br/>静态变量 · JIT 代码<br/>OOM: PermGen space"]
        end
      end
    end

    Direct["直接内存 Direct Memory<br/>NIO · -XX:MaxDirectMemorySize"]
  end
```

---

## 二、JDK 8 及以后（元空间时代）

```mermaid
flowchart TB
  subgraph jvm8["JVM 运行时数据区（JDK 8 及以后）"]
    subgraph private8["线程私有（每个线程一份）"]
      PC8["程序计数器<br/>PC Register"]
      Stack8["Java 虚拟机栈<br/>栈帧: 局部变量表、操作数栈<br/>动态链接、返回地址"]
      Native8["本地方法栈<br/>与 JDK 7 相同"]
    end

    subgraph shared8["线程共享"]
      subgraph heap8["Java 堆 Heap · 仅新生代+老年代 · -Xms/-Xmx"]
        subgraph young8["新生代 Young Gen"]
          Eden8["Eden"]
          S08["From Survivor"]
          S18["To Survivor"]
        end
        Old8["老年代 Old Gen<br/>★ 静态变量在堆中"]
      end

      subgraph meta["元空间 Metaspace（本地内存/堆外）"]
        MetaContent["-XX:MetaspaceSize/MaxMetaspaceSize<br/>类元数据 · 运行时常量池<br/>静态变量已迁到堆 · 按需扩展<br/>OOM: Metaspace"]
      end
    end

    Direct8["直接内存 Direct Memory<br/>NIO · -XX:MaxDirectMemorySize"]
  end
```

---

## 三、核心差异对照（一图看清）

```mermaid
flowchart LR
  subgraph jdk7["JDK 7 及以前"]
    subgraph heap7["Java 堆"]
      Y7["新生代<br/>Eden+S0+S1"]
      O7["老年代"]
      P7["永久代 PermGen<br/>方法区在堆内<br/>类+常量池+静态变量"]
    end
    param7["-XX:MaxPermSize<br/>OOM: PermGen space"]
  end

  subgraph jdk8["JDK 8 及以后"]
    subgraph heap8only["Java 堆"]
      Y8["新生代<br/>Eden+S0+S1"]
      O8["老年代<br/>静态变量在堆"]
    end
    subgraph meta8["本地内存"]
      M8["元空间 Metaspace<br/>仅类元数据、常量池"]
    end
    param8["-XX:MaxMetaspaceSize<br/>OOM: Metaspace"]
  end

  jdk7 --> jdk8
```

---

## 四、栈帧结构（线程私有，两版本相同）

```mermaid
flowchart TB
  subgraph frame["栈帧 Stack Frame"]
    Local["局部变量表 Local Variables<br/>slot0: this/参数 · slot1,2..: 参数、局部变量"]
    Operand["操作数栈 Operand Stack<br/>字节码指令操作数入栈/出栈"]
    Link["动态链接 Dynamic Linking<br/>指向运行时常量池中该方法引用"]
    Return["方法返回地址 Return Address<br/>恢复 PC，回到调用方"]
  end
  Local --> Operand --> Link --> Return
```

---

## 五、对象在堆中的走向（两版本相同）

```mermaid
flowchart TB
  New["new Object()"]
  Eden["Eden 区<br/>优先分配 · Eden 满则 Minor GC"]
  From["From Survivor"]
  To["To Survivor<br/>复制算法 · 年龄+1 · 默认15晋升"]
  Old["老年代<br/>Major/Full GC"]

  New --> Eden
  Eden -->|存活| From
  From <--> To
  To -->|年龄达阈值/大对象/动态年龄| Old
```

---

## 六、参数速查

| 区域       | JDK 7 及以前              | JDK 8 及以后                 |
|------------|---------------------------|------------------------------|
| 堆         | -Xms, -Xmx, -Xmn         | 同左                          |
| 方法区实现 | -XX:PermSize, -XX:MaxPermSize | -XX:MetaspaceSize, -XX:MaxMetaspaceSize |
| 栈         | -Xss                      | 同左                          |
| 直接内存   | -XX:MaxDirectMemorySize   | 同左                          |

---

以上为更详细的 JVM 内存模型对比图，便于区分 JDK 8 前后差异。  
在支持 Mermaid 的编辑器（如 VS Code 装 Mermaid 插件、Typora、GitHub 等）中预览即可看到自动对齐的流程图。
