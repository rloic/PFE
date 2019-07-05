package com.github.rloic.midori.global.round;

import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.AbstractXORPropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.List;

/**
 * A Midori model working with the globalXor constraint
 */
@SuppressWarnings("NonAsciiCharacters")
public abstract class MidoriGlobalRound {

    public final Model m;
    /* The variables that go through an SBoxPropagator */
    public final BoolVar[] sBoxes;

    public final AbstractXORPropagator propagator;
    public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

    private final ExtendedModel em;
    private int sBoxesInc = 0;

    abstract protected String getModelName();

    abstract protected InferenceEngine getInferenceEngine();

    abstract protected RulesApplier getRulesApplier();

    public final IntVar[] nbActives;

    public MidoriGlobalRound(int r, int objStep1) {
        em = new ExtendedModel(getModelName());

        BoolVar[][][] ΔX = new BoolVar[r][4][4];
        BoolVar[][][] ΔZ = new BoolVar[r - 1][4][4];
        BoolVar[][] ΔK = new BoolVar[4][4];
        BoolVar[][][] ΔY = new BoolVar[r - 1][4][4];

        sBoxes = new BoolVar[r * 4 * 4];

        nbActives = new IntVar[r];
        for (int i = 0; i < r; i++) {
            nbActives[i] = em.intVar("nbActives[" + i + "]", 0, objStep1);
            if (i >= 3) {
                IntVar[] activesUntilRoundI = new IntVar[i];
                System.arraycopy(nbActives, 0, activesUntilRoundI, 0, i);
                em.sum(activesUntilRoundI, ">=", i);
            }
        }

        // Init ΔX
        for (int i = 0; i < r; i++) {
            IntVar[] currentRound = new IntVar[16];
            int cpt = 0;
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    ΔX[i][j][k] = createSBox("ΔX[" + i + "][" + j + "][" + k + "]");
                    currentRound[cpt++] = ΔX[i][j][k];
                }
            }
            em.sum(currentRound, "=", nbActives[i]);
        }

        // Init ΔZ
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    ΔZ[i][j][k] = em.boolVar("ΔZ[" + i + "][" + j + "][" + k + "]");
                }
            }
        }

        // Init ΔK
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                ΔK[j][k] = em.boolVar("ΔK[" + j + "][" + k + "]");
            }
        }

        // Init ΔSX alias ΔY
        // We have ΔSX = ΔY but we don't have δSX = δY so we need to create other variables
        // SubCell
        for (int i = 0; i < r - 1; i++) {
            ΔY[i][0][0] = em.boolVar("ΔY/ΔSX[i][0][0]");
            em.equals(ΔY[i][0][0], ΔX[i][0][0]);
            ΔY[i][1][0] = em.boolVar("ΔY/ΔSX[i][1][0]");
            em.equals(ΔY[i][1][0], ΔX[i][2][2]);
            ΔY[i][2][0] = em.boolVar("ΔY/ΔSX[i][2][0]");
            em.equals(ΔY[i][2][0], ΔX[i][1][1]);
            ΔY[i][3][0] = em.boolVar("ΔY/ΔSX[i][3][0]");
            em.equals(ΔY[i][3][0], ΔX[i][3][3]);
            ΔY[i][0][1] = em.boolVar("ΔY/ΔSX[i][0][1]");
            em.equals(ΔY[i][0][1], ΔX[i][2][3]);
            ΔY[i][1][1] = em.boolVar("ΔY/ΔSX[i][1][1]");
            em.equals(ΔY[i][1][1], ΔX[i][0][1]);
            ΔY[i][2][1] = em.boolVar("ΔY/ΔSX[i][2][1]");
            em.equals(ΔY[i][2][1], ΔX[i][3][2]);
            ΔY[i][3][1] = em.boolVar("ΔY/ΔSX[i][3][1]");
            em.equals(ΔY[i][3][1], ΔX[i][1][0]);
            ΔY[i][0][2] = em.boolVar("ΔY/ΔSX[i][0][2]");
            em.equals(ΔY[i][0][2], ΔX[i][1][2]);
            ΔY[i][1][2] = em.boolVar("ΔY/ΔSX[i][1][2]");
            em.equals(ΔY[i][1][2], ΔX[i][3][0]);
            ΔY[i][2][2] = em.boolVar("ΔY/ΔSX[i][2][2]");
            em.equals(ΔY[i][2][2], ΔX[i][0][3]);
            ΔY[i][3][2] = em.boolVar("ΔY/ΔSX[i][3][2]");
            em.equals(ΔY[i][3][2], ΔX[i][2][1]);
            ΔY[i][0][3] = em.boolVar("ΔY/ΔSX[i][0][3]");
            em.equals(ΔY[i][0][3], ΔX[i][3][1]);
            ΔY[i][1][3] = em.boolVar("ΔY/ΔSX[i][1][3]");
            em.equals(ΔY[i][1][3], ΔX[i][1][3]);
            ΔY[i][2][3] = em.boolVar("ΔY/ΔSX[i][2][3]");
            em.equals(ΔY[i][2][3], ΔX[i][2][0]);
            ΔY[i][3][3] = em.boolVar("ΔY/ΔSX[i][3][3]");
            em.equals(ΔY[i][3][3], ΔX[i][0][2]);
        }

        // KeyAdd
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    em.abstractXor(ΔZ[i][j][k], ΔK[j][k], ΔX[i + 1][j][k]);
                }
            }
        }

        // MixColumn and ShuffleCell
        for (int i = 0; i < r - 1; i++) {
            for (int k = 0; k < 4; k++) {
                em.abstractXor(ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k], ΔZ[i][0][k]);
                em.abstractXor(ΔY[i][0][k], ΔY[i][2][k], ΔY[i][3][k], ΔZ[i][1][k]);
                em.abstractXor(ΔY[i][0][k], ΔY[i][1][k], ΔY[i][3][k], ΔZ[i][2][k]);
                em.abstractXor(ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔZ[i][3][k]);
            }
        }

        em.sum(nbActives, "=", objStep1);

        DeconstructedModel dm = em.build(getInferenceEngine(), getRulesApplier());
        this.m = dm.model;
        this.propagator = dm.propagator;
        this.constraintsOf = dm.constraintsOf;
    }

    private BoolVar createSBox(String name) {
        BoolVar var = em.boolVar(name);
        sBoxes[sBoxesInc++] = var;
        return var;
    }

}
