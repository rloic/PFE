package com.github.rloic.aes;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;

class AdvancedModelPaperTest {

    @Test
    void should_succeed_for_aes128_3_5() throws IOException {
        runTest(AES_128, 3, 5);
    }
    @Test
    void should_succeed_for_aes192_3_1() throws IOException {
        runTest(AES_192, 3, 1);
    }
    @Test
    void should_succeed_for_aes192_4_4() throws IOException {
        runTest(AES_192, 4, 4);
    }

    @Test
    void should_succeed_for_aes256_3_1() throws IOException {
        runTest(AES_256, 3, 1);
    }

    @Test
    void should_succeed_for_aes256_4_3() throws IOException {
        runTest(AES_256, 4, 3);
    }

    private List<String> readResponse(String keyBits, int r, int objStep1) throws IOException {
        ClassLoader cls = getClass().getClassLoader();
        File responseFile = new File(cls.getResource("picat_answers/solution_" + keyBits + "_" + r + "_" + objStep1).getFile());
        List<String> responseTuples = Files.readAllLines(responseFile.toPath());
        responseTuples.sort(String::compareTo);
        return responseTuples;
    }

    private String serialize(BoolVar[] sBoxes) {
        StringBuilder str = new StringBuilder("[");
        for(int i = 0; i < sBoxes.length; i++) {
            str.append(sBoxes[i].getValue())
                    .append(",");
        }
        str.setLength(str.length() - 1);
        str.append("]");
        return str.toString();
    }

    private void runTest(KeyBits kBits, int r, int objStep1) throws IOException {
        AdvancedModelPaper model = new AdvancedModelPaper(r, objStep1, kBits);
        String lcKeyBits = "";
        if (kBits == AES_128) {
            lcKeyBits = "aes128";
        } else if (kBits == AES_192) {
            lcKeyBits = "aes192";
        } else if (kBits == AES_256) {
            lcKeyBits = "aes256";
        }
        List<String> picatAnswers = readResponse(lcKeyBits, r, objStep1);
        Solver solver = model.m.getSolver();
        EnumFilter enumFilter = new EnumFilter(model.m, model.sBoxes, objStep1);
        solver.plugMonitor(enumFilter);

        List<String> javaAnswers = new ArrayList<>();
        while (solver.solve()) {
            javaAnswers.add(serialize(model.sBoxes));
        }
        javaAnswers.sort(String::compareTo);
        Assertions.assertEquals(picatAnswers, javaAnswers);
    }

}