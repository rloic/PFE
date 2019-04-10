package com.github.rloic.common;

import com.github.rloic.common.abstraction.MathSet;
import com.github.rloic.midori.ByteXOR;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.util.Logger;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.BasePropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.util.*;

public class ExtendedModel {

   private Model delegate;
   private final Int2ObjectMap<List<WeightedConstraint>> constraintsOf = new Int2ObjectArrayMap<>();

   private final List<BoolVar> globalXorElements = new ArrayList<>();
   private final List<BoolVar[]> globalXorEquations = new ArrayList<>();

   public ExtendedModel(String name) {
      this.delegate = new Model(name);
   }

   public Model getModel() {
      return delegate;
   }

   public BoolVar boolVar(String name) {
      BoolVar var = delegate.boolVar(name);
      constraintsOf.put(var.getId(), new ArrayList<>());
      return var;
   }

   public BoolVar[] boolVar(String name, int len) {
      BoolVar[] vars = new BoolVar[len];
      for (int i = 0; i < len; i++) {
         vars[i] = boolVar(name + "[" + i + "]");
      }
      return vars;
   }

   public BoolVar[][] boolVar(String name, int dimA, int dimB) {
      BoolVar[][] vars = new BoolVar[dimA][dimB];
      for (int i = 0; i < dimA; i++) {
         vars[i] = boolVar(name + "[" + i + "]", dimB);
      }
      return vars;
   }

   public BoolVar[][][] boolVar(String name, int dimA, int dimB, int dimC) {
      BoolVar[][][] vars = new BoolVar[dimA][dimB][dimC];
      for (int i = 0; i < dimA; i++) {
         vars[i] = boolVar(name + "[" + i + "]", dimB, dimC);
      }
      return vars;
   }

   public void abstractXor(BoolVar... equation) {
      WeightedConstraint xorConstraint = new WeightedConstraint<>(equation, this::sumEqualsOne);
      for (BoolVar var : equation) {
         constraintsOf.get(var.getId()).add(xorConstraint);

         if (!globalXorElements.contains(var)) {
            globalXorElements.add(var);
         }
      }
      globalXorEquations.add(equation);
   }

   public void byteXor(IntVar... equation) {
      delegate.post(new Constraint("Realisation XOR", new ByteXOR(equation)));
   }

   public void equals(BoolVar lhs, BoolVar rhs) {
      BoolVar[] vars = new BoolVar[]{lhs, rhs};
      WeightedConstraint equalityConstraint = new WeightedConstraint<>(vars, this::oneDifferent);
      for (BoolVar var : vars) {
         constraintsOf.get(var.getId()).add(equalityConstraint);
      }
      delegate.arithm(lhs, "=", rhs).post();
   }

   private WeightedConstraint<Variable> post(Constraint constraint, Variable[] variables) {
      constraint.post();
      WeightedConstraint<Variable> wConstraint = WeightedConstraint.wrap(constraint);
      for(Variable v : variables) {
         constraintsOf.get(v.getId()).add(wConstraint);
      }
      return wConstraint;
   }

   private void post(Constraint constraint, Variable[] variables, Variable... other) {
      constraint.post();
      WeightedConstraint<Variable> wConstraint = WeightedConstraint.wrap(constraint);
      for(Variable v : variables) {
         constraintsOf.get(v.getId()).add(wConstraint);
      }
      for (Variable v: other) {
         constraintsOf.get(v.getId()).add(wConstraint);
      }
   }

   public void sum(BoolVar[] sum, String operator, int value) {
      Constraint cSum  = delegate.sum(sum, operator, value);
      post(cSum, sum);
   }

   public void sum(IntVar[] sum, String operator, IntVar value) {
      Constraint cSum  = delegate.sum(sum, operator, value);
      post(cSum, sum);
   }

   public void sum(BoolVar[] sum, String operator, IntVar value) {
      Constraint cSum  = delegate.sum(sum, operator, value);
      post(cSum, sum, value);
   }

   public void arithm(IntVar varA, String operator, IntVar varB) {
      delegate.arithm(varA, operator, varB).post();
   }

   public void table(IntVar[] vars, Tuples tuples, String strategy) {
      delegate.table(vars, tuples, strategy).post();
   }

   public void table(IntVar[] vars, Tuples tuples) {
      delegate.table(vars, tuples).post();
   }

   public IntVar intVar(String name, int lb, int ub) {
      IntVar var = delegate.intVar(name, lb, ub);
      constraintsOf.put(var.getId(), new ArrayList<>());
      return var;
   }

   public IntVar intVar(int[] domain) {
      IntVar var = delegate.intVar(domain);
      constraintsOf.put(var.getId(), new ArrayList<>());
      return var;
   }

   public IntVar intVar(int lb, int ub) {
      IntVar var = delegate.intVar(lb, ub);
      constraintsOf.put(var.getId(), new ArrayList<>());
      return var;
   }

   public IntVar constant(String name, int value) {
      IntVar var = delegate.intVar(name, value);
      constraintsOf.put(var.getId(), new ArrayList<>());
      return var;
   }

   public DeconstructedModel build(InferenceEngine inferenceEngine, RulesApplier rulesApplier) {
      if (globalXorElements.size() != 0) {
         BoolVar[] vars = new BoolVar[globalXorElements.size()];
         globalXorElements.toArray(vars);

         BoolVar[][] xors = new BoolVar[globalXorEquations.size()][];
         globalXorEquations.toArray(xors);

         BasePropagator propagator = new BasePropagator(
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

}