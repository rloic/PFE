package com.github.rloic.abstraction;

import com.github.rloic.collections.IntTuple;
import com.github.rloic.collections.LexComparator;

import java.util.SortedSet;
import java.util.TreeSet;

public class XOREquation extends TreeSet<IntTuple> implements SortedSet<IntTuple> {

    public XOREquation() {
        super(LexComparator.LEX_COMPARATOR);
    }

    public XOREquation(IntTuple... elements) {
        super(LexComparator.LEX_COMPARATOR);
        for(IntTuple element : elements) {
            add(element);
        }
    }

    public XOREquation(XOREquation origin) {
        super(origin);
    }

    public XOREquation tail() {
        XOREquation copy = new XOREquation(this);
        copy.pollFirst();
        return copy;
    }

}
