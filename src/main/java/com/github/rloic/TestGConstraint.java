package com.github.rloic;

import com.github.rloic.aes.EnumFilter;
import com.github.rloic.aes.GlobalXOR;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.util.Logger.DebugLogger.DEBUG;
import static com.github.rloic.util.Logger.InfoLogger.INFO;
import static com.github.rloic.util.Logger.TraceLogger.TRACE;

public class TestGConstraint {

    public static void main(String[] args) {
        Logger.level(INFO);
        GlobalXOR gXor = new GlobalXOR(3, 5, AES_128);

        Solver solver = gXor.m.getSolver();
        EnumFilter enumFilter = new EnumFilter(gXor.m, gXor.sBoxes, 5);
        solver.plugMonitor(enumFilter);
        solver.setSearch(
              Search.intVarSearch(gXor.sBoxes),
              Search.intVarSearch(gXor.m.retrieveBoolVars()),
              Search.intVarSearch(gXor.m.retrieveIntVars(false))
        );
        int count = 0;
        System.out.println("Solve");
        while (solver.solve()) {
            printSBoxes(gXor.sBoxes);
            System.out.println(count++);
        }
        solver.printShortStatistics();
    }

    private static void printSBoxes(BoolVar[] sBoxes) {
        List<Integer> values = Arrays.stream(sBoxes)
                .map(IntVar::getValue)
                .collect(Collectors.toList());
        Logger.info("Solution: " + values);
    }

}
