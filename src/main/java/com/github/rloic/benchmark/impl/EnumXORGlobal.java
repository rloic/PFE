package com.github.rloic.benchmark.impl;

import com.github.rloic.filter.EnumFilter;
import com.github.rloic.benchmark.Experiment;
import com.github.rloic.benchmark.Implementation;
import com.github.rloic.wip.AdvancedModelPaperWithGlobalXOR;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;

import java.io.FileWriter;

public class EnumXORGlobal extends Implementation {
   public EnumXORGlobal(Experiment experiment, FileWriter logWriter, FileWriter resultWriter) {
      super(experiment, logWriter, resultWriter);
   }

   @Override
   public Void call() throws Exception {
      logStart();
      AdvancedModelPaperWithGlobalXOR gXor = new AdvancedModelPaperWithGlobalXOR(experiment.round, experiment.objStep1, experiment.key);

      Solver solver = gXor.m.getSolver();
      EnumFilter enumFilter = new EnumFilter(gXor.m, gXor.sBoxes, experiment.objStep1);
      solver.plugMonitor(enumFilter);
      solver.setSearch(
            Search.intVarSearch(gXor.sBoxes)
      );
      return run(solver, gXor.sBoxes);
   }
}
