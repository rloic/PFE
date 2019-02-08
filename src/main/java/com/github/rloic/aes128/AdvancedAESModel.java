package com.github.rloic.aes128;

import com.github.rloic.AESBlock;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import static com.github.rloic.ArrayExtensions.Array;
import static com.github.rloic.ArrayExtensions.Tensor;

/*
    Advanced AES Model (correspond to CP_{xor})

    Variables :
          Paper Name    Code Name
    - [x] Sboxes        SBoxes
    - [x] ΔX            DX
    - [x] ΔY            DY
    - [x] ΔZ            DZ
    - [x] ΔK            DK
    - [x] DK            diffK
            {δK_{i}[j][k], δSK_{i}[(j + 1) % 4][3] : i ∈ [1, r], k ∈ [0, 3]}
    - [x] DY            diffY
            {δY_{i}[j][k] : i ∈ [0, r − 2], k ∈ [0, 3]}
    - [x] DZ            diffZ
            {δZ_{i}[j][k] : i ∈ [0, r − 2], k ∈ [0, 3]}

    Constraints:
    - [x] C'1 objStep 1 = Sum(δB ∈ Sboxes_{l}) { ΔB }
    - [-] C'2 ∀δB ∈ Sboxes_{l} , ∆SB = ∆B
    - [x] C'3 ∀i ∈ [0, r − 2], ∀j, k ∈ [0, 3], XOR(∆Z_{i}[j][k], ∆K_{i+1}[j][k], ∆X_{i+1}[j][k])
    - [x] C'4 ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3], ∆Y_{i}[j][k] = ∆SX_{i}[j][(j + k) % 4]
    - [x] C'5 ∀i ∈ [0, r − 2], ∀k ∈ [0, 3], Sum(j ∈ 0..3) { ∆Y_{i}[j][k]+∆Z_{i}[j][k] } ∈ {0, 5, 6, 7, 8}
    - [x] C'6 ∀j, k ∈ [0, 3], ∆Z_{r−1}[j][k] = ∆Y_{r−1}[j][k]
    - [x] C'7 ∀D ∈ {DK_{j}, DY_{j}, DZ_{j}: j ∈ [0, 3]}, ∀{δB_{1}, δB_{2} } ⊆ D,
            diff δB_{1},δB_{2} = diff δB_{2},δB_{1}
    - [x] C'8 ∀D ∈ {DK_{j} , DY_{j}, DZ_{j} : j ∈ [0, 3]}, ∀{δB_{1} , δB_{2} , δB_{3} } ⊆ D,
            diff δB_{1} ,δB_{2} + diff δB_{2} ,δB_{3} + diff δB_{1} ,δB_{3} != 1
    - [x] C'9 ∀D ∈ {DK_{j} , DY_{j} , DZ_{j} : j ∈ [0, 3]}, ∀{δB_{1 , δB_{2} } ⊆ D,
            diff δB_{1} ,δB_{2} + ∆B_{1} + ∆B_{2} != 1
    - [ ] C'10 ∀(δB_{1} ⊕ δB_{2} ⊕ δB_{3} = 0) ∈ xorEq_{l} ,
            (diff δB_{1},δB_{2} = ∆B_{3} ) ∧ (diff δB_{1},δB_{3} = ∆B_{2} ) ∧ (diff δB_{2},δB_{3} = ∆B_{1} )
    - [ ] C'11 ∀(δB_{1} ⊕ δB_{2} ⊕ δB_{3} ⊕ δB_{4} = 0) ∈ xorEq_{l}
            (diff δB_{1},δB_{2} = diff δB_{3},δB_{4} ) ∧ (diff δB_{1},δB_{3} = diff δB_{2},δB_{4} ) ∧ (diff δB_{1},δB_{4} = diff δB_{2},δB_{3} )
    - [x] C'12 ∀i_{1} , i_{2} ∈ [0, r − 2], ∀k_{1} , k_{2} ∈ [0, 3]
            Sum(j ∈ 0..3) diff(δYi_{1}[j][k_{1}],δYi_{2}[j][k_{2}]) + diff(δZi_{1}[j][k_{1}],δZi_{2}[j][k_{2}]) ∈ {0, 5, 6, 7, 8}
    - [x] C'13 ∀i_{1} , i_{2} ∈ [0, r − 2], ∀j, k_{1} , k_{2} ∈ [0, 3]
            diff δK_{i1 + 1}[j][k1],δK_{i2 + 1}[j][k2] + diff δZ{i1}[j][k1],δZ_{i 2}[j][k2] + ∆X_{i1 + 1}[j][k 1 ] + ∆X_{i2 + 1}[j][k2] != 1

    Legend:
    - [ ] No implemented
    - [-] Implicit implementation
    - [x] Explicit implementation
 */
