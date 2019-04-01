package benchmark

import com.github.rloic.aes.EnumFilter
import com.github.rloic.aes.GlobalXOR
import com.github.rloic.aes.KeyBits
import com.github.rloic.aes.KeyBits.AES128.AES_128
import com.github.rloic.aes.KeyBits.AES192.AES_192
import com.github.rloic.aes.KeyBits.AES256.AES_256
import com.github.rloic.midori.MidoriGlobalFull
import com.github.rloic.strategy.CustomDomOverWDeg
import org.chocosolver.solver.Model
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin
import org.chocosolver.solver.variables.BoolVar

val midoriBenchmarks = mapOf(
    "Midori-128-3-3" to MidoriBenchmark(3, 3),
    "Midori-128-4-4" to MidoriBenchmark(4, 4),
    "Midori-128-5-5" to MidoriBenchmark(5, 5),
    "Midori-128-6-6" to MidoriBenchmark(6, 6),
    "Midori-128-7-7" to MidoriBenchmark(7, 7),
    "Midori-128-8-8" to MidoriBenchmark(8, 8)
)

val aesBenchmarks = mapOf(
    "AES-128-3-5" to AESBenchmark(AES_128, 3, 5),
    "AES-192-3-1" to AESBenchmark(AES_192, 3, 1),
    "AES-192-4-4" to AESBenchmark(AES_192, 4, 4),
    "AES-192-5-5" to AESBenchmark(AES_192, 5, 5),
    "AES-256-3-1" to AESBenchmark(AES_256, 3, 1),
    "AES-256-4-3" to AESBenchmark(AES_256, 4, 3),
    "AES-256-5-3" to AESBenchmark(AES_256, 5, 3),
    "AES-256-6-5" to AESBenchmark(AES_256, 6, 5),
    "AES-256-7-5" to AESBenchmark(AES_256, 7, 5)
)

val BASE_MIDORI = mapOf(
    "Midori-128-3-3" to StrategyResult(0.305, 2749),
    "Midori-128-4-4" to StrategyResult(0.683, 12899),
    "Midori-128-5-5" to StrategyResult(3.061, 47394),
    "Midori-128-6-6" to StrategyResult(6.729, 59902),
    "Midori-128-7-7" to StrategyResult(33.812, 398308),
    "Midori-128-8-8" to StrategyResult(226.672, 1337988)
)

val BEST_MIDORI = mapOf(
    "Midori-128-3-3" to StrategyResult(0.181, 2518),
    "Midori-128-4-4" to StrategyResult(0.320, 7240),
    "Midori-128-5-5" to StrategyResult(0.921, 26409),
    "Midori-128-6-6" to StrategyResult(4.513, 114487),
    "Midori-128-7-7" to StrategyResult(14.136, 288347),
    "Midori-128-8-8" to StrategyResult(170.697, 2928935)
)

val BASE_AES = mapOf(
    "AES-128-3-5" to StrategyResult(3.997, 20678),
    "AES-192-3-1" to StrategyResult(0.029, 53),
    "AES-192-4-4" to StrategyResult(9.521, 17429),
    "AES-192-5-5" to StrategyResult(41.766, 30173),
    "AES-256-3-1" to StrategyResult(0.023, 80),
    "AES-256-4-3" to StrategyResult(0.906, 3703),
    "AES-256-5-3" to StrategyResult(2.353, 3151),
    "AES-256-6-5" to StrategyResult(201.531, 264969),
    "AES-256-7-5" to StrategyResult(221.434, 178897)
)

val BEST_AES = mapOf(
    "AES-128-3-5" to StrategyResult(1.224, 9840),
    "AES-192-3-1" to StrategyResult(0.064, 53),
    "AES-192-4-4" to StrategyResult(0.841, 4845),
    "AES-192-5-5" to StrategyResult(3.266, 12169),
    "AES-256-3-1" to StrategyResult(0.049, 80),
    "AES-256-4-3" to StrategyResult(0.436, 2369),
    "AES-256-5-3" to StrategyResult(0.502, 1889),
    "AES-256-6-5" to StrategyResult(11.192, 26643),
    "AES-256-7-5" to StrategyResult(35.294, 67497)
)

fun main() {

    for ((name, benchmark) in midoriBenchmarks) {
        benchMidori(name, midori(benchmark))
    }

    for ((name, benchmark) in aesBenchmarks) {
        benchAES(name, aes(benchmark), benchmark.objStep)
    }

}

data class StrategyResult(
    val time: Double,
    val nodes: Int
)

data class MidoriBenchmark(
    val rounds: Int,
    val objStep: Int
)

data class AESBenchmark(
    val key: KeyBits,
    val rounds: Int,
    val objStep: Int
)

data class Components(
    val model: Model,
    val sBoxes: Array<BoolVar>
)

fun midori(bench: MidoriBenchmark): Components {
    val midori = MidoriGlobalFull(bench.rounds, bench.objStep)
    return Components(midori.m, midori.sBoxes)
}

fun aes(bench: AESBenchmark): Components {
    val aes = GlobalXOR(bench.rounds, bench.objStep, bench.key)
    return Components(aes.m, aes.sBoxes)
}

fun benchMidori(name: String, components: Components) {
    val (model, sBoxes) = components
    val solver = model.solver
    solver.setSearch(
        CustomDomOverWDeg(sBoxes, 0L, IntDomainMin())
    )
    while (solver.solve()) {
    }
    val result =
        "Current" to StrategyResult(solver.measures.timeCount.toDouble(), solver.measures.nodeCount.toInt())

    val baseResult = "Base" to BASE_MIDORI[name]
    val bestResult = "Best" to BEST_MIDORI[name]

    println("*******************************")
    println(name)
    println(
        listOf(
            result,
            baseResult,
            bestResult
        ).sortedBy { it.second!!.nodes }
            .joinToString("\n") { "${it.first} [${it.second!!.nodes} nodes]" }
    )
}

fun benchAES(name: String, components: Components, objStep: Int) {
    val (model, sBoxes) = components
    val solver = model.solver
    solver.plugMonitor(EnumFilter(model, sBoxes, objStep))
    solver.setSearch(
        CustomDomOverWDeg(sBoxes, 0L, IntDomainMin())
    )
    while (solver.solve()) {
    }
    val result =
        "Current" to StrategyResult(solver.measures.timeCount.toDouble(), solver.measures.nodeCount.toInt())

    val baseResult = "Base" to BASE_AES[name]
    val bestResult = "Best" to BEST_AES[name]

    println("*******************************")
    println(name)
    println(
        listOf(
            result,
            baseResult,
            bestResult
        ).sortedBy { it.second!!.nodes }
            .joinToString("\n") { "${it.first} [${it.second!!.nodes} nodes]" }
    )
}