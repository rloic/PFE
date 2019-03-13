package com.github.rloic.aes.aes128;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.common.collections.ArrayExtensions.intArrayOf;

@SuppressWarnings("NonAsciiCharacters")
public
class BasicAESModel extends Model {

    private final int r;
    private static final int NB_BYTES = 4;
    public final BoolVar[] sBoxes;

    public BasicAESModel(int rounds, int objStep1) {
        super("Basic AES Model(r=" + rounds + ", objStep1=" + objStep1 + ")");
        r = rounds;
        BoolVar[][][] ΔX = createRoundVariables();
        BoolVar[][][] ΔZ = createRoundVariables();
        BoolVar[][][] ΔK = createRoundVariables();
        BoolVar[][][] ΔY = shiftRows(ΔX); // C4
        sBoxes = linkSBoxes(ΔX, ΔK);

        numberOfActiveSBoxes(sBoxes, objStep1); // C1
        subBytes(); // C2
        addRoundKey(ΔX, ΔK, ΔZ); // C3
        mixColumns(ΔY, ΔZ); // C5 & C6
        keySchedule(ΔK); // C7 & C8
    }

    private BoolVar[][][] createRoundVariables() {
        BoolVar[][][] result = new BoolVar[r][][];
        for (int i = 0; i < r; i++) result[i] = boolVarMatrix(NB_BYTES, NB_BYTES);
        return result;
    }

    private Constraint xor(BoolVar... variables) {
        return sum(variables, "!=", 1);
    }

    private BoolVar[] linkSBoxes(BoolVar[][][] ΔX, BoolVar[][][] ΔK) {
        // Link sBoxes with deepAll variables that pass through an S-box (variables of ∆X, and variables in the last column of ∆K)
        // Link ΔSR with ∆X according to ShiftRows operation
        BoolVar[] sBoxes = new BoolVar[20 * r];
        int cpt = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    sBoxes[cpt++] = ΔX[i][j][k];
                }
                sBoxes[cpt++] = ΔK[i][j][3];
            }
        }
        return sBoxes;
    }

    /*
        Constraint C1 : Number of active sBoxes
        obj Step1 = Sum(δB ∈ Sboxes) { ∆B }
     */
    private void numberOfActiveSBoxes(BoolVar[] sBoxes, int objStep1) {
        sum(sBoxes, "=", objStep1).post();
    }

    /*
        Constraint C2 : SubBytes
        ∀δB ∈ Sboxes_{l} , ∆SB = ∆B
        Implicit we use ∆B instead of ∆SB
     */
    private void subBytes() {
    }

    /*
        Constraint C3 : AddRoundKey
        ∀i ∈ [0, r − 2], ∀j, k ∈ [0, 3], xor(∆Z_{i}[j][k], ∆K_{i+1}[j][k], ∆X_{i+1}[j][k])
     */
    private void addRoundKey(BoolVar[][][] ΔX, BoolVar[][][] ΔK, BoolVar[][][] ΔZ) {
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    xor(ΔZ[i][j][k], ΔK[i + 1][j][k], ΔX[i + 1][j][k]).post();
                }
            }
        }
    }

    /*
        Constraint C4
        ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3], ∆Y_{i}[j][k] = ∆SX_{i}[j][(j + k)%4]
     */
    private BoolVar[][][] shiftRows(BoolVar[][][] ΔX) {
        BoolVar[][][] DY = new BoolVar[r][NB_BYTES][NB_BYTES];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 4; k++)
                    DY[i][j][k] = ΔX[i][j][(j + k) % 4];
        return DY;
    }

    /*
        Constraint C5 : Mix Columns 1/2
        ∀i ∈ [0, r − 2], ∀k ∈ [0, 3], Sum(0..3) { j -> ∆Y_{i}[j][k] + ∆Z_{i}[j][k]) ∈ {0, 5, 6, 7, 8}

        Constraint C6 : Mix Columns 2/2
        ∀j, k ∈ [0, 3], ∆Z_{r−1}[j][k] = ∆Y_{r−1}[j][k]
     */
    private void mixColumns(BoolVar[][][] ΔY, BoolVar[][][] ΔZ) {
        final int[] possibleAnswers = intArrayOf(0, 5, 6, 7, 8);
        // C5
        for (int i = 0; i < r - 1; i++) {
            for (int k = 0; k < 4; k++) {
                sum(arrayOf(
                        ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                        ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
                ), "=", intVar(possibleAnswers)).post();
            }
        }

        // C6 : Last round
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                arithm(ΔZ[r - 1][j][k], "=", ΔY[r - 1][j][k]).post();
            }
        }
    }

    /*
        C7 : Key Schedule 1/2
        ∀i ∈ [0, r − 2], ∀j ∈ [0, 3], xor(∆K_{i+1}[j][0], ∆K_{i}[j][0], ∆SK_{i}[(j + 1)%4][3])
                 ^^^^^
        C8 : Key Schedule 2/2
        ∀i ∈ [0, r − 2], ∀j ∈ [0, 3], ∀k ∈ [1, 3], xor(∆K_{i+1}[j][k], ∆K_{i+1}[j][k − 1], ∆K_{i}[j][k])
                 ^^^^^
     */
    private void keySchedule(BoolVar[][][] ΔK) {
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                // C7
                xor(ΔK[i + 1][j][0], ΔK[i][j][0], ΔK[i][(j + 1) % 4][3]).post();
                for (int k = 1; k < 4; k++) {
                    // C8
                    xor(ΔK[i + 1][j][k], ΔK[i + 1][j][k - 1], ΔK[i][j][k]).post();
                }
            }
        }
    }

}
