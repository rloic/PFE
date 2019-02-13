package com.github.rloic;

import com.github.rloic.aes.AESBlock;
import com.github.rloic.aes.AdvancedAESModel;
import com.github.rloic.aes.aes128.BasicAESModel;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;

import static com.github.rloic.Logger.DebugLogger.DEBUG;
import static com.github.rloic.Logger.InfoLogger.INFO;
import static com.github.rloic.Logger.TraceLogger.TRACE;
import static com.github.rloic.Logger.WarnLogger.WARN;
import static com.github.rloic.aes.AESBlock.AESBlock128.AES_BLOCK_128;
import static com.github.rloic.aes.AESBlock.AESBlock192.AES_BLOCK_192;
import static com.github.rloic.aes.AESBlock.AESBlock256.AES_BLOCK_256;

public class App {

    private static final int DEFAULT_ROUND = 3;
    private static final int DEFAULT_OBJ_STEP_1 = 3;
    private static final AESBlock DEFAULT_BLOCK = AES_BLOCK_128;

    private static boolean any(String[] strArray, String content) {
        for(String strA : strArray) {
            if (strA.equals(content)) return true;
        }
        return false;
    }

    public static void main(String[] args) {

        if (any(args, "--debug")) {
            Logger.level(DEBUG);
        } else if (any(args, "--trace")) {
            Logger.level(TRACE);
        } else {
            Logger.level(INFO);
        }

        int rounds = (args.length >= 2) ? Integer.valueOf(args[0]) : DEFAULT_ROUND;
        int objStep1 = (args.length >= 2) ? Integer.valueOf(args[1]) : DEFAULT_OBJ_STEP_1;
        AESBlock block = DEFAULT_BLOCK;
        if (args.length >= 3) {
            switch (args[2]) {
                case "AES-128":
                    // default do nothing
                    break;
                case "AES-192":
                    block = AES_BLOCK_192;
                    break;
                case "AES-256":
                    block = AES_BLOCK_256;
                    break;
                default:
                    throw new RuntimeException("Invalid AES version, expected [AES-128, AES-192, AES-256], given: " + args[2]);
            }
        }

        if (args.length < 2) {
            Logger.info("\n" +
                    "***********************************************************************************************************\n" +
                    "Usage: java -jar file.jar [rounds] [objStep] [aes-version]\n" +
                    "  rounds\t[default=" + DEFAULT_ROUND + "]\t\t| Valid values: [0..10] for AES-128, [0..12] for AES-192, [0..14] for AES-256\n" +
                    "  objStep\t[default=" + DEFAULT_OBJ_STEP_1 + "]\n" +
                    "  aes-version\t[default=" + DEFAULT_BLOCK + "]\t| Valid values: [AES-128, AES-192, AES-256]\n" +
                    "***********************************************************************************************************"
            );
        }

        Logger.info("Rounds: " + rounds);
        Logger.info("ObjStep1: " + objStep1);
        Logger.info("Block: " + block);

        AdvancedAESModel advancedModel = new AdvancedAESModel(rounds, objStep1, block);
        benchModel(advancedModel);

        BasicAESModel basicModel = new BasicAESModel(rounds, objStep1);
        benchModel(basicModel);
    }

    private static void benchModel(Model model) {
        Solver solver = model.getSolver();
        //noinspection StatementWithEmptyBody
        while (solver.solve()) {
        }
        solver.printShortStatistics();
    }


}
