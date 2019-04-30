import com.github.rloic.aes.EnumFilter
import com.github.rloic.midori.MidoriGlobalFull
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier
import com.github.rloic.strategy.WDeg
import com.github.rloic.xorconstraint.BasePropagator
import org.chocosolver.solver.Model
import org.chocosolver.solver.constraints.Constraint
import org.chocosolver.solver.search.strategy.Search
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin
import org.chocosolver.solver.variables.IntVar

fun main() {

    val m = Model {
        val a = boolVar("A")
        val b = boolVar("B")
        val c = boolVar("C")
        val d = boolVar("D")
        val e = boolVar("E")

        val prop = BasePropagator(
            arrayOf(a, b, c, d, e),
            arrayOf(
                arrayOf(a, c, d),
                arrayOf(b, c, d),
                arrayOf(d, e)
            ),
            FullInferenceEngine(),
            FullRulesApplier(),
            solver
        )
        post(Constraint("globalXor", prop))
        sum(arrayOf(a,c,d), "=", 1).post()
    }

    val solver = m.solver
    solver.setLearningSignedClauses()
    solver.setSearch(Search.inputOrderLBSearch(*m.retrieveBoolVars()))
    while (solver.solve()) {
        println(m.retrieveBoolVars().map { it.name + "=" + it.value })
    }
    solver.printShortStatistics()

}

fun Model(init: Model.() -> Unit): Model {
    val m = Model()
    m.init()
    return m
}