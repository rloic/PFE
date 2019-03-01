package com.github.rloic.aes;

import com.github.rloic.util.Logger;
import com.github.rloic.abstraction.MathSet;
import com.github.rloic.abstraction.XOREquation;
import com.github.rloic.collections.BytePosition;
import com.github.rloic.util.Pair;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;
import static com.github.rloic.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.collections.ArrayExtensions.intArrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class AdvancedModelPaper {

    public final Model m;
    public final BoolVar[] sBoxes;
    private final int r;
    private final KeyBits KEY_BITS;

    public AdvancedModelPaper(
            int r,
            int objStep1,
            KeyBits keyBits
    ) {
        this.m = new Model("Advanced Model(r=" + r + ", objStep=" + objStep1 + ")");
        this.r = r;
        this.KEY_BITS = keyBits;

        BoolVar[][][] ΔX = buildΔX(r, 4, 4);
        BoolVar[][][] ΔY = c4(ΔX); // C'4 shift nbRows
        BoolVar[][][] ΔZ = c6(ΔY);
        BoolVar[][][] ΔK = buildΔK(r, 4, 5);
        sBoxes = c1(ΔX, ΔK, objStep1);

        c2();
        c3(ΔZ, ΔK, ΔX);
        c5(ΔY, ΔZ);

        MathSet<XOREquation> xorEql = xorEq();

        BoolVar[][][][][] diffK = c7DiffK();
        BoolVar[][][][][] diffY = c7DiffY();
        BoolVar[][][][][] diffZ = c7DiffZ();
        c8c9(diffK, ΔK, xorEql);
        c8c9(diffY, ΔY, diffZ, ΔZ);
        c10c11(diffK, ΔK, xorEql);
        c12(diffY, diffZ);
        c13(diffK, diffZ, ΔX);
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

    private void c2() {
        // Implicit use ΔB instead of ΔSB
        // C2: ∀δB ∈ Sboxes_{l} , ∆SB = ∆B
    }

    private void c3(BoolVar[][][] ΔZ, BoolVar[][][] ΔK, BoolVar[][][] ΔX) {
        // ∀i ∈ [0, r − 2], ∀j, k ∈ [0, 3]
        for (int i = 0; i <= r - 2; i++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    // C'3: XOR(∆Z[i][j][k], ∆K[i+1][j][k], ∆X[i+1][j][k])
                    m_xor(ΔZ[i][j][k], ΔK[i + 1][j][k], ΔX[i + 1][j][k]).post();
                }
            }
        }
    }

    private BoolVar[][][] c4(BoolVar[][][] ΔX) {
        BoolVar[][][] ΔY = new BoolVar[r][4][4];
        // ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3]
        for (int i = 0; i <= r - 1; i++) {
            for (int j = 0; j <= 3; j++) {
                for (int k = 0; k <= 3; k++) {
                    // C4: ∆Y[i][j][k] = ∆SX[i][j][(j + k) %4]
                    ΔY[i][j][k] = ΔX[i][j][(j + k) % 4];
                }
            }
        }
        return ΔY;
    }

    private void c5(BoolVar[][][] ΔY, BoolVar[][][] ΔZ) {
        int[] S = intArrayOf(0, 5, 6, 7, 8);
        // ∀i ∈ [0, r − 2], ∀k ∈ [0, 3]
        for (int i = 0; i <= r - 2; i++) {
            for (int k = 0; k <= 3; k++) {
                // C5: Sum(j ∈ 0..3) { ∆Y[i][j][k] + ∆Z[i][j][k] } ∈ {0, 5, 6, 7, 8}
                m.sum(
                        arrayOf(
                                ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                                ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
                        ), "=", m.intVar(S)
                ).post();
            }
        }
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

    // C'7 diffK
    private BoolVar[][][][][] c7DiffK() {
        BoolVar[][][][][] diffK = new BoolVar[4][r][5][r][5];
        // j ∈ [0, 3] i ∈ [0, r - 1], k ∈ [0, 3 + 1] pour δSK
        for (int j = 0; j <= 3; j++) {
            for (int i1 = 0; i1 <= r - 1; i1++) {
                for (int k1 = 0; k1 <= 4; k1++) {
                    for (int i2 = i1; i2 <= r - 1; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 <= 4; k2++) {
                            // C'7: diff(δB1,δB2) = diff(δB2,δB1)
                            BoolVar diff_δk1_δk2 = m.boolVar();
                            diffK[j][i1][k1][i2][k2] = diff_δk1_δk2;
                            diffK[j][i2][k2][i1][k1] = diff_δk1_δk2;
                        }
                    }
                }
            }
        }
        return diffK;
    }

    // C'7 diffY
    private BoolVar[][][][][] c7DiffY() {
        BoolVar[][][][][] diffY = new BoolVar[4][r - 1][4][r - 1][4];
        // j ∈ [0, 3], i ∈ [0, r − 2], k ∈ [0, 3]
        for (int j = 0; j <= 3; j++) {
            for (int i1 = 0; i1 <= r - 2; i1++) {
                for (int k1 = 0; k1 <= 3; k1++) {
                    for (int i2 = i1; i2 <= r - 2; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 <= 3; k2++) {
                            // C'7: diff(δB1,δB2) = diff(δB2,δB1)
                            BoolVar diff_δY1_δY2 = m.boolVar();
                            diffY[j][i1][k1][i2][k2] = diff_δY1_δY2;
                            diffY[j][i2][k2][i1][k1] = diff_δY1_δY2;
                        }
                    }
                }
            }
        }
        return diffY;
    }

    // C'7 diffZ
    private BoolVar[][][][][] c7DiffZ() {
        return c7DiffY();
    }

    private void c8c9(
            BoolVar[][][][][] diffK,
            BoolVar[][][] ΔK,
            MathSet<XOREquation> xorEq
    ) {
        // j ∈ [0, 3] i ∈ [0, r - 1], k ∈ [0, 3 + 1] pour δSK
        for (int j = 0; j <= 3; j++) {
            for (int i1 = 0; i1 <= r - 1; i1++) {
                for (int k1 = 0; k1 <= 4; k1++) {
                    for (int i2 = i1; i2 <= r - 1; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 <= 4; k2++) {
                            BytePosition B1 = new BytePosition(i1, j, k1);
                            BytePosition B2 = new BytePosition(i2, j, k2);
                            if(sameXor(xorEq, B1, B2)) {

                                //C'9: diff(δB_{1},δB_{2}) + ∆B_{1} + ∆B_{2} != 1
                                BoolVar diff_δ1_δ2 = diffOf(diffK, B1, B2);
                                m.sum(arrayOf(diff_δ1_δ2, deltaOf(ΔK, B1), deltaOf(ΔK, B2)), "!=", 1).post();

                                for (int i3 = i2; i3 < r - 1; i3++) {
                                    int k3Init = (i2 == i3) ? k2 + 1 : 0;
                                    for (int k3 = k3Init; k3 <= 4; k3++) {
                                        BytePosition B3 = new BytePosition(i3, j, k3);
                                        if(sameXor(xorEq, B1, B2, B3)) {
                                            // C'8 diffK: diff(δB1,δB2) + diff(δB2,δB3) + diff(δB1,δB3) != 1
                                            BoolVar diff_δ2_δ3 = diffOf(diffK, B2, B3);
                                            BoolVar diff_δ1_δ3 = diffOf(diffK, B1, B3);
                                            m.sum(arrayOf(diff_δ1_δ2, diff_δ2_δ3, diff_δ1_δ3), "!=", 1).post();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void c8c9(
            BoolVar[][][][][] diffY,
            BoolVar[][][] ΔY,
            BoolVar[][][][][] diffZ,
            BoolVar[][][] ΔZ
    ) {
        // j ∈ [0, 3], i ∈ [0, r − 2], k ∈ [0, 3]
        for (int j = 0; j <= 3; j++) {
            for (int i1 = 0; i1 <= r - 2; i1++) {
                for (int k1 = 0; k1 <= 3; k1++) {
                    for (int i2 = i1; i2 <= r - 2; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 <= 3; k2++) {
                            // C'9 diffY: diff(δB1,δB2) + ∆B1 + ∆B2 != 1
                            BoolVar diff_δY1_δY2 = diffY[j][i1][k1][i2][k2];
                            m.sum(arrayOf(diff_δY1_δY2, ΔY[i1][j][k1], ΔY[i2][j][k2]), "!=", 1).post();
                            // C'9 diffZ: diff(δB1,δB2) + ∆B1 + ∆B2 != 1
                            BoolVar diff_δZ1_δZ2 = diffZ[j][i1][k1][i2][k2];
                            m.sum(arrayOf(diff_δZ1_δZ2, ΔZ[i1][j][k1], ΔZ[i2][j][k2]), "!=", 1).post();

                            for (int i3 = i2; i3 <= r - 2; i3++) {
                                int k3Init = (i2 == i3) ? k2 + 1 : 0;
                                for (int k3 = k3Init; k3 <= 3; k3++) {
                                    // C'8 diffY: diff(δB1,δB2) + diff(δB2,δB3) + diff(δB1,δB3) != 1
                                    BoolVar diff_δY2_δY3 = diffY[j][i2][k2][i3][k3];
                                    BoolVar diff_δY1_δY3 = diffY[j][i1][k1][i3][k3];
                                    m.sum(arrayOf(diff_δY1_δY2, diff_δY2_δY3, diff_δY1_δY3), "!=", 1).post();

                                    // C'8 diffZ: diff(δB1,δB2) + diff(δB2,δB3) + diff(δB1,δB3) != 1
                                    BoolVar diff_δZ2_δZ3 = diffZ[j][i2][k2][i3][k3];
                                    BoolVar diff_δZ1_δZ3 = diffZ[j][i1][k1][i3][k3];
                                    m.sum(arrayOf(diff_δZ1_δZ2, diff_δZ2_δZ3, diff_δZ1_δZ3), "!=", 1).post();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // C'10 CHECKED
    private void c10(
            BoolVar[][][][][] diffK,
            BoolVar[][][] ΔK,
            BytePosition B1,
            BytePosition B2,
            BytePosition B3
    ) {
        if (B1.j == B2.j && B1.j == B3.j) {
            // C11: (diff(δB1,δB2) = ∆B_{3}) ∧ (diff(δB1,δB3) = ∆B_{2}) ∧ (diff(δB2,δB3) = ∆B_{1})
            m.arithm(diffOf(diffK, B1, B2), "=", deltaOf(ΔK, B3)).post();
            m.arithm(diffOf(diffK, B1, B3), "=", deltaOf(ΔK, B2)).post();
            m.arithm(diffOf(diffK, B2, B3), "=", deltaOf(ΔK, B1)).post();
        }
    }


    // C'11 CHECKED
    private void c11(
            BoolVar[][][][][] diffK,
            BytePosition B1,
            BytePosition B2,
            BytePosition B3,
            BytePosition B4
    ) {
        if (B1.j == B2.j && B1.j == B3.j && B1.j == B4.j) {

            // C11: (diff(δB1,δB2) = diff(δB3,δB4)) ∧ (diff(δB1,δB3) = diff(δB2,δB4)) ∧ (diff(δB1,δB4) = diff(δB2,δB3))
            m.arithm(diffOf(diffK, B1, B2), "=", diffOf(diffK, B3, B4)).post();
            m.arithm(diffOf(diffK, B1, B3), "=", diffOf(diffK, B2, B4)).post();
            m.arithm(diffOf(diffK, B1, B4), "=", diffOf(diffK, B2, B3)).post();
        }
    }

    // CHECKED
    private void c10c11(
            BoolVar[][][][][] diffK,
            BoolVar[][][] ΔK,
            MathSet<XOREquation> xorEq
    ) {
        for (XOREquation eq : xorEq) {
            List<BytePosition> elements = new ArrayList<>(eq);
            if (elements.size() == 3) {
                // C'10
                c10(diffK, ΔK, elements.get(0), elements.get(1), elements.get(2));
            } else {
                // C'11
                c11(diffK, elements.get(0), elements.get(1), elements.get(2), elements.get(3));
            }
        }
    }

    // CHECKED
    private void c12(
            BoolVar[][][][][] diffY,
            BoolVar[][][][][] diffZ
    ) {
        // ∀i1 , i2 ∈ [0, r − 2], ∀k1, k2 ∈ [0, 3]
        int[] S = intArrayOf(0, 5, 6, 7, 8);
        for (int i1 = 0; i1 <= r - 2; i1++) {
            for (int k1 = 0; k1 <= 3; k1++) {
                for (int i2 = i1; i2 <= r - 2; i2++) {
                    int k2Init = (i1 == i2) ? k1 + 1 : 0;
                    for (int k2 = k2Init; k2 <= 3; k2++) {
                        // Sum(j ∈ 0..3) { diff(δY[i1][j][k1],δY[i2][j][k2]) + diff(δZ[i1][j][k1],δZ[i2][j][k2]) } ∈ {0, 5, 6, 7, 8}
                        m.sum(
                                arrayOf(
                                        diffY[0][i1][k1][i2][k2], diffY[1][i1][k1][i2][k2], diffY[2][i1][k1][i2][k2], diffY[3][i1][k1][i2][k2],
                                        diffZ[0][i1][k1][i2][k2], diffZ[1][i1][k1][i2][k2], diffZ[2][i1][k1][i2][k2], diffZ[3][i1][k1][i2][k2]
                                ), "=", m.intVar(S)
                        ).post();
                    }
                }
            }
        }
    }

    // CHECKED
    private void c13(
            BoolVar[][][][][] diffK,
            BoolVar[][][][][] diffZ,
            BoolVar[][][] ΔX
    ) {
        // ∀i1 , i2 ∈ [0, r − 2], ∀j, k1 , k2 ∈ [0, 3]
        for (int j = 0; j <= 3; j++) {
            for (int i1 = 0; i1 <= r - 2; i1++) {
                for (int k1 = 0; k1 <= 3; k1++) {
                    for (int i2 = i1; i2 <= r - 2; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 <= 3; k2++) {
                            // C13 diff(δK[i1 + 1][j][k1],δK[i2 + 1][j][k2] + diff(δZ[i1][j][k1],δZ[i2][j][k2]) + ∆X[i1 + 1][j][k1] + ∆X[i2 + 1][j][k2] != 1
                            m.sum(arrayOf(
                                    diffK[j][i1 + 1][k1][i2 + 1][k2], diffZ[j][i1][k1][i2][k2], ΔX[i1 + 1][j][k1], ΔX[i2 + 1][j][k2]
                            ), "!=", 1).post();
                        }
                    }
                }
            }
        }
    }

    private Constraint m_xor(BoolVar A, BoolVar B, BoolVar C) {
        return m.sum(arrayOf(A, B, C), "!=", 1);
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

        MathSet<XOREquation> generatedXORs = combineXor(initialKeyScheduleXORs, initialKeyScheduleXORs);
        if (Logger.isDebug()) {
            Logger.debug("Number of initial XOR coming from KS = " + initialKeyScheduleXORs.size());
            Logger.debug("Number of new XORs = " + generatedXORs.size());
            Set<XOREquation> _3ElementsXORs = generatedXORs.stream()
                    .filter(t -> t.size() == 3)
                    .collect(Collectors.toSet());
            _3ElementsXORs.addAll(initialKeyScheduleXORs);
            Logger.debug("Number of XORs of length 3 = " + _3ElementsXORs.size());
            Set<XOREquation> _4ElementsXORs = generatedXORs.stream()
                    .filter(t -> t.size() == 4)
                    .collect(Collectors.toSet());
            Logger.debug("Number of XORs of length 4 = " + _4ElementsXORs.size());
            if (Logger.isTrace()) {
                List<String> eqString = new ArrayList<>(generatedXORs.size() + initialKeyScheduleXORs.size());
                for (XOREquation eq : generatedXORs.union(initialKeyScheduleXORs)) {
                    XOREquation eqForPicat = eq.stream()
                            .map(BytePosition::javaToPicat)
                            .collect(Collectors.toCollection(XOREquation::new));
                    eqString.add(eqForPicat.toString());
                }
                Logger.trace(eqString);
            }
        }

        initialKeyScheduleXORs.addAll(generatedXORs);
        return initialKeyScheduleXORs;
    }

    private MathSet<XOREquation> combineXor(MathSet<XOREquation> lhs, MathSet<XOREquation> rhs) {
        /*
            combineXOR(L1,L2) = Lxor =>
                NewXOR = [],
                foreach(X1 in L1, X2 in L2, X1 != X2)
                    X1X2 = merge(X1,X2),
                    if (len(X1X2)<min(len(X1)+len(X2),5), not membchk(X1X2,L2), not membchk(X1X2,NewXOR)) then
                        %write(X1), print(" + "), write(X2), print(" = "), writeln(X1X2),
                        NewXOR := [X1X2|NewXOR]
                        end
                    end,
                print("   [CombineXOR] Number of new XOR = "), writeln(len(NewXOR)), println("----------"), writeln(NewXOR), println("----------"),
                Lxor = NewXOR ++ combineXOR(NewXOR,NewXOR ++ L2).
         */

        if (lhs.isEmpty()) return new MathSet<>();
        MathSet<XOREquation> newEquationsSet = new MathSet<>();
        for (XOREquation equation1 : lhs) {
            for (XOREquation equation2 : rhs) {
                if (!equation1.equals(equation2)) {
                    XOREquation mergedEquation = equation1.merge(equation2);
                    if (mergedEquation.size() < Math.min(equation1.size() + equation2.size(), 5) && !rhs.contains(mergedEquation)) {
                        // not membchk(X1X2,NewXOR)) is not necessary since newEquationsSet is a Set
                        // so the same equation cannot be present twice
                        newEquationsSet.add(mergedEquation);
                    }
                }
            }
        }
        Logger.debug("    [CombinedXOR] Number of new XOR = " + newEquationsSet.size());
        return newEquationsSet.union(combineXor(newEquationsSet, newEquationsSet.union(rhs)));
    }

    private boolean sameXor(MathSet<XOREquation> xorEq, BytePosition... coordinates) {
        List<BytePosition> checkedCoordinates = Arrays.asList(coordinates);
        for (XOREquation eq : xorEq) {
            if (eq.containsAll(checkedCoordinates)) return true;
        }
        return false;
    }

    private BoolVar diffOf(BoolVar[][][][][] diff, BytePosition B1, BytePosition B2) {
        if (B1.j != B2.j)
            throw new IllegalArgumentException("B1.j must be equals to B2.j. Given: B1=" + B1 + ", B2=" + B2 + ".");
        return diff[B1.j][B1.i][B1.k][B2.i][B2.k];
    }

    private BoolVar deltaOf(BoolVar[][][] Δ, BytePosition B) {
        return Δ[B.i][B.j][B.k];
    }
}
