package com.github.rloic.collections;

public class Coordinates {

    public final int i;
    public final int j;
    public final int k;

    public Coordinates(int i, int j, int k) {
        this.i = i;
        this.j = j;
        this.k = k;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates)) return false;
        Coordinates tuple = (Coordinates) o;
        return i == tuple.i &&
                j == tuple.j &&
                k == tuple.k;
    }

    public Coordinates picatToJava() {
        return new Coordinates(i - 1, j - 1, k - 1);
    }

    public Coordinates javaToPicat() {
        return new Coordinates(i + 1, j + 1, k + 1);
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
