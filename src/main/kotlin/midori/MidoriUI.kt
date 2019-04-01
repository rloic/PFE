package midori

import com.github.rloic.strategy.CustomDomOverWDeg
import com.github.rloic.midori.MidoriAdvanced
import com.github.rloic.midori.MidoriBasic
import com.github.rloic.midori.MidoriGlobalFull
import com.github.rloic.midori.MidoriGlobalPartial
import org.chocosolver.solver.Model
import org.chocosolver.solver.Solver
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin
import org.chocosolver.solver.variables.BoolVar
import org.chocosolver.util.criteria.Criterion
import java.awt.Dimension
import java.io.File
import java.io.FileWriter
import java.lang.RuntimeException
import java.time.Duration
import java.time.LocalDateTime
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

const val TIMEOUT = 5
val UNIT = TimeUnit.MINUTES

fun searchStrategy(solver: Solver, sBoxes: Array<BoolVar>, assignedVars: Array<BoolVar>) {
    solver.setSearch(
        CustomDomOverWDeg(sBoxes, 0L, IntDomainMin())//,
        //CustomDomOverWDeg(assignedVars, 0L, IntDomainMin())
    )
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
    }

    fun click(): Future<*> {
        text = "$title - Waiting for worker"
        return workers.submit {
            val start = LocalDateTime.now()
            text = "$title - Working [starting @ $start]"
            onClick()
            Thread.sleep(1000L)
            val end = LocalDateTime.now()
            text = "$title - Done in ${Duration.between(start, end)}"
        }
    }

    init {
        addActionListener { click() }
    }

}

fun mkdir(folder: String, init: (File) -> Unit = {}) {
    val f = File(folder)
    init(f)
}

data class Components(
    val model: Model,
    val sBoxes: Array<BoolVar>,
    val assignedVar: Array<BoolVar>
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
    val (m, sBoxes, assignedVars) = model(rounds, objStep)
    val solver = m.solver
    searchStrategy(solver, sBoxes, assignedVars)

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
    return Components(version.m, version.sBoxes, version.assignedVar)
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