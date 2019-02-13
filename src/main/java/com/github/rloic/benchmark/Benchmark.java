package com.github.rloic.benchmark;

import java.util.function.Supplier;

public class Benchmark {

    public static <T> long measureTimeMillis(Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        supplier.get();
        return System.currentTimeMillis() - start;
    }

    public static long measureTimeMillis(Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        return System.currentTimeMillis() - start;
    }

}
