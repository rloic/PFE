package com.github.rloic.collections;

import java.util.Comparator;

public class LexComparator implements Comparator<IntTuple> {

    public static final LexComparator LEX_COMPARATOR = new LexComparator();

    private LexComparator() {}

    @Override
    public int compare(IntTuple a, IntTuple b) {
        int i = 0;

        while (i < a.length && i < b.length && a.get(i) == b.get(i)) {
            i++;
        }

        if (i < a.length && i < b.length) {
            return Integer.compare(a.get(i), b.get(i));
        } else {
            return Integer.compare(a.length, b.length);
        }
    }

    public static void main(String[] args) {

        IntTuple a = new IntTuple(1, 1, 1);
        IntTuple b = new IntTuple(1, 1, 1, 1);
        IntTuple c = new IntTuple(1, 1, 2);
        IntTuple d = new IntTuple(3, 1, 1);

        System.out.println(LEX_COMPARATOR.compare(a, a)); //  0
        System.out.println(LEX_COMPARATOR.compare(a, b)); // -1
        System.out.println(LEX_COMPARATOR.compare(a, c)); // -1
        System.out.println(LEX_COMPARATOR.compare(c, b)); //  1
        System.out.println(LEX_COMPARATOR.compare(d, a)); //  1
        System.out.println(LEX_COMPARATOR.compare(b, a)); //  1

    }

}
