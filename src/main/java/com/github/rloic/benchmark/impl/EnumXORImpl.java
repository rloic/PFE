package com.github.rloic.benchmark.impl;

import com.github.rloic.aes.AdvancedModelPaper;
import com.github.rloic.filter.EnumFilter;
import com.github.rloic.benchmark.Experiment;
import com.github.rloic.benchmark.Implementation;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;

import java.io.FileWriter;

public class EnumXORImpl extends Implementation {
   public EnumXORImpl(Experiment experiment, FileWriter logWriter, FileWriter resultWriter) {
      super(experiment, logWriter, resultWriter);
   }

   @Override
   public Void call() throws Exception {
      logStart();
      AdvancedModelPaper gXor = new AdvancedModelPaper(experiment.round, experiment.objStep1, experiment.key);

      Solver solver = gXor.m.getSolver();
      EnumFilter enumFilter = new EnumFilter(gXor.m, gXor.sBoxes, experiment.objStep1);
      solver.plugMonitor(enumFilter);
      solver.setSearch(
            Search.intVarSearch(gXor.sBoxes),
            Search.intVarSearch(gXor.m.retrieveBoolVars()),
            Search.intVarSearch(gXor.m.retrieveIntVars(false))
      );
      return run(solver, gXor.sBoxes);
   }
}
