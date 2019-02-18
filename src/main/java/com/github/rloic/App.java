package com.github.rloic;

import com.github.rloic.aes.*;
import org.apache.commons.cli.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.rloic.Logger.DebugLogger.DEBUG;
import static com.github.rloic.Logger.ErrorLogger.ERROR;
import static com.github.rloic.Logger.InfoLogger.INFO;
import static com.github.rloic.Logger.TraceLogger.TRACE;
import static com.github.rloic.Logger.WarnLogger.WARN;
import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;

public class App {

    private static final int DEFAULT_ROUND = 3;
    private static final int DEFAULT_OBJ_STEP_1 = 5;
    private static final KeyBits DEFAULT_VERSION = AES_128;

    public static void main(String[] args) {
        Logger.level(DEBUG);
        try {

            Options options = new Options();
            Option solve = Option.builder()
                    .longOpt("solve")
                    .argName("ROUNDS> <NB ACTIVE S-BOXES> <AES-VERSION")
                    .desc("Solve the problem with given arguments")
                    .numberOfArgs(3)
                    .valueSeparator(' ')
                    .build();

            options.addOption("h", "help", false, "print the help");
            options.addOption("l", "logger", true, "set the logger level");
            options.addOption(solve);
            HelpFormatter formatter = new HelpFormatter();
            try {
                CommandLineParser parser = new DefaultParser();
                CommandLine commandLine = parser.parse(options, args);

                if (commandLine.hasOption("logger")) {
                    String loggerLevel = commandLine.getOptionValue("logger");
                    if (loggerLevel.equalsIgnoreCase("error")) {
                        Logger.level(ERROR);
                    } else if (loggerLevel.equalsIgnoreCase("warn")) {
                        Logger.level(WARN);
                    } else if (loggerLevel.equalsIgnoreCase("info")) {
                        Logger.level(INFO);
                    } else if (loggerLevel.equalsIgnoreCase("debug")) {
                        Logger.level(DEBUG);
                    } else if (loggerLevel.equalsIgnoreCase("trace")) {
                        Logger.level(TRACE);
                    } else {
                        Logger.level(ERROR);
                        throw new IllegalArgumentException("Invalid logger level, accepted: [ERROR, WARN, INFO, DEBUG, TRACE], given: '" + loggerLevel + "'");
                    }
                }

                if (commandLine.hasOption("h")) {
                    formatter.printHelp(" ", options);
                } else {
                    if (commandLine.hasOption("solve")) {
                        String[] arguments = commandLine.getOptionValues("solve");

                        int rounds = Integer.valueOf(arguments[0]);
                        int objStep1 = Integer.valueOf(arguments[1]);
                        String aesVersion = arguments[2];

                        KeyBits version;
                        switch (aesVersion) {
                            case "AES-128":
                                version = AES_128;
                                break;
                            case "AES-192":
                                version = AES_192;
                                break;
                            case "AES-256":
                                version = AES_256;
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid parameter AES-Version, accepted: [AES-128, AES-192, AES-256], given: '" + aesVersion + "'");
                        }

                        AdvancedModelPaper model = new AdvancedModelPaper(rounds, objStep1, version);
                        benchModel(model.m, model.sBoxes, objStep1);
                    } else {
                        formatter.printHelp(" ", options);
                    }
                }
            } catch (ParseException e) {
                formatter.printHelp(" ", options);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.err(e);
        }
    }

    private static void benchModel(Model model, BoolVar[] sBoxes, int objStep1) {
        long start = System.currentTimeMillis();
        Solver solver = model.getSolver();
        EnumFilter enumFilter = new EnumFilter(model, sBoxes, objStep1);
        solver.plugMonitor(enumFilter);
        try {
            //noinspection StatementWithEmptyBody
            while (solver.solve()) {
                //solver.printShortStatistics();
                printSBoxes(sBoxes);
            }
        } catch (SolverException s) {
            Logger.warn(s);
        }
        long end = System.currentTimeMillis();
        Logger.info("CPU Time: " + (end - start) + " ms");
        solver.printShortStatistics();
    }


    private static void printSBoxes(BoolVar[] sBoxes) {
        List<Integer> values = Arrays.stream(sBoxes)
                .map(IntVar::getValue)
                .collect(Collectors.toList());
        Logger.info("Solution: " + values);
    }

}
