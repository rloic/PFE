package com.github.rloic.inference;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.exception.ContradictionException;

public interface InferenceMatrix {

    /**
     * Return the number of nbEquations in the inference matrix
     * @return The number of nbEquations in the inference matrix
     */
    int rows();

    /**
     * Return the number of variables in the inference matrix
     * @return Return the number of variables in the inference matrix
     */
    int cols();

    /**
     * Indicate if the variables belongs to the base part of the matrix
     *    v v v v
     *    _ _ _ x x x
     *    x _ _ _ x x
     *    _ x _ _ x _
     *    _ _ x _ _ _
     * Here the method must return true for 0, 1, 2, 3 and false for 4, 5.
     * @param variable The column index (starting from 0)
     * @return true if the column belongs to the base part
     */
    boolean isBase(int variable);

    /**
     * Return the number of the getRow that is the base for the column
     * @param variable The column index (starting from 0)
     * @return -1 if the column doesn't belongs to the base, else return
     * the index of the getRow that is the pivot of the column
     */
    int pivotOf(int variable);

    /**
     * Get the old base variables out of the base and add the new base column to the base
     * @param oldBaseVariable The index of the old variable
     * @param newBaseVariable The index of the new variable
     */
    void swapBase(int oldBaseVariable, int newBaseVariable);

    /**
     * Replace the rowA as rowA <- rowA xor rowB
     * @param rowA The index of the first getRow
     * @param rowB The index of the second getRow
     */
    void xor(int rowA, int rowB);

    /**
     * Set a variable to a fixed value
     * @param variable The index of the variable to set
     * @param value The fixed value of the variable
     */
    void fix(int variable, boolean value) throws IllegalStateException;

    /**
     * Unset the variable
     * @param variable The index of the variable to unset
     */
    void unfix(int variable);

    /**
     * Link the variable A to the variable B (A == B)
     * @param variableA A variable
     * @param variableB A second variable
     */
    void link(int variableA, int variableB) throws IllegalStateException;

    /**
     * Unlink a variable
     * @param variable The variable to unlink
     */
    void unlink(int variable);

    /**
     * Remove the variable to the base
     * @param variable The variable to remove from the base
     */
    void removeFromBase(int variable);

    /**
     * Add the variable to the base with a given pivot
     * @param variable The variable to append to the base
     * @param pivot The pivot used by the base variable
     */
    void appendToBase(int variable, int pivot);

    /**
     * Indicates if a variable is unknown. Ex:
     * _ x _ 1 0
     * x _ _ 0 x
     * The positions [0, 1], [1, 0] and [1, 4] are unknown
     * @param row The getRow index
     * @param col The column index
     * @return true is the variable is unknown else false
     */
    boolean isUnknown(int row, int col);

    /**
     * Return if the element at [getRow, col] is fixed to true
     * @param row The getRow
     * @param col The column
     * @return true if the element at [getRow, col] is fixed to true
     */
    boolean isTrue(int row, int col);

    /**
     * Return if the element at [getRow, col] is fixed to false
     * @param row The getRow
     * @param col The column
     * @return true if the element at [getRow, col] is fixed to false
     */
    boolean isFalse(int row, int col);

    /**
     * Return the list of nbEquations where the variable 'variable' is unknown
     * @param variable The variable
     * @return The list of nbEquations where the variable 'variable' is unknown
     */
    IntList rowsWhereUnknown(int variable);

    boolean isEquivalent(int varA, int varB);

    /**
     * Return if all the variables are set to a value
     * @return true if all the variables are set to a value else false
     */
    boolean isAllFixed();

    boolean isFixed(int variable);

    String varsToString();

    default void check(boolean proposition) throws IllegalStateException {
        if (!proposition) throw new IllegalStateException();
    }

    default void check(boolean proposition, String message) throws IllegalStateException {
        if (!proposition) throw new IllegalStateException(message);
    }

    IntList equivalents(int variable);

}
