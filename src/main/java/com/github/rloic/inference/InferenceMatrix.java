package com.github.rloic.inference;

import it.unimi.dsi.fastutil.ints.IntList;

public interface InferenceMatrix {

    /**
     * Return the number of rows in the inference matrix
     * @return The number of rows in the inference matrix
     */
    int rows();

    /**
     * Return the number of columns in the inference matrix
     * @return Return the number of columns in the inference matrix
     */
    int cols();

    /**
     * Indicate if the columns belongs to the base part of the matrix
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
     * Return the number of the row that is the base for the column
     * @param variable The column index (starting from 0)
     * @return -1 if the column doesn't belongs to the base, else return
     * the index of the row that is the pivot of the column
     */
    int pivotOf(int variable);

    /**
     * Get the old base columns out of the base and add the new base column to the base
     * @param oldBaseVariable The index of the old variable
     * @param newBaseVariable The index of the new variable
     */
    void swapBase(int oldBaseVariable, int newBaseVariable);

    /**
     * Replace the rowA as rowA <- rowA xor rowB
     * @param rowA The index of the first row
     * @param rowB The index of the second row
     */
    void xor(int rowA, int rowB);

    /**
     * Set a variable to a fixed value
     * @param variable The index of the variable to set
     * @param value The fixed value of the variable
     */
    void fix(int variable, boolean value);

    /**
     * Link the variable A to the variable B (A == B)
     * @param variableA A variable
     * @param variableB A second variable
     */
    void link(int variableA, int variableB);

    /**
     * Unlink a variable
     * @param variable The variable to unlink
     */
    void unlink(int variable);

    /**
     * Unset the variable
     * @param variable The index of the variable to unset
     */
    void unfix(int variable);

    void removeFromBase(int variable);

    void appendToBase(int variable, int pivot);

    /**
     * Indicates if a variable is unknown. Ex:
     * _ x _ 1 0
     * x _ _ 0 x
     * The positions [0, 1], [1, 0] and [1, 4] are unknown
     * @param row The row index
     * @param col The column index
     * @return true is the variable is unknown else false
     */
    boolean isUnknown(int row, int col);

    /**
     * Return the list of rows where the variable 'variable' is unknown
     * @param variable The variable
     * @return The list of rows where the variable 'variable' is unknown
     */
    IntList rowsWhereUnknown(int variable);

    /**
     * Return all the relationship that can be inferred from the current state
     * @return All the relationship that can be inferred from the current state
     */
    Inferences infer();

}
