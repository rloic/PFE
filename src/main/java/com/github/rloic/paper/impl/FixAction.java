package com.github.rloic.paper.impl;


import it.unimi.dsi.fastutil.ints.IntList;

public class FixAction {

    final int variable;
    final boolean value;

    public FixAction(int variable, boolean value) {
        this.variable = variable;
        this.value = value;
    }

}
