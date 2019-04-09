package com.github.rloic.common;

import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.BasePropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperator;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtendedModel {

   private Model delegate;
   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf = new Int2ObjectArrayMap<>();

   private final List<BoolVar> globalXorElements = new ArrayList<>();
   private final List<BoolVar[]> globalXorEquations = new ArrayList<>();

   public ExtendedModel(String name) {
      this.delegate = new Model(name);
   }

   public BoolVar boolVar(String name) {
      BoolVar var = delegate.boolVar();
      constraintsOf.put(var.getId(), new ArrayList<>());
      return var;
   }

   public void abstractXor(BoolVar... equation) {
      WeightedConstraint xorConstraint = new WeightedConstraint(
            equation,
            (eq) -> Arrays.stream(eq).mapToInt(IntVar::getValue).sum() == 1
      );
      for (BoolVar var : equation) {
         constraintsOf.get(var.getId()).add(xorConstraint);

         if (!globalXorElements.contains(var)) {
            globalXorElements.add(var);
         }
      }
      globalXorEquations.add(equation);
   }

   public void equals(BoolVar lhs, BoolVar rhs) {
      BoolVar[] vars = new BoolVar[]{lhs, rhs};
      WeightedConstraint equalityConstraint = new WeightedConstraint(
            vars,
            (v) -> Arrays.stream(v).anyMatch(it -> it.getValue() != v[0].getValue())
      );
      for (BoolVar var : vars) {
         constraintsOf.get(var.getId()).add(equalityConstraint);
      }
      delegate.arithm(lhs, "=", rhs).post();
   }

   public void sum(BoolVar[] sum, String operator, int value) {
      delegate.sum(sum, operator, value).post();
   }

   public DeconstructedModel build(InferenceEngine inferenceEngine, RulesApplier rulesApplier) {

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

   }

}