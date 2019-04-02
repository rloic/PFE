package com.github.rloic.paper.dancinglinks;

import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.cell.Column;
import com.github.rloic.paper.dancinglinks.cell.Data;
import com.github.rloic.paper.dancinglinks.cell.Row;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.differences.D;
import org.chocosolver.solver.variables.Variable;

import java.util.List;

public interface IDancingLinksMatrix {

   boolean isUnknown(int equation, int variable);

   boolean isTrue(int equation, int variable);

   boolean isTrue(int variable);

   boolean isFalse(int equation, int variable);

   boolean isFalse(int variable);

   boolean isNone(int equation, int variable);

   void xor(int target, int pivot);

   void setBase(int pivot, int variable);

   void setOffBase(int variable);

   void removeVariable(int variable);

   void restoreVariable(int variable);

   void removeEquation(int equation);

   void restoreEquation(int equation);

   int nbTrues(int equation);

   int nbUnknowns(int equation);

   boolean isValid(int equation);

   boolean isInvalid(int equation);

   boolean isEmpty(int equation);

   boolean isBase(int variable);

   boolean isUnused(int variable);

   Iterable<Data> equationsOf(int variable);

   int pivotOf(int variable);

   void set(int variable, boolean value);

   void unSet(int variable);

   boolean isUndefined(int variable);

   int eligibleBase(int pivot);

   int minEligibleBase(int pivot);

   int firstUnknown(int equation);

   int nbEquations();

   int nbVariables();

   Iterable<Data> variablesOf(int target);

   boolean sameOffBaseVariables(int eq1, int eq2);

   boolean sameOffBaseVariables(Row eq1, Row eq2);

   int baseVariableOf(int equation);

   int baseVariableOf(Row equation);

   Iterable<Row> activeEquations();

   int firstOffBase(int pivot);

   boolean subsetOf(int pivot, int equation);

   int numberOfUndefinedVariables();

   int numberOfEquationsOf(int variable);

   List<Affectation> getDecisions();

}
