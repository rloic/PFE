package com.github.rloic.paper;

public interface XORMatrix {

    int cols();
    int rows();
    boolean isUnknown(int row, int variable);
    boolean isUndefined(int variable);
    int nbUnknowns(int row);
    int nbTrues(int row);
    int pivotOf(int variable);
    boolean isBase(int variable);

    void xor(int rowA, int rowB);
    void fix(int variable, boolean value);
    void rollback();
    void appendToBase(int pivot, int variable);
    void removeFromBase(int variable);

    default void swapBase(int oldBaseVariable, int newBaseVariable) {
        appendToBase(pivotOf(oldBaseVariable), newBaseVariable);
        removeFromBase(oldBaseVariable);
    }
}
