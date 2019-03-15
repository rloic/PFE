package com.github.rloic.benchmark;

import com.github.rloic.util.Logger;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract class Implementation implements Callable<Void> {

   protected final Experiment experiment;
   private final FileWriter logWriter;
   private final FileWriter resultWriter;

   protected Implementation(Experiment experiment, FileWriter logWriter, FileWriter resultWriter) {
      this.experiment = experiment;
      this.logWriter = logWriter;
      this.resultWriter = resultWriter;
   }

   protected void logStart(
   ) throws IOException {
      Logger.info("Starting experiment: " + experiment + " for implementation " + getClass().getSimpleName() + " at " + LocalDateTime.now());
      logWriter.write("");
      resultWriter.write("");
   }

   protected Void run(
         Solver solver,
         BoolVar[] sBoxes
   ) throws IOException {
      while (solver.solve()) {
         printSBoxes(sBoxes, resultWriter);
         logWriter.append(solver.ref().getMeasures().toOneLineString())
               .append("\n");
      }
      logWriter.append("Ending properly\n");
      logWriter.append(solver.ref().getMeasures().toOneLineString()).append('\n');

      Logger.info("experiment: " + experiment + " ended for implementation " + getClass().getSimpleName() + " at " + LocalDateTime.now());
      return null;
   }

   private void printSBoxes(BoolVar[] sBoxes, FileWriter resultWriter) throws IOException {
      List<Integer> values = Arrays.stream(sBoxes)
            .map(IntVar::getValue)
            .collect(Collectors.toList());
      resultWriter.write(values + "\n");
      resultWriter.flush();
   }

}
