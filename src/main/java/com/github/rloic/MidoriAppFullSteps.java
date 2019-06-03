package com.github.rloic;

import com.github.rloic.midori.fullsteps.MidoriFullSteps;
import com.github.rloic.strategy.WDeg;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;

public class MidoriAppFullSteps {

    public static void main(String[] args) {

        int r = (args.length == 2) ? Integer.parseInt(args[0]) : 3;
        int numberOfActiveSBoxes = (args.length == 2) ? Integer.parseInt(args[1]) : 3;

        MidoriFullSteps midoriFullSteps = new MidoriFullSteps(128, r, numberOfActiveSBoxes);
        Solver solver = midoriFullSteps.solver;

        solver.setSearch(
                Search.inputOrderLBSearch(midoriFullSteps.nbActives),
                new WDeg(midoriFullSteps.ΔSBoxes(), 0L, new IntDomainMin(), midoriFullSteps.constraintsOf),
                new WDeg(midoriFullSteps.abstractVars(), 0L, new IntDomainMin(), midoriFullSteps.constraintsOf),
                Search.intVarSearch(midoriFullSteps.model.retrieveIntVars(true))
        );
        solver.setSearch(
                Search.lastConflict(solver.getSearch())
        );

        Solution bestSolution = solver.findOptimalSolution(midoriFullSteps.objective, Model.MINIMIZE);

        if (bestSolution != null) {
            midoriFullSteps.prettyPrint(bestSolution);
            solver.printShortStatistics();
            System.out.println(csv("2^{-" + bestSolution.getIntVal(midoriFullSteps.objective) + "}", solver.getMeasures().getSolutionCount(), solver.getMeasures().getNodeCount()));
        } else {
            System.out.println(",,");
        }

    }

    private static String csv(Object... o) {
        if (o.length == 0) return "";
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < o.length - 1; i++) {
            str.append(o[i].toString());
            str.append(",");
        }
        str.append(o[o.length - 1].toString());
        return str.toString();
    }

}
