package com.github.rloic;

import com.github.rloic.inference.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.BitSet;
import java.util.Scanner;

public class GaussExample {

    static Scanner reader = new Scanner(System.in);

    public static void main(String[] args) {

        DenseMatrix m = new DenseMatrix(new int[][]{
                new int[]{2, 5, 6},
                new int[]{4, 3, 5},
                new int[]{1, 4, 2},
                new int[]{1, 3, 0},
        }, 7);

        DenseMatrix copy = new DenseMatrix(m);

        System.out.println("Initial matrix");
        System.out.println(m);
        Inferences step0 = runStep(m);
        System.out.println(m);
        cIsTrue().apply(m);
        System.out.print(cIsTrue() + " => ");
        Inferences step1 = runStep(m);
        System.out.println(m);
        fIsFalse.apply(m);
        System.out.print(fIsFalse + " => ");
        Inferences step2 = runStep(m);
        System.out.println(m);

        System.out.println("Rollback");
        step2.unapply(m);
        fIsFalse.unapply(m);
        step1.unapply(m);
        cIsTrue().unapply(m);
        step0.unapply(m);
        System.out.println(m);

        System.out.println(m.equals(copy));

    }

    private static Inferences runStep(InferenceMatrix m) {
        Inferences step = new Inferences();
        Inferences i;
        do {
            i = m.infer();
            if (!i.isEmpty()) {
                step.addAll(i);
                i.apply(m);
            }
        } while (!i.isEmpty());
        System.out.println("Inferences: " + step);
        return step;
    }

    private static Inference cIsTrue() {

        IntList xors = new IntArrayList();
        xors.add(1);
        xors.add(2);

        return new AffectationWithBaseChange(2, true, 5, 0, xors, new IntArrayList());
    };
    private static Inference fIsFalse = new AffectationWithBaseRemoval(5, false, 0);


}
