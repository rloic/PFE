package heuristic

import com.github.rloic.aes.EnumFilter
import com.github.rloic.midori.MidoriGlobalFull
import com.github.rloic.strategy.DDeg
import com.github.rloic.strategy.Deg
import com.github.rloic.strategy.Rnd
import com.github.rloic.strategy.Sequential
import com.github.rloic.xorconstraint.BasePropagator
import org.chocosolver.solver.search.strategy.Search
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy
import org.chocosolver.solver.variables.IntVar
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

typealias StrategyBuilder = (BasePropagator, Array<out IntVar>, IntValueSelector, Long) -> AbstractStrategy<IntVar>

fun main() {

    val strategies = mapOf<String, StrategyBuilder>(
        "Sequential" to { _, vars, selector, _ -> Sequential(vars, selector) },
        "Random" to { prop, vars, selector, seed -> Rnd(vars, seed, selector) },
        "Deg" to { prop, vars, selector, _ -> Deg(vars, selector, prop) },
        "DDeg" to { prop, vars, selector, _ -> DDeg(vars, selector, prop) }
    )


    val workers = Executors.newFixedThreadPool(4)

    for (rounds in 3..20) {
        for ((name, strategy) in strategies) {
            workers.submit {
                try {
                    val midoriGlobal = MidoriGlobalFull(rounds, rounds)
                    val propagator = midoriGlobal.propagator
                    val sBoxes = midoriGlobal.sBoxes
                    val assignedVar = midoriGlobal.assignedVar

                    val m = midoriGlobal.m
                    val solver = m.solver
                    solver.plugMonitor(EnumFilter(m, sBoxes, rounds))
                    solver.setSearch(
                        strategy(propagator, sBoxes, IntDomainMin(), 0L),
                        strategy(propagator, assignedVar, IntDomainMin(), 0L)
                    )

                    var result = """
                        *************************************
                        Midori128 $rounds-$rounds
                        Heuristic: $name
                    """.trimIndent()

                    while (solver.solve()) {
                    }
                    result += "\n" + solver.ref().measures.toOneLineString()
                    println(result)
                    System.out.flush()

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    workers.shutdown()
    workers.awaitTermination(1, TimeUnit.DAYS)

}