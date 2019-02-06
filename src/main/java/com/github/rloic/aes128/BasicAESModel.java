package com.github.rloic.aes128;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

class BasicAESModel extends Model {

    private final int r;
    private static final int NB_BYTES = 4;

    BasicAESModel(int rounds, int objStep1) {
        r = rounds;
        BoolVar[][][] DX = createRoundVariables();
        BoolVar[][][] DZ = createRoundVariables();
        BoolVar[][][] DK = createRoundVariables();
        BoolVar[][][] DY = shiftRows(DX); // C4
        BoolVar[] sBoxes = linkSBoxes(DX, DK);

        enable(numberOfActiveSBoxes(sBoxes, objStep1)); // C1
        enable(subBytes()); // C2
        enable(addRoundKey(DX, DK, DZ)); // C3
        enable(mixColumns(DY, DZ)); // C5 & C6
        enable(keySchedule(DK)); // C7 & C8
    }

    private void enable(Constraint constraint) throws SolverException {
        if(constraint != null) constraint.post();
    }

    private void enable(List<Constraint> constraints) {
        for (Constraint subConstraint : constraints) {
            subConstraint.post();
        }
    }

    private BoolVar[][][] createRoundVariables() {
        BoolVar[][][] result = new BoolVar[r][][];
        for (int i = 0; i < r; i++) result[i] = boolVarMatrix(NB_BYTES, NB_BYTES);
        return result;
    }

    private Constraint xor(BoolVar... variables) {
        return sum(variables, "!=", 1);
    }

    private BoolVar[] linkSBoxes(BoolVar[][][] DX, BoolVar[][][] DK) {
        // Link sBoxes with all variables that pass through an S-box (variables of ∆X, and variables in the last column of ∆K)
        // Link ∆SR with ∆X according to ShiftRows operation
        BoolVar[] sBoxes = new BoolVar[20 * r];
        int cpt = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    sBoxes[cpt++] = DX[i][j][k];
                    if (k == 3) {
                        sBoxes[cpt++] = DK[i][j][k];
                    }
                }
            }
        }
        return sBoxes;
    }

    /*
        Constraint C1 : Number of active sBoxes
        obj Step1 = Sum(δB ∈ Sboxes) { ∆B }
     */
    private Constraint numberOfActiveSBoxes(BoolVar[] sBoxes, int objStep1) {
        return sum(sBoxes, "=", objStep1);
    }

    /*
        Constraint C2 : SubBytes
        ∀δB ∈ Sboxes_{l} , ∆SB = ∆B
        Implicit we use ∆B instead of ∆SB
     */
    private Constraint subBytes() {
        return null;
    }

    /*
        Constraint C3 : AddRoundKey
        ∀i ∈ [0, r − 2], ∀j, k ∈ [0, 3], XOR(∆Z_{i}[j][k], ∆K_{i+1}[j][k], ∆X_{i+1}[j][k])
     */
    private List<Constraint> addRoundKey(BoolVar[][][] DX, BoolVar[][][] DK, BoolVar[][][] DZ) {
        List<Constraint> constraints = new ArrayList<>();
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    constraints.add(
                            xor(DZ[i][j][k], DK[i + 1][j][k], DX[i + 1][j][k])
                    );
                }
            }
        }
        return constraints;
    }

    /*
        Constraint C4
        ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3], ∆Y_{i}[j][k] = ∆SX_{i}[j][(j + k)%4]
     */
    private BoolVar[][][] shiftRows(BoolVar[][][] DX) {
        BoolVar[][][] DY = new BoolVar[r][NB_BYTES][NB_BYTES];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 4; k++)
                    DY[i][j][k] = DX[i][j][(j + k) % 4];
        return DY;
    }

    /*
        Constraint C5 : Mix Columns 1/2
        ∀i ∈ [0, r − 2], ∀k ∈ [0, 3], Sum(0..3) { j -> ∆Y_{i}[j][k] + ∆Z_{i}[j][k]) ∈ {0, 5, 6, 7, 8}

        Constraint C6 : Mix Columns 2/2
        ∀j, k ∈ [0, 3], ∆Z_{r−1}[j][k] = ∆Y_{r−1}[j][k]
     */
    private List<Constraint> mixColumns(BoolVar[][][] DY, BoolVar[][][] DZ) {
        final int[] possibleAnswers = new int[]{0, 5, 6, 7, 8};
        List<Constraint> constraints = new ArrayList<>();
        // C5
        for (int i = 0; i < r - 1; i++) {
            for (int k = 0; k < 4; k++) {
                constraints.add(sum(new IntVar[]{
                                DY[i][0][k], DY[i][1][k], DY[i][2][k], DY[i][3][k],
                                DZ[i][0][k], DZ[i][1][k], DZ[i][2][k], DZ[i][3][k]},
                        "=", intVar(possibleAnswers)));
            }
        }

        // C6 : Last round
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                constraints.add(arithm(DZ[r - 1][j][k], "=", DY[r - 1][j][k]));
            }
        }
        return constraints;
    }

    /*
        C7 : Key Schedule 1/2
        ∀i ∈ [0, r − 2], ∀j ∈ [0, 3], XOR(∆K_{i+1}[j][0], ∆K_{i}[j][0], ∆SK_{i}[(j + 1)%4][3])
                 ^^^^^
        C8 : Key Schedule 2/2
        ∀i ∈ [0, r − 2], ∀j ∈ [0, 3], ∀k ∈ [1, 3], XOR(∆K_{i+1}[j][k], ∆K_{i+1}[j][k − 1], ∆K_{i}[j][k])
                 ^^^^^
     */
    private List<Constraint> keySchedule(BoolVar[][][] DK) {
        List<Constraint> constraints = new ArrayList<>();
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                // C7
                constraints.add(xor(DK[i + 1][j][0], DK[i][j][0], DK[i][(j + 1) % 4][3]));
                for (int k = 1; k < 4; k++) {
                    // C8
                    constraints.add(xor(DK[i + 1][j][k], DK[i + 1][j][k - 1], DK[i][j][k]));
                }
            }
        }
        return constraints;
    }

}
