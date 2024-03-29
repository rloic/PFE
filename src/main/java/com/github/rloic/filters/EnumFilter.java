package com.github.rloic.filters;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.BoolVar;

/**
 * Force results to have different sBoxes
 */
public class EnumFilter implements IMonitorSolution {

    private final Model m;
    private final BoolVar[] sBoxes;
    private final int objStep1;

    public EnumFilter(Model model, BoolVar[] sBoxes, int objStep1) {
        this.m = model;
        this.sBoxes = sBoxes;
        this.objStep1 = objStep1;
    }

    @Override
    public void onSolution() {
        BoolVar[] negations = new BoolVar[objStep1];
        int cpt = 0;
        for (BoolVar sBox : sBoxes) {
            if(!sBox.isInstantiated()) throw new IllegalStateException("Not instantiated sBox");
            if (sBox.getValue() == 1) {
                negations[cpt++] = sBox;
            }
        }
        m.sum(negations, "<", objStep1).post();
    }

}
