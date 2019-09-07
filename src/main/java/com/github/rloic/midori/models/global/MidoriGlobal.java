package com.github.rloic.midori.models.global;

import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.RulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.constraints.abstractxor.AbstractXORPropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Midori model working with the globalXor constraint
 */
@SuppressWarnings("NonAsciiCharacters")
public abstract class MidoriGlobal {

    public final Model m;
    /* The variables that go through an SBoxPropagator */
    public final BoolVar[] sBoxes;
    /* The variables that must ge instantiated */
    public final BoolVar[] variablesToAssign;
    public final AbstractXORPropagator propagator;
    public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

    private final ExtendedModel em;
    private int sBoxesInc = 0;
    private int variablesToAssignedInc = 0;

    abstract protected String getModelName();

    abstract protected InferenceEngine getInferenceEngine();

    abstract protected RulesApplier getRulesApplier();

    public MidoriGlobal(int r, int objStep1) {
        this(r, objStep1, null);
    }

    public MidoriGlobal(int r, int objStep1, IntVar[] nbActives) {
        em = new ExtendedModel(getModelName());

        BoolVar[][][] ΔX = new BoolVar[r][4][4];
        BoolVar[][][] ΔZ = new BoolVar[r - 1][4][4];
        BoolVar[][] ΔK = new BoolVar[4][4];
        BoolVar[][][] ΔY = new BoolVar[r - 1][4][4];

        sBoxes = new BoolVar[r * 4 * 4];
        variablesToAssign = new BoolVar[(2 * r - 1) * 4 * 4];

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
            if (nbActives != null) {
                em.sum(currentRound, "=", nbActives[i].getValue());
            }
        }

        // Init ΔZ
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    ΔZ[i][j][k] = createVariableToAssign("ΔZ[" + i + "][" + j + "][" + k + "]");
                }
            }
        }

        // Init ΔK
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                ΔK[j][k] = createVariableToAssign("ΔK[" + j + "][" + k + "]");
            }
        }

        // Init ΔSX alias ΔY
        // We have ΔSX = ΔY but we don't have δSX = δY so we need to create other variables
        // SubCell
        for (int i = 0; i < r - 1; i++) {
            ΔY[i][0][0] = createVariableToAssign("ΔY/ΔSX[i][0][0]");
            em.equals(ΔY[i][0][0], ΔX[i][0][0]);
            ΔY[i][1][0] = createVariableToAssign("ΔY/ΔSX[i][1][0]");
            em.equals(ΔY[i][1][0], ΔX[i][2][2]);
            ΔY[i][2][0] = createVariableToAssign("ΔY/ΔSX[i][2][0]");
            em.equals(ΔY[i][2][0], ΔX[i][1][1]);
            ΔY[i][3][0] = createVariableToAssign("ΔY/ΔSX[i][3][0]");
            em.equals(ΔY[i][3][0], ΔX[i][3][3]);
            ΔY[i][0][1] = createVariableToAssign("ΔY/ΔSX[i][0][1]");
            em.equals(ΔY[i][0][1], ΔX[i][2][3]);
            ΔY[i][1][1] = createVariableToAssign("ΔY/ΔSX[i][1][1]");
            em.equals(ΔY[i][1][1], ΔX[i][0][1]);
            ΔY[i][2][1] = createVariableToAssign("ΔY/ΔSX[i][2][1]");
            em.equals(ΔY[i][2][1], ΔX[i][3][2]);
            ΔY[i][3][1] = createVariableToAssign("ΔY/ΔSX[i][3][1]");
            em.equals(ΔY[i][3][1], ΔX[i][1][0]);
            ΔY[i][0][2] = createVariableToAssign("ΔY/ΔSX[i][0][2]");
            em.equals(ΔY[i][0][2], ΔX[i][1][2]);
            ΔY[i][1][2] = createVariableToAssign("ΔY/ΔSX[i][1][2]");
            em.equals(ΔY[i][1][2], ΔX[i][3][0]);
            ΔY[i][2][2] = createVariableToAssign("ΔY/ΔSX[i][2][2]");
            em.equals(ΔY[i][2][2], ΔX[i][0][3]);
            ΔY[i][3][2] = createVariableToAssign("ΔY/ΔSX[i][3][2]");
            em.equals(ΔY[i][3][2], ΔX[i][2][1]);
            ΔY[i][0][3] = createVariableToAssign("ΔY/ΔSX[i][0][3]");
            em.equals(ΔY[i][0][3], ΔX[i][3][1]);
            ΔY[i][1][3] = createVariableToAssign("ΔY/ΔSX[i][1][3]");
            em.equals(ΔY[i][1][3], ΔX[i][1][3]);
            ΔY[i][2][3] = createVariableToAssign("ΔY/ΔSX[i][2][3]");
            em.equals(ΔY[i][2][3], ΔX[i][2][0]);
            ΔY[i][3][3] = createVariableToAssign("ΔY/ΔSX[i][3][3]");
            em.equals(ΔY[i][3][3], ΔX[i][0][2]);
        }

        int nbEquations = 0;
        int nbVariables = 0;

        Set<BoolVar> distincVar = new HashSet<>();

        // KeyAdd
        for (int i = 0; i < r - 1; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    em.abstractXor(ΔZ[i][j][k], ΔK[j][k], ΔX[i + 1][j][k]);
                    distincVar.add(ΔZ[i][j][k]);
                    distincVar.add(ΔK[j][k]);
                    distincVar.add(ΔX[i + 1][j][k]);
                    nbEquations += 1;
                    nbVariables += 3;
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

                distincVar.add(ΔY[i][1][k]); distincVar.add(ΔY[i][2][k]); distincVar.add(ΔY[i][3][k]); distincVar.add(ΔZ[i][0][k]);
                distincVar.add(ΔY[i][0][k]); distincVar.add(ΔY[i][2][k]); distincVar.add(ΔY[i][3][k]); distincVar.add(ΔZ[i][1][k]);
                distincVar.add(ΔY[i][0][k]); distincVar.add(ΔY[i][1][k]); distincVar.add(ΔY[i][3][k]); distincVar.add(ΔZ[i][2][k]);
                distincVar.add(ΔY[i][0][k]); distincVar.add(ΔY[i][1][k]); distincVar.add(ΔY[i][2][k]); distincVar.add(ΔZ[i][3][k]);

                nbEquations += 4;
                nbVariables += 16;
            }
        }

        System.err.print(r + " & ");
        System.err.print(nbEquations + " & ");
        System.err.print(distincVar.size() + " & ");
        int dim = nbEquations* distincVar.size();
        System.err.print(dim + " & ");
        System.err.print(nbVariables + " & ");
        System.err.print(((double)nbVariables) / dim * 100.0 + " \\\\\n");


        em.sum(sBoxes, "=", objStep1);

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

    private BoolVar createVariableToAssign(String name) {
        BoolVar var = em.boolVar(name);
        variablesToAssign[variablesToAssignedInc++] = var;
        return var;
    }

}
