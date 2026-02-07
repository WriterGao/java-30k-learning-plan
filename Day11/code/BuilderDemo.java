import java.util.*;

/**
 * Day11 - 建造者模式演示
 * 
 * 包含：经典建造者（Director + Builder）和 链式建造者（Fluent Builder）
 */
public class BuilderDemo {

    // ================================================================
    //  一、经典建造者模式（Director + Builder）
    // ================================================================

    /** 产品类：电脑 */
    static class Computer {
        private String cpu;
        private String ram;
        private String ssd;
        private String gpu;
        private String mainboard;

        public void setCpu(String cpu) { this.cpu = cpu; }
        public void setRam(String ram) { this.ram = ram; }
        public void setSsd(String ssd) { this.ssd = ssd; }
        public void setGpu(String gpu) { this.gpu = gpu; }
        public void setMainboard(String mainboard) { this.mainboard = mainboard; }

        @Override
        public String toString() {
            return "Computer{" +
                    "cpu='" + cpu + '\'' +
                    ", ram='" + ram + '\'' +
                    ", ssd='" + ssd + '\'' +
                    ", gpu='" + gpu + '\'' +
                    ", mainboard='" + mainboard + '\'' +
                    '}';
        }
    }

    /** 抽象建造者 */
    static abstract class ComputerBuilder {
        protected Computer computer = new Computer();

        public abstract void buildCPU();
        public abstract void buildRAM();
        public abstract void buildSSD();
        public abstract void buildGPU();
        public abstract void buildMainboard();

        public Computer getResult() {
            return computer;
        }
    }

    /** 具体建造者A：游戏电脑 */
    static class GamingComputerBuilder extends ComputerBuilder {
        @Override public void buildCPU() { computer.setCpu("Intel i9-13900K"); }
        @Override public void buildRAM() { computer.setRam("64GB DDR5-5600"); }
        @Override public void buildSSD() { computer.setSsd("2TB NVMe Gen4"); }
        @Override public void buildGPU() { computer.setGpu("NVIDIA RTX 4090"); }
        @Override public void buildMainboard() { computer.setMainboard("ROG Z790 HERO"); }
    }

    /** 具体建造者B：办公电脑 */
    static class OfficeComputerBuilder extends ComputerBuilder {
        @Override public void buildCPU() { computer.setCpu("Intel i5-13400"); }
        @Override public void buildRAM() { computer.setRam("16GB DDR4-3200"); }
        @Override public void buildSSD() { computer.setSsd("512GB SATA SSD"); }
        @Override public void buildGPU() { computer.setGpu("集成显卡 UHD 730"); }
        @Override public void buildMainboard() { computer.setMainboard("华硕 B760M"); }
    }

    /** 指挥者（Director）：控制构建顺序 */
    static class ComputerDirector {
        private ComputerBuilder builder;

        public ComputerDirector(ComputerBuilder builder) {
            this.builder = builder;
        }

        public Computer construct() {
            System.out.println("    [Director] 开始构建电脑...");
            builder.buildCPU();
            System.out.println("    [Director] 安装 CPU 完成");
            builder.buildMainboard();
            System.out.println("    [Director] 安装主板完成");
            builder.buildRAM();
            System.out.println("    [Director] 安装内存完成");
            builder.buildSSD();
            System.out.println("    [Director] 安装 SSD 完成");
            builder.buildGPU();
            System.out.println("    [Director] 安装显卡完成");
            System.out.println("    [Director] 构建完成！");
            return builder.getResult();
        }
    }

    // ================================================================
    //  二、链式建造者（Fluent Builder / Lombok @Builder 风格）
    // ================================================================

    /**
     * 链式建造者：产品类内部包含静态 Builder 类
     * 特点：
     * 1. 产品对象不可变（final 字段）
     * 2. 链式调用，可读性好
     * 3. 支持必需参数和可选参数
     */
    static class HttpRequest {
        // 所有字段都是 final（不可变对象）
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final int timeout;
        private final String body;
        private final boolean followRedirects;

