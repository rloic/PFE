package com.github.rloic.common;

import com.github.rloic.common.abstraction.MathSet;
import com.github.rloic.xorconstraint.ByteXORPropagator;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.util.Logger;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.AbstractXORPropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.util.*;

/**
 * Extended model designed for AbstractXOR problems with utility methods
 */
@SuppressWarnings("NonAsciiCharacters")
public class ExtendedModel {

   private static final int DEFAULT_UPPER_BOUND = 255;
   private int tmpCounter = 0;

   /* The true model */
   private Model delegate;

   /* Weighted constraint for custom WDeg */
   private final Int2ObjectMap<List<WeightedConstraint>> constraintsOf = new Int2ObjectArrayMap<>();

   private final List<BoolVar> globalXorVariables = new ArrayList<>();
   private final List<BoolVar[]> globalXorEquations = new ArrayList<>();

   public ExtendedModel(String name) {
      this.delegate = new Model(name);
   }

   public Model getModel() {
      return delegate;
   }

   // Variables
   public BoolVar boolVar(String name) {
      return declare(delegate.boolVar(name));
   }

   public BoolVar[] boolVarArray(String name, int len) {
      BoolVar[] vars = new BoolVar[len];
      for (int i = 0; i < len; i++) {
         vars[i] = boolVar(name + "[" + i + "]");
      }
      return vars;
   }

   public BoolVar[][] boolVarMatrix(String name, int dimA, int dimB) {
      BoolVar[][] vars = new BoolVar[dimA][dimB];
      for (int i = 0; i < dimA; i++) {
         vars[i] = boolVarArray(name + "[" + i + "]", dimB);
      }
      return vars;
   }

   public BoolVar[][][] boolVarTensor3(String name, int dimA, int dimB, int dimC) {
      BoolVar[][][] vars = new BoolVar[dimA][dimB][dimC];
      for (int i = 0; i < dimA; i++) {
         vars[i] = boolVarMatrix(name + "[" + i + "]", dimB, dimC);
      }
      return vars;
   }

   public IntVar intVar(int[] domain) {
      return declare(delegate.intVar(domain));
   }

   public IntVar intVar(String name, int[] domain) {
      return declare(delegate.intVar(name, domain));
   }

   public IntVar intVar(int lb, int ub) {
      return declare(delegate.intVar(lb, ub));
   }

   public IntVar intVar(String name, int lb, int ub) {
      return declare(delegate.intVar(name, lb, ub));
   }

   public IntVar constant(String name, int value) {
      return declare(delegate.intVar(name, value));
   }

   public ByteVar byteVar(String name) {
      return new ByteVar(name, DEFAULT_UPPER_BOUND);
   }

   public ByteVar byteVar(String name, int max) {
      return new ByteVar(name, max);
   }

   public ByteVar byteVar(String name, int max, String eq) {
      return new ByteVar(name, max, eq);
   }

   public ByteVar[] byteVarArray(String name, int dimA) {
      return byteVarArray(name, dimA, DEFAULT_UPPER_BOUND);
   }

   public ByteVar[] byteVarArray(String name, int dimA, int max) {
      ByteVar[] bytes = new ByteVar[dimA];
      for (int i = 0; i < dimA; i++) {
         bytes[i] = new ByteVar(name + "[" + i + "]", max);
      }
      return bytes;
   }

   public ByteVar[][] byteVarMatrix(String name, int dimA, int dimB) {
      return byteVarMatrix(name, dimA, dimB, DEFAULT_UPPER_BOUND);
   }

   public ByteVar[][] byteVarMatrix(String name, int dimA, int dimB, int max) {
      ByteVar[][] bytes = new ByteVar[dimA][];
      for (int i = 0; i < dimA; i++) {
         bytes[i] = byteVarArray(name + "[" + i + "]", dimB, max);
      }
      return bytes;
   }

   public ByteVar[][][] byteVarTensor3(String name, int dimA, int dimB, int dimC) {
      return byteVarTensor3(name, dimA, dimB, dimC, DEFAULT_UPPER_BOUND);
   }

   public ByteVar[][][] byteVarTensor3(String name, int dimA, int dimB, int dimC, int max) {
      ByteVar[][][] bytes = new ByteVar[dimA][][];
      for (int i = 0; i < dimA; i++) {
         bytes[i] = byteVarMatrix(name, dimB, dimC, max);
      }
      return bytes;
   }

