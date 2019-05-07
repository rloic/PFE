package com.github.rloic.wip.fastset;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.util.FastSet;

public class FastSetMatrix implements IDancingLinksMatrix {

    private final int NO_PIVOT = -1;
    private final int NO_BASE = -1;
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
    private final int[] baseOf;

    private final FastSet unassignedVars;

    public FastSetMatrix(
            int[][] equations,
            int nbVariables
    ) {
        this.nbVariables = nbVariables;
        this.nbEquations = equations.length;

        valueOf = new int[nbVariables];

        activeVariables = new FastSet(nbVariables);
        for (int variable = 0; variable < nbVariables; variable++) {
            activeVariables.add(variable);
        }
        activeEquations = new FastSet(nbEquations);
        for (int equation = 0; equation < nbEquations; equation++) {
            activeEquations.add(equation);
        }

        equationsOf = new FastSet[nbVariables];
        variablesOf = new FastSet[nbEquations];

        nbX = new int[nbEquations];
        nbTrue = new int[nbEquations];

        activeVariables.forEach(activeVariable -> equationsOf[activeVariable] = new FastSet(nbEquations));

        activeEquations.forEach(equation -> {
            variablesOf[equation] = new FastSet(nbVariables);
            int[] initialVariables = equations[equation];
            nbX[equation] = initialVariables.length;
            for (int variable : initialVariables) {
                variablesOf[equation].add(variable);
                equationsOf[variable].add(equation);
            }
        });

        bases = new FastSet(nbVariables);
        pivotOf = new int[nbVariables];
        baseOf = new int[nbEquations];
        unassignedVars = new FastSet(nbVariables);
        for (int variable = 0; variable < nbVariables; variable++) {
            unassignedVars.add(variable);
        }
    }

    @Override
    public boolean isUnknown(int equation, int variable) {
        return variablesOf[equation].contains(variable)
                && equationsOf[variable].contains(equation)
                && isUndefined(variable);
    }

    @Override
    public boolean isTrue(int equation, int variable) {
        return variablesOf[equation].contains(variable)
                && equationsOf[variable].contains(equation)
                && isTrue(variable);
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
        final FastSet targetEquation = variablesOf[target];
        equationsOf[pivot].forEach(value -> {
            if (targetEquation.contains(value)) {
                targetEquation.remove(value);
                equationsOf[value].remove(target);
                if (isTrue(value)) {
                    nbTrue[target] -= 1;
                } else {
                    nbX[target] -= 1;
                }
            } else {
                targetEquation.add(value);
                equationsOf[value].add(target);
                if (isTrue(value)) {
                    nbTrue[target] +=1;
                } else {
                    nbX[target] += 1;
                }
            }
        });
    }

    @Override
    public void setBase(int pivot, int variable) {
        bases.add(variable);
        pivotOf[variable] = pivot;
        baseOf[pivot] = variable;
    }

    @Override
    public void setOffBase(int variable) {
        baseOf[pivotOf[variable]] = -1;
        pivotOf[variable] = -1;
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
        activeVariables.add(variable);
        for (int equation : equationsOf[variable]) {
            variablesOf[equation].add(variable);
        }
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
        activeEquations.add(equation);
        for (int variable : variablesOf[equation]) {
            equationsOf[variable].add(equation);
        }
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
        return nbTrue[equation] != 1 || nbX[equation] != 0;
    }

    @Override
    public boolean isInvalid(int equation) {
        return nbTrue[equation] == 1 && nbX[equation] == 1;
    }

    @Override
    public boolean isEmpty(int equation) {
        assert variablesOf[equation].isEmpty() == (nbTrue[equation] == 0 && nbX[equation] == 0);
        return nbTrue[equation] == 0 && nbX[equation] == 0;
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
    public Iterable<Integer> equationsOf(int variable) {
        return equationsOf[variable];
    }

    @Override
    public int pivotOf(int variable) {
        return (bases.contains(variable)) ? pivotOf[variable] : -1;
    }

    @Override
    public void set(int variable, boolean value) {
        valueOf[variable] = (value) ? TRUE : FALSE;
        equationsOf[variable].forEach(equation -> {
            nbX[equation] -= 1;
            if (value) {
                nbTrue[equation] += 1;
            }
        });
        unassignedVars.remove(variable);
    }

    @Override
    public void unSet(int variable) {
        boolean value = valueOf[variable] == TRUE;
        equationsOf[variable].forEach(equation -> {
            nbX[equation] += 1;
            if (value) {
                nbTrue[equation] -= 1;
            }
        });
        unassignedVars.add(variable);
    }

    @Override
    public boolean isUndefined(int variable) {
        return valueOf[variable] == UNDEFINED;
    }

    @Override
    public int eligibleBase(int pivot) {
        return variablesOf[pivot].first(value -> !bases.contains(value) && valueOf[value] != FALSE);
    }

    @Override
    public int firstUnknown(int equation) {
        return variablesOf[equation].first(variable -> valueOf[variable] == UNDEFINED);
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
    public Iterable<Integer> variablesOf(int target) {
        return variablesOf[target];
    }

    @Override
    public boolean sameOffBaseVariables(int eq1, int eq2) {
        return false;
    }

    @Override
    public int baseVariableOf(int equation) {
        return baseOf[equation];
    }

    @Override
    public Iterable<Integer> activeEquations() {
        return activeEquations;
    }

    @Override
    public int numberOfUndefinedVariables() {
        return unassignedVars().size();
    }

    @Override
    public int numberOfEquationsOf(int variable) {
        return equationsOf[variable].size();
    }

    @Override
    public int firstOffBase(int pivot) {
        return eligibleBase(pivot);
    }

    @Override
    public FastSet unassignedVars() {
        return unassignedVars;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        for (int variable = 0; variable < nbVariables; variable++) {
            for (int equation = 0; equation < nbEquations; equation++) {
                assert equationsOf[variable].contains(equation) == variablesOf[equation].contains(variable);
                if (variablesOf[equation].contains(variable)) {
                    if (isBase(variable)) {
                        if (isTrue(variable)) {
                            str.append("T");
                        } else {
                            str.append("x");
                        }
                    } else {
                        if (isTrue(variable)) {
                            str.append("t");
                        } else {
                            str.append("x");
                        }
                    }

                } else {
                    str.append("_");
                }
            }
            str.append("\n");
        }

        return str.toString();
    }
}
