package com.github.rloic;

import com.github.rloic.inference.InferenceEngine;
import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.DenseMatrix;
import com.github.rloic.inference.impl.InferenceEngineImpl;
import com.github.rloic.inference.impl.Inferences;
import org.chocosolver.solver.exception.ContradictionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {

    private static final InferenceEngine engine = new InferenceEngineImpl();

    public static void main(String[] args) throws IOException, ContradictionException {

        long sumTime = 0L;
        int sumNbChoices = 0;
        final int N = 1_000;
        for(int i = 0; i < N; i++) {
            InferenceMatrix matrix = new DenseMatrix(new int[][]{
                    new int[] { 0, 1, 2 },
                    new int[] { 3, 4, 5 },
                    new int[] { 6, 7, 8 },
                    new int[] { 9, 10, 11 },
                    new int[] { 12, 0, 13 },
                    new int[] { 14, 12, 15 },
                    new int[] { 16, 14, 17 },
                    new int[] { 18, 3, 19 },
                    new int[] { 20, 18, 21 },
                    new int[] { 22, 20, 23 },
                    new int[] { 24, 6, 25 },
                    new int[] { 26, 24, 27 },
                    new int[] { 28, 26, 29 },
                    new int[] { 30, 9, 31 },
                    new int[] { 32, 30, 33 },
                    new int[] { 34, 32, 35 }
            }, 36);
            long start = System.currentTimeMillis();
            sumNbChoices += solve(matrix);
            sumTime += (System.currentTimeMillis()) - start;
        }
        System.out.println("Mean nb choices: " + ((float)sumNbChoices / N));
        System.out.println("Mean execution time: " + ((float)sumTime / N));
    }

    private static final Random rand = new Random();

    private static Affectation nextRandomAffectation(InferenceMatrix m) {
        List<Integer> indices = new ArrayList<>(m.cols());
        for(int i = 0; i < m.cols(); i++) {
            indices.add(i);
        }
        int index = rand.nextInt(indices.size());
        while (m.isFixed(indices.get(index))) {
            indices.remove(index);
            index = rand.nextInt(indices.size());
        }
        return engine.createAffectation(m, indices.get(index), rand.nextBoolean());
    }

    private static int solve(InferenceMatrix matrix) throws IOException, ContradictionException {
        FileWriter output = new FileWriter(new File("output"));
        int choices = 0;
        while (!matrix.isAllFixed()) {
            Affectation newAffectation = nextRandomAffectation(matrix);
            output.write("Random choice" + newAffectation + "\n");
            newAffectation.apply(matrix);
            output.write(matrix.toString());
            output.write("infers => ");
            Inferences step = engine.inferAndUpdate(matrix);
            output.write(step + "\n");
            output.write(matrix + "\n");
            choices += 1;
        }
        output.close();
        return choices;
    }

}
