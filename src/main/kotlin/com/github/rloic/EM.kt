package com.github.rloic

import com.github.rloic.wip.WeightedConstraint
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import org.chocosolver.solver.Model
import org.chocosolver.solver.variables.BoolVar
import org.chocosolver.solver.variables.IntVar
import org.chocosolver.solver.variables.Variable

class EM(name: String) {

    private val delegate = Model(name)
    private val constraintsOf = Int2ObjectArrayMap<List<WeightedConstraint<Variable>>>()

    private fun <V : Variable> declare(v: V): V {
        constraintsOf[v.id] = arrayListOf()
        return v
    }

    fun boolVar(n: String): BoolVar {
        return declare(delegate.boolVar(n))
    }

    fun boolVarArray(n: String, len: Int) =
        Array(len) { i -> boolVar("$n[$i]") }

    fun boolVarMatrix(n: String, nbRows: Int, nbCols: Int) =
        Array(nbRows) { i ->
            boolVarArray("$n[$i]", nbCols)
        }

    fun boolVarTensor3(n: String, dimA: Int, dimB: Int, dimC: Int) =
        Array(dimA) { i ->
            boolVarMatrix("$n[$i]", dimB, dimC)
        }

    fun intVar(domain: IntArray): IntVar {
        return declare(delegate.intVar(domain))
    }



}