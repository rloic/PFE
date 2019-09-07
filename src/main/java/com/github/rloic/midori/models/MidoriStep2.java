package com.github.rloic.midori.models;

import com.github.rloic.common.ExtendedModel;
import com.github.rloic.midori.constrainttables.SBox;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class MidoriStep2 {

   private final ExtendedModel em;
   private final int version;
   private final int MAX_VALUE;
   public final Model m;
   public final IntVar objective;

   private final int r;
   private final IntVar[][] δPlainText;
   private final IntVar[][] δWK;
   private final IntVar[][][] δX;
   private final IntVar[][][] δSX;
   private final IntVar[][][] δY;
   private final IntVar[][][] δZ;
   private final IntVar[][][] δK;
   private final IntVar[][] δCipherText;

   public MidoriStep2(
         int version,
         int r,
         int numberOfActiveSBoxes,
         BoolVar[][][] ΔX,
         BoolVar[][][] ΔY,
         BoolVar[][][] ΔZ,
         BoolVar[][][] ΔK
   ) {
      assert version == 64 || version == 128;
      this.em = new ExtendedModel("MidoriStep2");
      this.m = em.getModel();
      this.version = version;
      this.r = r;
      MAX_VALUE = (version == 64) ? 15 : 255;
      final int INITIAL_KEY_MATRICES = (version == 64) ? 2 : 1;

      δPlainText = new IntVar[4][4];
      δWK = new IntVar[4][4];
      δX = new IntVar[r][4][4];
      δSX = new IntVar[r][4][4];
      δY = new IntVar[r - 1][4][4];
      δZ = new IntVar[r - 1][4][4];
      δK = new IntVar[INITIAL_KEY_MATRICES][4][4];
      δCipherText = new IntVar[4][4];

      IntVar[][][] probabilities = new IntVar[r][4][4];
      IntVar[] flattenedProbabilities = new IntVar[r * 4 * 4];

      for (int i = 0; i < INITIAL_KEY_MATRICES; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               δK[i][j][k] = realisation(ΔK[i][j][k]);
            }
         }
      }

      int inc = 0;
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               δX[i][j][k] = realisation(ΔX[i][j][k]);

               δSX[i][j][k] = byteVar("δSX[" + i + "][" + j + "][" + k + "]");
               probabilities[i][j][k] = probability("probabilities[" + i + "][" + j + "][" + k + "]");
               flattenedProbabilities[inc++] = probabilities[i][j][k];

               δY[i][j][k] = realisation(ΔY[i][j][k]);
               δZ[i][j][k] = realisation(ΔZ[i][j][k]);
            }
         }
      }
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            δPlainText[j][k] = byteVar("δPlainText[" + j + "][" + k + "]");

            δX[r - 1][j][k] = realisation(ΔX[r - 1][j][k]);
            δSX[r - 1][j][k] = byteVar("δSX[" + (r - 1) + "][" + j + "][" + k + "]");
            probabilities[r - 1][j][k] = probability("probabilities[" + (r - 1) + "][" + j + "][" + k + "]");
            flattenedProbabilities[inc++] = probabilities[r - 1][j][k];

            if (version == 64) {
               δWK[j][k] = byteVar("δWK[" + j + "][" + k + "]");
               em.byteXor(δK[0][j][k], δK[1][j][k], δWK[j][k]);
            } else { // version == 128
               δWK[j][k] = δK[0][j][k];
            }

            δCipherText[j][k] = byteVar("δCipherText[" + j + "][" + k + "]");
         }
      }

      // δX[0] = δPlainText xor δWK
      ark(δX[0], δPlainText, δWK);
      for (int i = 0; i < r - 1; i++) {
         // δSX[i] = SBox(δX[i]) with a probability wrap p[i]
         sBox(δSX[i], δX[i], probabilities[i]);
         // δY[i] = shuffleCell(δSX[i])
         shuffleCell(δY[i], δSX[i]);
         // δZ[i] = mixColumn(δY[i])
         mixColumns(δZ[i], δY[i]);
         // δX[i + 1] = δZ[i] xor δK[i]
         if (version == 64) {
            ark(δX[i + 1], δZ[i], δK[i % 2]);
         } else { // version == 128
            ark(δX[i + 1], δZ[i], δK[0]);
         }
      }
      // δX[r-1] = SBoxPropagator(δSX[r-1]) with a probability wrap p[i]
      sBox(δX[r - 1], δSX[r - 1], probabilities[r - 1]);
      // δCipherText = δSX[r - 1] xor δWK
      ark(δCipherText, δSX[r - 1], δWK);

      objective = em.intVar(2 * numberOfActiveSBoxes, 6 * numberOfActiveSBoxes);
      em.sum(flattenedProbabilities, "=", objective);
   }

   // δSX_{i} = sBox(δX_{i})
   private void sBox(IntVar[][] δSX, IntVar[][] δX, IntVar[][] probabilities) {
      if (version == 64) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               em.table(arrayOf(δX[j][k], δSX[j][k], probabilities[j][k]), SBox.midori64, "FC");
            }
         }
      } else { // version == 128
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               em.table(arrayOf(δX[j][k], δSX[j][k], probabilities[j][k]), SBox.midori128[j], "FC");
            }
         }
      }
   }

   // δY_{i} = shuffleCell(δSX_{i})
   private void shuffleCell(IntVar[][] δY, IntVar[][] δSX) {
      em.arithm(δY[0][0], "=", δSX[0][0]);
      em.arithm(δY[1][0], "=", δSX[2][2]);
      em.arithm(δY[2][0], "=", δSX[1][1]);
      em.arithm(δY[3][0], "=", δSX[3][3]);

      em.arithm(δY[0][1], "=", δSX[2][3]);
      em.arithm(δY[1][1], "=", δSX[0][1]);
      em.arithm(δY[2][1], "=", δSX[3][2]);
      em.arithm(δY[3][1], "=", δSX[1][0]);

      em.arithm(δY[0][2], "=", δSX[1][2]);
      em.arithm(δY[1][2], "=", δSX[3][0]);
      em.arithm(δY[2][2], "=", δSX[0][3]);
      em.arithm(δY[3][2], "=", δSX[2][1]);

      em.arithm(δY[0][3], "=", δSX[3][1]);
      em.arithm(δY[1][3], "=", δSX[1][3]);
      em.arithm(δY[2][3], "=", δSX[2][0]);
      em.arithm(δY[3][3], "=", δSX[0][2]);
   }

   // δZ_{i} = mixColumns(δY_{i})
   private void mixColumns(IntVar[][] δZ, IntVar[][] δY) {
      for (int k = 0; k < 4; k++) {
         em.byteXor(δY[0][k], δY[1][k], δY[2][k], δZ[3][k]);
         em.byteXor(δY[1][k], δY[2][k], δY[3][k], δZ[0][k]);
         em.byteXor(δY[2][k], δY[3][k], δY[0][k], δZ[1][k]);
         em.byteXor(δY[3][k], δY[0][k], δY[1][k], δZ[2][k]);
      }
   }

   // δX_{i+1} = ark(δZ_{i}, δK_{i})
   private void ark(IntVar[][] δX1, IntVar[][] δZ, IntVar[][] δWK) {
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            em.byteXor(δX1[j][k], δZ[j][k], δWK[j][k]);
         }
      }
   }

   /*
      δi = realisation(Δi):
      Δi = 0 => δi = 0
      Δi = 1 => δi ∈ [1; 2^(b - 1)]
   */
   private IntVar realisation(BoolVar var) {
      IntVar result;
      if (var.getValue() == 1) {
         result = em.intVar("real(" + var.getName() + ")", 1, MAX_VALUE);
      } else {
         result = em.constant("real(" + var.getName() + ")", 0);
      }
      return result;
   }

   private IntVar byteVar(String name) {
      return em.intVar(name, 0, MAX_VALUE);
   }

   private IntVar probability(String name) {
      return em.intVar(name, 0, 6);
   }

   private void print(IntVar[][] vars, int j) {
      for (int k = 0; k < 4; k++) {
         System.out.print(String.format("% 3d", vars[j][k].getValue()));
         System.out.print(" ");
      }
      System.out.print("      ");
   }

   private void print(IntVar[][] vars, int j, String join) {
      for (int k = 0; k < 4; k++) {
         System.out.print(String.format("% 3d", vars[j][k].getValue()));
         System.out.print(" ");
      }
      if (j == 1) {
         System.out.print(join);
      } else {
         System.out.print("      ");

      }
   }

   public void print() {
      System.out.println("Probability: 2^-" + objective.getValue());

      for (int j = 0; j < 4; j++) {
         print(δWK, j);

         for (int i = 0; i < r - 1; i++) {
            for (int inc = 0; inc < 4; inc++) {
               for (int k = 0; k < 4; k++) {
                  System.out.print("    ");
               }
               System.out.print("      ");
            }
            for (int k = 0; k < 4; k++) {
               if (version == 64) {
                  System.out.print(String.format("% 3d", δK[i % 2][j][k].getValue()));
               } else { // version == 128
                  System.out.print(String.format("% 3d", δK[0][j][k].getValue()));
               }
               System.out.print(" ");
            }
            System.out.print("  ");
         }
         System.out.println();
      }

      System.out.println();

      for (int j = 0; j < 4; j++) {
         print(δPlainText, j, "-ARK->");

         for (int i = 0; i < r - 1; i++) {
            print(δX[i], j, "-SB-->");
            print(δSX[i], j, "-SC-->");
            print(δY[i], j, "-MC-->");
            print(δZ[i], j, "-ARK->");
            for (int k = 0; k < 4; k++) {
               System.out.print("    ");
            }
            System.out.print("  ");
         }

         print(δX[r-1], j, "-SB-->");
         print(δSX[r-1], j, "-ARK->");
         print(δCipherText, j);

         System.out.println();
      }

   }


}
