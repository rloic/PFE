package com.github.rloic;

import com.github.rloic.aes.AESGlobal;
import com.github.rloic.aes.AESGlobalRound;
import com.github.rloic.aes.KeyBits;
import com.github.rloic.filter.EnumFilter;
import com.github.rloic.filter.EnumFilterRound;
import com.github.rloic.midori.MidoriGlobalFull;
import com.github.rloic.strategy.WDeg;
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

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;

public class AESApp {

    private final static int DEFAULT_NB_ROUNDS = 3;
    private final static int DEFAULT_NB_SBOXES = 5;
    private final static KeyBits DEFAULT_AES_KEY = AES_128;

    public static void main(String[] args) {

        final KeyBits key;
        final int nbRounds;
        final int nbSBoxes;

        if (args.length == 3) {
            key = parseKeyBitsOrDefault(args[0], DEFAULT_AES_KEY);
            nbRounds = parseIntOrDefault(args[1], DEFAULT_NB_ROUNDS);
            nbSBoxes = parseIntOrDefault(args[2], DEFAULT_NB_SBOXES);
        } else {
            key = DEFAULT_AES_KEY;
            nbRounds = DEFAULT_NB_ROUNDS;
            nbSBoxes = DEFAULT_NB_SBOXES;
        }

        System.out.println(key + " " + nbRounds + " " + nbSBoxes);

        AESGlobalRound aesGlobalRound = new AESGlobalRound(nbRounds, nbSBoxes, key);
        runModel(
                aesGlobalRound.m,
                aesGlobalRound.nbActives,
                aesGlobalRound.sBoxes,
                aesGlobalRound.constraintsOf,
                nbRounds,
                nbSBoxes,
                key
        );

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
            Int2ObjectMap<List<WeightedConstraint>> constraintsOf,
            int r,
            int objStep1,
            KeyBits keyBits
    ) {

        final Solver solver = m.getSolver();
        IntVar[] invNbActives = new IntVar[nbActives.length];
        for (int i = 0; i < nbActives.length; i++) {
            invNbActives[nbActives.length - 1 - i] = nbActives[i];
        }
        if (constraintsOf != null) {
            solver.setSearch(
                    Search.inputOrderUBSearch(invNbActives),
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
        while (solver.solve()) {
            display(nbActives);
            solver.printShortStatistics();

            AESGlobal midoriGlobalFull = new AESGlobal(r, objStep1, keyBits, nbActives);
            run(
                    midoriGlobalFull.m,
                    midoriGlobalFull.sBoxes,
                    midoriGlobalFull.varsToAssign,
                    midoriGlobalFull.constraintsOf,
                    objStep1
            );
        }
        solver.printShortStatistics();
    }

    private static int parseIntOrDefault(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static KeyBits parseKeyBitsOrDefault(String str, KeyBits def) {
        if (str.equalsIgnoreCase("aes-128") || str.equalsIgnoreCase("aes128")) {
            return AES_128;
        } else if (str.equalsIgnoreCase("aes-192") || str.equalsIgnoreCase("aes192")) {
            return AES_192;
        } else if (str.equalsIgnoreCase("aes-256") || str.equalsIgnoreCase("aes256")) {
            return AES_256;
        }
        return def;
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
