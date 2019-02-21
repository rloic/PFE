package com.github.rloic.util;

import com.github.rloic.inference.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GaussExampleXorList {

    public static void main(String[] args) throws IOException {

        FileWriter output = new FileWriter(new File("output"));
        output.write("");

        DenseMatrix m = new DenseMatrix(
                new int[][]{
                        new int[]{0, 1, 2},
                        new int[]{3, 2, 4},
                        new int[]{5, 4, 6},
                        new int[]{7, 6, 8}
                },
                9
        );
        runStep(m, output);
        output.write(m.toString());
        output.write("\n");
        applyAndInfer(makeAffectation(m, 1, false), m, output);
        output.write(m.toString());
        output.write("\n");
        applyAndInfer(makeAffectation(m, 0, true), m, output);
        output.write(m.toString());
        output.write("\n");

        output.close();
    }

    private static Inferences runStep(InferenceMatrix m, FileWriter writer) throws IOException {
        Inferences step = new Inferences();
        Inferences i;
        do {
            i = m.infer();
            if (!i.isEmpty()) {
                step.addAll(i);
                i.apply(m);
            }
        } while (!i.isEmpty());
        writer.write(step + "\n");
        return step;
    }

    public static void applyAndInfer(Inference i, InferenceMatrix m, FileWriter writer) throws IOException {
        i.apply(m);
        writer.write(i + " => ");
        runStep(m, writer);
    }

    public static Affectation makeAffectation(InferenceMatrix m, int variable, boolean value) {
        if (m.isBase(variable)) {
            int pivot = m.pivotOf(variable);
            int newBaseVariable = 0;
            IntList xorBases = new IntArrayList();
            while (newBaseVariable < m.cols() && (m.isBase(newBaseVariable) || !m.isUnknown(pivot, newBaseVariable))) {
                if (m.isBase(newBaseVariable) && newBaseVariable > variable) {
                    xorBases.add(m.pivotOf(newBaseVariable));
                }
                newBaseVariable += 1;
            }

            if (newBaseVariable == m.cols()) {
                return new AffectationWithBaseRemoval(variable, value, pivot);
            } else {
                IntList xors = m.rowsWhereUnknown(newBaseVariable);
                xors.rem(pivot);
                return new AffectationWithBaseChange(variable, value, newBaseVariable, pivot, xors, xorBases);
            }
        } else {
            return new Affectation(variable, value);
        }

    }

}
