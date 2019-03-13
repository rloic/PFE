package com.github.rloic.common.collections;

public class BytePosition {

    public final int i;
    public final int j;
    public final int k;

    public BytePosition(int i, int j, int k) {
        this.i = i;
        this.j = j;
        this.k = k;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BytePosition)) return false;
        BytePosition tuple = (BytePosition) o;
        return i == tuple.i &&
                j == tuple.j &&
                k == tuple.k;
    }

    public BytePosition picatToJava() {
        return new BytePosition(i - 1, j - 1, k - 1);
    }

    public BytePosition javaToPicat() {
        return new BytePosition(i + 1, j + 1, k + 1);
    }

    @Override
    public int hashCode() {
        return (((i << 10) + j) << 10) + k;
    }

    @Override
    public String toString() {
        return "[" + i + "," + j + "," + k + "]";
    }
}