        // 私有构造器，只能通过 Builder 创建
        private HttpRequest(Builder builder) {
            this.method = builder.method;
            this.url = builder.url;
            this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
            this.timeout = builder.timeout;
            this.body = builder.body;
            this.followRedirects = builder.followRedirects;
        }

        // 只提供 getter，没有 setter（不可变）
        public String getMethod() { return method; }
        public String getUrl() { return url; }
        public Map<String, String> getHeaders() { return headers; }
        public int getTimeout() { return timeout; }
        public String getBody() { return body; }
        public boolean isFollowRedirects() { return followRedirects; }

        @Override
        public String toString() {
            return "HttpRequest{\n" +
                    "      method='" + method + "'\n" +
                    "      url='" + url + "'\n" +
                    "      headers=" + headers + "\n" +
                    "      timeout=" + timeout + "ms\n" +
                    "      body='" + (body != null ? body : "null") + "'\n" +
                    "      followRedirects=" + followRedirects + "\n" +
                    "    }";
        }

        /** 静态内部类 Builder */
        public static class Builder {
            // 必需参数
            private final String method;
            private final String url;

            // 可选参数（带默认值）
            private Map<String, String> headers = new HashMap<>();
            private int timeout = 30000; // 默认 30 秒
            private String body = null;
            private boolean followRedirects = true;

            /**
             * 必需参数通过构造器传入
             */
            public Builder(String method, String url) {
                if (method == null || method.isEmpty()) {
                    throw new IllegalArgumentException("method 不能为空");
                }
                if (url == null || url.isEmpty()) {
                    throw new IllegalArgumentException("url 不能为空");
                }
                this.method = method;
                this.url = url;
            }

            /** 添加请求头 */
            public Builder header(String key, String value) {
                this.headers.put(key, value);
                return this; // 返回 this，支持链式调用
            }

            /** 设置超时时间 */
            public Builder timeout(int timeout) {
                if (timeout <= 0) {
                    throw new IllegalArgumentException("timeout 必须大于 0");
                }
                this.timeout = timeout;
                return this;
            }

            /** 设置请求体 */
            public Builder body(String body) {
                this.body = body;
                return this;
            }

            /** 设置是否跟随重定向 */
            public Builder followRedirects(boolean followRedirects) {
                this.followRedirects = followRedirects;
                return this;
            }

            /** 构建最终对象 */
            public HttpRequest build() {
                // 可以在这里做参数校验
                if (("POST".equals(method) || "PUT".equals(method)) && body == null) {
                    System.out.println("    ⚠️ 警告: " + method + " 请求没有设置 body");
                }
                return new HttpRequest(this);
            }
        }
    }

    // ================================================================
    //  三、模拟 StringBuilder 的建造者
    // ================================================================

    static class SimpleStringBuilder {
        private char[] buffer;
        private int size;

        public SimpleStringBuilder() {
            buffer = new char[16];
            size = 0;
        }

        public SimpleStringBuilder append(String str) {
            if (str == null) str = "null";
            ensureCapacity(size + str.length());
            str.getChars(0, str.length(), buffer, size);
            size += str.length();
            return this; // 链式调用
        }

        public SimpleStringBuilder append(int num) {
            return append(String.valueOf(num));
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity > buffer.length) {
                int newCapacity = Math.max(buffer.length * 2, minCapacity);
                buffer = Arrays.copyOf(buffer, newCapacity);
            }
        }

