package com.github.rloic;

import com.github.rloic.aes.EnumFilter;
import com.github.rloic.aes.GlobalXOR;
import com.github.rloic.aes.KeyBits;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;
import static com.github.rloic.util.Logger.InfoLogger.INFO;

public class TestGConstraint {

   public static void main(String[] args) throws InterruptedException {
      Logger.level(INFO);

      ExecutorService executor = Executors.newFixedThreadPool(4);
      List<Exp> exps = Arrays.asList(
            new Exp(3, 5, AES_128, 2, TimeUnit.HOURS),
           // new Exp(4, 12, AES_128, 2, TimeUnit.HOURS),

            new Exp(3, 1, AES_192, 2, TimeUnit.HOURS),
      /*      new Exp(4, 4, AES_192, 2, TimeUnit.HOURS),
            new Exp(5, 5, AES_192, 2, TimeUnit.HOURS),
            new Exp(6, 10, AES_192, 2, TimeUnit.HOURS),
            new Exp(7, 13, AES_192, 2, TimeUnit.HOURS),
            new Exp(8, 18, AES_192, 2, TimeUnit.HOURS),
            new Exp(9, 24, AES_192, 2, TimeUnit.HOURS),
            new Exp(10, 27, AES_192, 2, TimeUnit.HOURS),
*/
            new Exp(3, 1, AES_256, 2, TimeUnit.HOURS),
            new Exp(4, 3, AES_256, 2, TimeUnit.HOURS),
            new Exp(5, 3, AES_256, 2, TimeUnit.HOURS)
       /*     new Exp(6, 5, AES_256, 2, TimeUnit.HOURS),
            new Exp(7, 5, AES_256, 2, TimeUnit.HOURS),
            new Exp(8, 10, AES_256, 2, TimeUnit.HOURS),
            new Exp(9, 15, AES_256, 2, TimeUnit.HOURS),
            new Exp(10, 16, AES_256, 2, TimeUnit.HOURS),
            new Exp(11, 20, AES_256, 2, TimeUnit.HOURS),
            new Exp(12, 20, AES_256, 2, TimeUnit.HOURS),
            new Exp(13, 24, AES_256, 2, TimeUnit.HOURS),
            new Exp(14, 24, AES_256, 2, TimeUnit.HOURS),

            new Exp(5, 17, AES_128, 4, TimeUnit.DAYS)*/
      );

      for (final Exp exp : exps) {
         executor.submit(() -> {
            File logFile = new File("results/log__" + exp + ".text");
            File resultFile = new File("results/result__" + exp + ".txt");
            try (
                  FileWriter logWriter = new FileWriter(logFile);
                  FileWriter resultWriter = new FileWriter(resultFile)
            ) {
               ExecutorService monoExecutor = Executors.newSingleThreadExecutor();
               Future<Void> task = monoExecutor.submit(new ExpWorker(exp, logWriter, resultWriter));
               try {
                  task.get(exp.timeout, exp.unit);
               } catch (ExecutionException e) {
                  e.printStackTrace();
                  Logger.err("ExecutionException on exp: " + exp);
               } catch (TimeoutException | InterruptedException e) {
                  Logger.info("Timeout reached for " + exp);
                  logWriter.write("Timeout after: " + exp.timeout + " " + exp.unit);
               } finally {
                  monoExecutor.shutdown();
               }
            } catch (IOException ioe) {
               Logger.err(ioe);
            }
         });
      }

      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.DAYS);
   }

   static class Exp {

      final int round;
      final int objStep1;
      final KeyBits key;
      final long timeout;
      final TimeUnit unit;

      Exp(int round, int objStep1, KeyBits key, long timeout, TimeUnit unit) {
         this.round = round;
         this.objStep1 = objStep1;
         this.key = key;
         this.timeout = timeout;
         this.unit = unit;
      }

      @Override
      public String toString() {
         String keyStr = "";
         if (key == AES_128) {
            keyStr = "AES-128";
         } else if (key == AES_192) {
            keyStr = "AES-192";
         } else if (key == AES_256) {
            keyStr = "AES-256";
         }
         return keyStr + "-" + round + "_" + objStep1;
      }
   }

   static class ExpWorker implements Callable<Void> {
      private final Exp exp;
      private final FileWriter logWriter;
      private final FileWriter resultWriter;

      ExpWorker(Exp exp, FileWriter logWriter, FileWriter resultWriter) {
         super();
         this.exp = exp;
         this.logWriter = logWriter;
         this.resultWriter = resultWriter;
      }

      @Override
      public Void call() throws Exception {
         Logger.info("Starting exp: " + exp);
         logWriter.write("");
         resultWriter.write("");
         GlobalXOR gXor = new GlobalXOR(exp.round, exp.objStep1, exp.key);

         Solver solver = gXor.m.getSolver();
         EnumFilter enumFilter = new EnumFilter(gXor.m, gXor.sBoxes, exp.objStep1);
         solver.plugMonitor(enumFilter);
         solver.setSearch(
               Search.intVarSearch(gXor.sBoxes),
               Search.intVarSearch(gXor.assignedVar)
         );
         while (solver.solve()) {
            printSBoxes(gXor.sBoxes);
            logWriter.append(solver.ref().getMeasures().toOneLineString())
                  .append("\n");
         }
         logWriter.append("Ending properly\n");
         logWriter.append(solver.ref().getMeasures().toOneLineString()).append('\n');

         Logger.info("exp: " + exp + " ended");
         return null;
      }

      void printSBoxes(BoolVar[] sBoxes) throws IOException {
         List<Integer> values = Arrays.stream(sBoxes)
               .map(IntVar::getValue)
               .collect(Collectors.toList());
         resultWriter.write(values + "\n");
         resultWriter.flush();
      }

   }


}
