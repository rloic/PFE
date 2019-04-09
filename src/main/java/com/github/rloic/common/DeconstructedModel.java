package com.github.rloic.common;

import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.BasePropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;

import java.util.List;

public class DeconstructedModel {

   public final Model model;
   public final BasePropagator propagator;
   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   public DeconstructedModel(
         Model model,
         BasePropagator propagator,
         Int2ObjectMap<List<WeightedConstraint>> constraintsOf
   ) {
      this.model = model;
      this.propagator = propagator;
      this.constraintsOf = constraintsOf;
   }
}
