package com.github.rloic.midori.round;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.common.collections.ArrayExtensions.intArrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class MidoriRound {

    public final Model m;
    public final IntVar[] nbActives;
    public final BoolVar[] sBoxes;

    public MidoriRound(
            int r,
            int objStep1
    ) {
        this.m = new Model("Advanced Model(r=" + r + ", objStep=" + objStep1 + ")");

        BoolVar[][][] ΔX = new BoolVar[r][][];
        for (int i = 0; i < r; i++) ΔX[i] = m.boolVarMatrix(4, 4);
        BoolVar[][][] ΔY = new BoolVar[r - 1][4][4];
        BoolVar[][][] ΔZ = new BoolVar[r - 1][][];
        for (int i = 0; i < r - 1; i++) ΔZ[i] = m.boolVarMatrix(4, 4);
        BoolVar[][] ΔK = m.boolVarMatrix(4, 4);
        BoolVar[][][][] diffY = new BoolVar[r - 1][4][4][r - 1];
        BoolVar[][][][] diffZ = new BoolVar[r - 1][4][4][r - 1];
        for (int i1 = 0; i1 < r - 1; i1++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int i2 = 0; i2 < i1; i2++) {
                        diffY[i1][j][k][i2] = m.boolVar();
                        diffY[i2][j][k][i1] = diffY[i1][j][k][i2];
                        diffZ[i1][j][k][i2] = m.boolVar();
                        diffZ[i2][j][k][i1] = diffZ[i1][j][k][i2];
                    }
                }
            }
        }
        sBoxes = new BoolVar[r * 4 * 4];
        int cpt = 0;
        for (int i = 0; i <= r - 1; i++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    sBoxes[cpt++] = ΔX[i][j][k];
                }
            }
        }

        nbActives = m.intVarArray(r, 0, objStep1);
        for (int i = 0; i < r; i++) {
            IntVar[] roundX = new IntVar[16];
            cpt = 0;
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    roundX[cpt++] = ΔX[i][j][k];
                }
            }
            m.sum(roundX, "=", nbActives[i]).post();
        }
        m.sum(nbActives, "=", objStep1).post();

        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    xor(ΔZ[i][j][k], ΔK[j][k], ΔX[i + 1][j][k], m);
                }
            }
        }
        for (int i = 0; i < r - 1; i++) {
            ΔY[i][0][0] = ΔX[i][0][0];
            ΔY[i][1][0] = ΔX[i][2][2];
            ΔY[i][2][0] = ΔX[i][1][1];
            ΔY[i][3][0] = ΔX[i][3][3];
            ΔY[i][0][1] = ΔX[i][2][3];
            ΔY[i][1][1] = ΔX[i][0][1];
            ΔY[i][2][1] = ΔX[i][3][2];
            ΔY[i][3][1] = ΔX[i][1][0];
            ΔY[i][0][2] = ΔX[i][1][2];
            ΔY[i][1][2] = ΔX[i][3][0];
            ΔY[i][2][2] = ΔX[i][0][3];
            ΔY[i][3][2] = ΔX[i][2][1];
            ΔY[i][0][3] = ΔX[i][3][1];
            ΔY[i][1][3] = ΔX[i][1][3];
            ΔY[i][2][3] = ΔX[i][2][0];
            ΔY[i][3][3] = ΔX[i][0][2];
            for (int k = 0; k < 4; k++) {
                for (int j = 0; j < 4; j++) {
                    xor(ΔY[i][(j + 1) % 4][k], ΔY[i][(j + 2) % 4][k], ΔY[i][(j + 3) % 4][k], ΔZ[i][j][k], m);
                }
                int[] S = intArrayOf(0, 4, 5, 6, 7, 8);
                IntVar X = m.intVar(S);
                m.sum(
                        arrayOf(
                                ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                                ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
                        ), "=", X).post();
                m.ifOnlyIf(m.sum(arrayOf(ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k]), "=", 0),
                        m.sum(arrayOf(ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]), "=", 0));

            }
        }

        for (int i1 = 0; i1 <= r - 2; i1++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    for (int i2 = 0; i2 < i1; i2++) {
                        xor(diffZ[i1][j][k][i2], ΔZ[i1][j][k], ΔZ[i2][j][k], m);
                        xor(diffY[i1][j][k][i2], ΔY[i1][j][k], ΔY[i2][j][k], m);
                        xor(diffZ[i1][j][k][i2], ΔX[i1 + 1][j][k], ΔX[i2 + 1][j][k], m);
                        for (int i3 = 0; i3 < i2; i3++) {
                            xor(diffY[i1][j][k][i2], diffY[i2][j][k][i3], diffY[i1][j][k][i3], m);
                            xor(diffZ[i1][j][k][i2], diffZ[i2][j][k][i3], diffZ[i1][j][k][i3], m);
                        }
                    }
                }
            }
        }
        for (int i1 = 0; i1 <= r - 2; i1++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    for (int i2 = 0; i2 < i1; i2++) {
                        int[] S = intArrayOf(0, 4, 5, 6, 7, 8);
                        IntVar X = m.intVar(S);
                        m.sum(arrayOf(diffY[i1][0][k][i2], diffY[i1][1][k][i2], diffY[i1][2][k][i2], diffY[i1][3][k][i2],
                                diffZ[i1][0][k][i2], diffZ[i1][1][k][i2], diffZ[i1][2][k][i2], diffZ[i1][3][k][i2]), "=", X).post();
                    }
                }
            }
        }
    }


    void xor(BoolVar A, BoolVar B, BoolVar C, Model m) {
        m.sum(arrayOf(A, B, C), "!=", 1).post();
    }

    void xor(BoolVar A, BoolVar B, BoolVar C, BoolVar D, Model m) {
        m.sum(arrayOf(A, B, C, D), "!=", 1).post();
    }
}