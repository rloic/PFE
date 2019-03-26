package com.github.rloic.benchmark;

import com.github.rloic.benchmark.impl.EnumXORGlobal;
import com.github.rloic.benchmark.impl.EnumXORImpl;
import com.github.rloic.benchmark.impl.GlobalXORImpl;
import com.github.rloic.util.Logger;
import com.github.rloic.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;
import static com.github.rloic.util.Logger.InfoLogger.INFO;

public class Benchmark {

   private static final int time = 10;
   private static final TimeUnit unit = TimeUnit.MINUTES;

   public static void main(String[] args) throws InterruptedException {
      Logger.level(INFO);
      ExecutorService executor = Executors.newFixedThreadPool(4);
      for (Pair<String, Function<ExperimentData, Implementation>> implementation : implementations()) {
         for (final Experiment experiment : experiments()) {
            executor.submit(() -> {
               File logFile = new File("results/log__" + implementation._0 + "__" + experiment + ".text");
               File resultFile = new File("results/result__" + implementation._0 + "__" + experiment + ".txt");
               try (
                     FileWriter logWriter = new FileWriter(logFile);
                     FileWriter resultWriter = new FileWriter(resultFile)
               ) {
                  ExecutorService monoExecutor = Executors.newSingleThreadExecutor();
                  Implementation impl = implementation._1.apply(new ExperimentData(experiment, logWriter, resultWriter));
                  Future<Void> task = monoExecutor.submit(impl);
                  try {
                     task.get(experiment.timeout, experiment.unit);
                  } catch (ExecutionException e) {
                     e.printStackTrace();
                     Logger.err("ExecutionException on experiment: " + experiment);
                  } catch (TimeoutException | InterruptedException e) {
                     task.cancel(true);
                     Logger.info("Timeout reached for " + experiment + " for implementation " + impl.getClass().getSimpleName() + " at " + LocalDateTime.now());
                     logWriter.write("Timeout after: " + experiment.timeout + " " + experiment.unit);
                  }
                  monoExecutor.shutdownNow();
               } catch (IOException ioe) {
                  Logger.err(ioe);
               }
            });
         }
      }

      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.DAYS);
   }

   private static List<Pair<String, Function<ExperimentData, Implementation>>> implementations() {
      return Arrays.asList(
            //new Pair<>("enum_xor", data -> new EnumXORImpl(data.experiment, data.logWriter, data.resultWriter)),
            //new Pair<>("enum_global_xor", data -> new EnumXORGlobal(data.experiment, data.logWriter, data.resultWriter)),
            new Pair<>("global_xor", data -> new GlobalXORImpl(data.experiment, data.logWriter, data.resultWriter))
      );
   }

   private static List<Experiment> experiments() {
      return Arrays.asList(
            new Experiment(3, 5, AES_128, time, unit),
//            new Experiment(4, 12, AES_128, time, unit),
//            new Experiment(5, 17, AES_128, time, unit),
            new Experiment(3, 1, AES_192, time, unit),
            new Experiment(4, 4, AES_192, time, unit),
            new Experiment(5, 5, AES_192, time, unit),
//            new Experiment(6, 10, AES_192, time, unit),
//            new Experiment(7, 13, AES_192, time, unit),
//            new Experiment(8, 18, AES_192, time, unit),
//            new Experiment(9, 24, AES_192, time, unit),
//            new Experiment(10, 27, AES_192, time, unit),
            new Experiment(3, 1, AES_256, time, unit),
            new Experiment(4, 3, AES_256, time, unit),
            new Experiment(5, 3, AES_256, time, unit),
            new Experiment(6, 5, AES_256, time, unit),
            new Experiment(7, 5, AES_256, time, unit)
//            new Experiment(8, 10, AES_256, time, unit)
//            new Experiment(9, 15, AES_256, time, unit),
//            new Experiment(10, 16, AES_256, time, unit),
//            new Experiment(11, 20, AES_256, time, unit),
//            new Experiment(12, 20, AES_256, time, unit),
//            new Experiment(13, 24, AES_256, time, unit),
//            new Experiment(14, 24, AES_256, time, unit)
      );
   }

}