public class AdvancedAESModel extends Model {

    private final int rounds;
    private final int objStep1;

    private final BoolVar FALSE = boolVar(false);

    AdvancedAESModel(int rounds, int objStep1) {
        this(rounds, objStep1, AESBlock.AES128);
    }

    AdvancedAESModel(int rounds, int objStep1, AESBlock block) {
        this.rounds = rounds;
        this.objStep1 = objStep1;

        BoolVar[][][] DX = Array(rounds, () -> boolVarMatrix(4, 4));
        // C'4
        BoolVar[][][] DY = shitRows(DX); // DYi = SB(S(DXi)) <=> DYi = SB(DXi)
        BoolVar[][][] DZ = Array(rounds, () -> boolVarMatrix(4, 4));
        BoolVar[][][] DK = Array(rounds, () -> boolVarMatrix(4, 5));

        BoolVar[][][][][] diffK = buildDiffK();
        BoolVar[][][][][] diffY = buildDiffY();
        BoolVar[][][][][] diffZ = buildDiffZ();

        /*
            Implicit constraint C'2 implemented by using ∆B instead of ∆SB
         */

        // C'1
        sBox(DX, DK); // Objects from DX and DK that pass through and S-box
        // C'3
        addRoundKey(DZ, DK, DX); // DXi = DZi xor DK(i-1)
        // C'5 & C'6
        mixColumns(DY, DZ); // Zi = MC(Yi)
        c8andC9ForDiffK(diffK, DK);
        c8ForDiffYAndDiffZ(diffY, DY, diffZ, DZ);
        mdsProperty(diffY, diffZ);
        c13(diffK, diffZ, DX);
    }

