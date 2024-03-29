package com.github.rloic.midori.models.fullsteps;

import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.common.ExtendedModel.ByteVar;
import com.github.rloic.midori.constrainttables.SBox;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.List;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class MidoriFullSteps {

    private final int version;
    private final int r;

    private final ExtendedModel em;
    public final IntVar objective;
    public final Model model;
    public final Solver solver;
    public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

    private final ExtendedModel.ByteVar[][] δPlainText;
    private final ExtendedModel.ByteVar[][] δWK;
    private final ExtendedModel.ByteVar[][][] δX;
    private final ByteVar[][][] δSX;
    private final ExtendedModel.ByteVar[][][] δY;
    private final ExtendedModel.ByteVar[][][] δZ;
    private final ByteVar[][][] δK;
    private final ByteVar[][] δCipherText;

    public final IntVar[] nbActives;
    public final IntVar[] flattenedProbabilities;

    public MidoriFullSteps(
            int version,
            int r,
            Integer numberOfActiveSBoxes
    ) {
        em = new ExtendedModel("MidoriFullSteps");
        this.r = r;
        this.version = version;

        final int MAX_VALUE = (version == 64) ? 15 : 255;

        δPlainText = em.byteVarMatrix("PlainText", 4, 4, MAX_VALUE);
        δWK = em.byteVarMatrix("WK", 4, 4, MAX_VALUE);
        δX = em.byteVarTensor3("X", r, 4, 4, MAX_VALUE);
        δSX = em.byteVarTensor3("SX", r, 4, 4, MAX_VALUE);
        δY = em.byteVarTensor3("Y", r - 1, 4, 4, MAX_VALUE);
        δZ = em.byteVarTensor3("Z", r - 1, 4, 4, MAX_VALUE);
        δK = new ExtendedModel.ByteVar[][][]{δWK,δWK};
        δCipherText = em.byteVarMatrix("CipherText", 4, 4, MAX_VALUE);

        IntVar[][][] probabilities = new IntVar[r][4][4];
        flattenedProbabilities = new IntVar[r * 4 * 4];
        int cpt = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    probabilities[i][j][k] = em.intVar("probability[" + i + "][" + j + "][" + k + "]", new int[]{0,2, 3, 4, 5, 6});
                    flattenedProbabilities[cpt++] = probabilities[i][j][k];
                }
            }
        }

        if (numberOfActiveSBoxes != null) {
            nbActives = new IntVar[r];
            for (int i = 0; i < r; i++) {
                nbActives[i] = em.intVar("nbActives[" + i + "]", 0, numberOfActiveSBoxes);
                if (i >= 3) {
                    IntVar[] activesUntilRoundI = new IntVar[i];
                    System.arraycopy(nbActives, 0, activesUntilRoundI, 0, i);
                    em.sum(activesUntilRoundI, ">=", i);
                }
            }

            for (int i = 0; i < r; i++) {
                IntVar[] currentRound = new IntVar[16];
                int sbCpt = 0;
                for (int j = 0; j < 4; j++) {
                    for (int k = 0; k < 4; k++) {
                        currentRound[sbCpt++] = δX[i][j][k].abstraction;
                    }
                }
                em.sum(currentRound, "=", nbActives[i]);
            }
            em.sum(nbActives, "=", numberOfActiveSBoxes);
        } else {
            nbActives = null;
        }

        // δX[0] = δPlainText xor δWK
        ark(δX[0], δPlainText, δWK);
        for (int i = 0; i < r - 1; i++) {
            // δSX[i] = SBox(δX[i]) with a probability wrap p[i]
            sBox(δSX[i], δX[i], probabilities[i]);
            // δY[i] = shuffleCell(δSX[i])
            shuffleCell(δY[i], δSX[i]);
            // δZ[i] = mixColumn(δY[i])
            mixColumns(δZ[i], δY[i]);
            // δX[i + 1] = δZ[i] xor δK[i]
            if (version == 64) {
                ark(δX[i + 1], δZ[i], δK[i % 2]);
            } else { // version == 128
                ark(δX[i + 1], δZ[i], δK[0]);
            }
        }
        // δX[r-1] = SBoxPropagator(δSX[r-1]) with a probability wrap p[i]
        sBox(δX[r - 1], δSX[r - 1], probabilities[r - 1]);
        // δCipherText = δSX[r - 1] xor δWK
        ark(δCipherText, δSX[r - 1], δWK);

        if (numberOfActiveSBoxes != null) {
            objective = em.intVar(2 * numberOfActiveSBoxes, 6 * numberOfActiveSBoxes);
        } else {
            objective = em.intVar(2, 128/6);
        }
        em.sum(flattenedProbabilities, "=", objective);

        DeconstructedModel d = em.build(new FullInferenceEngine(), new FullRulesApplier());
        model = d.model;
        solver = model.getSolver();
        constraintsOf = d.constraintsOf;
    }

    // δSX_{i} = sBox(δX_{i}) with a probability p
    private void sBox(ByteVar[][] δSX, ByteVar[][] δX, IntVar[][] probabilities) {
        if (version == 64) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    em.table(
                            arrayOf(
                                    δX[j][k].realization,
                                    δSX[j][k].realization,
                                    probabilities[j][k]),
                            SBox.midori64,
                            "FC"
                    );
                    em.equals(δX[j][k].abstraction, δSX[j][k].abstraction);
                }
            }
        } else { // version == 128
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    em.table(
                            arrayOf(
                                    δX[j][k].realization,
                                    δSX[j][k].realization,
                                    probabilities[j][k]
                            ),
                            SBox.midori128[j],
                            "FC"
                    );
                    em.equals(δX[j][k].abstraction, δSX[j][k].abstraction);
                }
            }
        }
    }

    // δY_{i} = shuffleCell(δSX_{i})
    private void shuffleCell(ExtendedModel.ByteVar[][] δY, ExtendedModel.ByteVar[][] δSX) {
        em.equals(δY[0][0], δSX[0][0]);
        em.equals(δY[1][0], δSX[2][2]);
        em.equals(δY[2][0], δSX[1][1]);
        em.equals(δY[3][0], δSX[3][3]);

        em.equals(δY[0][1], δSX[2][3]);
        em.equals(δY[1][1], δSX[0][1]);
        em.equals(δY[2][1], δSX[3][2]);
        em.equals(δY[3][1], δSX[1][0]);

        em.equals(δY[0][2], δSX[1][2]);
        em.equals(δY[1][2], δSX[3][0]);
        em.equals(δY[2][2], δSX[0][3]);
        em.equals(δY[3][2], δSX[2][1]);

        em.equals(δY[0][3], δSX[3][1]);
        em.equals(δY[1][3], δSX[1][3]);
        em.equals(δY[2][3], δSX[2][0]);
        em.equals(δY[3][3], δSX[0][2]);
    }

    // δZ_{i} = mixColumns(δY_{i})
    private void mixColumns(ExtendedModel.ByteVar[][] δZ, ExtendedModel.ByteVar[][] δY) {
        for (int k = 0; k < 4; k++) {
            em.xor(δY[0][k], δY[1][k], δY[2][k], δZ[3][k]);
            em.xor(δY[1][k], δY[2][k], δY[3][k], δZ[0][k]);
            em.xor(δY[2][k], δY[3][k], δY[0][k], δZ[1][k]);
            em.xor(δY[3][k], δY[0][k], δY[1][k], δZ[2][k]);
        }
    }

    // δX_{i+1} = ark(δZ_{i}, δK_{i})
    private void ark(ExtendedModel.ByteVar[][] δX1, ExtendedModel.ByteVar[][] δZ, ByteVar[][] δWK) {
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                em.xor(δX1[j][k], δZ[j][k], δWK[j][k]);
            }
        }
    }

    public BoolVar[] ΔSBoxes() {
        return ΔX();
    }

    public BoolVar[] ΔX() {
        BoolVar[] ΔX = new BoolVar[r * 4 * 4];
        int cpt = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    ΔX[cpt++] = δX[i][j][k].abstraction;
                }
            }
        }
        return ΔX;
    }

    public BoolVar[] abstractVars() {
        BoolVar[] ΔVars = new BoolVar[(2 * r - 1) * 4 * 4];
        int cpt = 0;

        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                ΔVars[cpt++] = δK[0][j][k].abstraction;
                for (int i = 0; i < r - 1; i++) {
                    ΔVars[cpt++] = δZ[i][j][k].abstraction;
                    ΔVars[cpt++] = δY[i][j][k].abstraction;
                }
            }
        }
        return ΔVars;
    }

    public void prettyPrint(Solution solution) {
        System.out.println("δPlainText\t\t\tδWK");
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                System.out.print(String.format("% 3d", solution.getIntVal(δPlainText[j][k].realization)));
            }
            System.out.print("\t\t");
            for (int k = 0; k < 4; k++) {
                System.out.print(String.format("% 3d", solution.getIntVal(δWK[j][k].realization)));
            }
            System.out.println();
        }

        System.out.println("--------------------------------");
        System.out.println("--------------------------------");

        for (int i = 0; i < r - 1; i++) {
            System.out.println("δX[" + i + "]");
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 3d", solution.getIntVal(δX[i][j][k].realization)));
                }
                System.out.println();
            }

            System.out.println("δSX[" + i + "]");
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 3d", solution.getIntVal(δSX[i][j][k].realization)));
                }
                System.out.println();
            }

            System.out.println("δY[" + i + "]");
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 3d", solution.getIntVal(δY[i][j][k].realization)));
                }
                System.out.println();
            }

            System.out.println("δZ[" + i + "]\t\t\t\tδK[" + i + "]");
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    System.out.print(String.format("% 3d", solution.getIntVal(δZ[i][j][k].realization)));
                }
                System.out.print("\t\t");
                for (int k = 0; k < 4; k++) {
                    if (version == 64)
                        System.out.print(String.format("% 3d", solution.getIntVal(δK[i % 2][j][k].realization)));
                    else
                        System.out.print(String.format("% 3d", solution.getIntVal(δK[0][j][k].realization)));
                }
                System.out.println();
            }

            System.out.println("--------------------------------");
        }

        System.out.println("--------------------------------");

        System.out.println("δX[" + (r - 1) + "]");
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                System.out.print(String.format("% 3d", solution.getIntVal(δX[(r - 1)][j][k].realization)));
            }
            System.out.println();
        }

        System.out.println("δSX[" + (r - 1) + "]");
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                System.out.print(String.format("% 3d", solution.getIntVal(δSX[(r - 1)][j][k].realization)));
            }
            System.out.println();
        }

        System.out.println("δCipherText\t\t\tδWK");
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                System.out.print(String.format("% 3d", solution.getIntVal(δCipherText[j][k].realization)));
            }
            System.out.print("\t\t");
            for (int k = 0; k < 4; k++) {
                if (version == 64)
                    System.out.print(String.format("% 3d", solution.getIntVal(δWK[j][k].realization)));
                else
                    System.out.print(String.format("% 3d", solution.getIntVal(δK[0][j][k].realization)));
            }
            System.out.println();
        }

        System.out.println("obj_{step2} = 2^{-" + solution.getIntVal(objective) + "}");
        System.out.println();
    }

}
