package com.github.rloic.benchmark;

import java.io.FileWriter;

class ExperimentData {
   final Experiment experiment;
   final FileWriter logWriter;
   final FileWriter resultWriter;

   ExperimentData(Experiment experiment, FileWriter logWriter, FileWriter resultWriter) {
      this.experiment = experiment;
      this.logWriter = logWriter;
      this.resultWriter = resultWriter;
   }
}
