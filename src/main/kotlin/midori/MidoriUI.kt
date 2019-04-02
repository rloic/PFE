package midori

import com.github.rloic.midori.MidoriAdvanced
import com.github.rloic.midori.MidoriBasic
import com.github.rloic.midori.MidoriGlobalFull
import com.github.rloic.midori.MidoriGlobalPartial
import com.github.rloic.strategy.CustomDomOverWDeg
import com.github.rloic.strategy.WDeg
import com.github.rloic.xorconstraint.BasePropagator
import org.chocosolver.solver.Model
import org.chocosolver.solver.Solver
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin
import org.chocosolver.solver.variables.BoolVar
import org.chocosolver.util.criteria.Criterion
import java.awt.Dimension
import java.awt.event.ActionListener
import java.io.File
import java.io.FileWriter
import java.lang.RuntimeException
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFrame

val algorithms = arrayOf(
    Algorithm("Basic", ::createBasic),
    Algorithm("Global[1-3]", ::createGlobalPartial),
    Algorithm("Global[1-5]", ::createGlobalFull),
    Algorithm("Advanced", ::createAdvanced)
)

const val TIMEOUT = 1
val UNIT = TimeUnit.HOURS

fun searchStrategy(solver: Solver, sBoxes: Array<BoolVar>, assignedVars: Array<BoolVar>, model: Model, basePropagator: BasePropagator?) {

    if (basePropagator != null) {
        solver.setSearch(
            WDeg(sBoxes, 0L, IntDomainMin(), model)//,
            //CustomDomOverWDeg(assignedVars, 0L, IntDomainMin())
        )
    } else {
        solver.setSearch(
            WDeg(sBoxes, 0L, IntDomainMin(), model)//,
            //CustomDomOverWDeg(assignedVars, 0L, IntDomainMin())
        )
    }


}

fun main() {
    mkdir("results") { outputFolder ->
        for ((name, version) in algorithms) {
            Window("$name - Timeout: $TIMEOUT $UNIT") {
                preferredSize = Dimension(460, 530)
                contentPane.layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
                val buttons = List(18) {
                    val r = 3 + it
                    TaskButton("Round $r-$r") {
                        val writer = FileWriter(outputFolder / "${name}_${r}_$r.txt")
                        bench(writer, r, r, version)
                        writer.close()
                    }
                }
                for (button in buttons) {
                    contentPane.add(button)
                }
                val runAll = TaskButton("Run all") {
                    buttons
                        .map { it.click() }
                        .map { it.get() }
                }
                contentPane.add(runAll)
            }
        }
    }

}

class Window(
    name: String,
    init: Window.() -> Unit
) : JFrame(name) {
    init {
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        init()
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}

class TaskButton(
    val title: String,
    private val onClick: () -> Unit
) : JButton("$title - Wait") {

    companion object {
        private val workers = Executors.newFixedThreadPool(4)
        private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    private var clickListener: ActionListener = ActionListener{ click() }

    fun click(): Future<*> {
        text = "$title - Waiting for worker"
        return workers.submit {
            removeActionListener(clickListener)
            val start = LocalDateTime.now()
            text = "$title - Working [starting@${format.format(start)}]"
            try {
                onClick()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val end = LocalDateTime.now()
            text = "$title - Done in ${Duration.between(start, end)}"
            addActionListener(clickListener)
        }
    }

    init {
        addActionListener(clickListener)
    }

}

fun mkdir(folder: String, init: (File) -> Unit = {}) {
    val f = File(folder)
    init(f)
}

data class Components(
    val model: Model,
    val sBoxes: Array<BoolVar>,
    val assignedVar: Array<BoolVar>,
    val propagator: BasePropagator? = null
)

data class Algorithm(
    val name: String,
    val make: (Int, Int) -> Components
)

fun bench(
    writer: FileWriter,
    rounds: Int,
    objStep: Int,
    model: (Int, Int) -> Components
) {
    val (m, sBoxes, assignedVars, propagator) = model(rounds, objStep)
    val solver = m.solver
    searchStrategy(solver, sBoxes, assignedVars, m, propagator)

    var cancelled = false
    Timer().schedule(object : TimerTask() {
        override fun run() {
            cancelled = true
            solver.addStopCriterion(Criterion { true })
        }
    }, TIMEOUT * UNIT)
    while (solver.solve()) {
        writer.write(solver.ref().measures.toOneLineString() + "\n")
    }
    writer.write("**END**\n")
    if (cancelled) {
        writer.write("Model[${m.name}], timeout after $TIMEOUT $UNIT\n")
    } else {
        writer.write(solver.ref().measures.toOneLineString() + "\n")
    }
    writer.flush()
}

operator fun File.div(file: String) = File(path + File.separator + file)

fun createBasic(r: Int, objStep: Int): Components {
    val version = MidoriBasic(r, objStep)
    return Components(version.m, version.sBoxes, version.assignedVar)
}

fun createGlobalPartial(r: Int, objStep: Int): Components {
    val version = MidoriGlobalPartial(r, objStep)
    return Components(version.m, version.sBoxes, version.assignedVar)
}

fun createGlobalFull(r: Int, objStep: Int): Components {
    val version = MidoriGlobalFull(r, objStep)
    return Components(version.m, version.sBoxes, version.assignedVar, version.propagator)
}

fun createAdvanced(r: Int, objStep: Int): Components {
    val version = MidoriAdvanced(r, objStep)
    return Components(version.m, version.sBoxes, version.assignedVar)
}

operator fun Int.times(unit: TimeUnit): Long {
    return when (unit) {
        TimeUnit.MILLISECONDS -> this.toLong()
        TimeUnit.SECONDS -> this * 1000L
        TimeUnit.MINUTES -> this * 60 * 1000L
        TimeUnit.HOURS -> this * 60 * 60 * 1000L
        TimeUnit.DAYS -> this * 24 * 60 * 60 * 1000L
        else -> throw RuntimeException()
    }
}