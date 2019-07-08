package com.github.rloic;

import com.github.rloic.filter.EnumFilter;
import com.github.rloic.filter.EnumFilterRound;
import com.github.rloic.midori.global.MidoriGlobal;
import com.github.rloic.midori.global.gac.MidoriGlobalFull;
import com.github.rloic.midori.global.round.MidoriGlobalRound;
import com.github.rloic.midori.global.gac.round.MidoriGlobalRoundFull;
import com.github.rloic.strategy.WDeg;
import com.github.rloic.util.Pair;
import com.github.rloic.wip.WeightedConstraint;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MidoriApp {

    private final static int DEFAULT_NB_ROUNDS = 3;

    public static void main(String[] args) {
        final int rounds = (args.length >= 1) ? parseIntOrDefault(args[0], DEFAULT_NB_ROUNDS) : DEFAULT_NB_ROUNDS;
        final int numberOfActiveSBoxes = rounds;

        MidoriGlobal midoriGlobalFull = new MidoriGlobalFull(rounds, numberOfActiveSBoxes, null);
        Pair<Integer, Long> stats = step1(
              midoriGlobalFull.m,
              midoriGlobalFull.sBoxes,
              midoriGlobalFull.variablesToAssign,
              midoriGlobalFull.constraintsOf,
              numberOfActiveSBoxes
        );
       System.out.println(stats._0 + "," + stats._1);
    }

    private static Pair<Integer, Long> step1(
            Model model,
            BoolVar[] sBoxes,
            BoolVar[] varsToAssign,
            Int2ObjectMap<List<WeightedConstraint>> constraintsOf,
            int objStep1
    ) {
        Solver solver = model.getSolver();
        solver.setSearch(
                new WDeg(sBoxes, 0L, new IntDomainMin(), constraintsOf),
                new WDeg(varsToAssign, 0L, new IntDomainMin(), constraintsOf),
                Search.intVarSearch(model.retrieveIntVars(true))
        );
        solver.plugMonitor(new EnumFilter(model, sBoxes, objStep1));
        int nbSolutions = 0;
        while (solver.solve()) {
            display(sBoxes);
            nbSolutions += 1;
        }
        return new Pair<>(nbSolutions, solver.getMeasures().getNodeCount());
    }

    private static int parseIntOrDefault(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (Exception unused) {
            return def;
        }
    }

    private static void display(BoolVar[] variables) {
        String output = Arrays.stream(variables)
                .map(v ->
                        String.valueOf(v.getValue())
                )
                .collect(Collectors.joining(", "));
        System.out.println(output);
    }

    private static void display(IntVar[] variables) {
        String output = Arrays.stream(variables)
                .map(v ->
                        String.valueOf(v.getValue())
                )
                .collect(Collectors.joining(", "));
        System.out.println(output);
    }

}