   public ByteVar xorVar(ByteVar δA, ByteVar δB) {
      return xorVar(δA, δB, DEFAULT_UPPER_BOUND);
   }

   public ByteVar xorVar(ByteVar δA, ByteVar δB, int upperBound) {
      ByteVar xorVar = byteVar(
            "tmp_xor2_" + tmpCounter++,
            upperBound,
            "(" + δA.name + " xor " + δB.name + ")"
      );
      xor(xorVar, δA, δB);
      return xorVar;
   }

   public ByteVar xorVar(ByteVar δA, ByteVar δB, ByteVar δC, ByteVar δD) {
      return xorVar(δA, δB, δC, δD, DEFAULT_UPPER_BOUND);
   }

   public ByteVar xorVar(ByteVar δA, ByteVar δB, ByteVar δC, ByteVar δD, int upperBound) {
      ByteVar diffAB = xorVar(δA, δB, upperBound);
      ByteVar diffCD = xorVar(δC, δD, upperBound);
      ByteVar result = byteVar(
            "tmp_xor4_" + tmpCounter++,
            upperBound,
            "(" + δA.name + " xor " + δB.name + " xor " + δC.name + " xor " + δD.name + ")"
      );
      xor(result, diffAB, diffCD);
      return result;
   }

   // Constraints
   public void equals(BoolVar lhs, BoolVar rhs) {
      BoolVar[] vars = new BoolVar[]{lhs, rhs};
      WeightedConstraint equalityConstraint = new WeightedConstraint<>(vars, this::oneDifferent);
      for (BoolVar var : vars) {
         constraintsOf.get(var.getId()).add(equalityConstraint);
      }
      delegate.arithm(lhs, "=", rhs).post();
   }

   public void equals(BoolVar... vars) {
      for (int i = 0; i < vars.length; i++) {
         for (int j = i + 1; j < vars.length; j++) {
            WeightedConstraint equalityConstraint = new WeightedConstraint<>(new BoolVar[]{vars[i], vars[j]}, this::oneDifferent);
            constraintsOf.get(vars[i].getId()).add(equalityConstraint);
            constraintsOf.get(vars[j].getId()).add(equalityConstraint);
            delegate.arithm(vars[i], "=", vars[j]).post();
         }
      }
   }

   public void equals(ByteVar lhs, ByteVar rhs) {
      arithm(lhs.realization, "=", rhs.realization);
      arithm(lhs.abstraction, "=", rhs.abstraction);
   }

   public void sum(BoolVar[] sum, String operator, int value) {
      Constraint cSum = delegate.sum(sum, operator, value);
      post(cSum, sum);
   }

   public void sum(IntVar[] sum, String operator, IntVar value) {
      Constraint cSum = delegate.sum(sum, operator, value);
      post(cSum, sum, value);
   }

   public void sum(IntVar[] sum, String operator, int value) {
      Constraint cSum = delegate.sum(sum, operator, value);
      post(cSum, sum);
   }

   public void sum(BoolVar[] sum, String operator, IntVar value) {
      Constraint cSum = delegate.sum(sum, operator, value);
      post(cSum, sum, value);
   }

   public void arithm(IntVar varA, String operator, IntVar varB) {
      post(delegate.arithm(varA, operator, varB), new Variable[]{varA, varB});
   }
   public void arithm(IntVar varA, String operator, int varB) {
      post(delegate.arithm(varA, operator, varB), new Variable[]{varA});
   }

   public void arithm(BoolVar varA, String operator, BoolVar varB) {
      post(delegate.arithm(varA, operator, varB), new Variable[]{varA, varB});
   }

   public void table(IntVar[] vars, Tuples tuples, String strategy) {
      post(delegate.table(vars, tuples, strategy), vars);
   }

   public void table(IntVar[] vars, Tuples tuples) {
      post(delegate.table(vars, tuples), vars);
      // delegate.table(vars, tuples).post();
   }

   public void byteXor(IntVar δA, IntVar δB, IntVar δC) {
      IntVar[] variables = new IntVar[]{δA, δB, δC};
      WeightedConstraint equalityConstraint = new WeightedConstraint<>(variables, this::notXor);
      for (IntVar var : variables) {
         constraintsOf.get(var.getId()).add(equalityConstraint);
      }
      delegate.post(new Constraint("byte xor", new ByteXORPropagator(δA, δB, δC)));
      delegate.table(new IntVar[]{δA, δB, δC}, xor3_8bits, "FC").post();
   }

