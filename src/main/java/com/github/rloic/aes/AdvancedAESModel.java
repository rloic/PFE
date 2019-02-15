package com.github.rloic.aes;

import com.github.rloic.Logger;
import com.github.rloic.abstraction.MathSet;
import com.github.rloic.abstraction.XOREquation;
import com.github.rloic.collections.BytePosition;
import com.github.rloic.collections.Pair;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;
import static com.github.rloic.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.collections.ArrayExtensions.intArrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class AdvancedAESModel extends Model implements AESModel {

    final int rounds;
    private final int objStep1;
    private final KeyBits block;
    private final BoolVar[] sBoxes;

    public AdvancedAESModel(int rounds, int objStep1) {
        this(rounds, objStep1, AES_128);
    }

    public AdvancedAESModel(int rounds, int objStep1, KeyBits block) {
        super("Advanced AES Model(r=" + rounds + ", objStep1=" + objStep1 + ")");
        this.rounds = rounds;
        this.objStep1 = objStep1;
        this.block = block;
        MathSet<XOREquation> xorEq = xorEq();

        //----- Variables -------------------------------------------------------------
        BoolVar[][][] ΔX = fill(new BoolVar[rounds][4][4]);
        BoolVar[][][] ΔY = shiftRows(ΔX);
        BoolVar[][][] ΔZ = fill(new BoolVar[rounds][4][4]);
        BoolVar[][][] ΔK = fill(new BoolVar[rounds][4][4 + 1]); // The 5th column corresponds to ΔSK
        BoolVar[][][][][] diffK = buildD(new BoolVar[4][rounds][5][rounds][5]);
        BoolVar[][][][][] diffY = buildD(new BoolVar[4][rounds - 1][4][rounds - 1][4]);
        BoolVar[][][][][] diffZ = buildD(new BoolVar[4][rounds - 1][4][rounds - 1][4]);
        //----- Variables -------------------------------------------------------------
        keySubBytes(ΔK);
        sBoxes = activeSBoxes(ΔX, ΔK, objStep1);
        addRoundKey(ΔZ, ΔK, ΔX);
        mixColumns(ΔY, ΔZ);
        c6(ΔZ, ΔY);
        mds(diffY, diffZ);
        augmentedConstraints(ΔK, diffK, xorEq);
        keyConstraint(ΔK, diffK, xorEq);
        c8C9(ΔY, ΔZ, ΔX, diffY, diffZ, diffK);
    }

    @Override
    public BoolVar[] getSBoxes() {
        return sBoxes;
    }

    // Variables initializers
    private BoolVar[][][] fill(BoolVar[][][] Δ) {
        for (int i = 0; i < Δ.length; i++) {
            for (int j = 0; j < Δ[i].length; j++) {
                for (int k = 0; k < Δ[i][j].length; k++) {
                    Δ[i][j][k] = boolVar();
                }
            }
        }
        return Δ;
    }

    // C'7
    private BoolVar[][][][][] buildD(BoolVar[][][][][] D) {
        for (int j = 0; j < D.length; j++) {
            for (int i1 = 0; i1 < D[j].length; i1++) {
                for (int k1 = 0; k1 < D[j][i1].length; k1++) {
                    for (int i2 = i1; i2 < D[j][i1][k1].length; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 < D[j][i1][k1][i2].length; k2++) {
                            BoolVar diff_δ1_δ2 = boolVar();
                            D[j][i1][k1][i2][k2] = diff_δ1_δ2;
                            D[j][i2][k2][i1][k1] = diff_δ1_δ2; // Symmetry
                        }
                    }
                }
            }
        }
        return D;
    }

    // Constraints

    // C'4
    private BoolVar[][][] shiftRows(BoolVar[][][] ΔX) {
        final BoolVar[][][] ΔY = new BoolVar[rounds][4][4];
        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    ΔY[i][j][k] = ΔX[i][j][(j + k) % 4];
                }
            }
        }
        return ΔY;
    }

    // C'1
    private BoolVar[] activeSBoxes(BoolVar[][][] ΔX, BoolVar[][][] ΔK, int objStep1) {
        BoolVar[] sBoxes = new BoolVar[20 * rounds]; // TODO replace for 192 / 256
        int cpt = 0;
        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    sBoxes[cpt++] = ΔX[i][j][k];
                }
                if (block.isSBRound(i)) {
                    sBoxes[cpt++] = ΔK[i][j][4];
                }
            }
        }
        sum(sBoxes, "=", objStep1).post();
        return sBoxes;
    }

    // OPTIMIZE do not create unused boolVar when initiating ΔK
    private void keySubBytes(BoolVar[][][] ΔK) {
        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                if (block.isSBRound(i)) {
                    if (block != AES_256 || i % 2 == 1) {
                        arithm(ΔK[i][j][4], "=", ΔK[i][(j + 1) % 4][block.getNbCol(i)]).post();
                    } else {
                        arithm(ΔK[i][j][4], "=", ΔK[i][j][block.getNbCol(i)]).post();
                    }
                } else {
                    arithm(ΔK[i][j][4], "=", 0).post();
                }
            }
        }
    }

    // C'3
    private void addRoundKey(BoolVar[][][] ΔZ, BoolVar[][][] ΔK, BoolVar[][][] ΔX) {
        for (int i = 0; i < rounds - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    xor(ΔZ[i][j][k], ΔK[i + 1][j][k], ΔX[i + 1][j][k]).post();
                }
            }
        }
    }

    // C'5
    private void mixColumns(BoolVar[][][] ΔY, BoolVar[][][] ΔZ) {
        int[] S = intArrayOf(0, 5, 6, 7, 8);
        for (int i = 0; i < rounds - 1; i++) {
            for (int k = 0; k < 4; k++) {
                sum(arrayOf(
                        ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                        ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
                ), "=", intVar(S)).post();
            }
        }
    }

    // C'6 Can be optimized by removing the last round of ΔZ
    private void c6(BoolVar[][][] ΔZ, BoolVar[][][] ΔY) {
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                arithm(ΔZ[rounds - 1][j][k], "=", ΔY[rounds - 1][j][k]).post();
            }
        }
    }

    // C'12
    private void mds(BoolVar[][][][][] diffY, BoolVar[][][][][] diffZ) {
        // forall(i1 in 1..r-1, j in 0..3, k1 in 0..3,i2 in 1..r-1, k2 in 0..3 where i2>i1 \/ (i2==i1 /\ k2>k1))
        int[] S = intArrayOf(0, 5, 6, 7, 8);
        for (int i1 = 0; i1 < rounds - 1; i1++) {
            for (int k1 = 0; k1 < 4; k1++) {
                for (int i2 = i1; i2 < rounds - 1; i2++) {
                    int k2Init = (i1 == i2) ? k1 + 1 : 0;
                    for (int k2 = k2Init; k2 < 4; k2++) {
                        sum(arrayOf(
                                diffY[0][i1][k1][i2][k2], diffY[1][i1][k1][i2][k2], diffY[2][i1][k1][i2][k2], diffY[3][i1][k1][i2][k2],
                                diffZ[0][i1][k1][i2][k2], diffZ[1][i1][k1][i2][k2], diffZ[2][i1][k1][i2][k2], diffZ[3][i1][k1][i2][k2]),
                                "=", intVar(S)).post();
                    }
                }
            }
        }
    }

    // C'10 & C'11
    private void augmentedConstraints(BoolVar[][][] ΔK, BoolVar[][][][][] diffK, MathSet<XOREquation> xorEq) {
        for (XOREquation equation : xorEq) {
            List<BytePosition> elements = new ArrayList<>(equation);
            if (equation.size() == 3) {
                xor3(elements.get(0), elements.get(1), elements.get(2), ΔK, diffK);
            } else if (equation.size() == 4) {
                xor4(elements.get(0), elements.get(1), elements.get(2), elements.get(3), diffK);
            }
        }
    }

    // C'9
    private void keyConstraint(BoolVar[][][] ΔK, BoolVar[][][][][] diffK, Set<XOREquation> xorEq) {
        for (int j = 0; j < 4; j++) {
            for (int i1 = 1; i1 < rounds; i1++) {
                for (int k1 = 0; k1 < 5; k1++) {
                    for (int i2 = i1; i2 < rounds; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 < 5; k2++) {
                            BytePosition A = new BytePosition(i1, j, k1);
                            BytePosition B = new BytePosition(i2, j, k2);

                            if (sameXor(A, B, xorEq)) {
                                sum(arrayOf(diffK[j][i1][k1][i2][k2], ΔK[i1][j][k1], ΔK[i2][j][k2]), "!=", 1).post();
                                for (int i3 = i2; i3 < rounds; i3++) {
                                    int k3Init = (i2 == i3) ? k2 + 1 : 0;
                                    for (int k3 = k3Init; k3 < 5; k3++) {
                                        BytePosition C = new BytePosition(i3, j, k3);
                                        if (sameXor(A, B, C, xorEq)) {
                                            sum(arrayOf(diffK[j][i1][k1][i2][k2], diffK[j][i2][k2][i3][k3], diffK[j][i1][k1][i3][k3]), "!=", 1);

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

    private void c8C9(
            BoolVar[][][] ΔY,
            BoolVar[][][] ΔZ,
            BoolVar[][][] ΔX,
            BoolVar[][][][][] diffY,
            BoolVar[][][][][] diffZ,
            BoolVar[][][][][] diffK
    ) {
        // TODO ajouter SameXOR
        for (int j = 0; j < 4; j++) {
            for (int i1 = 0; i1 < rounds - 1; i1++) {
                for (int k1 = 0; k1 < 4; k1++) {
                    for (int i2 = i1; i2 < rounds - 1; i2++) {
                        int k2Init = (i1 == i2) ? k1 + 1 : 0;
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            BoolVar diff_δY1_δY2 = diffY[j][i1][k1][i2][k2];
                            sum(arrayOf(diff_δY1_δY2, ΔY[i1][j][k1], ΔY[i2][j][k2]), "!=", 1).post();

                            BoolVar diff_δZ1_δZ2 = diffZ[j][i1][k1][i2][k2];
                            sum(arrayOf(diff_δZ1_δZ2, ΔZ[i1][j][k1], ΔZ[i2][j][k2]), "!=", 1).post();

                            BoolVar diff_δK1_δK2 = diffK[j][i1 + 1][k1][i2 + 1][k2];
                            sum(arrayOf(diff_δK1_δK2, diff_δZ1_δZ2, ΔX[i1 + 1][j][k1], ΔX[i2 + 1][j][k2]), "!=", 1).post();

                            for (int i3 = i2; i3 < rounds - 1; i3++) {
                                int k3Init = (i2 == i3) ? k2 + 1 : 0;
                                for (int k3 = k3Init; k3 < 4; k3++) {
                                    BoolVar diff_δY2_δY3 = diffY[j][i2][k2][i3][k3];
                                    BoolVar diff_δY1_δY3 = diffY[j][i1][k1][i3][k3];

                                    sum(arrayOf(diff_δY1_δY2, diff_δY2_δY3, diff_δY1_δY3), "!=", 1).post();

                                    BoolVar diff_δZ2_δZ3 = diffZ[j][i2][k2][i3][k3];
                                    BoolVar diff_δZ1_δZ3 = diffZ[j][i1][k1][i3][k3];
                                    sum(arrayOf(diff_δZ1_δZ2, diff_δZ2_δZ3, diff_δZ1_δZ3), "!=", 1).post();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Utils
    private MathSet<XOREquation> xorEq() {
        MathSet<XOREquation> initialKeyScheduleXORs = new MathSet<>();
        for (int i = 1; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    if (!block.isInitialKey(i, k)) {
                        Pair<BytePosition, BytePosition> xorKeySchedule = block.xorKeySchedulePi(i, j, k);
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

    private Constraint xor(BoolVar... elements) {
        return sum(arrayOf(elements), "!=", 1);
    }

    private void xor3(
            BytePosition A,
            BytePosition B,
            BytePosition C,
            BoolVar[][][] ΔK,
            BoolVar[][][][][] diffK
    ) {
        if (A.j == B.j && A.j == C.j) {
            arithm(diffK[A.j][A.i][A.k][B.i][B.k], "=", ΔK[C.i][C.j][C.k]).post();
            arithm(diffK[A.j][A.i][A.k][C.i][C.k], "=", ΔK[B.i][B.j][B.k]).post();
            arithm(diffK[B.j][B.i][B.k][C.i][C.k], "=", ΔK[A.i][A.j][A.k]).post();
        }
    }

    private void xor4(
            BytePosition A,
            BytePosition B,
            BytePosition C,
            BytePosition D,
            BoolVar[][][][][] diffK
    ) {
        if (A.j == B.j && A.j == C.j && A.j == D.j) {
            arithm(diffK[A.j][A.i][A.k][B.i][B.k], "=", diffK[C.j][C.i][C.k][D.i][D.k]).post();
            arithm(diffK[A.j][A.i][A.k][C.i][C.k], "=", diffK[B.j][B.i][B.k][D.i][D.k]).post();
            arithm(diffK[A.j][A.i][A.k][D.i][D.k], "=", diffK[B.j][B.i][B.k][C.i][C.k]).post();
        }
    }

    private boolean sameXor(BytePosition a, BytePosition b, Set<XOREquation> xorL) {
        for (XOREquation eq : xorL) {
            if (eq.contains(a) && eq.contains(b)) return true;
        }
        return false;
    }

    private boolean sameXor(
            BytePosition a,
            BytePosition b,
            BytePosition c,
            Set<XOREquation> xorL
    ) {
        for (XOREquation eq : xorL) {
            if (eq.contains(a) && eq.contains(b) && eq.contains(c)) return true;
        }
        return false;
    }

}
