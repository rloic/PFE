package com.github.rloic.collections;

import java.util.Arrays;

public class IntTuple {

    private final int[] values;
    public final int length;

    public IntTuple(int... values) {
        this.values = values;
        this.length = values.length;
    }

    public int get(int index) {
        return values[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntTuple)) return false;
        IntTuple intTuple = (IntTuple) o;
        return Arrays.equals(values, intTuple.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

}
