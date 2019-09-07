package com.github.rloic.common.abstraction;

import com.github.rloic.common.collections.BytePosition;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

import static com.github.rloic.common.utils.LexComparator.LEX_COMPARATOR;

public class XOREquation extends MathSet<BytePosition> {

    public XOREquation() {}

    public XOREquation(BytePosition... elements) {
        addAll(Arrays.asList(elements));
    }

    public XOREquation(XOREquation origin) {
        super(origin);
    }

    public XOREquation(@NotNull Collection<? extends BytePosition> c) {
        super(c);
    }

    @Override
    public String toString() {
        if (isEmpty()) return "[]";
        StringBuilder str = new StringBuilder("[");
        stream().sorted(LEX_COMPARATOR)
                .forEach(c -> {
                    str.append(c)
                            .append(",");
                });
        str.setLength(str.length() - 1);
        str.append("]");
        return str.toString();
    }

    /*
        Picat:
        merge([],L2) = L3 => L3=L2.
        merge(L1,[]) = L3 => L3=L1.
        merge([H1|T1],[H2|T2]) = L3, H1 @< H2 => L3 = [H1|merge(T1,[H2|T2])].
        merge([H1|T1],[H2|T2]) = L3, H2 @< H1 => L3 = [H2|merge([H1|T1],T2)].
        merge([H|T1],[H|T2]) = L3 => L3 = merge(T1,T2).
     */
    public XOREquation merge(XOREquation other) {
        XOREquation mergeEq = new XOREquation();
        for (BytePosition tuple : this) {
            if (!other.contains(tuple)) {
                mergeEq.add(tuple);
            }
        }
        for (BytePosition tuple : other) {
            if (!contains(tuple)) {
                mergeEq.add(tuple);
            }
        }
        return mergeEq;
    }

}
