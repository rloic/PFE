package com.github.rloic.benchmark.impl;

import com.github.rloic.aes.AESGlobal;
import com.github.rloic.filter.EnumFilter;
import com.github.rloic.benchmark.Experiment;
import com.github.rloic.benchmark.Implementation;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;

import java.io.FileWriter;

public class GlobalXORImpl extends Implementation {
   public GlobalXORImpl(Experiment experiment, FileWriter logWriter, FileWriter resultWriter) {
      super(experiment, logWriter, resultWriter);
   }

   @Override
   public Void call() throws Exception {
      logStart();
      AESGlobal gXor = new AESGlobal(experiment.round, experiment.objStep1, experiment.key);

      Solver solver = gXor.m.getSolver();
      EnumFilter enumFilter = new EnumFilter(gXor.m, gXor.sBoxes, experiment.objStep1);
      solver.plugMonitor(enumFilter);
      solver.setSearch(
            Search.intVarSearch(gXor.sBoxes),
            Search.intVarSearch(gXor.varsToAssign)/*
            new WDeg(gXor.sBoxes, 0L, IntVar::getLB, gXor.constraintsOf),
            new WDeg(gXor.varsToAssign, 0L, IntVar::getLB, gXor.constraintsOf)*/
      );
      solver.setSearch(
            Search.lastConflict(solver.getSearch())
      );
      return run( solver, gXor.sBoxes);
   }
}
