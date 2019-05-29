package com.github.rloic;

import com.github.rloic.midori.fullsteps.MidoriFullSteps;
import com.github.rloic.strategy.WDeg;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;

public class MidoriAppFullSteps {

    public static void main(String[] args) throws ContradictionException {

        MidoriFullSteps midoriFullSteps = new MidoriFullSteps(128, 3, 3);
        Solver solver = midoriFullSteps.solver;

        Solution bestSolution = solver.findOptimalSolution(midoriFullSteps.objective, Model.MINIMIZE);

        solver.setSearch(
                new WDeg(midoriFullSteps.ΔSBoxes(), 0L, new IntDomainMin(), midoriFullSteps.constraintsOf),
                new WDeg(midoriFullSteps.abstractVars(), 0L, new IntDomainMin(), midoriFullSteps.constraintsOf)
        );
        solver.setSearch(
                Search.lastConflict(solver.getSearch())
        );

        if (bestSolution != null) {
            bestSolution.restore();
        }

    }

}
