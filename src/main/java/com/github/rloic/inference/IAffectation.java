package com.github.rloic.inference;

import com.github.rloic.paper.XORMatrix;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;

import java.util.List;

public interface IAffectation {
   int variable();
   boolean value();

   void apply(XORMatrix matrix, List<IAffectation> queue) throws ContradictionException;

   void unapply(XORMatrix matrix);

   void propagate(BoolVar[] vars, ICause cause) throws ContradictionException;
}
