import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * CompletableFuture 异步编程全面演示
 *
 * 演示内容：
 * 1. 创建异步任务（supplyAsync / runAsync）
 * 2. 链式调用（thenApply / thenAccept / thenRun / thenCompose / thenCombine）
 * 3. 异常处理（exceptionally / handle / whenComplete）
 * 4. 多任务组合（allOf / anyOf）
 * 5. 实战案例：并行调用多个微服务
 */
public class CompletableFutureDemo {

    // 自定义线程池（推荐生产环境使用，避免使用公共 ForkJoinPool）
    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    public static void main(String[] args) throws Exception {
        System.out.println("========== CompletableFuture 演示 ==========\n");

        demo1_CreateAsync();
        System.out.println();

        demo2_ChainCalls();
        System.out.println();

        demo3_ExceptionHandling();
        System.out.println();

        demo4_CombineMultiple();
        System.out.println();

        demo5_ParallelServiceCalls();

        executor.shutdown();
    }

    /**
     * 演示1：创建异步任务
     */
    static void demo1_CreateAsync() throws Exception {
        System.out.println("--- 演示1：创建异步任务 ---");

        // supplyAsync —— 有返回值
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            System.out.println("  supplyAsync 执行线程: " + Thread.currentThread().getName());
            return "Hello CompletableFuture";
        }, executor);

        // runAsync —— 无返回值
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            sleep(300);
            System.out.println("  runAsync 执行线程: " + Thread.currentThread().getName());
        }, executor);

        System.out.println("  supplyAsync 结果: " + future1.get());
        future2.get();
        System.out.println("  ✅ 两个异步任务均已完成");
    }

    /**
     * 演示2：链式调用
     */
    static void demo2_ChainCalls() throws Exception {
        System.out.println("--- 演示2：链式调用 ---");

        // thenApply: 转换结果（Function）
        CompletableFuture<Integer> thenApplyFuture = CompletableFuture
                .supplyAsync(() -> "100", executor)
                .thenApply(s -> {
                    System.out.println("  thenApply: 将字符串 \"" + s + "\" 转为整数");
                    return Integer.parseInt(s);
                })
                .thenApply(i -> {
                    System.out.println("  thenApply: " + i + " × 2 = " + (i * 2));
                    return i * 2;
                });
        System.out.println("  thenApply 最终结果: " + thenApplyFuture.get());

        // thenAccept: 消费结果（Consumer）
        CompletableFuture.supplyAsync(() -> "thenAccept消费的数据", executor)
                .thenAccept(s -> System.out.println("  thenAccept 消费: " + s))
                .get();

        // thenRun: 执行后续操作，不关心前一步结果（Runnable）
        CompletableFuture.supplyAsync(() -> "某个结果", executor)
                .thenRun(() -> System.out.println("  thenRun: 上一步已完成，执行后续操作"))
                .get();

        // thenCompose: 扁平化嵌套的 CompletableFuture（类似 flatMap）
        CompletableFuture<String> composeFuture = CompletableFuture
                .supplyAsync(() -> 42, executor)
                .thenCompose(id -> queryUserById(id));
        System.out.println("  thenCompose 结果: " + composeFuture.get());

        // thenCombine: 合并两个独立的 CompletableFuture 的结果
        CompletableFuture<String> combineFuture = CompletableFuture
                .supplyAsync(() -> {
                    sleep(300);
                    return "价格: 99.9";
                }, executor)
                .thenCombine(
                        CompletableFuture.supplyAsync(() -> {
                            sleep(200);
                            return "库存: 100";
                        }, executor),
                        (price, stock) -> price + ", " + stock
                );
        System.out.println("  thenCombine 合并结果: " + combineFuture.get());
    }

    /**
     * 演示3：异常处理
     */
    static void demo3_ExceptionHandling() throws Exception {
        System.out.println("--- 演示3：异常处理 ---");

        // exceptionally: 捕获异常并返回兜底值
        CompletableFuture<String> exceptionallyFuture = CompletableFuture
                .supplyAsync(() -> {
                    if (true) throw new RuntimeException("模拟异常");
                    return "正常结果";
                }, executor)
                .exceptionally(ex -> {
                    System.out.println("  exceptionally 捕获异常: " + ex.getMessage());
                    return "兜底默认值";
                });
        System.out.println("  exceptionally 结果: " + exceptionallyFuture.get());

        // handle: 无论成功或异常都执行（BiFunction）
        CompletableFuture<String> handleFuture = CompletableFuture
                .supplyAsync(() -> {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        throw new RuntimeException("随机异常");
                    }
                    return "成功结果";
                }, executor)
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.out.println("  handle 处理异常: " + ex.getMessage());
                        return "handle 兜底值";
                    }
                    System.out.println("  handle 处理成功: " + result);
                    return result;
                });
        System.out.println("  handle 结果: " + handleFuture.get());

        // whenComplete: 观察结果或异常，不改变返回值
        CompletableFuture<String> whenCompleteFuture = CompletableFuture
                .supplyAsync(() -> "whenComplete测试", executor)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("  whenComplete 异常: " + ex.getMessage());
                    } else {
                        System.out.println("  whenComplete 成功: " + result);
                    }
                });
        System.out.println("  whenComplete 结果: " + whenCompleteFuture.get());
    }

    /**
     * 演示4：多任务组合 (allOf / anyOf)
     */
    static void demo4_CombineMultiple() throws Exception {
        System.out.println("--- 演示4：多任务组合 ---");

        // allOf: 等待所有任务完成
        long start = System.currentTimeMillis();
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> { sleep(1000); return "任务1"; }, executor);
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> { sleep(800); return "任务2"; }, executor);
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> { sleep(600); return "任务3"; }, executor);

        CompletableFuture<Void> allOf = CompletableFuture.allOf(f1, f2, f3);
        allOf.get(); // 阻塞等待全部完成
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("  allOf 全部完成（耗时 " + elapsed + "ms，约等于最慢的任务）:");
        System.out.println("    f1=" + f1.get() + ", f2=" + f2.get() + ", f3=" + f3.get());

        // anyOf: 任一任务完成即返回
        start = System.currentTimeMillis();
        CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> { sleep(200); return "快速任务"; }, executor);
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> { sleep(2000); return "慢速任务"; }, executor);

        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(fast, slow);
        Object firstResult = anyOf.get();
        elapsed = System.currentTimeMillis() - start;

        System.out.println("  anyOf 首个完成（耗时 " + elapsed + "ms）: " + firstResult);
    }

    /**
     * 演示5：实战案例 —— 并行调用多个微服务
     * 模拟电商场景：同时查询 用户信息、商品信息、库存信息、优惠信息，最后聚合
     */
    static void demo5_ParallelServiceCalls() throws Exception {
        System.out.println("--- 演示5：实战案例（并行调用多个微服务）---");

        long start = System.currentTimeMillis();

        // 并行调用4个服务
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return "{\"userId\":1001, \"name\":\"张三\"}";
        }, executor);

        CompletableFuture<String> productFuture = CompletableFuture.supplyAsync(() -> {
            sleep(600);
            return "{\"productId\":2001, \"name\":\"iPhone 15\", \"price\":7999}";
        }, executor);

        CompletableFuture<String> stockFuture = CompletableFuture.supplyAsync(() -> {
            sleep(400);
            return "{\"productId\":2001, \"stock\":128}";
        }, executor);

        CompletableFuture<String> couponFuture = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "{\"couponId\":3001, \"discount\":500}";
        }, executor);

        // 等待所有服务调用完成
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                userFuture, productFuture, stockFuture, couponFuture
        );

        // 聚合结果
        CompletableFuture<String> aggregated = allDone.thenApply(v -> {
            try {
                String user = userFuture.get();
                String product = productFuture.get();
                String stock = stockFuture.get();
                String coupon = couponFuture.get();

                return String.format("聚合结果:\n    用户: %s\n    商品: %s\n    库存: %s\n    优惠: %s",
                        user, product, stock, coupon);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        String result = aggregated.get();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("  " + result);
        System.out.println("  ⚡ 并行调用总耗时: " + elapsed + "ms（约等于最慢的服务 800ms，远小于串行 2300ms）");

        // 额外演示：List<CompletableFuture> 批量处理
        System.out.println("\n  --- 批量异步处理示例 ---");
        List<String> serviceNames = Arrays.asList("订单服务", "支付服务", "物流服务", "通知服务");

        start = System.currentTimeMillis();
        List<CompletableFuture<String>> futures = serviceNames.stream()
                .map(name -> CompletableFuture.supplyAsync(() -> {
                    sleep(ThreadLocalRandom.current().nextInt(300, 800));
                    return name + " -> 调用成功";
                }, executor))
                .collect(Collectors.toList());

        // 收集所有结果
        List<String> results = futures.stream()
                .map(CompletableFuture::join) // join 不抛受检异常
                .collect(Collectors.toList());

        elapsed = System.currentTimeMillis() - start;
        results.forEach(r -> System.out.println("    " + r));
        System.out.println("  ⚡ 批量调用总耗时: " + elapsed + "ms");
    }

    // ========== 辅助方法 ==========

    static CompletableFuture<String> queryUserById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "User{id=" + id + ", name='张三'}";
        }, executor);
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
