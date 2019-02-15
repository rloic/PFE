package com.github.rloic.collections;

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

    public static void main(String[] args) {

        BytePosition a = new BytePosition(1, 1, 1);
        BytePosition b = new BytePosition(1, 2, 1);
        BytePosition c = new BytePosition(1, 1, 2);
        BytePosition d = new BytePosition(3, 1, 1);

        System.out.println(LEX_COMPARATOR.compare(a, a)); //  0
        System.out.println(LEX_COMPARATOR.compare(a, b)); // -1
        System.out.println(LEX_COMPARATOR.compare(a, c)); // -1
        System.out.println(LEX_COMPARATOR.compare(c, b)); //  1
        System.out.println(LEX_COMPARATOR.compare(d, a)); //  1
        System.out.println(LEX_COMPARATOR.compare(b, a)); //  2

    }

}
