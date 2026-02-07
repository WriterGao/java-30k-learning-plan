import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Day11 - 原型模式演示
 * 
 * 浅拷贝 vs 深拷贝（三种深拷贝实现方式）
 */
public class PrototypeDemo {

    // ================================================================
    //  一、地址类（引用类型字段）
    // ================================================================

    static class Address implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        private String city;
        private String street;

        public Address(String city, String street) {
            this.city = city;
            this.street = street;
        }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }

        @Override
        public Address clone() {
            try {
                return (Address) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "Address{city='" + city + "', street='" + street + "'}";
        }
    }

    // ================================================================
    //  二、人员类 - 浅拷贝版本
    // ================================================================

    static class PersonShallow implements Cloneable {
        private String name;       // String 是不可变的，浅拷贝安全
        private int age;           // 基本类型，浅拷贝安全
        private Address address;   // 引用类型，浅拷贝有问题！
        private List<String> hobbies; // 引用类型

        public PersonShallow(String name, int age, Address address, List<String> hobbies) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.hobbies = hobbies;
        }

        @Override
        public PersonShallow clone() {
            try {
                return (PersonShallow) super.clone(); // 浅拷贝！
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }
        public List<String> getHobbies() { return hobbies; }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age +
                    ", address=" + address + ", hobbies=" + hobbies + "}";
        }
    }

    // ================================================================
    //  三、人员类 - 深拷贝版本（重写 clone）
    // ================================================================

    static class PersonDeepClone implements Cloneable {
        private String name;
        private int age;
        private Address address;
        private List<String> hobbies;

        public PersonDeepClone(String name, int age, Address address, List<String> hobbies) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.hobbies = hobbies;
        }

        @Override
        public PersonDeepClone clone() {
            try {
                PersonDeepClone cloned = (PersonDeepClone) super.clone();
                // 对引用类型字段进行深拷贝
                cloned.address = this.address.clone();
                cloned.hobbies = new ArrayList<>(this.hobbies);
                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public Address getAddress() { return address; }
        public List<String> getHobbies() { return hobbies; }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age +
                    ", address=" + address + ", hobbies=" + hobbies + "}";
        }
    }

    // ================================================================
    //  四、人员类 - 深拷贝版本（序列化方式）
    // ================================================================

    static class PersonSerializable implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private int age;
        private Address address;
        private List<String> hobbies;

        public PersonSerializable(String name, int age, Address address, List<String> hobbies) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.hobbies = hobbies;
        }

        /**
         * 通过序列化实现深拷贝
         */
        public PersonSerializable deepCopy() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(this);
                oos.close();

                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bis);
                PersonSerializable copy = (PersonSerializable) ois.readObject();
                ois.close();
                return copy;
            } catch (Exception e) {
                throw new RuntimeException("深拷贝失败", e);
            }
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public Address getAddress() { return address; }
        public List<String> getHobbies() { return hobbies; }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age +
                    ", address=" + address + ", hobbies=" + hobbies + "}";
        }
    }

    // ================================================================
    //  主方法
    // ================================================================

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Day11 - 原型模式演示（浅拷贝 vs 深拷贝）");
        System.out.println("========================================\n");

        // 一、浅拷贝问题
        testShallowCopy();

        // 二、深拷贝 - 重写 clone
        testDeepCopyClone();

        // 三、深拷贝 - 序列化
        testDeepCopySerialization();

        // 四、基本类型 vs 引用类型
        testBasicVsReference();

        // 五、总结
        printSummary();
    }

    static void testShallowCopy() {
        System.out.println("=== 一、浅拷贝演示（Object.clone() 默认行为）===\n");

        List<String> hobbies = new ArrayList<>();
        hobbies.add("编程");
        hobbies.add("游泳");

        PersonShallow p1 = new PersonShallow("张三", 25,
                new Address("北京", "长安街"), hobbies);
        PersonShallow p2 = p1.clone(); // 浅拷贝

        System.out.println("  克隆前：");
        System.out.println("    原对象 p1: " + p1);
        System.out.println("    克隆对象 p2: " + p2);
        System.out.println("    p1 == p2 ? " + (p1 == p2) + " (不同对象 ✅)");
        System.out.println("    p1.address == p2.address ? " + (p1.getAddress() == p2.getAddress()) +
                " ⚠️ 引用同一个 Address 对象！");
        System.out.println("    p1.hobbies == p2.hobbies ? " + (p1.getHobbies() == p2.getHobbies()) +
                " ⚠️ 引用同一个 List 对象！");

        // 修改克隆对象的引用类型字段
        System.out.println("\n  修改克隆对象 p2 的 address.city 为 '上海'：");
        p2.getAddress().setCity("上海");
        p2.getHobbies().add("阅读");
        p2.setName("李四");  // String 是不可变的
        p2.setAge(30);       // 基本类型

        System.out.println("    p1: " + p1);
        System.out.println("    p2: " + p2);
        System.out.println();
        System.out.println("    ⚠️ p1.address.city 也变成了 '上海'！浅拷贝导致原对象被污染！");
        System.out.println("    ⚠️ p1.hobbies 也多了 '阅读'！浅拷贝的 List 也被共享修改！");
        System.out.println("    ✅ p1.name 不受影响（String 不可变，修改 name 实际上是指向了新字符串）");
        System.out.println("    ✅ p1.age 不受影响（基本类型是值拷贝）");
        System.out.println();
    }

    static void testDeepCopyClone() {
        System.out.println("=== 二、深拷贝（重写 clone 方法）===\n");

        List<String> hobbies = new ArrayList<>();
        hobbies.add("编程");
        hobbies.add("游泳");

        PersonDeepClone p1 = new PersonDeepClone("张三", 25,
                new Address("北京", "长安街"), hobbies);
        PersonDeepClone p2 = p1.clone(); // 深拷贝

        System.out.println("  克隆后：");
        System.out.println("    p1.address == p2.address ? " + (p1.getAddress() == p2.getAddress()) +
                " ✅ 不同的 Address 对象");
        System.out.println("    p1.hobbies == p2.hobbies ? " + (p1.getHobbies() == p2.getHobbies()) +
                " ✅ 不同的 List 对象");

        // 修改克隆对象
        System.out.println("\n  修改克隆对象 p2：");
        p2.getAddress().setCity("上海");
        p2.getHobbies().add("阅读");

        System.out.println("    p1: " + p1);
        System.out.println("    p2: " + p2);
        System.out.println("    ✅ p1 不受影响！深拷贝成功！");
        System.out.println();
    }

    static void testDeepCopySerialization() {
        System.out.println("=== 三、深拷贝（序列化方式）===\n");

        List<String> hobbies = new ArrayList<>();
        hobbies.add("编程");
        hobbies.add("游泳");

        PersonSerializable p1 = new PersonSerializable("张三", 25,
                new Address("北京", "长安街"), hobbies);
        PersonSerializable p2 = p1.deepCopy(); // 通过序列化深拷贝

        System.out.println("  克隆后：");
        System.out.println("    p1.address == p2.address ? " + (p1.getAddress() == p2.getAddress()) +
                " ✅ 不同的 Address 对象");
        System.out.println("    p1.hobbies == p2.hobbies ? " + (p1.getHobbies() == p2.getHobbies()) +
                " ✅ 不同的 List 对象");

        // 修改克隆对象
        System.out.println("\n  修改克隆对象 p2：");
        p2.getAddress().setCity("深圳");
        p2.getHobbies().add("摄影");

        System.out.println("    p1: " + p1);
        System.out.println("    p2: " + p2);
        System.out.println("    ✅ p1 不受影响！序列化深拷贝成功！");
        System.out.println();
    }

    static void testBasicVsReference() {
        System.out.println("=== 四、基本类型 vs 引用类型在拷贝中的行为 ===\n");

        System.out.println("  【拷贝规则】");
        System.out.println("  ┌──────────────┬───────────────────────┬──────────────────────┐");
        System.out.println("  │ 类型         │ 浅拷贝                │ 深拷贝               │");
        System.out.println("  ├──────────────┼───────────────────────┼──────────────────────┤");
        System.out.println("  │ 基本类型      │ 复制值 ✅             │ 复制值 ✅            │");
        System.out.println("  │ (int,double) │ 修改不影响原对象       │ 修改不影响原对象      │");
        System.out.println("  ├──────────────┼───────────────────────┼──────────────────────┤");
        System.out.println("  │ String       │ 安全 ✅（不可变）      │ 安全 ✅              │");
        System.out.println("  │              │ 指向同一字符串，但不可改 │ 新的字符串对象        │");
        System.out.println("  ├──────────────┼───────────────────────┼──────────────────────┤");
        System.out.println("  │ 引用类型      │ 共享引用 ⚠️           │ 独立副本 ✅          │");
        System.out.println("  │ (对象/集合)   │ 修改会影响原对象       │ 修改不影响原对象      │");
        System.out.println("  └──────────────┴───────────────────────┴──────────────────────┘");

        System.out.println("\n  【String 为什么安全？】");
        System.out.println("  因为 String 是不可变类（final class，字符数组也是 final）");
        System.out.println("  '修改' String 实际上是创建了新的字符串对象，原引用不受影响");

        // 验证 String 的安全性
        PersonShallow p1 = new PersonShallow("张三", 25,
                new Address("北京", "街道"), new ArrayList<>());
        PersonShallow p2 = p1.clone();

        System.out.println("\n  验证：");
        System.out.println("    克隆后 p1.name == p2.name ? " +
                (p1.getName() == p2.getName()) + " (指向同一字符串对象)");
        p2.setName("李四");
        System.out.println("    p2.name = '李四' 后：");
        System.out.println("    p1.name = '" + p1.getName() + "' (不受影响 ✅)");
        System.out.println("    p2.name = '" + p2.getName() + "'");
        System.out.println("    p1.name == p2.name ? " +
                (p1.getName() == p2.getName()) + " (现在指向不同的字符串对象)");

        System.out.println();
    }

    static void printSummary() {
        System.out.println("========================================");
        System.out.println("  原型模式总结");
        System.out.println("========================================");
        System.out.println();
        System.out.println("  【三种深拷贝方式对比】");
        System.out.println("  | 方式        | 优点           | 缺点                   |");
        System.out.println("  |------------|----------------|------------------------|");
        System.out.println("  | 重写clone() | 性能最好        | 嵌套深时很麻烦,易遗漏    |");
        System.out.println("  | 序列化      | 简单通用        | 需实现Serializable,性能差 |");
        System.out.println("  | JSON       | 最简单,无需接口  | 需第三方库,性能最差       |");
        System.out.println();
        System.out.println("  【适用场景】");
        System.out.println("  ✅ 创建对象成本大（IO/数据库/网络）");
        System.out.println("  ✅ 需要大量相似但不完全相同的对象");
        System.out.println("  ✅ 对象创建过程复杂（多层嵌套）");
        System.out.println();
        System.out.println("  【注意事项】");
        System.out.println("  ⚠️ 使用 clone() 前一定要明确是浅拷贝还是深拷贝");
        System.out.println("  ⚠️ 包含引用类型字段时，浅拷贝可能导致意外的数据共享");
        System.out.println("  ⚠️ 深拷贝的性能开销大于浅拷贝，按需选择");
    }
}
