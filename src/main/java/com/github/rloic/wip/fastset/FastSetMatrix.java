package com.github.rloic.wip.fastset;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.cell.Data;
import com.github.rloic.paper.dancinglinks.cell.Row;
import com.github.rloic.util.FastSet;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

public class FastSetMatrix implements IDancingLinksMatrix {

    private final int NO_PIVOT = -1;
    private final int UNDEFINED = 0;
    private final int TRUE = 1;
    private final int FALSE = -1;

    private final int[] valueOf;
    private final FastSet activeVariables;
    private final FastSet activeEquations;

    private final FastSet[] equationsOf;
    private final FastSet[] variablesOf;

    private final int nbVariables;
    private final int nbEquations;

    private final int[] nbX;
    private final int[] nbTrue;

    private final FastSet bases;
    private final int[] pivotOf;

    public FastSetMatrix(
            int[][] equations,
            int nbVariables
    ) {
        this.nbVariables = nbVariables;
        this.nbEquations = equations.length;

        valueOf = new int[nbVariables];

        activeVariables = FastSet.full(nbVariables);
        activeEquations = FastSet.full(nbEquations);

        equationsOf = new FastSet[nbVariables];
        variablesOf = new FastSet[nbEquations];

        nbX = new int[nbEquations];
        nbTrue = new int[nbEquations];

        for (int variable : activeVariables) {
            equationsOf[variable] = new FastSet(nbEquations);
        }

        for (int equation : activeEquations) {
            variablesOf[equation] = new FastSet(nbVariables);
            int[] initialVariables = equations[equation];
            nbX[equation] = initialVariables.length;
            for (int variable : initialVariables) {
                variablesOf[equation].add(variable);
                equationsOf[variable].add(equation);
            }
        }

        bases = new FastSet(nbVariables);
        pivotOf = new int[nbEquations];
        Arrays.fill(pivotOf, NO_PIVOT);

    }

    @Override
    public boolean isUnknown(int equation, int variable) {
        return variablesOf[equation].contains(variable) && valueOf[variable] == UNDEFINED;
    }

    @Override
    public boolean isTrue(int equation, int variable) {
        return variablesOf[equation].contains(variable) && valueOf[variable] == TRUE;
    }

    @Override
    public boolean isTrue(int variable) {
        return valueOf[variable] == TRUE;
    }

    @Override
    public boolean isFalse(int variable) {
        return valueOf[variable] == FALSE;
    }

    @Override
    public void xor(int target, int pivot) {
        FastSet xor = new FastSet(nbVariables);

        int nbXOfTarget = 0;
        int nbTrueOfTarget = 0;

        for (int variable : variablesOf[target]) {
            if (!variablesOf[pivot].contains(variable)) {
                xor.add(variable);
                if (valueOf[variable] == TRUE) {
                    nbTrueOfTarget += 1;
                } else {
                    nbXOfTarget += 1;
                }
            } else {
                equationsOf[variable].remove(target);
            }
        }
        for (int variable : variablesOf[pivot]) {
            if (!variablesOf[target].contains(variable)) {
                xor.add(variable);
                if (valueOf[variable] == TRUE) {
                    nbTrueOfTarget += 1;
                } else {
                    nbXOfTarget += 1;
                }
            }
        }

        variablesOf[target] = xor;
        nbX[target] = nbXOfTarget;
        nbTrue[target] = nbTrueOfTarget;
    }

    @Override
    public void setBase(int pivot, int variable) {
        bases.add(variable);
        pivotOf[variable] = pivot;
    }

    @Override
    public void setOffBase(int variable) {
        bases.remove(variable);
    }

    @Override
    public void removeVariable(int variable) {
        for (int equation : equationsOf[variable]) {
            variablesOf[equation].remove(variable);
        }
        activeVariables.remove(variable);
    }

    @Override
    public void restoreVariable(int variable) {
        for (int equation : equationsOf[variable]) {
            variablesOf[equation].add(variable);
        }
        activeVariables.add(variable);
    }

    @Override
    public void removeEquation(int equation) {
        for (int variable : variablesOf[equation]) {
            equationsOf[variable].remove(equation);
        }
        activeEquations.remove(equation);
    }

    @Override
    public void restoreEquation(int equation) {
        for (int variable : variablesOf[equation]) {
            equationsOf[variable].add(equation);
        }
        activeEquations.add(equation);
    }

    @Override
    public int nbTrues(int equation) {
        return nbTrue[equation];
    }

    @Override
    public int nbUnknowns(int equation) {
        return nbX[equation];
    }

    @Override
    public boolean isValid(int equation) {
        return !isInvalid(equation);
    }

    @Override
    public boolean isInvalid(int equation) {
        return nbTrue[equation] == 1 && nbX[equation] == 0;
    }

    @Override
    public boolean isEmpty(int equation) {
        return nbX[equation] == 0 && nbTrue[equation] == 0;
    }

    @Override
    public boolean isBase(int variable) {
        return bases.contains(variable);
    }

    @Override
    public boolean isUnused(int variable) {
        return equationsOf[variable].isEmpty();
    }

    @Override
    public Iterable<Data> equationsOf(int variable) {
        return null;
    }

    @Override
    public int pivotOf(int variable) {
        return pivotOf[variable];
    }

    @Override
    public void set(int variable, boolean value) {
        valueOf[variable] = (value) ? TRUE : FALSE;
        for (int equation : equationsOf[variable]) {
            if (value) {
                nbTrue[equation] += 1;
            }
            nbX[equation] -= 1;
        }
    }

    @Override
    public void unSet(int variable) {
        for (int equation : equationsOf[variable]) {
            if (valueOf[variable] == TRUE) {
                nbTrue[equation] -= 1;
            }
            nbX[equation] += 1;
        }
        valueOf[variable] = UNDEFINED;
    }

    @Override
    public boolean isUndefined(int variable) {
        return valueOf[variable] == UNDEFINED;
    }

    @Override
    public int eligibleBase(int pivot) {
        for (int variable : variablesOf[pivot]) {
            if (bases.contains(variable)) return variable;
        }
        return -1;
    }

    @Override
    public int firstUnknown(int equation) {
        for (int variable : variablesOf[equation]) {
            if (isUndefined(variable)) return variable;
        }
        return -1;
    }

    @Override
    public int nbEquations() {
        return nbEquations;
    }

    @Override
    public int nbVariables() {
        return nbVariables;
    }

    @Override
    public Iterable<Data> variablesOf(int target) {
        return null;
    }

    @Override
    public boolean sameOffBaseVariables(int eq1, int eq2) {
        return false;
    }

    @Override
    public boolean sameOffBaseVariables(Row eq1, Row eq2) {
        return false;
    }

    @Override
    public int baseVariableOf(int equation) {
        return 0;
    }

    @Override
    public int baseVariableOf(Row equation) {
        return 0;
    }

    @Override
    public Iterable<Row> activeEquations() {
        return null;
    }

    @Override
    public int numberOfUndefinedVariables() {
        return 0;
    }

    @Override
    public int numberOfEquationsOf(int variable) {
        return 0;
    }

    @Override
    public int firstOffBase(int pivot) {
        return 0;
    }

    @Override
    public IntList unassignedVars() {
        return null;
    }
}
