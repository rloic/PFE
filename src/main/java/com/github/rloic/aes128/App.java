package com.github.rloic.aes128;

import org.chocosolver.solver.Solver;

public class App {

    public static void main(String[] args) {
        bench(3, 2);
        bench(3, 3);
        bench(3, 4);
    }

    private static void bench(int r, int objStep1) {
        System.out.println("r=" + r + " and objStep1=" + objStep1);
        //new BasicAESSolver(null, r, objStep1);
        Solver s = new BasicModelAES128(r, objStep1).getSolver();
        while (s.solve()) {
        }
        s.printShortStatistics();

    }

}
