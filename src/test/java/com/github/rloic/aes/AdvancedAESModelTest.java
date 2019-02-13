package com.github.rloic.aes;

import org.chocosolver.solver.variables.BoolVar;
import org.junit.jupiter.api.Test;

import static com.github.rloic.aes.AESBlock.AESBlock128.AES_BLOCK_128;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedAESModelTest {

    @Test
    void should_create_a_valid_diffk() {
        AdvancedAESModel model = new AdvancedAESModel(3, 2, AES_BLOCK_128);
        BoolVar[][][][][] DK = model.DK;
        for(int j = 0; j < 4; j++) {
            for (int k1 = 0; k1 < 5; k1++) {
                for (int k2 = 0; k2 < 5; k2++) {
                    assertNull(DK[j][0][k1][0][k2], "DK must not be defined for i == 0");
                }
            }
        }
        for(int j = 0; j < 4; j++) {
            for(int i1 = 1; i1 < model.rounds + 1; i1++) {
                for(int k1 = 0; k1 < 5; k1++) {
                    for(int i2 = i1; i2 < model.rounds + 1; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for(int k2 = k2Init; k2 < 5; k2++) {
                            assertNotNull(DK[j][i1][k1][i2][k2], "DK must be defined in its domain");
                        }
                    }
                    assertNull(DK[j][i1][k1][i1][k1], "DK must not be defined for diff(δk_{1}, δk_{2}) where k_{1} == k_{2}");
                }
            }
        }
    }

}