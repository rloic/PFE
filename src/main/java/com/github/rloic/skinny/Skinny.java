package com.github.rloic.skinny;

import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.text.DecimalFormat;
import java.util.List;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class Skinny {

    private static int[] PROBABILITIES = new int[]{70, 60, 54, 50, 44, 40, 37, 34, 32, 30, 27, 24, 20, 0};

    private final ExtendedModel em = new ExtendedModel("Skinny");
    private final int r;

    public final Model m;
    public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

    private final ExtendedModel.Byte[][][] δX;
    private final ExtendedModel.Byte[][][] δSX;
    private final ExtendedModel.Byte[][][] δAC;
    private final ExtendedModel.Byte[][][] δSR;
    private final IntVar[][][] probabilities;
    public final IntVar[] nbActives;
    private final int BOUND;
    public IntVar objective;

    public Skinny(
            int version,
            int r,
            int objStep1
    ) {
        this.r = r;
        BOUND = (version == 64) ? 15 : 255;
        objective = em.intVar(20 * objStep1, 70 * objStep1);

        δX = new ExtendedModel.Byte[r + 1][4][4];
        δSX = new ExtendedModel.Byte[r][][];
        δAC = new ExtendedModel.Byte[r][][];
        δSR = new ExtendedModel.Byte[r][][];
        probabilities = new IntVar[r][4][4];
        IntVar[] flattenedProbabilities = new IntVar[r * 4 * 4];
        int cptP = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    probabilities[i][j][k] = em.intVar(PROBABILITIES);
                    flattenedProbabilities[cptP++] = probabilities[i][j][k];
                }
            }
        }
        em.sum(flattenedProbabilities, "=", objective);

        δX[0] = em.byteVar("X[0]", BOUND, 4, 4);
        for (int i = 0; i < r; i++) {
            δSX[i] = subCells(δX[i], probabilities[i]);
            δAC[i] = addConstants(δSX[i]);
            δSR[i] = shiftRows(δAC[i]);
            δX[i + 1] = mixColumns(δSR[i]);
        }

        nbActives = new IntVar[r];
        for (int i = 0; i < r; i++) {
            nbActives[i] = em.intVar("nbActives[" + i + "]", 0, objStep1);
            em.sum(sBoxes(i), "=", nbActives[i]);
        }
        em.sum(nbActives, "=", objStep1);

        DeconstructedModel d = em.buildWithWeightedConstraintsGeneration(
                new FullInferenceEngine(),
                new FullRulesApplier()
        );
        m = d.model;
        constraintsOf = d.constraintsOf;

    }

    private ExtendedModel.Byte[][] subCells(ExtendedModel.Byte[][] δX, IntVar[][] probabilities) {
        ExtendedModel.Byte[][] δSX = em.byteVar("SX", BOUND, 4, 4);
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                em.table(
                        arrayOf(
                                δX[j][k].realization,
                                δSX[j][k].realization,
                                probabilities[j][k]
                        ),
                        SBox.skinny128,
                        "FC"
                );
                em.equals(δX[j][k].abstraction, δSX[j][k].abstraction);
            }
        }
        return δSX;
    }

    private ExtendedModel.Byte[][] addConstants(ExtendedModel.Byte[][] δSCX) {
        return δSCX;
    }

    private ExtendedModel.Byte[][] shiftRows(ExtendedModel.Byte[][] δAC) {
        ExtendedModel.Byte[][] δSR = new ExtendedModel.Byte[4][4];
        for (int k = 0; k < 4; k++) {
            δSR[0][k] = δAC[0][k];
            δSR[1][k] = δAC[1][(k + 3) % 4];
            δSR[2][k] = δAC[2][(k + 2) % 4];
            δSR[3][k] = δAC[3][(k + 1) % 4];
        }
        return δSR;
    }

    private ExtendedModel.Byte[][] mixColumns(ExtendedModel.Byte[][] δSR) {
        ExtendedModel.Byte[][] δMC = new ExtendedModel.Byte[4][4];
        for (int k = 0; k < 4; k++) {
            δMC[1][k] = δSR[0][k];
            δMC[2][k] = xor("MC[2][" + k + "]", δSR[1][k], δSR[2][k]);
            δMC[3][k] = xor("MC[3][" + k + "]", δSR[0][k], δSR[2][k]);
            δMC[0][k] = xor("MC[0][" + k + "]", δMC[3][k], δSR[3][k]);
        }
        return δMC;
    }

    public BoolVar[] sBoxes() {
        BoolVar[] sBoxes = new BoolVar[r * 4 * 4];
        int cpt = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    sBoxes[cpt++] = δX[i][j][k].abstraction;
                }
            }
        }
        return sBoxes;
    }

    public BoolVar[] sBoxes(int round) {
        BoolVar[] sBoxes = new BoolVar[4 * 4];
        int cpt = 0;
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                sBoxes[cpt++] = δX[round][j][k].abstraction;
            }
        }
        return sBoxes;
    }

    private ExtendedModel.Byte xor(String name, ExtendedModel.Byte lhs, ExtendedModel.Byte rhs) {
        ExtendedModel.Byte res = em.byteVar(name, BOUND);
        em.byteXor(res.realization, lhs.realization, rhs.realization);
        em.abstractXor(res.abstraction, lhs.abstraction, rhs.abstraction);
        return res;
    }

    public void prettyPrint(Solution solution) {
        for (int i = 0; i < r; i++) {
            System.out.println("δX[" + i + "]\t\t\t\t\tδSX[" + i + "]\t\t\t\t\tprobabilities[" + i + "]\t\t\t\tδAC[" + i + "]\t\t\t\t\tδSR[" + i + "]");
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 4d", solution.getIntVal(δX[i][j][k].realization)));
                }
                System.out.print("\t\t");
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 4d", solution.getIntVal(δSX[i][j][k].realization)));
                }
                System.out.print("\t\t");
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 4.1f", (solution.getIntVal(probabilities[i][j][k]) / 10.0)));
                }
                System.out.print("\t\t");
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 4d", solution.getIntVal(δAC[i][j][k].realization)));
                }
                System.out.print("\t\t");
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 4d", solution.getIntVal(δSR[i][j][k].realization)));
                }
                System.out.println();
            }
        }

    }

}
