package com.github.rloic;

import com.github.rloic.filter.EnumFilter;
import com.github.rloic.filter.EnumFilterRound;
import com.github.rloic.midori.MidoriGlobalFull;
import com.github.rloic.midori.round.MidoriGlobalRound;
import com.github.rloic.midori.round.MidoriGlobalRoundFull;
import com.github.rloic.midori.round.MidoriRound;
import com.github.rloic.strategy.WDeg;
import com.github.rloic.util.VoidPredicate;
import com.github.rloic.wip.WeightedConstraint;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MidoriApp {

    private final static int DEFAULT_NB_ROUNDS = 3;

    public static void main(String[] args) {
        final int rounds = (args.length == 1) ? parseIntOrDefault(args[0], DEFAULT_NB_ROUNDS) : DEFAULT_NB_ROUNDS;
        final int numberOfActiveSBoxes = rounds;

        MidoriRound activesSBoxesByRounds = new MidoriRound(rounds, numberOfActiveSBoxes);
        runModel(
                activesSBoxesByRounds.m,
                activesSBoxesByRounds.nbActives,
                activesSBoxesByRounds.sBoxes,
                rounds,
                numberOfActiveSBoxes
        );

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

    private static void run(
            Model model,
            BoolVar[] sBoxes,
            BoolVar[] varsToAssign,
            Int2ObjectMap<List<WeightedConstraint>> constraintsOf,
            int objStep1
    ) {
        Solver solver = model.getSolver();
        solver.setSearch(
                Search.intVarSearch(sBoxes),
                Search.intVarSearch(varsToAssign)
        );
        solver.plugMonitor(new EnumFilter(model, sBoxes, objStep1));
        while (solver.solve()) {
            display(sBoxes);
        }
        solver.printShortStatistics();
    }

    private static void runModel(
            Model m,
            IntVar[] nbActives,
            BoolVar[] sBoxes,
            int r,
            int objStep1
    ) {
        Solver solver = m.getSolver();
        solver.setSearch(
                Search.intVarSearch(sBoxes),
                Search.intVarSearch(nbActives)
        );
        solver.setSearch(
                Search.lastConflict(solver.getSearch())
        );
        solver.plugMonitor(new EnumFilterRound(m, nbActives, objStep1));
        while (solver.solve()) {
            display(nbActives);
            solver.printShortStatistics();

            MidoriGlobalFull midoriGlobalFull = new MidoriGlobalFull(r, objStep1, nbActives);
            run(
                    midoriGlobalFull.m,
                    midoriGlobalFull.sBoxes,
                    midoriGlobalFull.variablesToAssign,
                    midoriGlobalFull.constraintsOf,
                    objStep1
            );
        }
        solver.printShortStatistics();
    }

}
