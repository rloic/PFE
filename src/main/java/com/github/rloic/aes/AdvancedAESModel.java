package com.github.rloic.aes;

import com.github.rloic.Logger;
import com.github.rloic.abstraction.MathSet;
import com.github.rloic.abstraction.XOREquation;
import com.github.rloic.collections.Coordinates;
import com.github.rloic.collections.Pair;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.rloic.aes.AESBlock.AESBlock128.AES_BLOCK_128;
import static com.github.rloic.aes.AESBlock.AESBlock256.AES_BLOCK_256;
import static com.github.rloic.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.collections.ArrayExtensions.intArrayOf;
import static java.lang.Math.*;

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
@SuppressWarnings("NonAsciiCharacters")
public class AdvancedAESModel extends Model {

    final int rounds;
    private final int objStep1;
    private final AESBlock block;
    private final MathSet<XOREquation> xorEq;

    final BoolVar[][][] ΔX;
    final BoolVar[][][] ΔY;
    final BoolVar[][][] ΔZ;
    final BoolVar[][][] ΔK;
    final BoolVar[][][][][] DK;
    final BoolVar[][][][][] DY;
    final BoolVar[][][][][] DZ;

    public AdvancedAESModel(int rounds, int objStep1) {
        this(rounds, objStep1, AES_BLOCK_128);
    }

