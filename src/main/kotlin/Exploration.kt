import com.github.rloic.aes.EnumFilter
import com.github.rloic.aes.GlobalXOR
import com.github.rloic.aes.KeyBits.AES128.AES_128
import org.chocosolver.solver.search.strategy.Search
import org.chocosolver.solver.variables.IntVar

fun main() {

    val model = GlobalXOR(3, 5, AES_128)

    val solver = model.m.solver
    solver.setSearch(
        Search.intVarSearch(*model.sBoxes),
        Search.intVarSearch(*model.assignedVar)
    )
    solver.plugMonitor(EnumFilter(model.m, model.sBoxes, 5))
    solver.setSearch(
        Search.lastConflict(solver.getSearch<IntVar>())
    )
    while (solver.solve()) {
        solver.printShortStatistics()
    }

}