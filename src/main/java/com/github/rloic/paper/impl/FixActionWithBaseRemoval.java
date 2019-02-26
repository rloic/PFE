package com.github.rloic.paper.impl;

public class FixActionWithBaseRemoval extends FixAction {

    final int pivot;

    public FixActionWithBaseRemoval(int variable, boolean value, int pivot) {
        super(variable, value);
        this.pivot = pivot;
    }
}
