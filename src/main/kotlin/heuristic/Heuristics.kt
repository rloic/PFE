package heuristic

import com.github.rloic.aes.EnumFilter
import com.github.rloic.midori.MidoriGlobalFull
import com.github.rloic.strategy.*
import com.github.rloic.xorconstraint.BasePropagator
import org.chocosolver.solver.search.strategy.Search
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy
import org.chocosolver.solver.variables.IntVar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

typealias StrategyBuilder = (BasePropagator, Array<out IntVar>, IntValueSelector, Long) -> AbstractStrategy<IntVar>

fun main() {

    val strategies = mapOf<String, StrategyBuilder>(
        "Sequential" to { _, vars, selector, _ -> Sequential(vars, selector) },
        "Random" to { _, vars, selector, seed -> Rnd(vars, seed, selector) },
        "Deg" to { prop, vars, selector, _ -> Deg(vars, selector, prop) },
        "DDeg" to { prop, vars, selector, _ -> DDeg(vars, selector, prop) },
        "DomOverWeg" to { _, vars, _, _ -> Search.intVarSearch(*vars) },
        "BrokenWDeg" to { prop, vars, selector, seed -> WDeg(vars, seed, selector, vars[0].model, prop) }
    )


    val workers = Executors.newFixedThreadPool(4)
    for ((name, strategy) in strategies) {
        launch(workers, 3, name, strategy)
    }

}

fun launch(
    worker: ExecutorService,
    rounds: Int,
    name: String,
    strategy: StrategyBuilder
) {
    worker.execute {
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

            while (solver.solve()) {}
            result += "\n" + solver.ref().measures.toOneLineString()
            println(result)
            System.out.flush()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (rounds < 19) {
            synchronized(worker) {
                launch(worker, rounds + 1, name, strategy)
            }
        }
    }

}