    public AdvancedAESModel(int rounds, int objStep1, AESBlock block) {
        super("Advanced AES Model(r=" + rounds + ", objStep1=" + objStep1 + ")");
        this.rounds = rounds;
        this.objStep1 = objStep1;
        this.block = block;
        this.xorEq = xorEq();

        ΔX = buildΔ(rounds, 4, 4); // State after ARK
        BoolVar[][][] ΔSX = subBytes(ΔX); // State after SB
        ΔY = shitRows(ΔSX);  //
        ΔZ = buildΔ(rounds, 4, 4);
        ΔK = buildΔ(rounds, 4, 5);

        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    if (block.isSBox(i, k)) {
                        if (block != AES_BLOCK_256 || i % 2 == 1) {
                            // TODO replace nbColumnsThroughSBox
                            ΔK[i][j][k] = ΔK[i][floorMod(j + 1, 4)][block.nbColumnsThroughSBox(i)];
                        } else {
                            ΔK[i][j][k] = ΔK[i][j][4];
                        }
                    }
                }
            }
        }

        DK = buildDK();
        DY = buildDY();
        DZ = buildDZ();

        /*
            Implicit constraint C'2 implemented by using ∆B instead of ∆SB
         */

        // C'1
        sBox(ΔX, ΔK); // Objects from ΔX and ΔK that pass through and S-box
        // C'3
        addRoundKey(ΔZ, ΔK, ΔX); // ΔXi = ΔZi xor ΔK(i-1)
        // C'5 & C'6
        // OPTIMIZE maybe use ΔZ = ΔY for last round (reference equality) instead of constraints
        mixColumns(ΔY, ΔZ); // ΔZi = MC(ΔYi)
        c8andC9ForDK(DK, ΔK);
        c8ForDiffYAndDiffZ(DY, ΔY, DZ, ΔZ);
        mdsProperty(DY, DZ);
        c10AndC11(ΔK, DK);
        c13(DK, DZ, ΔX);
    }

    private BoolVar[][][] buildΔ(int rounds, int rows, int columns) {
        BoolVar[][][] D = new BoolVar[rounds][][];
        for (int i = 0; i < rounds; i++) D[i] = boolVarMatrix(rows, columns);
        return D;
    }

    /*
        Variable DK (diffK)
            DK = {δK_{i}[j][k], δSK_{i}[(j + 1) % 4][3] : i ∈ [1, r], k ∈ [0, 3]}
        such as diffK[j][i1][k1][i2][k2] = diff(δK_{i1}[j][k1], δK_{i2}[j][k2])
        implicit C'7 implemented by diffK[j][i2][k2][i1][k1] = diffK[j][i1][k1][i2][k2]
     */
    private BoolVar[][][][][] buildDK() {
        // All objects with i1 == 0 or i2 == 0 are nulls, since i ∈ [1, r]
        BoolVar[][][][][] DK = new BoolVar[4][rounds][5][rounds][5];
        // j ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [1, r]
            for (int i1 = 1; i1 < rounds; i1++) {
                // k ∈ [0, 3 + 1]
                for (int k1 = 0; k1 < 5; k1++) {
                    // i ∈ [1, r]
                    for (int i2 = i1; i2 < rounds; i2++) {
                        int k2Init = (i1 != i2) ? 0 : k1 + 1;
                        // k ∈ [0, 3 + 1]
                        for (int k2 = k2Init; k2 < 5; k2++) {
                            BoolVar diff_δk1_δk2 = boolVar();
                            DK[j][i1][k1][i2][k2] = diff_δk1_δk2;
                            DK[j][i2][k2][i1][k1] = diff_δk1_δk2;
                        }
                    }
                }
            }
        }
        return DK;
    }

    /*
        Variable DY (diffY)
            DY = {δY_{i}[j][k] : i ∈ [0, r − 2], k ∈ [0, 3]}
        such as diffY[j][i1][k1][i2][k2] = diff(δY_{i1}[j][k1], δY_{i2}[j][k2])
        implicit C'7 implemented by diffY[j][i2][k2][i1][k1] = diffY[j][i1][k1][i2][k2]
     */
    private BoolVar[][][][][] buildDY() {
        BoolVar[][][][][] DY = new BoolVar[4][rounds - 1][4][rounds - 1][4];
        // j ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [0, r − 2]
            for (int i1 = 0; i1 < rounds - 1; i1++) {
                // k ∈ [0, 3]
                for (int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [0, r − 2]
                    for (int i2 = i1; i2 < rounds - 1; i2++) {
                        int k2Init = (i1 != i2) ? 0 : k1 + 1;
                        // k ∈ [0, 3]
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            BoolVar diff_δy1_δy2 = boolVar();
                            DY[j][i1][k1][i2][k2] = diff_δy1_δy2;
                            DY[j][i2][k2][i1][k1] = diff_δy1_δy2;
                        }
                    }
                }
            }
        }

        return DY;
    }

    /*
        Variable DZ (diffZ)
            DZ = {δZ_{i}[j][k] : i ∈ [0, r − 2], k ∈ [0, 3]}
        such as diffZ[j][i1][k1][i2][k2] = diff(δZ_{i1}[j][k1], δZ_{i2}[j][k2])
        implicit C'7 implemented by diffZ[j][i2][k2][i1][k1] = diffZ[j][i1][k1][i2][k2]
     */
    private BoolVar[][][][][] buildDZ() {
        return buildDY();
    }


    private BoolVar[][][] subBytes(BoolVar[][][] ΔX) {
        return ΔX;
    }

    /*
        C'1 objStep 1 = Sum(δB ∈ Sboxes_{l}) { ΔB }
     */
    private void sBox(BoolVar[][][] ΔX, BoolVar[][][] ΔK) {
        BoolVar[] sBoxes = new BoolVar[20 * rounds];
        int cpt = 0;
        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k <= 4; k++) {
                    if (k < 4) {
                        sBoxes[cpt++] = ΔX[i][j][k];
                    } else {
                        if (block.isSBRound(i))
                        sBoxes[cpt++] = ΔK[i][j][4];
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
    private void addRoundKey(BoolVar[][][] ΔZ, BoolVar[][][] ΔK, BoolVar[][][] ΔX) {
        /*
            Picat:
            foreach(I in 2..R, J in 1..4, K in 1..4)
                DZ[I-1,J,K] + DK[I,J,K] + DX[I,J,K] #!= 1
            end
         */
        for (int i = 0; i < rounds - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    xor(ΔZ[i][j][k], ΔK[i + 1][j][k], ΔX[i + 1][j][k]).post();
                }
            }
        }
    }

    /*
        Constraint C'4 : Shift Rows
        ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3], ∆Y_{i}[j][k] = ∆SX_{i}[j][(j + k) % 4]
     */
    private BoolVar[][][] shitRows(BoolVar[][][] ΔX) {
        /*
            Picat:
            foreach(I in 1..R, J in 1..4, K in 1..4)
                DY[I,J,K] = DX[I, J, ((J+K-2) mod 4)+1],
            end
         */
        BoolVar[][][] ΔY = new BoolVar[rounds][4][4];
        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    ΔY[i][j][k] = ΔX[i][j][floorMod(j + k, 4)];
                }
            }
        }
        return ΔY;
    }

    /*
        Constraint C'5 : Mix Columns 1/2
        ∀i ∈ [0, r − 2], ∀k ∈ [0, 3], Sum(j ∈ 0..3) { ∆Y_{i}[j][k]+∆Z_{i}[j][k] } ∈ {0, 5, 6, 7, 8}
        Constraint C'6 : Mix Columns 2/2
        ∀j, k ∈ [0, 3], ∆Z_{r−1}[j][k] = ∆Y_{r−1}[j][k]
     */
    private void mixColumns(BoolVar[][][] ΔZ, BoolVar[][][] ΔY) {
        /*
            Picat:
            foreach(I in 1..R-1, K in 1..4)
                S :: [0,5,6,7,8],
                sum([DY[I,J,K] : J in 1..4]) + sum([DZ[I,J,K] : J in 1..4]) #= S
            end
         */
        final int[] S = intArrayOf(0, 5, 6, 7, 8);
        for (int i = 0; i < rounds - 1; i++) {
            for (int k = 0; k < 4; k++) {
                // C'5
                sum(arrayOf(
                        ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                        ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
                ), "=", intVar(S)).post();
            }
        }
        // C'6
        /*
            Picat:
            foreach(J in 1..4, K in 1..4)
                DZ[R,J,K] = DY[R,J,K],
            end
         */
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                arithm(ΔZ[rounds - 1][j][k], "=", ΔY[rounds - 1][j][k]);
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
    private void c8andC9ForDK(BoolVar[][][][][] DK, BoolVar[][][] ΔK) {
        /*
            Picat:
            foreach(I1 in 2..R, J in 1..4, K1 in 1..5, I2 in 2..I1, K2 in 1..5, [I1,K1] @> [I2,K2])
                > DIFFK[I1,J,K1,I2,K2] = DIFFK[I2,J,K2,I1,K1], < Implemented in buildDK
                if (sameXOR([I1,J,K1],[I2,J,K2],LXor)) then
                    DIFFK[I1,J,K1,I2,K2] + DK[I1,J,K1] + DK[I2,J,K2] #!= 1,
                    foreach(I3 in 2..I2, K3 in 1..5, [I2,K2] @> [I3,K3], sameXOR([I1,J,K1],[I2,J,K2],[I3,J,K3],LXor))
                        DIFFK[I1,J,K1,I2,K2] + DIFFK[I2,J,K2,I3,K3] + DIFFK[I1,J,K1,I3,K3] #!= 1 %%%
                    end
                end
            end
         */
        // j ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [1, r]
            for (int i1 = 1; i1 < rounds; i1++) {
                // k ∈ [0, 3 + 1 (ΔSEK)]
                for (int k1 = 0; k1 < 5; k1++) {
                    // i ∈ [1, r]
                    for (int i2 = i1; i2 < rounds; i2++) {
                        int k2Init = (i1 != i2) ? 0 : k1 + 1;
                        // k ∈ [0, 3 + 1 (ΔSEK)]
                        for (int k2 = k2Init; k2 < 5; k2++) {
                            // CHECK semantic of the sameXOR
                            Coordinates element1 = new Coordinates(i1, j, k1);
                            Coordinates element2 = new Coordinates(i2, j, k2);
                            if (sameXOR(element1, element2, xorEq)) {
                                BoolVar diff_δK1_δK2 = DK[j][i1][k1][i2][k2];
                                // C'9 for DZ
                                xor(diff_δK1_δK2, ΔK[i1][j][k1], ΔK[i2][j][k2]).post();
                                // i ∈ [1, r]
                                for (int i3 = i2; i3 < rounds; i3++) {
                                    int k3Init = (i2 != i3) ? 0 : k2 + 1;
                                    // k ∈ [0, 3 + 1 (ΔSEK)]
                                    for (int k3 = k3Init; k3 < 5; k3++) {
                                        Coordinates element3 = new Coordinates(i3, j, k3);
                                        if (sameXOR(element1, element2, element3, xorEq)) {
                                            BoolVar diff_δK2_δK3 = DK[j][i2][k2][i3][k3];
                                            BoolVar diff_δK1_δK3 = DK[j][i1][k1][i3][k3];
                                            // C'8 for DZ
                                            xor(diff_δK1_δK2, diff_δK2_δK3, diff_δK1_δK3).post();
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

    /*
        Constraint C'8 for DY (diffY) && DZ (diffZ)
        ∀{δB_{1} , δB_{2} , δB_{3}} ⊆ DY (resp. DZ),
            diff δB_{1} ,δB_{2} + diff δB_{2} ,δB_{3} + diff δB_{1} ,δB_{3} != 1
        Constraint C'9 for DK (diffK) && DZ (diffZ)
        ∀{δB_{1} , δB_{2}} ⊆ DY (resp. DZ),
            diff(δB_{1},δB_{2}) + ∆B_{1} + ∆B_{2} != 1
     */
    private void c8ForDiffYAndDiffZ(
            BoolVar[][][][][] DY,
            BoolVar[][][] ΔY,
            BoolVar[][][][][] DZ,
            BoolVar[][][] ΔZ
    ) {
        /*
            Picat:
            foreach(I1 in 1..R-1, J in 1..4, K1 in 1..4, I2 in 1..I1, K2 in 1..4, [I1,K1] @> [I2,K2])
                DY[I1,J,K1] + DY[I2,J,K2] + DIFFY[I1,J,K1,I2,K2] #!= 1,
                DZ[I1,J,K1] + DZ[I2,J,K2] + DIFFZ[I1,J,K1,I2,K2] #!= 1,
                DIFFZ[I1,J,K1,I2,K2] = DIFFZ[I2,J,K2,I1,K1],
                DIFFY[I1,J,K1,I2,K2] = DIFFY[I2,J,K2,I1,K1],
                > DIFFK[I1+1,J,K1,I2+1,K2] + DIFFZ[I1,J,K1,I2,K2] + DX[I1+1,J,K1] + DX[I2+1,J,K2] #!= 1, < Extract to C'13
                foreach(I3 in 1..I2, K3 in 1..4, [I2,K2] @> [I3,K3])
                    DIFFY[I1,J,K1,I2,K2] + DIFFY[I2,J,K2,I3,K3] + DIFFY[I1,J,K1,I3,K3] #!= 1,
                    DIFFZ[I1,J,K1,I2,K2] + DIFFZ[I2,J,K2,I3,K3] + DIFFZ[I1,J,K1,I3,K3] #!= 1
                end
            end
         */

        // k ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [0, r − 2]
            for (int i1 = 0; i1 < rounds - 1; i1++) {
                // k ∈ [0, 3]
                for (int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [0, r − 2]
                    for (int i2 = i1; i2 < rounds - 1; i2++) {
                        int k2Init = (i1 != i2) ? 0 : k1 + 1;
                        // k ∈ [0, 3]
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            // C'9 for DY
                            BoolVar diff_δY1_δY2 = DY[j][i1][k1][i2][k2];
                            xor(diff_δY1_δY2, ΔY[i1][j][k1], ΔY[i2][j][k2]).post();

                            // C'9 for DZ
                            BoolVar diff_δZ1_δZ2 = DZ[j][i1][k1][i2][k2];
                            xor(diff_δZ1_δZ2, ΔZ[i1][j][k1], ΔZ[i2][j][k2]).post();

                            // i ∈ [0, r − 2]
                            for (int i3 = i2; i3 < rounds - 1; i3++) {
                                int k3Init = (i2 != i3) ? 0 : k2 + 1;
                                // k ∈ [0, 3]
                                for (int k3 = k3Init; k3 < 4; k3++) {
                                    BoolVar diff_δY2_δY3 = DY[j][i2][k2][i3][k3];
                                    BoolVar diff_δY1_δY3 = DY[j][i1][k1][i3][k3];
                                    // C'8 for DY
                                    xor(diff_δY1_δY2, diff_δY2_δY3, diff_δY1_δY3).post();

                                    // C'8 for DZ
                                    BoolVar diff_δZ2_δZ3 = DZ[j][i2][k2][i3][k3];
                                    BoolVar diff_δZ1_δZ3 = DZ[j][i1][k1][i3][k3];
                                    xor(diff_δZ1_δZ2, diff_δZ2_δZ3, diff_δZ1_δZ3).post();
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
    private void mdsProperty(BoolVar[][][][][] DY, BoolVar[][][][][] DZ) {
        /*
            Picat:
            foreach(I1 in 1..R-1, K1 in 1..4, I2 in I1..R-1, K2 in 1..4, [I2,K2] @> [I1,K1])
                S2 :: [0,5,6,7,8],
                sum([DIFFY[I1,J,K1,I2,K2] : J in 1..4]) + sum([DIFFZ[I1,J,K1,I2,K2] : J in 1..4]) #= S2
            end
         */
        int[] S = intArrayOf(0, 5, 6, 7, 8);
        // i ∈ [0, r − 2]
        for (int i1 = 0; i1 < rounds - 1; i1++) {
            // k ∈ [0, 3]
            for (int k1 = 0; k1 < 4; k1++) {
                // i ∈ [0, r − 2]
                for (int i2 = i1; i2 < rounds - 1; i2++) {
                    int k2Init = (i1 != i2) ? 0 : k1 + 1;
                    // k ∈ [0, 3]
                    for (int k2 = k2Init; k2 < 4; k2++) {
                        sum(arrayOf(
                                DY[0][i1][k1][i2][k2], DY[1][i1][k1][i2][k2], DY[2][i1][k1][i2][k2], DY[3][i1][k1][i2][k2],
                                DZ[0][i1][k1][i2][k2], DZ[1][i1][k1][i2][k2], DZ[2][i1][k1][i2][k2], DZ[3][i1][k1][i2][k2]
                        ), "=", intVar(S)).post();
                    }
                }
            }
        }
    }

    /*
        C'10 ∀(δB_{1} ⊕ δB_{2} ⊕ δB_{3} = 0) ∈ xorEq_{l} ,
            (diff δB_{1},δB_{2} = ∆B_{3} ) ∧ (diff δB_{1},δB_{3} = ∆B_{2} ) ∧ (diff δB_{2},δB_{3} = ∆B_{1} )
        C'11 ∀(δB_{1} ⊕ δB_{2} ⊕ δB_{3} ⊕ δB_{4} = 0) ∈ xorEq_{l}
            (diff δB_{1},δB_{2} = diff δB_{3},δB_{4} ) ∧ (diff δB_{1},δB_{3} = diff δB_{2},δB_{4} ) ∧ (diff δB_{1},δB_{4} = diff δB_{2},δB_{3} )
     */
    private void c10AndC11(BoolVar[][][] ΔK, BoolVar[][][][][] DK) {
        // TODO check the index matching between Picat and Java
        //  and how handle the S-Box (not in an extended column)
        /*
            Picat:
            foreach(XOR in LXor) %%% (C10' and C11')
                xor(XOR,DK,DIFFK)
            end
         */
        for (XOREquation xor : xorEq) {
            List<Coordinates> xorElements = new ArrayList<>(xor);
            if (xor.size() == 3) {
                /*
                    xor(L, DK, DIFF), L = [[IA,J,KA],[IB,J,KB],[IC,J,KC]] =>
                        DIFF[IA,J,KA,IB,KB] = DK[IC,J,KC],
                        DIFF[IA,J,KA,IC,KC] = DK[IB,J,KB],
                        DIFF[IB,J,KB,IC,KC] = DK[IA,J,KA].
                 */
                Coordinates A = xorElements.get(0);
                Coordinates B = xorElements.get(1);
                Coordinates C = xorElements.get(2);

                if (A.j == B.j && B.j == C.j && A.i != 0 && B.i != 0 && C.i != 0) {
                    int J = A.j;
                    arithm(DK[J][A.i][A.k][B.i][B.k], "=", ΔK[C.i][J][C.k]).post();
                    arithm(DK[J][A.i][A.k][C.i][C.k], "=", ΔK[C.i][J][C.k]).post();
                    arithm(DK[J][A.i][A.k][B.i][B.k], "=", ΔK[C.i][J][C.k]).post();
                }
            } else if (xor.size() == 4) {
                /*
                    xor(L, _, DIFF), L = [[IA,J,KA],[IB,J,KB],[IC,J,KC],[ID,J,KD]] =>
                        DIFF[IA,J,KA,IB,KB] = DIFF[IC,J,KC,ID,KD],
                        DIFF[IA,J,KA,IC,KC] = DIFF[IB,J,KB,ID,KD],
                        DIFF[IA,J,KA,ID,KD] = DIFF[IB,J,KB,IC,KC].
                 */
                Coordinates A = xorElements.get(0);
                Coordinates B = xorElements.get(1);
                Coordinates C = xorElements.get(2);
                Coordinates D = xorElements.get(3);

                if (A.j == B.j && B.j == C.j && C.j == D.j && A.i != 0 && B.i != 0 && C.i != 0 && D.i != 0) {
                    int J = A.j;
                    arithm(DK[J][A.i][A.k][B.i][B.k], "=", DK[J][C.i][C.k][D.i][D.k]).post();
                    arithm(DK[J][A.i][A.k][C.i][C.k], "=", DK[J][B.i][B.k][D.i][D.k]).post();
                    arithm(DK[J][A.i][A.k][D.i][D.k], "=", DK[J][B.i][B.k][C.i][C.k]).post();
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /*
        Constraint C'13
        ∀i_{1} , i_{2} ∈ [0, r − 2], ∀j, k_{1} , k_{2} ∈ [0, 3]
            diff δK_{i1 + 1}[j][k1],δK_{i2 + 1}[j][k2] + diff δZ{i1}[j][k1],δZ_{i 2}[j][k2] + ∆X_{i1 + 1}[j][k1] + ∆X_{i2 + 1}[j][k2] != 1
     */
    private void c13(BoolVar[][][][][] DK, BoolVar[][][][][] DZ, BoolVar[][][] ΔX) {
        /*
            Picat:
            foreach(I1 in 1..R-1, J in 1..4, K1 in 1..4, I2 in 1..I1, K2 in 1..4, [I1,K1] @> [I2,K2])
                DIFFK[I1+1,J,K1,I2+1,K2] + DIFFZ[I1,J,K1,I2,K2] + DX[I1+1,J,K1] + DX[I2+1,J,K2] #!= 1,
            end
         */
        // j ∈ [0, 3]
        for (int j = 0; j < 4; j++) {
            // i ∈ [0, r − 2]
            for (int i1 = 0; i1 < rounds - 1; i1++) {
                // k ∈ [0, 3]
                for (int k1 = 0; k1 < 4; k1++) {
                    // i ∈ [0, r − 2]
                    for (int i2 = i1; i2 < rounds - 1; i2++) {
                        int k2Init = (i1 != i2) ? 0 : k1 + 1;
                        // k ∈ [0, 3]
                        for (int k2 = k2Init; k2 < 4; k2++) {
                            xor(DK[j][i1 + 1][k1][i2 + 1][k2], DZ[j][i1][k1][i2][k2], ΔX[i1 + 1][j][k1], ΔX[i2 + 1][j][k2]).post();
                        }
                    }
                }
            }
        }
    }

    public MathSet<XOREquation> xorEq() {
        MathSet<XOREquation> initialKeyScheduleXORs = new MathSet<>();
        for (int i = 1; i < rounds; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    if (!block.isInitialKey(i, k)) {
                        Pair<Coordinates, Coordinates> xorKeySchedule = block.xorKeySchedulePi(i, j, k);
                        XOREquation res = new XOREquation(new Coordinates(i, j, k), xorKeySchedule._0, xorKeySchedule._1);
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
                for(XOREquation eq: generatedXORs.union(initialKeyScheduleXORs)) {
                    List<Coordinates> picatCoordinates = eq.stream()
                            .map(Coordinates::javaToPicat)
                            .collect(Collectors.toList());
                    XOREquation eqForPicat = new XOREquation(picatCoordinates);
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

    private boolean sameXOR(Coordinates a, Coordinates b, Set<XOREquation> equationSet) {
        /*
            Picat:
            sameXOR(A,B,[X|_]), membchk(A,X), membchk(B,X) => true.
            sameXOR(A,B,[_|L]) => sameXOR(A,B,L).
         */
        for (XOREquation eq : equationSet) {
            if (eq.contains(a) && eq.contains(b)) return true;
        }
        return false;
    }

    private boolean sameXOR(Coordinates a, Coordinates b, Coordinates c, Set<XOREquation> equationSet) {
        /*
            Picat:
            sameXOR(A,B,C,[X|_]), membchk(A,X), membchk(B,X), membchk(C,X) => true.
            sameXOR(A,B,C,[_|L]) => sameXOR(A,B,C,L).
         */
        for (XOREquation eq : equationSet) {
            if (eq.contains(a) && eq.contains(b) && eq.contains(c)) return true;
        }
        return false;
    }

    private Constraint xor(BoolVar... elements) {
        return sum(arrayOf(elements), "!=", 1);
    }


}
