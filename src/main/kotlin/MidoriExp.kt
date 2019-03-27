import com.github.rloic.CustomDomOverWDeg
import com.github.rloic.midori.MidoriGlobalSum
import com.github.rloic.midori.MidoriGlobalXOR
import com.github.rloic.midori.MidoriSumXOR
import com.github.rloic.util.Logger
import org.chocosolver.solver.Model
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin
import org.chocosolver.solver.variables.BoolVar
import org.chocosolver.util.criteria.Criterion
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.util.*
import java.util.concurrent.*

fun main() {

    val experiments = listOf(
        RoundAndObjStep(3, 3),
        RoundAndObjStep(4, 4),
        RoundAndObjStep(5, 5),
        RoundAndObjStep(6, 6),
        RoundAndObjStep(7, 7),
        RoundAndObjStep(8, 8),
        RoundAndObjStep(9, 9),
        RoundAndObjStep(10, 10),
        RoundAndObjStep(11, 11),
        RoundAndObjStep(12, 12),
        RoundAndObjStep(13, 13),
        RoundAndObjStep(14, 14),
        RoundAndObjStep(15, 15),
        RoundAndObjStep(16, 16),
        RoundAndObjStep(17, 17),
        RoundAndObjStep(18, 18),
        RoundAndObjStep(19, 19),
        RoundAndObjStep(20, 20)
    )
    val versions = listOf(
        "sum" to ::createMidoriSum,
        "gXor" to ::createMidoriGlobalXOR,
        "gSum" to ::createMidoriGlobalSum
    )
    for (experiment in experiments) {
        println("***************************")
        println(experiment)
        val threads = Executors.newFixedThreadPool(4)
        val executions = versions.map { (_, algorithm) ->
            Callable { benchModel(algorithm(experiment)) }
        }
        threads.invokeAll(executions)
        threads.shutdown()
    }
}

data class RoundAndObjStep(
    val rounds: Int,
    val objStep: Int
)

data class Components(
    val model: Model,
    val sBoxes: Array<BoolVar>,
    val assignedVar: Array<BoolVar>
)

fun benchModel(components: Components) {
    val TIMEOUT = 20 * 60 * 1000L

    val solver = components.model.solver
    solver.setSearch(
        CustomDomOverWDeg(components.sBoxes, 0L, IntDomainMin())
        // CustomDomOverWDeg(components.assignedVar, 0L, IntDomainMin())
    )
    var cancelled = false
    Timer().schedule(object : TimerTask() {
        override fun run() {
            cancelled = true
            solver.addStopCriterion(Criterion { true })
        }
    }, TIMEOUT)
    while (solver.solve()) {
    }
    if (cancelled) {
        println("Model[${components.model.name}], timeout")
    } else {
        solver.printShortStatistics()
    }

}

fun Components(model: MidoriSumXOR) = Components(model.m, model.sBoxes, model.assignedVar)

fun Components(model: MidoriGlobalXOR) = Components(model.m, model.sBoxes, model.assignedVar)

fun Components(model: MidoriGlobalSum) = Components(model.m, model.sBoxes, model.assignedVar)

fun createMidoriSum(exp: RoundAndObjStep): Components {
    return Components(MidoriSumXOR(exp.rounds, exp.objStep))
}

fun createMidoriGlobalXOR(exp: RoundAndObjStep): Components {
    return Components(MidoriGlobalXOR(exp.rounds, exp.objStep))
}

fun createMidoriGlobalSum(exp: RoundAndObjStep): Components {
    return Components(MidoriGlobalSum(exp.rounds, exp.objStep))
}
