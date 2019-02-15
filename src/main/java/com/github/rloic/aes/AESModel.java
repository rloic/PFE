package com.github.rloic.aes;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;

public interface AESModel {

    BoolVar[] getSBoxes();
    Solver getSolver();

}