        @Override
        public String toString() {
            return new String(buffer, 0, size);
        }
    }

    // ================================================================
    //  主方法
    // ================================================================

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Day11 - 建造者模式演示");
        System.out.println("========================================\n");

        // 一、经典建造者
        testClassicBuilder();

        // 二、链式建造者
        testFluentBuilder();

        // 三、StringBuilder 模拟
        testStringBuilder();

        // 四、对比总结
        printSummary();
    }

    static void testClassicBuilder() {
        System.out.println("=== 一、经典建造者模式（Director + Builder）===\n");

        // 构建游戏电脑
        System.out.println("  --- 构建游戏电脑 ---");
        ComputerDirector director1 = new ComputerDirector(new GamingComputerBuilder());
        Computer gamingPC = director1.construct();
        System.out.println("  结果: " + gamingPC);

        System.out.println();

        // 构建办公电脑（同样的构建过程，不同的配置）
        System.out.println("  --- 构建办公电脑 ---");
        ComputerDirector director2 = new ComputerDirector(new OfficeComputerBuilder());
        Computer officePC = director2.construct();
        System.out.println("  结果: " + officePC);

        System.out.println();
    }

    static void testFluentBuilder() {
        System.out.println("=== 二、链式建造者（Fluent Builder）===\n");

        // GET 请求（只有必需参数 + 少量可选参数）
        System.out.println("  --- 构建 GET 请求 ---");
        HttpRequest getRequest = new HttpRequest.Builder("GET", "https://api.example.com/users")
                .header("Accept", "application/json")
                .timeout(5000)
                .build();
        System.out.println("  结果:\n    " + getRequest);

        System.out.println();

        // POST 请求（使用更多可选参数）
        System.out.println("  --- 构建 POST 请求 ---");
        HttpRequest postRequest = new HttpRequest.Builder("POST", "https://api.example.com/users")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9...")
                .timeout(10000)
                .body("{\"name\":\"张三\",\"age\":25}")
                .followRedirects(false)
                .build();
        System.out.println("  结果:\n    " + postRequest);

        System.out.println();

        // 最简请求（只有必需参数，其他使用默认值）
        System.out.println("  --- 构建最简请求（使用所有默认值）---");
        HttpRequest simpleRequest = new HttpRequest.Builder("GET", "https://example.com")
                .build();
        System.out.println("  结果:\n    " + simpleRequest);

        // 验证不可变性
        System.out.println("\n  --- 验证不可变性 ---");
        try {
            getRequest.getHeaders().put("hack", "value"); // 应该抛异常
            System.out.println("  ⚠️ 不可变性被破坏！");
        } catch (UnsupportedOperationException e) {
            System.out.println("  ✅ Headers 是不可变的（UnmodifiableMap），无法修改");
        }

        System.out.println();
    }

    static void testStringBuilder() {
        System.out.println("=== 三、StringBuilder 模拟（建造者模式的典型应用）===\n");

        // 使用 JDK StringBuilder
        String result1 = new StringBuilder()
                .append("Hello")
                .append(" ")
                .append("World")
                .append("! ")
                .append("Count: ")
                .append(42)
                .toString();
        System.out.println("  JDK StringBuilder: " + result1);

        // 使用自己实现的 SimpleStringBuilder
        String result2 = new SimpleStringBuilder()
                .append("Hello")
                .append(" ")
                .append("World")
                .append("! ")
                .append("Count: ")
                .append(42)
                .toString();
        System.out.println("  自定义 StringBuilder: " + result2);

        System.out.println("  ✅ 两者结果一致: " + result1.equals(result2));

        System.out.println();
    }

    static void printSummary() {
        System.out.println("========================================");
        System.out.println("  建造者模式总结");
        System.out.println("========================================");
        System.out.println();
        System.out.println("  【经典建造者 vs 链式建造者】");
        System.out.println("  | 维度       | 经典建造者           | 链式建造者           |");
        System.out.println("  |-----------|--------------------|--------------------|");
        System.out.println("  | Director  | 有（控制构建顺序）     | 无（客户端自己控制）   |");
        System.out.println("  | 关注点     | 构建步骤和顺序        | 参数配置             |");
        System.out.println("  | 不可变性   | 产品通常可变          | 产品通常不可变(final) |");
        System.out.println("  | 使用频率   | 较少                 | 非常常见             |");
        System.out.println("  | 典型应用   | 复杂对象分步构建       | Lombok @Builder     |");
        System.out.println();
        System.out.println("  【建造者 vs 工厂】");
        System.out.println("  工厂模式：关注创建\"哪种\"对象（类型不同）");
        System.out.println("  建造者模式：关注\"如何\"创建对象（配置不同）");
        System.out.println();
        System.out.println("  【适用场景】");
        System.out.println("  ✅ 构造器参数超过 4 个");
        System.out.println("  ✅ 有很多可选参数");
        System.out.println("  ✅ 需要不可变对象");
        System.out.println("  ✅ 需要链式调用（Fluent API）");
    }
}
