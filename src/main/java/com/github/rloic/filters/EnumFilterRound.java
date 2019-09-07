package com.github.rloic.filters;

import org.chocosolver.sat.PropSat;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;

public class EnumFilterRound implements IMonitorSolution {

    private final IntVar[] nbActive;
    private final Model model;
    private PropSat psat;

    public EnumFilterRound(Model model, IntVar[] nbActive, int objStep1) {
        this.model = model;
        this.nbActive = nbActive;
        psat = model.getMinisat().getPropSat();
    }

    @Override
    public void onSolution() {
        int[] lits = new int[nbActive.length];
        int cpt = 0;
        for (IntVar s : nbActive){
            lits[cpt++] = psat.makeLiteral(model.arithm(s, "=", s.getValue()).reify(), false);
        }
        psat.addLearnt(lits);
    }

}
