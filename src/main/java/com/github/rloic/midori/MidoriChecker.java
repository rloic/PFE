package com.github.rloic.midori;

import com.github.rloic.xorconstraint.ByteXORPropagator;
import com.github.rloic.xorconstraint.SBoxPropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MidoriChecker {

   public static void main(String[] args) throws IOException {
      List<String> lines = Files.readAllLines(Paths.get("diff_solutions.txt"));
      int nbValids = 0;
      long start = System.currentTimeMillis();
      for (int i = 0; i < lines.size(); i++) {
         if (check(i, lines.get(i))) {
            nbValids += 1;
         }
      }
      System.out.println("Concrete solutions: " + nbValids);
      long end = System.currentTimeMillis();
      System.out.println("Time: " + (end - start) + " ms");
   }

   static boolean check(int number, String line) {
      String[] parts = line.split(", ");
      boolean[] domains = new boolean[parts.length];
      for (int i = 0; i < parts.length; i++) {
         domains[i] = parts[i].equals("1");
      }
      return check(number, domains);
   }

   static boolean check(int number, boolean[] domains) {
      int cpt = 0;
      Model m = new Model();
      IntVar[][][] DX = new IntVar[3][4][4];
      IntVar[][][] DZ = new IntVar[2][4][4];
      IntVar[][] DK = new IntVar[4][4];
      IntVar[][][] DY = new IntVar[3][4][4];
      IntVar[] variables = new IntVar[domains.length];

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               if (domains[cpt]) {
                  DX[i][j][k] = m.intVar(1, 255);
               } else {
                  DX[i][j][k] = m.intVar(0);
               }
               variables[cpt] = DX[i][j][k];
               cpt++;
            }
         }
      }

      for (int i = 0; i < 2; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               if (domains[cpt]) {
                  DZ[i][j][k] = m.intVar(1, 255);
               } else {
                  DZ[i][j][k] = m.intVar(0);
               }
               variables[cpt] = DZ[i][j][k];
               cpt++;
            }
         }
      }

      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            if (domains[cpt]) {
               DK[j][k] = m.intVar(1, 255);
            } else {
               DK[j][k] = m.intVar(0);
            }
            variables[cpt] = DK[j][k];
            cpt++;
         }
      }

      for (int i = 0; i < 3 - 1; i++) {
         DY[i][0][0] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][0][0], DY[i][0][0])));
         DY[i][1][0] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][2][2], DY[i][1][0])));
         DY[i][2][0] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][1][1], DY[i][2][0])));
         DY[i][3][0] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][3][3], DY[i][3][0])));

         DY[i][0][1] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][2][3], DY[i][0][1])));
         DY[i][1][1] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][0][1], DY[i][1][1])));
         DY[i][2][1] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][3][2], DY[i][2][1])));
         DY[i][3][1] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][1][0], DY[i][3][1])));

         DY[i][0][2] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][1][2], DY[i][0][2])));
         DY[i][1][2] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][3][0], DY[i][1][2])));
         DY[i][2][2] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][0][3], DY[i][2][2])));
         DY[i][3][2] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][2][1], DY[i][3][2])));

         DY[i][0][3] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][3][1], DY[i][0][3])));
         DY[i][1][3] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][1][3], DY[i][1][3])));
         DY[i][2][3] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][2][0], DY[i][2][3])));
         DY[i][3][3] = m.intVar(0, 255);
         m.post(new Constraint("SBoxPropagator", new SBoxPropagator(DX[i][0][2], DY[i][3][3])));
      }

      for (int i = 0; i < 3 - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               xor(m, DZ[i][j][k], DK[j][k], DX[i + 1][j][k]);
            }
         }
      }

      for (int i = 0; i < 3 - 1; i++) {
         for (int k = 0; k < 4; k++) {
            xor(m, DY[i][1][k], DY[i][2][k], DY[i][3][k], DZ[i][0][k]);
            xor(m, DY[i][0][k], DY[i][2][k], DY[i][3][k], DZ[i][1][k]);
            xor(m, DY[i][0][k], DY[i][1][k], DY[i][3][k], DZ[i][2][k]);
            xor(m, DY[i][0][k], DY[i][1][k], DY[i][2][k], DZ[i][3][k]);
         }
      }

      Solver solver = m.getSolver();
      int solutions = 0;
      while (solver.solve()) {
         solutions += 1;
      }

      System.out.println(number + " solutions " + solutions);
      return solutions >= 1;
   }

   private static void xor(Model m, IntVar... variables) {
      m.post(new Constraint("XOR Byte", new ByteXORPropagator(variables)));
   }

   private static void println(IntVar... variables) {
      String output = Arrays.stream(variables).map(it -> {
         if (it.isInstantiated()) {
            return String.valueOf(it.getValue());
         } else {
            return "x";
         }
      }).collect(Collectors.joining());
      System.out.println(output);
   }

}
