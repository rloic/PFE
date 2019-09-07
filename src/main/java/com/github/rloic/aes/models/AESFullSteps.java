package com.github.rloic.aes.models;

import com.github.rloic.aes.utils.KeyBits;
import com.github.rloic.aes.constrainttables.GaloisFieldMultiplication;
import com.github.rloic.aes.constrainttables.SBox;
import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.common.ExtendedModel.ByteVar;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

import static com.github.rloic.aes.utils.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.utils.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.utils.KeyBits.AES256.AES_256;
import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class AESFullSteps {

   private final static String STRATEGY = "FC";
   private final static int[] PROBABILITIES = new int[]{0, 6, 7};

   private final List<IntVar> _probabilities = new ArrayList<>();
   private final List<BoolVar> _sBoxes = new ArrayList<>();
   private final ExtendedModel em = new ExtendedModel("AESFullSteps");

   // Public for solver
   public final IntVar objective;
   public final IntVar[] nbActives;

   public final BoolVar[] sBoxes;
   public final BoolVar[] ΔX;
   public final BoolVar[] ΔSX;
   public final BoolVar[] ΔY;
   public final BoolVar[] ΔZ;
   public final BoolVar[] ΔK;

   private final BoolVar[][] ΔSK_i;

   public final Model m;
   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   public AESFullSteps(KeyBits version, int Nr, int sb) {
      // Parameters
      int Nk = version.keyColumns;

      // Encryption Process
      ByteVar[][] δPlainText = em.byteVarMatrix("PlainText", 4, 4);
      ByteVar[][][] δX = new ByteVar[Nr][][];
      ByteVar[][][] δSX = new ByteVar[Nr][][];
      ByteVar[][][] δY = new ByteVar[Nr][][];
      ByteVar[][][] δZ = new ByteVar[Nr - 1][][];

      // KeySchedule
      ByteVar[][] δCipherKey = em.byteVarMatrix("CipherKey", 4, Nk);
      ΔSK_i = new BoolVar[Nr][];
      ByteVar[][] δWK = keyExpansion(δCipherKey, Nk, Nr);


      // Initial round
      δX[0] = addRoundKey(δPlainText, subKey(δWK, 0));

      // Main rounds
      for (int i = 0; i < Nr - 1; i++) {
         δSX[i] = subBytes(δX[i]);
         δY[i] = shiftRows(δSX[i]);
         δZ[i] = mixColumn(δY[i]);
         δX[i + 1] = addRoundKey(δZ[i], subKey(δWK, i + 1));
      }

      // Last round
      δSX[Nr - 1] = subBytes(δX[Nr - 1]);
      δY[Nr - 1] = shiftRows(δSX[Nr - 1]);
      @SuppressWarnings("unused")
      ByteVar[][] δCipherText = addRoundKey(δY[Nr - 1], subKey(δWK, Nr));

      ΔX = abstraction(δX);
      ΔSX = abstraction(δSX);
      ΔY = abstraction(δY);
      ΔZ = abstraction(δZ);
      ΔK = abstraction(δWK);

      sBoxes = new BoolVar[_sBoxes.size()];
      _sBoxes.toArray(sBoxes);
      em.sum(sBoxes, "=", sb);

      IntVar[] probabilities = new IntVar[_probabilities.size()];
      _probabilities.toArray(probabilities);
      objective = em.intVar("objective", 6 * sb, 7 * sb);
      em.sum(probabilities, "=", objective);

      nbActives = new IntVar[Nr];
      for (int i = 0; i < Nr; i++) {
         nbActives[i] = em.intVar("nbActives[" + i + "]", 0, sb);
      }

      if (version == AES_128) {
         if (Nr >= 3) em.sum(take(nbActives, 3), ">=", 5);
         if (Nr >= 4) em.sum(take(nbActives, 4), ">=", 12);
      } else if (version == AES_192) {
         if (Nr >= 3) em.sum(take(nbActives, 3), ">=", 1);
         if (Nr >= 4) em.sum(take(nbActives, 4), ">=", 4);
         if (Nr >= 5) em.sum(take(nbActives, 5), ">=", 5);
         if (Nr >= 6) em.sum(take(nbActives, 6), ">=", 10);
         if (Nr >= 7) em.sum(take(nbActives, 7), ">=", 13);
         if (Nr >= 8) em.sum(take(nbActives, 8), ">=", 18);
         if (Nr >= 9) em.sum(take(nbActives, 9), ">=", 24);
      } else if (version == AES_256) {
         if (Nr >= 3) em.sum(take(nbActives, 3), ">=", 1);
         if (Nr >= 4) em.sum(take(nbActives, 4), ">=", 3);
         if (Nr >= 5) em.sum(take(nbActives, 5), ">=", 3);
         if (Nr >= 6) em.sum(take(nbActives, 6), ">=", 5);
         if (Nr >= 7) em.sum(take(nbActives, 7), ">=", 5);
         if (Nr >= 8) em.sum(take(nbActives, 8), ">=", 10);
         if (Nr >= 8) em.sum(take(nbActives, 8), ">=", 15);
         if (Nr >= 10) em.sum(take(nbActives, 10), ">=", 16);
         if (Nr >= 11) em.sum(take(nbActives, 11), ">=", 20);
         if (Nr >= 12) em.sum(take(nbActives, 12), ">=", 20);
         if (Nr >= 13) em.sum(take(nbActives, 13), ">=", 24);
         if (Nr >= 14) em.sum(take(nbActives, 14), ">=", 24);
      }

     // em.sum(concat(abstraction(δPlainText), abstraction(δCipherKey)), ">=", 1);

      for (int i = 0; i < Nr; i++) {
         int cpt = 0;

         IntVar[] currentRound = version.isSBRound(i)? new IntVar[16 + 4] : new IntVar[16];
         if(version.isSBRound(i)) {
            ByteVar[][] subKey_i = subKey(δWK , i);
            int subByteColumnAtRound_i = version.getSubByteColumnInRoundKey(i);
            for (int j = 0; j < 4; j++) {
               currentRound[cpt++] = subKey_i[j][subByteColumnAtRound_i].abstraction;
            }
         }
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               currentRound[cpt++] = δX[i][j][k].abstraction;
            }

         }
         em.sum(currentRound, "=", nbActives[i]);
      }
      em.sum(nbActives, "=", sb);

      DeconstructedModel dm = em.build(new FullInferenceEngine(), new FullRulesApplier());
      m = dm.model;
      constraintsOf = dm.constraintsOf;
   }

   // KeySchedule
   private ByteVar[][] keyExpansion(ByteVar[][] δCipherKey, int Nk, int Nr) {
      ByteVar[][] δWK = new ByteVar[4][(Nr + 1) * 4];
      for (int k = 0; k < Nk; k++) {
         // WK[*][k] = δCipherKey[*][k]
         setColumn(δWK, k, getColumn(δCipherKey, k));
      }
      for (int k = Nk; k < (Nr + 1) * 4; k++) {
         if (k % Nk == 0) {
            // WK[*][k] = WK[*][k-Nk] xor SubWord(RotWord(WK[*][k-1]))
            setColumn(δWK, k,
                  elementsWiseXor(getColumn(δWK, k - Nk), subWord(rotWord(getColumn(δWK, k - 1))))
            );
         } else if (Nk > 6 && k % Nk == 4) {
            // WK[*][k] = WK[*][k-Nk] xor SubWord(WK[*][k-1])
            setColumn(δWK, k,
                  elementsWiseXor(getColumn(δWK, k - Nk), subWord(getColumn(δWK, k - 1)))
            );
         } else {
            // WK[*][k] = WK[*][k-Nk] xor WK[*][k-1]
            setColumn(δWK, k,
                  elementsWiseXor(getColumn(δWK, k - Nk), getColumn(δWK, k - 1))
            );
         }
      }
      return δWK;
   }

   private ByteVar[][] subKey(ByteVar[][] δWK, int i) {
      return block((j, k) -> δWK[j][i * 4 + k]);
   }

   // KeySchedule sub-process
   private ByteVar[] rotWord(ByteVar[] column) {
      return column((j) -> column[(j + 1) % 4]);
   }

   private ByteVar[] subWord(ByteVar[] K_i) {
      return column((j) -> subBytes("SK_i", K_i[j]));
   }

   // Encryption process
   private ByteVar[][] addRoundKey(
         ByteVar[][] block,
         ByteVar[][] subKey_i
   ) {
      return block((j, k) -> em.xorVar(block[j][k], subKey_i[j][k]));
   }

   private ByteVar[][] subBytes(ByteVar[][] δX_i) {
      return block((j, k) -> subBytes("SX_i", δX_i[j][k]));
   }

   private ByteVar[][] shiftRows(ByteVar[][] δSX_i) {
      return block((j, k) -> δSX_i[j][(j + k) % 4]);
   }

   private ByteVar[][] mixColumn(ByteVar[][] _1Y_i) {
      ByteVar[][] _2Y_i = block((j, k) -> mul2(_1Y_i[j][k]));
      ByteVar[][] _3Y_i = block((j, k) -> em.xorVar(_1Y_i[j][k], _2Y_i[j][k]));

      ByteVar[][] δZ_i = new ByteVar[4][4];
      for (int k = 0; k < 4; k++) {
         δZ_i[0][k] = em.xorVar(_2Y_i[0][k], _3Y_i[1][k], _1Y_i[2][k], _1Y_i[3][k]);  // 2 3 1 1
         δZ_i[1][k] = em.xorVar(_1Y_i[0][k], _2Y_i[1][k], _3Y_i[2][k], _1Y_i[3][k]);  // 1 2 3 1
         δZ_i[2][k] = em.xorVar(_1Y_i[0][k], _1Y_i[1][k], _2Y_i[2][k], _3Y_i[3][k]);  // 1 1 2 3
         δZ_i[3][k] = em.xorVar(_3Y_i[0][k], _1Y_i[1][k], _1Y_i[2][k], _2Y_i[3][k]);  // 3 1 1 2
      }

      ByteVar[][] _09Z_i = block((j, k) -> mul9(δZ_i[j][k]));
      ByteVar[][] _11Z_i = block((j, k) -> mul11(δZ_i[j][k]));
      ByteVar[][] _13Z_i = block((j, k) -> mut13(δZ_i[j][k]));
      ByteVar[][] _14Z_i = block((j, k) -> mul14(δZ_i[j][k]));
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            // _rY_i = _1Y_i but using the reverse matrix (_rY_i is computed from Z_i)
            ByteVar[] _rY_i = new ByteVar[]{
                  em.xorVar(_14Z_i[0][k], _11Z_i[1][k], _13Z_i[2][k], _09Z_i[3][k]),    // 14 11 13  9
                  em.xorVar(_09Z_i[0][k], _14Z_i[1][k], _11Z_i[2][k], _13Z_i[3][k]),    //  9 14 11 13
                  em.xorVar(_13Z_i[0][k], _09Z_i[1][k], _14Z_i[2][k], _11Z_i[3][k]),    // 13  9 14 11
                  em.xorVar(_11Z_i[0][k], _13Z_i[1][k], _09Z_i[2][k], _14Z_i[3][k])     // 11 13  9 14
            };
            em.equals(_1Y_i[0][k], _rY_i[0]);
            em.equals(_1Y_i[1][k], _rY_i[1]);
            em.equals(_1Y_i[2][k], _rY_i[2]);
            em.equals(_1Y_i[3][k], _rY_i[3]);
         }
      }

      return δZ_i;
   }

   // KeySchedule utils
   private ByteVar[] elementsWiseXor(ByteVar[] vecA, ByteVar[] vecB) {
      return column((j) -> em.xorVar(vecA[j], vecB[j]));
   }

   private ByteVar[] getColumn(ByteVar[][] M, int k) {
      return column((j) -> M[j][k]);
   }

   private void setColumn(ByteVar[][] M, int k, ByteVar[] column) {
      for (int j = 0; j < M.length; j++) {
         M[j][k] = column[j];
      }
   }

   // Main utils
   private ByteVar subBytes(String name, ByteVar δ) {
      ByteVar SBδ = em.byteVar(name, 255, "SB(" + δ.name + ")");
      em.table(
            arrayOf(
                  δ.realization,
                  SBδ.realization,
                  newProbability()
            ),
            SBox.aes,
            STRATEGY
      );
      em.equals(δ.abstraction, SBδ.abstraction);
      _sBoxes.add(δ.abstraction);
      return SBδ;
   }

   private ByteVar mul2(ByteVar δ) {
      ByteVar δx2 = em.byteVar("2 * " + δ.name);
      em.table(arrayOf(δ.realization, δx2.realization), GaloisFieldMultiplication.mul2, STRATEGY);
      em.equals(δ.abstraction, δx2.abstraction);
      return δx2;
   }

   private ByteVar mul9(ByteVar δ) {
      ByteVar δx9 = em.byteVar("9 * " + δ.name);
      em.table(arrayOf(δ.realization, δx9.realization), GaloisFieldMultiplication.mul9, STRATEGY);
      em.equals(δ.abstraction, δx9.abstraction);
      return δx9;
   }

   private ByteVar mul11(ByteVar δ) {
      ByteVar δx11 = em.byteVar("11 * " + δ.name);
      em.table(arrayOf(δ.realization, δx11.realization), GaloisFieldMultiplication.mul11, STRATEGY);
      em.equals(δ.abstraction, δx11.abstraction);
      return δx11;
   }

   private ByteVar mut13(ByteVar δ) {
      ByteVar δx13 = em.byteVar("13 * " + δ.name);
      em.table(arrayOf(δ.realization, δx13.realization), GaloisFieldMultiplication.mul13, STRATEGY);
      em.equals(δ.abstraction, δx13.abstraction);
      return δx13;
   }

   private ByteVar mul14(ByteVar δ) {
      ByteVar δx14 = em.byteVar("14 * " + δ.name);
      em.table(arrayOf(δ.realization, δx14.realization), GaloisFieldMultiplication.mul14, STRATEGY);
      em.equals(δ.abstraction, δx14.abstraction);
      return δx14;
   }

   private IntVar newProbability() {
      IntVar p = em.intVar("probability[" + _probabilities.size() + "]", PROBABILITIES);
      _probabilities.add(p);
      return p;
   }

   private interface MatrixInitializer<T> {
      T get(int row, int col);
   }

   private interface ColumnInitializer<T> {
      T get(int row);
   }

   private ByteVar[] column(ColumnInitializer<ByteVar> initializer) {
      ByteVar[] matrix = new ByteVar[4];
      for (int j = 0; j < 4; j++) {
         matrix[j] = initializer.get(j);
      }
      return matrix;
   }

   private ByteVar[][] block(MatrixInitializer<ByteVar> initializer) {
      ByteVar[][] matrix = new ByteVar[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            matrix[j][k] = initializer.get(j, k);
         }
      }
      return matrix;
   }

   private BoolVar[] abstraction(ByteVar[][][] tensor) {
      int length = 0;
      for (ByteVar[][] matrix : tensor) {
         for (ByteVar[] row : matrix) {
            length += row.length;
         }
      }
      BoolVar[] abstractions = new BoolVar[length];
      int cpt = 0;
      for (ByteVar[][] matrix : tensor) {
         for (ByteVar[] row : matrix) {
            for (ByteVar δ : row) {
               abstractions[cpt++] = δ.abstraction;
            }
         }
      }
      return abstractions;
   }

   private BoolVar[] abstraction(ByteVar[][] matrix) {
      int length = 0;
      for (ByteVar[] row : matrix) length += row.length;
      BoolVar[] result = new BoolVar[length];
      int cpt = 0;
      for (ByteVar[] row : matrix) {
         for (ByteVar δ : row) {
            result[cpt++] = δ.abstraction;
         }
      }
      return result;
   }

   private BoolVar[] concat(BoolVar[] a1, BoolVar[] a2) {
      BoolVar[] result = new BoolVar[a1.length + a2.length];
      System.arraycopy(a1, 0, result, 0, a1.length);
      System.arraycopy(a2, 0, result, a1.length, a2.length);
      return result;
   }

   private IntVar[] take(IntVar[] array, int n) {
      IntVar[] result = new IntVar[n];
      System.arraycopy(array, 0, result, 0, n);
      return result;
   }

}
