package com.github.rloic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.rloic.paper.Algorithms;
import com.github.rloic.paper.XORMatrix;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import com.github.rloic.util.Logger;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import org.chocosolver.memory.IStateBitSet;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;


public class ToyExample {

    public static void main(String[] args) {
        Model m = new Model();
        BoolVar[] vars = m.boolVarArray(2);
        m.post(new Constraint("My Constraint", new MyPropagator(vars)));
        Solver s = m.getSolver();
        while (s.solve()) {}
        s.printShortStatistics();
    }

    static class MyPropagator extends Propagator<BoolVar> {

        public MyPropagator(BoolVar[] vars) {
            super(vars, PropagatorPriority.LINEAR, true);
        }

        @Override
        public void propagate(int evtmask) throws ContradictionException {

        }

        @Override
        public void propagate(int idxVarInProp, int mask) throws ContradictionException {
            if(idxVarInProp != 0) {
                vars[0].setToTrue(this);
            }
        }

        @Override
        public ESat isEntailed() {
            return ESat.UNDEFINED;
        }
    }

}
