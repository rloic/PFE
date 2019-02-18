package com.github.rloic;

import com.github.rloic.abstraction.XOREquation;
import com.github.rloic.collections.BytePosition;
import com.github.rloic.collections.Pair;
import org.chocosolver.solver.variables.BoolVar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AssignableComparison implements Comparator<XOREquation> {

    private final BoolVar[][][][][] diff;

    public AssignableComparison(BoolVar[][][][][] diff) {
        this.diff = diff;
    }

    @Override
    public int compare(XOREquation lhs, XOREquation rhs) {
        return Integer.compare(assignedValues(lhs), assignedValues(rhs));
    }


    private int assignedValues(XOREquation eq) {
        List<BytePosition> elements = new ArrayList<>(eq);
        BytePosition B1 = elements.get(0);
        BytePosition B2 = elements.get(1);
        BytePosition B3 = elements.get(2);
        BytePosition B4 = elements.get(3);

        int assigned = 0;
        if (diff[B1.j][B1.i][B1.k][B2.i][B2.k] != null) {
            assigned += 1;
        }
        if (diff[B3.j][B3.i][B3.k][B4.i][B4.k] != null) {
            assigned += 1;
        }
        return assigned;
    }

    public void insertionSort(List<XOREquation> equations) {
        int n = equations.size();
        for(int i = 1; i < n; i++) {
            XOREquation key = equations.get(i);
            int j = i;
            while (j > 0 && compare(equations.get(j - 1), key) > 0) {
                equations.set(j, equations.get(j - 1));
                j = j - 1;
            }
            equations.set(j, key);
        }
    }


}