   public void xor(ByteVar δA, ByteVar δB, ByteVar δC) {
      byteXor(δA.realization, δB.realization, δC.realization);
      abstractXor(δA.abstraction, δB.abstraction, δC.abstraction);
   }

   public void xor(ByteVar δA, ByteVar δB, ByteVar δC, ByteVar δD) {
      byteXor(δA.realization, δB.realization, δC.realization, δD.realization);
      abstractXor(δA.abstraction, δB.abstraction, δC.abstraction, δD.abstraction);
   }

   public void byteXor(IntVar δA, IntVar δB, IntVar δC, IntVar δD) {
      // delegate.post(new Constraint("byte xor", new ByteXORPropagator(δA, δB, δC, δD)));
      //delegate.table(new IntVar[]{δA, δB, δC, δD}, xor4_8bits, "FC");
      IntVar diffAB_CD = intVar("diff_AB_CD", 0, DEFAULT_UPPER_BOUND);
      IntVar diffAC_BD = intVar("diff_AC_BD", 0, DEFAULT_UPPER_BOUND);
      IntVar diffAD_BC = intVar("diff_AD_BC", 0, DEFAULT_UPPER_BOUND);
      byteXor(diffAB_CD, δA, δB);
      byteXor(diffAB_CD, δC, δD);
      byteXor(diffAC_BD, δA, δC);
      byteXor(diffAC_BD, δB, δD);
      byteXor(diffAD_BC, δA, δD);
      byteXor(diffAD_BC, δB, δC);
   }

   public void abstractXor(BoolVar... equation) {
      WeightedConstraint xorConstraint = new WeightedConstraint<>(equation, this::sumEqualsOne);
      for (BoolVar var : equation) {
         constraintsOf.get(var.getId()).add(xorConstraint);

         if (!globalXorVariables.contains(var)) {
            globalXorVariables.add(var);
         }
      }
      globalXorEquations.add(equation);
   }

   // Generate custom WDeg utilities
   public DeconstructedModel build(InferenceEngine inferenceEngine, RulesApplier rulesApplier) {
      if (globalXorVariables.size() != 0) {
         BoolVar[] vars = new BoolVar[globalXorVariables.size()];
         globalXorVariables.toArray(vars);

         BoolVar[][] xors = new BoolVar[globalXorEquations.size()][];
         globalXorEquations.toArray(xors);

         AbstractXORPropagator propagator = new AbstractXORPropagator(
               vars,
               xors,
               inferenceEngine,
               rulesApplier,
               delegate.getSolver()
         );
         delegate.post(new Constraint("Global XOR", propagator));

         return new DeconstructedModel(
               delegate,
               propagator,
               constraintsOf
         );
      } else {
         return new DeconstructedModel(
               delegate,
               null,
               constraintsOf
         );
      }
   }

   public DeconstructedModel buildWithWeightedConstraintsGeneration(
         InferenceEngine inferenceEngine,
         RulesApplier rulesApplier
   ) {
      DeconstructedModel model = build(inferenceEngine, rulesApplier);

      MathSet<Set<BoolVar>> initialEquations = new MathSet<>();
      globalXorEquations.forEach(it ->
            initialEquations.add(setOf(it))
      );

      MathSet<Set<BoolVar>> generatedEquations = combineXor(initialEquations, initialEquations);
      for (Set<BoolVar> equation : generatedEquations) {
         WeightedConstraint xorConstraint = new WeightedConstraint<>(arrayOf(equation), this::sumEqualsOne);

         for (BoolVar var : equation) {
            constraintsOf.get(var.getId()).add(xorConstraint);
         }
      }
      return model;
   }

   // Constraints utils
   private WeightedConstraint<Variable> post(Constraint constraint, Variable[] variables) {
      constraint.post();
      WeightedConstraint<Variable> wConstraint = WeightedConstraint.wrap(constraint);
      for (Variable v : variables) {
         constraintsOf.get(v.getId()).add(wConstraint);
      }
      return wConstraint;
   }