    /*
        Variable DK (diffK)
            DK = {δK_{i}[j][k], δSK_{i}[(j + 1) % 4][3] : i ∈ [1, r], k ∈ [0, 3]}
        such as diffK[j][i1][k1][i2][k2] = diff(δK_{i1}[j][k1], δK_{i2}[j][k2])
        implicit C'7 implemented by diffK[j][i2][k2][i1][k1] = diffK[j][i1][k1][i2][k2]
     */
    private BoolVar[][][][][] buildDiffK() {
        // All objects with i1 == 0 or i2 == 0 are nulls, since i ∈ [1, r]
        BoolVar[][][][][] diffK = Tensor(4, rounds + 1, 5, rounds + 1, 5);
        // j ∈ [0, 3]
        for(int j = 0; j < 4; j++) {
            // i ∈ [1, r]
            for (int i1 = 1; i1 < rounds + 1; i1++) {
                // k ∈ [0, 3]
                for (int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [1, r]
                    for (int i2 = i1; i2 < rounds + 1; i2++) {
                        int k2Init = 0;
                        if (i1 == i2) k2Init = k1 + 1;
                        // k ∈ [0, 3]
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            BoolVar diff_dk1_dk2 = boolVar();
                            diffK[j][i1][k1][i2][k2] = diff_dk1_dk2;
                            diffK[j][i2][k2][i1][k1] = diff_dk1_dk2;
                        }
                    }
                }
            }
        }
        return diffK;
    }

    /*
        Variable DY (diffY)
            DY = {δY_{i}[j][k] : i ∈ [0, r − 2], k ∈ [0, 3]}
        such as diffY[j][i1][k1][i2][k2] = diff(δY_{i1}[j][k1], δY_{i2}[j][k2])
        implicit C'7 implemented by diffY[j][i2][k2][i1][k1] = diffY[j][i1][k1][i2][k2]
     */
    private BoolVar[][][][][] buildDiffY() {
        BoolVar[][][][][] diffY = Tensor(4, rounds - 1, 4, rounds - 1, 4);
        // j ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [0, r − 2]
            for (int i1 = 0; i1 < rounds - 1; i1++) {
                // k ∈ [0, 3]
                for(int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [0, r − 2]
                    for(int i2 = i1; i2 < rounds - 1; i2++) {
                        int k2Init = 0;
                        if (i1 == i2) k2Init = k1 + 1;
                        // k ∈ [0, 3]
                        for(int k2 = k2Init; k2 < 4; k2++) {
                            BoolVar diff_dy1_dy2 = boolVar();
                            diffY[j][i1][k1][i2][k2] = diff_dy1_dy2;
                            diffY[j][i2][k2][i1][k1] = diff_dy1_dy2;
                        }
                    }
                }
            }
        }

        return diffY;
    }

    /*
        Variable DZ (diffZ)
            DZ = {δZ_{i}[j][k] : i ∈ [0, r − 2], k ∈ [0, 3]}
        such as diffZ[j][i1][k1][i2][k2] = diff(δZ_{i1}[j][k1], δZ_{i2}[j][k2])
        implicit C'7 implemented by diffZ[j][i2][k2][i1][k1] = diffZ[j][i1][k1][i2][k2]
     */
    private BoolVar[][][][][] buildDiffZ() {
        return buildDiffY();
    }

    /*
        C'1 objStep 1 = Sum(δB ∈ Sboxes_{l}) { ΔB }
     */
    private void sBox(BoolVar[][][] DX, BoolVar[][][] DK) {
        BoolVar[] sBoxes = new BoolVar[20 * rounds];
        int cpt = 0;
        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    sBoxes[cpt++] = DX[i][j][k];
                    if (k == 3) {
                        sBoxes[cpt++] = DK[i][j][k];
                    }
                }
            }
        }
        sum(sBoxes, "=", objStep1).post();
    }

    /*
        Constraint C'3 : Add Round Key
        ∀i ∈ [0, r − 2], ∀j, k ∈ [0, 3], XOR(∆Z_{i}[j][k], ∆K_{i+1}[j][k], ∆X_{i+1}[j][k])
     */
    private void addRoundKey(BoolVar[][][] DZ, BoolVar[][][] DK, BoolVar[][][] DX) {
        for (int i = 0; i < rounds; i++) {
            for(int j = 0; j < 4; j++) {
                for(int k = 0; k < 4; k++) {
                    sum(new IntVar[]{DZ[i-1][j][k], DK[i][j][k], DX[i][j][k]}, "!=", 1).post();
                }
            }
        }
    }

    /*
        Constraint C'4 : Shift Rows
        ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3], ∆Y_{i}[j][k] = ∆SX_{i}[j][(j + k) % 4]
     */
    private BoolVar[][][] shitRows(BoolVar[][][] DX) {
        return Tensor(rounds, 4, 4, (i, j, k) -> DX[i][j][(j + k) % 4]);
    }

    /*
        Constraint C'5 : Mix Columns 1/2
        ∀i ∈ [0, r − 2], ∀k ∈ [0, 3], Sum(j ∈ 0..3) { ∆Y_{i}[j][k]+∆Z_{i}[j][k] } ∈ {0, 5, 6, 7, 8}
        Constraint C'6 : Mix Columns 2/2
        ∀j, k ∈ [0, 3], ∆Z_{r−1}[j][k] = ∆Y_{r−1}[j][k]
     */
    private void mixColumns(BoolVar[][][] DY, BoolVar[][][] DZ) {
        int[] S = new int[]{0, 5, 6, 7, 8};
        for(int i = 0; i < rounds - 1; i++) {
            for (int k = 0; k < 4; k++) {
                // C'5
                sum(new IntVar[]{
                        DY[i][0][k], DY[i][1][k], DY[i][2][k], DY[i][3][k],
                        DZ[i][0][k], DZ[i][1][k], DZ[i][2][k], DZ[i][3][k]
                }, "=", intVar(S)).post();
            }
        }
        // C'6
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                arithm(DZ[rounds - 1][j][k], "=", DY[rounds-1][j][k]);
            }
        }
    }

    /*
        Constraint C'8 for DK (diffK)
        ∀{δB_{1} , δB_{2} , δB_{3}} ⊆ DK,
            diff δB_{1} ,δB_{2} + diff δB_{2} ,δB_{3} + diff δB_{1} ,δB_{3} != 1
        Constraint C'9 for DK (diffK)
        ∀{δB_{1} , δB_{2}} ⊆ DK,
            diff(δB_{1},δB_{2}) + ∆B_{1} + ∆B_{2} != 1
     */
    private void c8andC9ForDiffK(BoolVar[][][][][] diffK, BoolVar[][][] DK) {
        // k ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [1, r]
            for(int i1 = 1; i1 < rounds + 1; i1++) {
                // k ∈ [0, 3]
                for (int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [1, r]
                    for(int i2 = i1; i2 < rounds + 1; i2++) {
                        int k2Init = 0;
                        if (i1 == i2) k2Init = k1 + 1;
                        // k ∈ [0, 3]
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            BoolVar diffk_dK1_dK2 = diffK[j][i1][k1][i2][k2];
                            // C'9 for DZ
                            sum(new IntVar[]{diffk_dK1_dK2, DK[i1][j][k1], DK[i2][j][k2]}, "!=", 1).post();
                            // i ∈ [1, r]
                            for(int i3 = i2; i3 < rounds + 1; i3++) {
                                int k3Init = 0;
                                if (i2 == i3) k3Init = k2 + 1;
                                // k ∈ [0, 3]
                                for(int k3 = k3Init; k3 < 4; k3++) {
                                    BoolVar diffk_dK2_dK3 = diffK[j][i2][k2][i3][k3];
                                    BoolVar diffk_dK1_dK3 = diffK[j][i1][k1][i3][k3];
                                    // C'8 for DZ
                                    sum(new IntVar[]{diffk_dK1_dK2, diffk_dK2_dK3, diffk_dK1_dK3}, "!=", 1).post();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
        Constraint C'8 for DY (diffY) && DZ (diffZ)
        ∀{δB_{1} , δB_{2} , δB_{3}} ⊆ DY (resp. DZ),
            diff δB_{1} ,δB_{2} + diff δB_{2} ,δB_{3} + diff δB_{1} ,δB_{3} != 1
        Constraint C'9 for DK (diffK) && DZ (diffZ)
        ∀{δB_{1} , δB_{2}} ⊆ DY (resp. DZ),
            diff(δB_{1},δB_{2}) + ∆B_{1} + ∆B_{2} != 1
     */
    private void c8ForDiffYAndDiffZ(
            BoolVar[][][][][] diffY,
            BoolVar[][][] DY,
            BoolVar[][][][][] diffZ,
            BoolVar[][][] DZ
    ) {
        // k ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [0, r − 2]
            for(int i1 = 0; i1 < rounds - 1; i1++) {
                // k ∈ [0, 3]
                for (int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [0, r − 2]
                    for(int i2 = i1; i2 < rounds - 1; i2++) {
                        int k2Init = 0;
                        if (i1 == i2) k2Init = k1 + 1;
                        // k ∈ [0, 3]
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            // C'9 for DY
                            BoolVar diffy_dY1_dY2 = diffY[j][i1][k1][i2][k2];
                            sum(new IntVar[]{diffy_dY1_dY2, DY[i1][j][k1], DY[i2][j][k2]}, "!=", 1).post();

                            // C'9 for DZ
                            BoolVar diffz_dZ1_dZ2 = diffZ[j][i1][k1][i2][k2];
                            sum(new IntVar[]{diffz_dZ1_dZ2, DZ[i1][j][k1], DZ[i2][j][k2]}, "!=", 1).post();

                            // i ∈ [0, r − 2]
                            for(int i3 = i2; i3 < rounds - 1; i3++) {
                                int k3Init = 0;
                                if (i2 == i3) k3Init = k2 + 1;
                                // k ∈ [0, 3]
                                for(int k3 = k3Init; k3 < 4; k3++) {
                                    BoolVar diffy_dY2_dY3 = diffY[j][i2][k2][i3][k3];
                                    BoolVar diffy_dY1_dY3 = diffY[j][i1][k1][i3][k3];
                                    // C'8 for DY
                                    sum(new IntVar[]{diffy_dY1_dY2, diffy_dY2_dY3, diffy_dY1_dY3}, "!=", 1).post();
                                    // C'8 for DZ
                                    BoolVar diffz_dZ2_dZ3 = diffZ[j][i2][k2][i3][k3];
                                    BoolVar diffz_dZ1_dZ3 = diffZ[j][i1][k1][i3][k3];
                                    sum(new IntVar[]{diffz_dZ1_dZ2, diffz_dZ2_dZ3, diffz_dZ1_dZ3}, "!=", 1).post();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
        Constraint C'12 : MDS Property
        ∀i_{1} , i_{2} ∈ [0, r − 2], ∀k_{1} , k_{2} ∈ [0, 3]
            Sum(j ∈ 0..3) diff(δYi_{1}[j][k_{1}],δYi_{2}[j][k_{2}]) + diff(δZi_{1}[j][k_{1}],δZi_{2}[j][k_{2}]) ∈ {0, 5, 6, 7, 8}
     */
    private void mdsProperty(BoolVar[][][][][] diffY, BoolVar[][][][][] diffZ) {
        int[] S = new int[]{ 0, 5, 6, 7, 8 };
        // i ∈ [0, r − 2]
        for(int i1 = 0; i1 < rounds - 1; i1++) {
            // k ∈ [0, 3]
            for(int k1 = 0; k1 < 4; k1++) {
                // i ∈ [0, r − 2]
                for(int i2 = i1; i2 < rounds - 1; i2++) {
                    int k2Init = 0;
                    if (i1 == i2) k2Init = k1 + 1;
                    // k ∈ [0, 3]
                    for(int k2 = k2Init; k2 < 4; k2++) {
                        sum(new IntVar[]{
                                diffY[0][i1][k1][i2][k2],diffY[1][i1][k1][i2][k2],diffY[2][i1][k1][i2][k2],diffY[3][i1][k1][i2][k2],
                                diffZ[0][i1][k1][i2][k2],diffZ[1][i1][k1][i2][k2],diffZ[2][i1][k1][i2][k2],diffZ[3][i1][k1][i2][k2]
                        }, "=", intVar(S)).post();
                    }
                }
            }
        }
    }

    /*
        Constraint C'13
        ∀i_{1} , i_{2} ∈ [0, r − 2], ∀j, k_{1} , k_{2} ∈ [0, 3]
            diff δK_{i1 + 1}[j][k1],δK_{i2 + 1}[j][k2] + diff δZ{i1}[j][k1],δZ_{i 2}[j][k2] + ∆X_{i1 + 1}[j][k 1 ] + ∆X_{i2 + 1}[j][k2] != 1
     */
    private void c13(BoolVar[][][][][] diffK, BoolVar[][][][][] diffZ, BoolVar[][][] DX) {
        // j ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [0, r − 2]
            for(int i1 = 0; i1 < rounds - 1; i1++) {
                // k ∈ [0, 3]
                for(int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [0, r − 2]
                    for(int i2 = i1; i2 < rounds - 1; i2++) {
                        int k2Init = 0;
                        if (i1 == i2) k2Init = k1 + 1;
                        // k ∈ [0, 3]
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            sum(new IntVar[]{
                                    diffK[j][i1 + 1][k1][i2 + 1][k2],
                                    diffZ[j][i1][k1][i2][k2],
                                    DX[i1 + 1][j][k1],
                                    DX[i2 + 1][j][k2]
                            }, "!=", 1).post();
                        }
                    }
                }
            }
        }
    }

}
