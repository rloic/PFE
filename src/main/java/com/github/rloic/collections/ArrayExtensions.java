package com.github.rloic.collections;

import com.github.rloic.kt.aes128.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ArrayExtensions {

    public static int[] intArrayOf(int... i) {
        return i;
    }

    public static <T> T[] arrayOf(T... args) {
        return args;
    }

    public static <T> T[] Array(int size) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[size];
        return result;
    }

    public static <T> T[] Array(int size, Supplier<T> init) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[size];
        for(int i = 0; i < size; i++) result[i] = init.get();
        return result;
    }

    public static <T> T[] Array(int size, Function<Integer, T> init) {
        Object[] result = new Object[size];
        for(int i = 0; i < size; i++) result[i] = init.apply(i);
        return (T[]) result;
    }

    public static <T> T[][] Matrix(int dim1, int dim2) {
        @SuppressWarnings("unchecked")
        T[][] result = (T[][]) new Object[dim1][dim2];
        return result;
    }

    public static <T> T[][] Matrix(int dim1, int dim2, Supplier<T> init) {
        @SuppressWarnings("unchecked")
        T[][] result = (T[][]) new Object[dim1][dim2];
        for(int i = 0; i < dim1; i++)
            for(int j = 0; j < dim2; j++)
                result[i][j] = init.get();
        return result;
    }

    public static <T> T[][] Matrix(int dim1, int dim2, BiFunction<Integer, Integer, T> init) {
        @SuppressWarnings("unchecked")
        T[][] result = (T[][]) new Object[dim1][dim2];
        for(int i = 0; i < dim1; i++)
            for(int j = 0; j < dim2; j++)
                result[i][j] = init.apply(i, j);
        return result;
    }

    public static <T> T[][][] Tensor(int dim1, int dim2, int dim3, TriFunction<Integer, Integer, Integer, T> init) {
        @SuppressWarnings("unchecked")
        T[][][] result = (T[][][])new Object[dim1][dim2][dim3];
        for(int i = 0; i < dim1; i++)
            for(int j = 0; j < dim2; j++)
                for(int k = 0; k < dim3; k++)
                    result[i][j][k] = init.apply(i, j, k);
        return result;
    }

    public static <T> T[][][][][] Tensor(int dim1, int dim2, int dim3, int dim4, int dim5) {
        @SuppressWarnings("unchecked")
        T[][][][][] result = (T[][][][][]) new Object[dim1][dim2][dim3][dim4][dim5];
        return result;
    }

    public static <T> T[][][][][] Tensor(int dim1, int dim2, int dim3, int dim4, int dim5, Supplier<T> supplier) {
        @SuppressWarnings("unchecked")
        T[][][][][] result = (T[][][][][]) new Object[dim1][dim2][dim3][dim4][dim5];
        for(int i = 0; i < dim1; i++)
            for(int j = 0; j < dim2; j++)
                for(int k = 0; j < dim3; k++)
                    for(int l = 0; l < dim4; l++)
                        for(int m = 0; m < dim5; m++)
                            result[i][j][k][l][m] = supplier.get();
        return result;
    }

}
