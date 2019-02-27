package com.github.rloic.aes;

import com.github.rloic.abstraction.MathSet;
import com.github.rloic.abstraction.XOREquation;
import com.github.rloic.collections.BytePosition;
import com.github.rloic.util.Pair;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;
import static com.github.rloic.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.collections.ArrayExtensions.intArrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class GlobalXOR {

    public final Model m;
    public final BoolVar[] sBoxes;
    private final int r;
    private final KeyBits KEY_BITS;

    private final Set<BoolVar> variables = new HashSet<>();
    private final List<BoolVar[]> equations = new ArrayList<>();

    public GlobalXOR(
            int r,
            int objStep1,
            KeyBits keyBits
    ) {
        this.m = new Model("Advanced Model(r=" + r + ", objStep=" + objStep1 + ")");
        this.r = r;
        this.KEY_BITS = keyBits;

        BoolVar[][][] ΔX = buildΔX(r, 4, 4);
        BoolVar[][][] ΔY = new BoolVar[r][4][4];
        // C'4 = SR: ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3], ∆Y[i][j][k] = ∆X[i][j][(j + k) %4]
        for (int i = 0; i <= r - 1; i++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    ΔY[i][j][k] = ΔX[i][j][(j + k) % 4];
                }
            }
        }
        BoolVar[][][] ΔZ = c6(ΔY);
        BoolVar[][][] ΔK = buildΔK(r, 4, 5);
        sBoxes = c1(ΔX, ΔK, objStep1);

        // C'3 = ARK: ∀i ∈ [0, r − 2], ∀j, k ∈ [0, 3], XOR(∆Z[i][j][k], ∆K[i+1][j][k], ∆X[i+1][j][k])
        for (int i = 0; i <= r - 2; i++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    appendToGlobalXor(ΔZ[i][j][k], ΔK[i + 1][j][k], ΔX[i + 1][j][k]);
                }
            }
        }
        // C'5 = MC: ∀i ∈ [0, r − 2], ∀k ∈ [0, 3], Sum(j ∈ 0..3) { ∆Y[i][j][k] + ∆Z[i][j][k] } ∈ {0, 5, 6, 7, 8}
        for (int i = 0; i <= r - 2; i++) {
            for (int k = 0; k <= 3; k++) {
                m.sum(arrayOf(ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                        ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
                ), "=", m.intVar(intArrayOf(0, 5, 6, 7, 8))).post();
            }
        }
        // KeySchedule
        for (XOREquation eq : xorEq()) {
            List<BytePosition> elements = new ArrayList<>(eq);
            appendToGlobalXor(
                    ΔK[elements.get(0).i][elements.get(0).j][elements.get(0).k],
                    ΔK[elements.get(1).i][elements.get(1).j][elements.get(1).k],
                    ΔK[elements.get(2).i][elements.get(2).j][elements.get(2).k]
            );
        }
        BoolVar[][][][][] DY2 = new BoolVar[4][r - 1][4][r - 1][4];
        BoolVar[][][][][] DZ2 = new BoolVar[4][r - 1][4][r - 1][4];
        // MDS constraint
        for (int i1 = 0; i1 < r - 1; i1++) {
            for (int k1 = 0; k1 < 4; k1++) {
                for (int i2 = i1; i2 < r - 1; i2++) {
                    int firstk2 = 0;
                    if (i2 == i1) firstk2 = k1 + 1;
                    for (int k2 = firstk2; k2 < 4; k2++) {
                        for (int j = 0; j < 4; j++) {
                            DY2[j][i1][k1][i2][k2] = m.boolVar();
                            appendToGlobalXor(DY2[j][i1][k1][i2][k2], ΔY[i1][j][k1], ΔY[i2][j][k2]);
                            DZ2[j][i1][k1][i2][k2] = m.boolVar();
                            appendToGlobalXor(DZ2[j][i1][k1][i2][k2], ΔZ[i1][j][k1], ΔZ[i2][j][k2]);
                        }
                        m.sum(arrayOf(
                                DY2[0][i1][k1][i2][k2], DY2[1][i1][k1][i2][k2], DY2[2][i1][k1][i2][k2], DY2[3][i1][k1][i2][k2],
                                DZ2[0][i1][k1][i2][k2], DZ2[1][i1][k1][i2][k2], DZ2[2][i1][k1][i2][k2], DZ2[3][i1][k1][i2][k2]),
                                "=", m.intVar(intArrayOf(0, 5, 6, 7, 8))).post();
                    }
                }
            }
        }

        BoolVar[] vars = new BoolVar[variables.size()];
        variables.toArray(vars);
        BoolVar[][] eqs = new BoolVar[equations.size()][];
        equations.toArray(eqs);

        m.post(new Constraint("GlobalXor", new GlobalXorPropagator(vars, eqs)));
    }

    private BoolVar[][][] buildΔX(int r, int rows, int columns) {
        BoolVar[][][] result = new BoolVar[r][][];
        for (int i = 0; i < r; i++) result[i] = m.boolVarMatrix(rows, columns);
        return result;
    }

    private BoolVar[][][] buildΔK(int r, int rows, int columns) {
        BoolVar[][][] result = new BoolVar[r][rows][columns];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < rows; j++) {
                for (int k = 0; k < columns - 1; k++) {
                    result[i][j][k] = m.boolVar();
                }
            }
        }
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < rows; j++) {
                if (KEY_BITS.isSBRound(i)) {
                    if (KEY_BITS == AES_256 && i % 2 == 0) {
                        result[i][j][4] = result[i][j][getNbCol(i)];
                    } else {
                        result[i][j][4] = result[i][(j + 1) % 4][getNbCol(i)];
                    }
                }
            }
        }
        return result;
    }

    private int getNbCol(int r) {
        if (KEY_BITS == AES_128) {
            return 3;
        } else if (KEY_BITS == AES_192) {
            if (r % 3 == 1) {
                return 1;
            } else {
                return 3;
            }
        } else if (KEY_BITS == AES_256) {
            return 3;
        }
        throw new IllegalStateException();
    }

    private BoolVar[] c1(
            BoolVar[][][] ΔX,
            BoolVar[][][] ΔK,
            int objStep1
    ) {
        List<BoolVar> sBoxesList = new ArrayList<>();
        // C1: objStep1 = Sum(δB ∈ Sboxes_{l}) { ∆B }
        for (int i = 0; i <= r - 1; i++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    sBoxesList.add(ΔX[i][j][k]);
                }
            }
        }
        for (int i = 0; i <= r - 1; i++) {
            for (int j = 0; j <= 3; j++) {
                if (KEY_BITS.isSBRound(i)) {
                    sBoxesList.add(ΔK[i][j][4]);
                }
            }
        }
        BoolVar[] sBoxes = new BoolVar[sBoxesList.size()];
        sBoxesList.toArray(sBoxes);
        m.sum(sBoxes, "=", objStep1).post();
        return sBoxes;
    }


    private BoolVar[][][] c6(BoolVar[][][] ΔY) {
        BoolVar[][][] ΔZ = new BoolVar[r][4][4];
        // ∀j, k ∈ [0, 3]
        for (int j = 0; j <= 3; j++) {
            for (int k = 0; k <= 3; k++) {
                for (int i = 0; i <= r - 2; i++) {
                    ΔZ[i][j][k] = m.boolVar();
                }
                // C6: ∆Z[r−1][j][k] = ∆Y[r−1][j][k]
                ΔZ[r - 1][j][k] = ΔY[r - 1][j][k];
            }
        }
        return ΔZ;
    }


    private void appendToGlobalXor(BoolVar A, BoolVar B, BoolVar C) {
        // m.sum(arrayOf(A, B, C), "!=", 1).post();

        variables.add(A);
        variables.add(B);
        variables.add(C);

        equations.add(arrayOf(A, B, C));
    }


    private MathSet<XOREquation> xorEq() {
        MathSet<XOREquation> initialKeyScheduleXORs = new MathSet<>();
        for (int i = 1; i <= r - 1; i++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    if (!KEY_BITS.isInitialKey(i, k)) {
                        Pair<BytePosition, BytePosition> xorKeySchedule = KEY_BITS.xorKeySchedulePi(i, j, k);
                        XOREquation res = new XOREquation(new BytePosition(i, j, k), xorKeySchedule._0, xorKeySchedule._1);
                        initialKeyScheduleXORs.add(res);
                    }
                }
            }
        }

        return initialKeyScheduleXORs;
    }
}
