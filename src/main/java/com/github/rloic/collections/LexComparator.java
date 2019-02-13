package com.github.rloic.collections;

import java.util.Comparator;

public class LexComparator implements Comparator<Coordinates> {

    public static final LexComparator LEX_COMPARATOR = new LexComparator();

    private LexComparator() {}

    @Override
    public int compare(Coordinates a, Coordinates b) {
        if (a.i != b.i) return a.i - b.i;
        if (a.j != b.j) return a.j - b.j;
        return a.k - b.k;
    }

    public static void main(String[] args) {

        Coordinates a = new Coordinates(1, 1, 1);
        Coordinates b = new Coordinates(1, 2, 1);
        Coordinates c = new Coordinates(1, 1, 2);
        Coordinates d = new Coordinates(3, 1, 1);

        System.out.println(LEX_COMPARATOR.compare(a, a)); //  0
        System.out.println(LEX_COMPARATOR.compare(a, b)); // -1
        System.out.println(LEX_COMPARATOR.compare(a, c)); // -1
        System.out.println(LEX_COMPARATOR.compare(c, b)); //  1
        System.out.println(LEX_COMPARATOR.compare(d, a)); //  1
        System.out.println(LEX_COMPARATOR.compare(b, a)); //  2

    }

}
