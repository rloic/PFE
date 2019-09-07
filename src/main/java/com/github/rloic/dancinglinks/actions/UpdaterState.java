package com.github.rloic.dancinglinks.actions;

/**
 * Represents the state of a matrix update
 */
public enum  UpdaterState {

   /**
    * When the update crashed before updating the matrix (some preCondition is invalid)
    */
   EARLY_FAIL,

   /**
    * When the update crashed after updating the matrix (the update goes to an invalid state)
    */
   LATE_FAIL,

   /**
    * When the update was done finely
    */
   DONE

}
