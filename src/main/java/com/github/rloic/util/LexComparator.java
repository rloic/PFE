package com.github.rloic.util;

import com.github.rloic.common.collections.BytePosition;

import java.util.Comparator;

public class LexComparator implements Comparator<BytePosition> {

    public static final LexComparator LEX_COMPARATOR = new LexComparator();

    private LexComparator() {}

    @Override
    public int compare(BytePosition a, BytePosition b) {
        if (a.i != b.i) return a.i - b.i;
        if (a.j != b.j) return a.j - b.j;
        return a.k - b.k;
    }

}
