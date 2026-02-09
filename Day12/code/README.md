# Day12 代码运行说明

本目录包含结构型设计模式的示例代码。

## 文件说明

| 文件 | 说明 |
|------|------|
| `AdapterDemo.java` | 适配器模式演示（类适配器、对象适配器） |
| `DecoratorDemo.java` | 装饰器模式演示（咖啡加料系统） |
| `ProxyDemo.java` | 代理模式演示（静态代理、JDK动态代理、CGLIB） |
| `FacadeDemo.java` | 外观模式演示（家庭影院系统） |
| `BridgeDemo.java` | 桥接模式演示（形状和颜色） |

## 运行方式

### 1. 适配器模式

```bash
javac AdapterDemo.java
java AdapterDemo
```

### 2. 装饰器模式

```bash
javac DecoratorDemo.java
java DecoratorDemo
```

### 3. 代理模式

```bash
# 注意：CGLIB 需要引入依赖，如果只运行静态代理和 JDK 动态代理，可以注释掉 CGLIB 相关代码
javac ProxyDemo.java
java ProxyDemo
```

### 4. 外观模式

```bash
javac FacadeDemo.java
java FacadeDemo
```

### 5. 桥接模式

```bash
javac BridgeDemo.java
java BridgeDemo
```

## 注意事项

1. **CGLIB 动态代理**：需要引入 CGLIB 依赖。如果不想引入依赖，可以只运行静态代理和 JDK 动态代理的代码。
2. **JDK 版本**：建议使用 JDK 8 或更高版本。
3. **编译顺序**：如果文件之间有依赖关系，需要按顺序编译。

## 学习建议

1. **先运行代码**：观察输出，理解模式的行为
2. **阅读代码**：理解模式的实现方式
3. **修改代码**：尝试修改参数，观察变化
4. **手写代码**：不看示例，自己实现一遍
