package com.github.rloic.common.collections;

public class ArrayExtensions {
    public static int[] intArrayOf(int... i) {
        return i;
    }

    public static <T> T[] arrayOf(T... args) {
        return args;
    }

    public static int[] deepCopy(int[] array) {
        int[] result = new int[array.length];
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }

    public static byte[][] deepCopy(byte[][] matrix) {
        byte[][] result = new byte[matrix.length][];
        for(int i = 0; i < matrix.length; i++) {
            result[i] = deepCopy(matrix[i]);
        }
        return result;
    }

    public static byte[] deepCopy(byte[] array) {
        byte[] result = new byte[array.length];
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }

    public static boolean[] deepCopy(boolean[] array) {
        boolean[] result = new boolean[array.length];
        System.arraycopy(array, 0, result, 0, result.length);
        return result;
    }


}
