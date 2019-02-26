package com.github.rloic.paper.impl;

import it.unimi.dsi.fastutil.ints.IntList;

public class FixActionWithBaseSwap extends FixAction {

    final int newBaseVar;
    final IntList xors;

    public FixActionWithBaseSwap(int variable, boolean value, int newBaseVar, IntList xors) {
        super(variable, value);
        this.newBaseVar = newBaseVar;
        this.xors = xors;
    }
}
