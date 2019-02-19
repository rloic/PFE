package com.github.rloic.util;

import java.util.Objects;

public class Pair<T1, T2> {

    public final T1 _0;
    public final T2 _1;

    public Pair(T1 _0, T2 _1) {
        this._0 = _0;
        this._1 = _1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(_0, pair._0) &&
                Objects.equals(_1, pair._1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_0, _1);
    }

    @Override
    public String toString() {
        return "Pair(" + _0 + ", " + _1 + ")";
    }
}
