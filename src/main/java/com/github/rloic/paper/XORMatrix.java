package com.github.rloic.paper;

import it.unimi.dsi.fastutil.ints.IntList;

public interface XORMatrix {

    /**
     * Return the number of nbRows of the matrix
     * @return The number of nbRows of the matrix
     */
    int nbRows();

    /**
     * Return the number of columns of the matrix
     * @return The number of columns of the matrix
     */
    int nbColumns();

    IntList rows();

    IntList columns();

    /**
     * Return if M[row][col] == 'x'
     * @param row The row of the element
     * @param col The column of the element
     * @return True if M[row][col] = 'x' else false
     */
    boolean isUndefined(int row, int col);

    /**
     * Return if M[row][col] == 0
     * @param row The row of the element
     * @param col The column of the element
     * @return True if M[row][col] = 0 else false
     */
    boolean isFalse(int row, int col);

    /**
     * Return if M[row][col] == 1
     * @param row The row of the element
     * @param col The column of the element
     * @return True if M[row][col] = 1 else false
     */
    boolean isTrue(int row, int col);

    /**
     * Return if M[row][col] == _
     * @param row The row of the element
     * @param col The column of the element
     * @return True if M[row][col] = _ else false
     */
    boolean isNone(int row, int col);

    boolean isFixed(int variable);

    /**
     * Return if the variable belongs to the base
     * @param variable The index of the variable
     * @return True if the variable belongs to the base else false
     */
    boolean isBase(int variable);

    /**
     * Return the pivot line of a base variable
     * @param variable The index of the base variable
     * @return The pivot of the variable is isBase(variable) else -1
     */
    int pivotOf(int variable);

    /**
     * Remove a column from the matrix
     * @param col The number of the column to delete
     */
    void removeVar(int col);

    /**
     * Remove a row from the matrix
     * @param row The number of the row to delete
     */
    void removeRow(int row);

    /**
     * Return the number of 'x' on the row
     * @param row The row
     * @return The number of 'x' on the row
     */
    int nbUnknowns(int row);

    /**
     * Decrement the number of 'x' on the row and return the value
     * @param row The row
     * @return The new number of 'x' on the row
     */
    int decrementUnknowns(int row);

    /**
     * Return the number of 1 on the row
     * @param row The row
     * @return The number of 1 on the row
     */
    int nbTrues(int row);

    boolean isTrue(int variable);

    boolean isFalse(int variable);

    boolean xor(int target, int pivot);

    void setBase(int pivot, int variable);

    void removeFromBase(int variable);

    void swap(int rowA, int rowB);

    void fix(int variable, boolean value);

    int firstUndefined(int row);

    int firstUndefined(int row, int except);

    int firstEligiblePivot(int row);

    void incrementUnknowns(int pivot);

    boolean stableState();

    void clear();

    default boolean emptyRow(int row) {
        return nbUnknowns(row) == 0 && nbTrues(row) == 0;
    }

    default boolean isInvalid(int row) {
        return nbUnknowns(row) == 0 && nbTrues(row) == 1;
    }

}
