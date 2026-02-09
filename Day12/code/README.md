# Day12 结构型设计模式实验代码

本目录包含 7 种结构型设计模式的完整示例代码。

## 文件说明

| 文件 | 说明 | 运行命令 |
|------|------|---------|
| `AdapterDemo.java` | 适配器模式演示（对象适配器） | `javac AdapterDemo.java && java AdapterDemo` |
| `BridgeDemo.java` | 桥接模式演示（形状×颜色） | `javac BridgeDemo.java && java BridgeDemo` |
| `CompositeDemo.java` | 组合模式演示（文件系统） | `javac CompositeDemo.java && java CompositeDemo` |
| `DecoratorDemo.java` | 装饰器模式演示（咖啡加料） | `javac DecoratorDemo.java && java DecoratorDemo` |
| `FacadeDemo.java` | 外观模式演示（家庭影院） | `javac FacadeDemo.java && java FacadeDemo` |
| `FlyweightDemo.java` | 享元模式演示（围棋棋子） | `javac FlyweightDemo.java && java FlyweightDemo` |
| `ProxyDemo.java` | 代理模式演示（静态、JDK、CGLIB） | `javac ProxyDemo.java && java ProxyDemo` |

## 运行说明

### 方式一：逐个运行

```bash
cd Day12/code
javac AdapterDemo.java
java AdapterDemo
```

### 方式二：批量运行

```bash
cd Day12/code
for file in *.java; do
    echo "编译并运行: $file"
    javac "$file" && java "${file%.java}"
    echo "---"
done
```

## 注意事项

1. **CGLIB 依赖**：`ProxyDemo.java` 中的 CGLIB 示例需要引入 CGLIB 依赖
   ```xml
   <dependency>
       <groupId>cglib</groupId>
       <artifactId>cglib</artifactId>
       <version>3.3.0</version>
   </dependency>
   ```
   如果没有 CGLIB，可以只运行静态代理和 JDK 动态代理部分

2. **Java 版本**：建议使用 JDK 8 或更高版本

3. **编码格式**：所有文件使用 UTF-8 编码

## 学习建议

1. **先看课件**：运行代码前，先阅读 `00-教学课件_设计模式之结构型模式详解.md`
2. **观察输出**：运行代码后，仔细观察输出，理解模式的行为
3. **修改代码**：尝试修改代码，观察模式的变化
4. **自己实现**：不看代码，自己手写一遍，加深理解

## 实验顺序建议

1. **适配器模式** → 理解接口转换
2. **桥接模式** → 理解抽象与实现分离
3. **组合模式** → 理解树形结构
4. **装饰器模式** → 理解动态扩展功能
5. **外观模式** → 理解简化接口
6. **享元模式** → 理解对象共享
7. **代理模式** → 理解访问控制（最重要，需要重点掌握）

---

**提示**：每个 Demo 都是完整的、可运行的示例，可以直接编译运行。建议先运行看效果，再阅读代码理解原理。