   private void post(Constraint constraint, Variable[] variables, Variable... other) {
      WeightedConstraint<Variable> wConstraint = post(constraint, variables);
      for (Variable v : other) {
         if (constraintsOf.containsKey(v.getId())) {
            constraintsOf.get(v.getId()).add(wConstraint);
         }
      }
   }

   private MathSet<Set<BoolVar>> combineXor(MathSet<Set<BoolVar>> lhs, MathSet<Set<BoolVar>> rhs) {
      if (lhs.isEmpty()) return new MathSet<>();
      MathSet<Set<BoolVar>> newEquationsSet = new MathSet<>();
      for (Set<BoolVar> equation1 : lhs) {
         for (Set<BoolVar> equation2 : rhs) {
            if (!equation1.equals(equation2)) {
               Set<BoolVar> mergedEquation = merge(equation1, equation2);
               if (mergedEquation.size() < Math.min(equation1.size() + equation2.size(), 5) && !rhs.contains(mergedEquation)) {
                  newEquationsSet.add(mergedEquation);
               }
            }
         }
      }
      Logger.debug("    [CombinedXOR] Number wrap new XOR = " + newEquationsSet.size());
      return newEquationsSet.union(combineXor(newEquationsSet, newEquationsSet.union(rhs)));
   }

   private Set<BoolVar> merge(Set<BoolVar> lhs, Set<BoolVar> rhs) {
      Set<BoolVar> result = new HashSet<>();
      for (BoolVar var : lhs) {
         if (!rhs.contains(var)) {
            result.add(var);
         }
      }

      for (BoolVar var : rhs) {
         if (!lhs.contains(var)) {
            result.add(var);
         }
      }
      return result;
   }

   private BoolVar[] arrayOf(Collection<BoolVar> vars) {
      BoolVar[] result = new BoolVar[vars.size()];
      vars.toArray(result);
      return result;
   }

   private Set<BoolVar> setOf(BoolVar[] vars) {
      return new HashSet<>(Arrays.asList(vars));
   }

   private boolean sumEqualsOne(BoolVar[] vars) {
      int sum = 0;
      for (BoolVar var : vars) {
         sum += var.getValue();
         if (sum > 1) return false;
      }
      return sum == 1;
   }

   private boolean oneDifferent(BoolVar[] vars) {
      int value = vars[0].getValue();
      for (int i = 1; i < vars.length; i++) {
         if (vars[i].getValue() != value) {
            return true;
         }
      }
      return false;
   }

   private boolean notXor(IntVar[] vars) {
      int value = 0;
      for (int i = 0; i < vars.length; i++) {
         value ^= vars[i].getValue();
      }
      return value != 0;
   }

   public class ByteVar {
      public final String name;
      public final String equation;
      public final IntVar realization;
      public final BoolVar abstraction;

      ByteVar(String name, int max) {
         this.name = name;
         this.equation = null;
         realization = intVar("δ" + name, 0, max);
         abstraction = boolVar("Δ" + name);
         delegate.arithm(realization, "!=", 0).reifyWith(abstraction);
      }

      ByteVar(String name, int max, String equation) {
         this.name = name;
         this.equation = equation;
         realization = intVar("δ" + name, 0, max);
         abstraction = boolVar("Δ" + name);
         delegate.arithm(realization, "!=", 0).reifyWith(abstraction);
      }

      @Override
      public String toString() {
         if (equation == null) {
            return name;
         } else {
            return name + " = " + equation;
         }
      }
   }

   private Tuples xor3_8bits = xor3(256);
   // private Tuples xor4_8bits = xor4(256);

   private Tuples xor3(int max) {
      int[][] xor3 = new int[max * max][3];
      int cpt = 0;
      for (int i = 0; i < max; i++) {
         for (int j = 0; j < max; j++) {
            xor3[cpt++] = new int[]{i, j, i ^ j};
         }
      }
      return new Tuples(xor3, true);
   }

   private Tuples xor4(int max) {
      int[][] xor4 = new int[max * max * max][4];
      int cpt = 0;
      for (int i = 0; i < max; i++) {
         for (int j = 0; j < max; j++) {
            for (int k = 0; k < max; k++) {
               xor4[cpt++] = new int[]{i, j, k, i ^ j ^ k};
            }
         }
      }
      return new Tuples(xor4, true);
   }

   private <V extends Variable> V declare(V variable) {
      constraintsOf.put(variable.getId(), new ArrayList<>());
      return variable;
   }

}