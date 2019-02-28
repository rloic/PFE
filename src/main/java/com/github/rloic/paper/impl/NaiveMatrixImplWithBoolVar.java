package com.github.rloic.paper.impl;

import com.github.rloic.paper.XORMatrix;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NaiveMatrixImplWithBoolVar implements XORMatrix {

    private BoolVar[][] data;
    private BoolVar[] variables;

    private int rows;
    private int cols;
    private boolean[] isBase;
    private int[] pivotOf;
    private List<FixAction> stack;
    private final Propagator<BoolVar> propagator;

    public NaiveMatrixImplWithBoolVar(
            BoolVar[] variables,
            BoolVar[][] equations,
            int cols,
            Propagator<BoolVar> prop
    ) {
        this.rows = equations.length;
        this.cols = cols;
        isBase = new boolean[cols];
        pivotOf = new int[cols];
        Arrays.fill(pivotOf, -1);
        stack = new ArrayList<>();
        this.variables = variables;
        this.data = equations;
        this.propagator = prop;
        XORMatrix.normalize(this);
    }

    @Override
    public int cols() {
        return cols;
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public boolean isUnknown(int row, int variable) {
        return data[row][variable] != null;
    }

    @Override
    public boolean isUndefined(int variable) {
        return !variables[variable].isInstantiated();
    }

    @Override
    public boolean isFixedToTrue(int variable) {
        return variables[variable].isInstantiated() && variables[variable].getValue() == 1;
    }

    @Override
    public boolean isFixedToFalse(int variable) {
        return variables[variable].isInstantiated() && variables[variable].getValue() == 0;
    }

    @Override
    public int nbUnknowns(int row) {
        int count = 0;
        for (int j = 0; j < cols; j++) {
            if (data[row][j] != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int nbTrues(int row) {
        int count = 0;
        for (int j = 0; j < cols; j++) {
            if (data[row][j] != null && isFixedToTrue(j)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int pivotOf(int variable) {
        return pivotOf[variable];
    }

    @Override
    public boolean isBase(int variable) {
        return isBase[variable];
    }

    @Override
    public boolean xor(int rowA, int rowB) {
        for (int j = 0; j < cols; j++) {
            BoolVar xorResult;
            if (data[rowA][j] == null && data[rowB][j] == null) {
                xorResult = null;
            } else if (data[rowA][j] == null && data[rowB][j] != null) {
                xorResult = data[rowB][j];
            } else if (data[rowA][j] != null && data[rowB][j] == null) {
                xorResult = data[rowA][j];
            } else {
                xorResult = null;
            }
            data[rowA][j] = xorResult;
        }
        return false;
    }

    @Override
    public void fix(int variable, boolean value) throws ContradictionException {
        if(value) {
            variables[variable].setToTrue(propagator);
        } else {
            variables[variable].setToFalse(propagator);
        }
        for(int i = 0; i < rows; i++) {
            if (data[i][variable] != null) {
                data[i][variable] = null;
            }
        }
        if(isBase(variable)) {
            int newBaseVar = 0;
            while (newBaseVar < cols && (!isUnknown(pivotOf[variable], newBaseVar) || variables[variable].isInstantiated() || newBaseVar ==variable)) {
                newBaseVar++;
            }
            if(newBaseVar < cols) {
                swapBase(variable, newBaseVar);
                IntList memXors = new IntArrayList();
                for(int i = 0; i < rows(); i++) {
                    if (i != pivotOf[newBaseVar] && isUnknown(i, newBaseVar)) {
                        xor(i, pivotOf[newBaseVar]);
                        memXors.add(i);
                    }
                }
                stack.add(new FixActionWithBaseSwap(variable, value, newBaseVar, memXors));
            } else {
                stack.add(new FixActionWithBaseRemoval(variable, value, pivotOf[variable]));
                removeFromBase(variable);
            }
        } else {
            stack.add(new FixAction(variable, value));
        }
    }

    @Override
    public void rollback() {
        FixAction action = stack.remove(stack.size() - 1);
        if (action instanceof FixActionWithBaseSwap) {
            FixActionWithBaseSwap a = (FixActionWithBaseSwap) action;
            for(int i : a.xors) {
                xor(i, pivotOf[a.newBaseVar]);
            }
            swapBase(a.newBaseVar, action.variable);
        } else if (action instanceof FixActionWithBaseRemoval) {
            FixActionWithBaseRemoval a = (FixActionWithBaseRemoval) action;
            appendToBase(a.pivot, a.variable);
        }
    }

    @Override
    public void appendToBase(int pivot, int variable) {
        pivotOf[variable] = pivot;
        isBase[variable] = true;
    }

    @Override
    public void removeFromBase(int variable) {
        isBase[variable] = false;
        pivotOf[variable] = -1;
    }

    @Override
    public boolean isFixed(int variable) {
        return variables[variable].isInstantiated();
    }

    @Override
    public boolean isFull() {
        for(int j = 0; j < cols; j++) {
            if (!variables[j].isInstantiated()) return false;
        }
        return true;
    }
}
