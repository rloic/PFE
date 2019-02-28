package com.github.rloic.paper.impl;


import it.unimi.dsi.fastutil.ints.IntList;

public class FixActionWithBaseRemoval extends FixAction {

    final int pivot;

    public FixActionWithBaseRemoval(int variable, boolean value, int pivot) {
        super(variable, value);
        this.pivot = pivot;
    }
}
