package com.github.rloic;

import com.github.rloic.aes.AdvancedAESModel;
import com.github.rloic.aes.aes128.BasicAESModel;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;

public class App {

    public static void main(String[] args) {
        AdvancedAESModel model = new AdvancedAESModel(5, 2);
        model.buildXorList();
    }

    private static void bench(int r, int objStep1) {
        System.out.println("r=" + r + " and objStep1=" + objStep1);
        //new BasicAESSolver(null, r, objStep1);
        Solver s = new BasicAESModel(r, objStep1).getSolver();
        while (s.solve()) {
        }
        s.printShortStatistics();

    }

}
