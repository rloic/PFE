package com.github.rloic.paper.impl.dancinglinks;

import com.github.rloic.paper.impl.dancinglinks.dancinglinks.Cell;

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

   Iterable<Integer> equationsOf(int variable);

   int pivotOf(int variable);

   void set(int variable, boolean value);

   void unSet(int variable);

   boolean isUndefined(int variable);

   int eligibleBase(int pivot);

   Iterable<Integer> equations();

   int firstUnknown(int equation);

   int nbEquations();

   int nbVariables();

   Iterable<Cell.Data> variablesOf(int target);
}
