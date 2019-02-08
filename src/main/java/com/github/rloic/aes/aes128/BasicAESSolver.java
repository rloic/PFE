package com.github.rloic.aes.aes128;

import java.io.PrintWriter;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;


public class BasicAESSolver {
   BasicAESSolver(PrintWriter file, int r, int objStep1) {
      // r = number of rounds
      // objStep1 = number of active Sboxes
      Model m = new Model();
      // Declare variables
      BoolVar[][][] DX = new BoolVar[r][][]; // forall i in [0,r-1], DX[i] = text at the beginning of round i (DX[r] = cipher text)
      BoolVar[][][] DZ = new BoolVar[r][][]; // forall i in [0,r-1], DZ[i] = text after MC at round i
      BoolVar[][][] DK = new BoolVar[r][][]; // forall i in [0,r-1], c7PrimC8PrimC9PrimForDK[i] = subkey at round i (c7PrimC8PrimC9PrimForDK[0] = initial key)
      for (int i = 0; i < r; i++) {
         DX[i] = m.boolVarMatrix(4, 4);
         DK[i] = m.boolVarMatrix(4, 4);
         DZ[i] = m.boolVarMatrix(4, 4);
      }
      // Redundant variables
      BoolVar[][][] DY = new BoolVar[r][4][4];// forall i in [0,r-1], DY[i] = text after SB and SR at round i
      BoolVar[] sBoxes = new BoolVar[20 * r]; // deepAll variables that pass through an S-box
      linkSBoxes(DX, DK, sBoxes, r);

      // Declare constraints: Number of active SBoxes (C1)
      m.sum(sBoxes, "=", objStep1).post();

      // Declare constraints: AddRoundKeys (C3)
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               XOR(new IntVar[]{DZ[i][j][k], DK[i + 1][j][k], DX[i + 1][j][k]}, m);
            }
         }
      }
      // ShiftRows (C4)
      for (int i = 0; i < r; i++)
         for (int j = 0; j < 4; j++)
            for (int k = 0; k < 4; k++)
               DY[i][j][k] = DX[i][j][(j + k) % 4];
      // Declare constraints: MixColumns (C5)
      for (int i = 0; i < r - 1; i++) {
         for (int k = 0; k < 4; k++) {
            m.sum(new IntVar[]{
                        DY[i][0][k], DY[i][1][k], DY[i][2][k], DY[i][3][k],
                        DZ[i][0][k], DZ[i][1][k], DZ[i][2][k], DZ[i][3][k]},
                  "=", m.intVar(new int[]{0, 5, 6, 7, 8})).post();
         }
      }
      // Last round (C6)
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            m.arithm(DZ[r - 1][j][k], "=", DY[r - 1][j][k]).post();
         }
      }
      // Declare constraints: KeySChedule
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            XOR(new IntVar[]{DK[i + 1][j][0], DK[i][j][0], DK[i][(j + 1) % 4][3]}, m); // (C7)
            for (int k = 1; k < 4; k++) {
               XOR(new IntVar[]{DK[i + 1][j][k], DK[i + 1][j][k - 1], DK[i][j][k]}, m); // C8)
            }
         }
      }

      // Solve
      Solver s = m.getSolver();
		/*
		s.setSearch(
        		Search.inputOrderLBSearch(sBoxes),
                Search.intVarSearch(m.retrieveIntVars(true)) // puis sur toutes les variables entières (booléens inclus) du modèle
                );
		s.setSearch(Search.lastConflict(s.getSearch()));*/
      while (s.solve()) { }
      s.printShortStatistics();
   }

   private void linkSBoxes(BoolVar[][][] DX, BoolVar[][][] DK, BoolVar[] sBoxes, int r) {
      // Link sBoxes with deepAll variables that pass through an Sbox (variables of DX, and variables in the last column of c7PrimC8PrimC9PrimForDK)
      // Link DSR with DX according to ShiftRows operation
      int cpt = 0;
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               sBoxes[cpt++] = DX[i][j][k];
               if (k == 3) {
                  sBoxes[cpt++] = DK[i][j][k];
               }
            }
         }
      }
   }

   private void XOR(IntVar[] listOfVar, Model m) {
      m.sum(listOfVar, "!=", 1).post();
   }

   public void prettyOut(BoolVar[][][] DK, BoolVar[][][] DX, BoolVar[][][] DSR, int r) {
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) System.out.print("  ");
         System.out.print("  ");
         for (int k = 0; k < 4; k++) System.out.print(DK[0][k][j].getValue() + " ");
         System.out.print("  ");
         for (int k = 0; k < 4; k++) System.out.print(DX[0][k][j].getValue() + " ");
         System.out.print("  ");
         for (int k = 0; k < 4; k++) System.out.print(DSR[0][k][j].getValue() + " ");
         System.out.println();
      }
      System.out.println();
      for (int i = 1; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) System.out.print(DX[i][k][j].getValue() + " ");
            System.out.print("  ");
            for (int k = 0; k < 4; k++) System.out.print(DSR[i][k][j].getValue() + " ");
            System.out.println();
         }
         System.out.println();
      }
   }
}