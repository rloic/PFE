package com.github.rloic.paper.dancinglinks;

import it.unimi.dsi.fastutil.ints.IntSet;

public interface IDancingLinksMatrix {

   /**
    * Return if the variables belongs to the equation and its value is yet defined
    * @param equation The equation
    * @param variable The variable
    * @return true if the variable belongs to the equation and it's not already defined
    */
   boolean isUnknown(int equation, int variable);

   /**
    * Return if the variable belongs to the equation and its value is true
    * @param equation The equation
    * @param variable The variable
    * @return true if the variable belongs to the equation and its value is true
    */
   boolean isTrue(int equation, int variable);

   /**
    * Return true if the variable is assigned to true
    * @param variable The variable
    * @return true if the variable is assigned to true else false
    */
   boolean isTrue(int variable);

   /**
    * Return false if the variable is assigned to false
    * @param variable The variable
    * @return true if the variable is assigned to false else true
    */
   boolean isFalse(int variable);

   /**
    * Transform the matrix into M' such as
    * { v \in 0 until variables | M[target][v] = M[target][v] xor M[pivot][v] }
    * @param target The target equation
    * @param pivot The pivot equation
    */
   void xor(int target, int pivot);

   /**
    * Records the variable 'variable' as a Base for the equation pivot
    * @param pivot The pivot equation
    * @param variable The variable
    */
   void setBase(int pivot, int variable);

   /**
    * Remove the variable of the bases
    * @param variable The variable
    */
   void setOffBase(int variable);

   /**
    * Remove the variable from the matrix
    * @param variable The variable to remove
    */
   void removeVariable(int variable);

   /**
    * Restore the variable into the matrix
    * @param variable The variable to restore
    */
   void restoreVariable(int variable);

   /**
    * Remove the equation from the matrix
    * @param equation The equation to remove
    */
   void removeEquation(int equation);

   /**
    * Restore the equation into the matrix
    * @param equation The equation to restore
    */
   void restoreEquation(int equation);

   /**
    * Return the number of variables that belong to the equation and that are assigned to true
    * @param equation The equation
    * @return The number of variables that belong to equation and that are assigned to true
    */
   int nbTrues(int equation);

   /**
    * Return the number of variables that belong to the equation and that are not assigned yet
    * @param equation The equation
    * @return The number of variables that belong to the equation and that are not assigned yet
    */
   int nbUnknowns(int equation);

   /**
    * Return if an equation is valid
    * @param equation The equation
    * @return true if the equation is valid else false
    */
   boolean isValid(int equation);

   /**
    * Return if the equation is NOT valid
    * @param equation The equation
    * @return true if the equation is NOT valid else false
    */
   boolean isInvalid(int equation);

   /**
    * Return if an equation is empty (no unknowns and no trues)
    * @param equation The equation
    * @return true if the equation is empty else false
    */
   boolean isEmpty(int equation);

   /**
    * Return if the variable is a base
    * @param variable The variable
    * @return true if the variable is a base else false
    */
   boolean isBase(int variable);

   /**
    * Return true if the variable doesn't belong to any equation
    * @param variable The variable
    * @return true if the variable doesn't belong to any equation else false
    */
   boolean isUnused(int variable);

   /**
    * The equations of the variable
    * @param variable The variable
    * @return An iterable over the equations of the variable
    */
   Iterable<Integer> equationsOf(int variable);

   /**
    * Return the pivot equation of the variable
    * @param variable The variable
    * @return the pivot equation of the variable if the variable is a base else -1
    */
   int pivotOf(int variable);

   /**
    * Transform the matrix M into a matrix M' such as M' = M[variable / value] (variable <- value)
    * @param variable The variable to assign
    * @param value The value
    */
   void set(int variable, boolean value);

   /**
    * Transform the matrix M into a matrix M' such as M' = M[variable \in {true, false}]
    * @param variable The variable to unset
    */
   void unSet(int variable);

   /**
    * Return if the variable is not defined (its domain is {true, false})
    * @param variable The variable
    * @return true if the variable is not defined else false
    */
   boolean isUndefined(int variable);

   /**
    * Return the next variable that's eligible as a base
    * @param pivot The pivot equation
    * @return The next variable that's eligible as a base (if one) else -1
    */
   int eligibleBase(int pivot);

   /**
    * Return the next unassigned variable from the equation
    * @param equation The equation
    * @return The next variable of the equation that's not assigned (if one) else -1
    */
   int firstUnknown(int equation);

   /**
    * Return the initial number of equations of the matrix (the number of rows)
    * @return The initial number of equations of the matrix
    */
   int nbEquations();

   /**
    * Return the initial number of variables of the matrix (the number of columns)
    * @return The initial number of variables of the matrix
    */
   int nbVariables();

   /**
    * Return an iterator over the variables of the equation 'target'
    * @param target The equation
    * @return An iterable of the variables of the equation 'target'
    */
   Iterable<Integer> variablesOf(int target);

   /**
    * Return if the equation eq1 and the equation eq2 have the same variables (except for their base)
    * @param eq1 An equation
    * @param eq2 An other equation
    * @return true if the two equations are the same excepted for their base
    */
   boolean sameOffBaseVariables(int eq1, int eq2);

   /**
    * Return the base variable of the equation
    * @param equation The equation
    * @return The base variable of the equation if one else -1
    */
   int baseVariableOf(int equation);

   /**
    * Return an iterator over the active equations of the matrix
    * @return The
    */
   Iterable<Integer> activeEquations();

   /**
    * Return the number of variables that are not defined
    * @return The number of variables that are not defined
    */
   int numberOfUndefinedVariables();

   /**
    * Returns the number of equations to which the variable belongs
    * @param variable The variable
    * @return The number of equations to which the variable belongs
    */
   int numberOfEquationsOf(int variable);

   /**
    * Return the first variable of the pivot that is not a base
    * @param pivot An equation
    * @return The first variable of the pivot that is not a base (if one) else -1
    */
   int firstOffBase(int pivot);

   /**
    * Return the unassigned variables of the matrix
    * @return The unassigned variables of the matrix
    */
   IntSet unassignedVars();

}
