package com.github.rloic;

import com.github.rloic.inference.InferenceEngine;
import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.DenseMatrix;
import com.github.rloic.inference.impl.InferenceEngineImpl;
import com.github.rloic.inference.impl.Inferences;
import com.github.rloic.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import static com.github.rloic.util.Logger.DebugLogger.DEBUG;

public class GaussExampleXorList {

    private static final Random random = new Random();
    private static final InferenceEngine engine = new InferenceEngineImpl();

    private static boolean rand() {
        return random.nextBoolean();
    }

    private static Affectation nextRandomAffectation(InferenceMatrix m) {
        for(int variable = 0; variable < m.cols(); variable++) {
            if (!m.isFixed(variable)) {
                return engine.createAffectation(m, variable, rand());
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        Logger.level(DEBUG);

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
        while (!m.isAllFixed()) {
            Affectation newAffectation = nextRandomAffectation(m);
            output.write("Random choice" + newAffectation + "\n");
            newAffectation.apply(m);
            output.write(m.toString());
            output.write("infers => ");
            runStep(m, output);
            output.write(m + "\n");
        }
        output.close();
    }

    private static Inferences runStep(InferenceMatrix m, FileWriter writer) throws IOException {
        Inferences step = engine.inferAndUpdate(m);
        writer.write(step + "\n");
        return step;
    }

}
