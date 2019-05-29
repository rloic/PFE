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

        MidoriGlobalRound activesSBoxesByRounds = new MidoriGlobalRoundFull(rounds, numberOfActiveSBoxes);
        Pair<Integer, Long> stats = step0(
                activesSBoxesByRounds.m,
                activesSBoxesByRounds.nbActives,
                activesSBoxesByRounds.sBoxes,
                activesSBoxesByRounds.constraintsOf,
                rounds,
                numberOfActiveSBoxes
        );
       System.out.println(stats._0 + "," + stats._1);
    }

    private static Pair<Integer, Long> step0(
            Model m,
            IntVar[] nbActives,
            BoolVar[] sBoxes,
            Int2ObjectMap<List<WeightedConstraint>> constraintsOf,
            int r,
            int objStep1
    ) {
        final Solver solver = m.getSolver();
        IntVar[] invNbActives = new IntVar[nbActives.length];
        for (int i = 0; i < nbActives.length; i++) {
            invNbActives[nbActives.length - 1 - i] = nbActives[i];
        }
        if (constraintsOf != null) {
            solver.setSearch(
                    Search.minDomLBSearch(invNbActives),
                    new WDeg(sBoxes, 0L, new IntDomainMin(), constraintsOf)
            );
        } else {
            solver.setSearch(
                    Search.intVarSearch(nbActives),
                    Search.intVarSearch(sBoxes)
            );
        }
        solver.setSearch(
                Search.lastConflict(solver.getSearch())
        );
        solver.plugMonitor(new EnumFilterRound(m, nbActives, objStep1));
        int nbSolutions = 0;
        long nbNodes = 0L;
        while (solver.solve()) {
            display(nbActives);
            solver.printShortStatistics();

            MidoriGlobal midoriGlobalFull = new MidoriGlobalFull(r, objStep1, nbActives);
            Pair<Integer, Long> subResult = step1(
                    midoriGlobalFull.m,
                    midoriGlobalFull.sBoxes,
                    midoriGlobalFull.variablesToAssign,
                    objStep1
            );

            nbSolutions += subResult._0;
            nbNodes += subResult._1;
        }
        nbNodes += solver.getMeasures().getNodeCount();
        solver.printShortStatistics();
        return new Pair<>(nbSolutions, nbNodes);
    }

    private static Pair<Integer, Long> step1(
            Model model,
            BoolVar[] sBoxes,
            BoolVar[] varsToAssign,
            int objStep1
    ) {
        Solver solver = model.getSolver();
        solver.setSearch(
                Search.intVarSearch(sBoxes),
                Search.intVarSearch(varsToAssign)
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
