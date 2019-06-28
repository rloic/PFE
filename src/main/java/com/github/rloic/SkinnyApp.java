package com.github.rloic;

import com.github.rloic.skinny.Skinny;
import com.github.rloic.strategy.WDeg;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;

public class SkinnyApp {

    public static void main(String[] args) {
        Skinny model = new Skinny(128, 7, 26);
        Solver solver = model.m.getSolver();

        solver.setSearch(
                Search.inputOrderLBSearch(model.nbActives),
                new WDeg(model.sBoxes(), 0L, new IntDomainMin(), model.constraintsOf),
                Search.intVarSearch(model.m.retrieveIntVars(true))
        );

        solver.setSearch(
                Search.lastConflict(solver.getSearch())
        );

        Solution solution = solver.findOptimalSolution(model.objective, Model.MINIMIZE);
        System.out.println("2^{-" + (solution.getIntVal(model.objective) / 10.0) + "}");
        model.prettyPrint(solution);
        solver.printShortStatistics();
    }

}
