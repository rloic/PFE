package refactor

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix
import com.github.rloic.paper.dancinglinks.cell.Data
import com.github.rloic.paper.dancinglinks.cell.Row
import it.unimi.dsi.fastutil.ints.IntList

class DancingLinksKt(
    equations: Array<IntArray>,
    nbVariables: Int
) : IDancingLinksMatrix {

    init {

    }

    override fun isUnknown(equation: Int, variable: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isTrue(equation: Int, variable: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isTrue(variable: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isFalse(variable: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun xor(target: Int, pivot: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setBase(pivot: Int, variable: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setOffBase(variable: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeVariable(variable: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun restoreVariable(variable: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeEquation(equation: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun restoreEquation(equation: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun nbTrues(equation: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun nbUnknowns(equation: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isValid(equation: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isInvalid(equation: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(equation: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isBase(variable: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isUnused(variable: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun equationsOf(variable: Int): MutableIterable<Data> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun pivotOf(variable: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun set(variable: Int, value: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unSet(variable: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isUndefined(variable: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun eligibleBase(pivot: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun firstUnknown(equation: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun nbEquations(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun nbVariables(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun variablesOf(target: Int): MutableIterable<Data> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sameOffBaseVariables(eq1: Int, eq2: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sameOffBaseVariables(eq1: Row?, eq2: Row?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun baseVariableOf(equation: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun baseVariableOf(equation: Row?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun activeEquations(): MutableIterable<Row> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun numberOfUndefinedVariables(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun numberOfEquationsOf(variable: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun firstOffBase(pivot: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unassignedVars(): IntList {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}