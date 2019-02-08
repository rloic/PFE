@file:Suppress("NonAsciiCharacters")

package com.github.rloic.kt.aes128

import org.chocosolver.solver.Model
import org.chocosolver.solver.constraints.Constraint
import org.chocosolver.solver.variables.BoolVar



fun main() {
   val model = AdvancedAESModel(3, 2)
   val solver = model.solver
   while (solver.solve()) {
   }
   solver.printShortStatistics()
}

class AdvancedAESModel(val rounds: Int, objStep1: Int) : Model() {

   val FALSE = boolVar(false)

   init {
      val ΔX = Array(rounds) { boolVarMatrix(4, 4) }
      val ΔK = Array(rounds) { boolVarMatrix(4, 4) }
      val ΔZ = Array(rounds) { boolVarMatrix(4, 4) }
      val sBoxes = linkSBoxes(ΔX, ΔK)

      c1Prim(sBoxes, objStep1)
      c2Prim() // Do nothing: implicit constraint
      c3Prim(ΔX, ΔK, ΔZ)
      val ΔY = c4Prim(ΔX)
      c5Prim(ΔY, ΔZ)
      c6Prim(ΔY, ΔZ)
      val DK = buildDK() // Implicit C7' for DK
      c8C9ForDK(DK, ΔK)
      val DY = buildDY() // Implicit C7' for DY
      val DZ = buildDZ() // Implicit C7' for DZ
      c8c9ForDYAndDZ(DY, ΔY, DZ, ΔZ)
   }

   fun c1Prim(sBoxes: Array<BoolVar>, objStep1: Int) {
      sum(sBoxes, "=", objStep1).post()
   }

   fun c2Prim() { /* Implicit constraint */
   }

   fun c3Prim(ΔX: Tensor3<BoolVar>, ΔK: Tensor3<BoolVar>, ΔZ: Tensor3<BoolVar>) {
      for (i in 0..rounds - 2) {
         for (j in 0..3) {
            for (k in 0..3) {
               xor(ΔZ[i][j][k], ΔK[i + 1][j][k], ΔX[i + 1][j][k]).post()
            }
         }
      }
   }

   fun c4Prim(ΔX: Tensor3<BoolVar>): Tensor3<BoolVar> {
      return Tensor(rounds, 4, 4) { i, j, k -> ΔX[i][j][(j + k) % 4] }
   }

   fun c5Prim(ΔY: Tensor3<BoolVar>, ΔZ: Tensor3<BoolVar>) {
      for (i in 0..rounds - 2) {
         for (k in 0..3) {
            sum(
               arrayOf(
                  ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                  ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
               ),
               "=", intVar(intArrayOf(0, 5, 6, 7, 8))
            ).post()
         }
      }
   }

   fun c6Prim(ΔY: Tensor3<BoolVar>, ΔZ: Tensor3<BoolVar>) {
      for (j in 0..3) {
         for (k in 0..3) {
            arithm(ΔZ[rounds - 1][j][k], "=", ΔY[rounds - 1][j][k]).post()
         }
      }
   }

   private fun linkSBoxes(ΔX: Tensor3<BoolVar>, ΔK: Tensor3<BoolVar>): Array<BoolVar> {
      // Link sBoxes with all variables that pass through an S-box (variables of ∆X, and variables in the last column of ∆K)
      // Link ∆SR with ∆X according to ShiftRows operation
      val sBoxes = ArrayList<BoolVar>(20)
      for (i in 0..rounds - 1) {
         for (j in 0..3) {
            for (k in 0..3) {
               sBoxes += ΔX[i][j][k]
               if (k == 3) {
                  sBoxes += ΔK[i][j][k]
               }
            }
         }
      }
      return sBoxes.toTypedArray()
   }

   fun xor(vararg boolVar: BoolVar): Constraint = sum(boolVar, "!=", 1)

   fun buildDK(): Tensor5<BoolVar> {
      val DK = Tensor<BoolVar>(4, rounds, 4, rounds, 4)
      for (j in 0..3) {
         for (i1 in 0..rounds - 1) {
            for (k1 in 0..3) {
               for (i2 in i1..rounds - 1) {
                  val k2Init = if (i1 != i2) 0 else k1 + 1
                  for (k2 in k2Init..3) {
                     val diff_δB1_δB2 = boolVar()
                     DK[j][i1][k1][i2][k2] = diff_δB1_δB2
                     DK[j][i2][k2][i1][k1] = diff_δB1_δB2
                  }
               }
               // diff(δb1, δb1) == 0
               DK[j][i1][k1][i1][k1] = FALSE
            }
         }
      }

      if (DK.any { variable: BoolVar? -> variable == null })
         throw NullPointerException()

      @Suppress("UNCHECKED_CAST") // Nullability was manually checked
      return DK as Tensor5<BoolVar>
   }

   fun buildDY(): Tensor5<BoolVar> {
      val DY = Tensor<BoolVar>(4, rounds - 1, 4, rounds - 1, 4)
      for (j in 0..3) {
         for (i1 in 0..rounds - 2) {
            for (k1 in 0..3) {
               for (i2 in i1..rounds - 2) {
                  val k2Init = if (i1 != i2) 0 else k1 + 1
                  for (k2 in k2Init..3) {
                     val diff_δB1_δB2 = boolVar()
                     DY[j][i1][k1][i2][k2] = diff_δB1_δB2
                     DY[j][i2][k2][i1][k1] = diff_δB1_δB2
                  }
               }
               // diff(δB1, δB1) == 0
               DY[j][i1][k1][i1][k1] = FALSE
            }
         }
      }

      if (DY.any { v: BoolVar? -> v == null })
         throw NullPointerException()

      @Suppress("UNCHECKED_CAST") // Nullability was manually checked
      return DY as Tensor5<BoolVar>
   }

   fun buildDZ() = buildDY()

   fun nbColumnsTroughSboxes(round: Int) {

   }


}