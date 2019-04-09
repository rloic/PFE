package com.github.rloic.midori;

import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.PartialInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.PartialRulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.BasePropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.function.Predicate;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;

final public class MidoriGlobalPartial extends MidoriGlobal {

   public MidoriGlobalPartial(int r, int objStep1) {
      super(r, objStep1);
   }

   @Override
   protected String getModelName() {
      return "Midori Global[1-3]";
   }

   @Override
   protected InferenceEngine getInferenceEngine() {
      return new PartialInferenceEngine();
   }

   @Override
   protected RulesApplier getRulesApplier() {
      return new PartialRulesApplier();
   }

}