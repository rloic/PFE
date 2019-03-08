package com.github.rloic.paper;

import it.unimi.dsi.fastutil.ints.IntList;

public interface XORMatrix {

    /**
     * Return the number of nbEquations of the matrix
     * @return The number of nbEquations of the matrix
     */
    int nbEquations();

    /**
     * Return the number of variables of the matrix
     * @return The number of variables of the matrix
     */
    int nbVariables();

    /**
     * Return a list of equation (int)
     * @return The equations of the matrix
     */
    IntList equations();

    /**
     * Return a list of variables (int)
     * @return The variables of the matrix
     */
    IntList variables();

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

    /**
     * Return if the variable is fixed to true or false (!= undefined)
     * @param variable The variable
     * @return True if the variable is assigned to a value else false
     */
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
     * Return the number of 1 on the row
     * @param row The row
     * @return The number of 1 on the row
     */
    int nbTrues(int row);

    /**
     * Return if the variable is fixed to true
     * @param variable The variable
     * @return True if the variable is assigned to true
     */
    boolean isTrue(int variable);

    /**
     * Return if the variable is fixed to false
     * @param variable The variable
     * @return False is the variable is  to false
     */
    boolean isFalse(int variable);

    /**
     * Perform the assignment target <- target xor pivot
     * @param target The target equation
     * @param pivot The pivot equation
     * @return Perform a xor between target and pivot then assign target to this result
     */
    boolean xor(int target, int pivot);

    /**
     * Mark the variable as a Base variable with the pivot
     * @param pivot The line pivot
     * @param variable The variable
     */
    void setBase(int pivot, int variable);

    /**
     * Remove the variable from the base
     * @param variable The variable
     */
    void removeFromBase(int variable);

    /**
     * Perform a swap between the equationA and the equationB
     * @param equationA The equationA
     * @param equationB The equationB
     */
    void swap(int equationA, int equationB);

    /**
     * Assign variable to value
     * @param variable The variable
     * @param value The value
     */
    void fix(int variable, boolean value);

    /**
     * Return the first unknown variable of the equation
     * @param equation The equation
     * @return The first variable of the equation (-1 if no unknown is present in the equation)
     */
    int firstUnknown(int equation);

    /**
     * Return the first variable that is eligible as a pivot on the given equation
     * @param equation The equation
     * @return The variable of the equation that is eligible as a pivot (-1 if none was found)
     */
    int firstEligiblePivot(int equation);

    /**
     * Return true if the matrix is stable (none constraints are broken)
     * @return True if the matrix is stable else false
     */
    boolean stableState();

    /**
     * Reset the matrix to its initial state
     */
    void clear();

    /**
     * Return if the current equation is empty
     * @param equation The equation
     * @return True if the equation does not contains 'x' or '1'
     */
    default boolean isEmptyEquation(int equation) {
        return nbUnknowns(equation) == 0 && nbTrues(equation) == 0;
    }

    /**
     * Return if the equation is unsatisfied
     * @param equation The equation
     * @return True if the equation cannot be satisfied else false
     */
    default boolean isInvalid(int equation) {
        return nbUnknowns(equation) == 0 && nbTrues(equation) == 1;
    }

    /**
     * Remove all the empty equations from the matrix
     */
    void removeEmptyEquations();

    /**
     * Remove all the unused variables from the matrix
     */
    void removeUnusedVariables();

    /**
     * Return the list of equations where the variable is present
     * @param variable The variable
     * @return The list of equations of the variable
     */
    IntList equationsOf(int variable);

    IntList unknownsOf(int equation);

}
