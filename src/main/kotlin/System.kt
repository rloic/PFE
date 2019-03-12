import java.util.*
import kotlin.system.measureTimeMillis

fun main() {

    val variables = Array(4) { BoolVariable() }

    val equations = system(
        equation(variables[0], variables[2], variables[3]),
        equation(variables[1], variables[2], variables[3])
    )

    fun isSatisfiable(system: System): Boolean {
        for (equation in system) {
            var count = 0
            for (variable in equation) {
                count += variable.value
            }
            if (count == 1) return false
        }
        return true
    }

    fun onSolution(variables: Variables) {
        println(variables.map { it.value })
    }
    solve(variables, equations, ::isSatisfiable, ::onSolution)
}


fun solve(
    variables: Variables,
    system: System,
    isCorrect: (System) -> Boolean,
    onSolution: (Variables) -> Unit,
    offset: Int = 0
) {
    if (offset == variables.size) {
        if (isCorrect(system)) {
            onSolution(variables)
        }
    } else {
        while (variables[offset].next()) {
            solve(variables, system, isCorrect, onSolution, offset + 1)
        }
        variables[offset].reset()
    }

}

typealias System = Array<Equation>
typealias Equation = Array<out IntVariable>
typealias Variables = Array<out IntVariable>

open class IntVariable(
    private val domain: IntArray
) {
    private var i = -1
    val value get() = domain[i]

    fun next(): Boolean {
        if (i == domain.size - 1) return false
        i += 1
        return true
    }

    fun reset() {
        i = -1;
    }

}
class BoolVariable : IntVariable(intArrayOf(0, 1))

fun system(vararg equations: Equation) = arrayOf(*equations)
fun equation(vararg variables: IntVariable) = arrayOf(*variables)