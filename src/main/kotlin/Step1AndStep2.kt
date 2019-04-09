import com.github.rloic.aes.EnumFilter
import com.github.rloic.midori.MidoriGlobalFull
import com.github.rloic.midori.MidoriStep2
import com.github.rloic.strategy.WDeg
import org.chocosolver.solver.Model
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin

fun main() {

    val midori = MidoriGlobalFull(3, 3)
    val model = midori.m
    val solver = model.solver
    solver.setSearch(
        WDeg(midori.sBoxes, 0L, IntDomainMin(), midori.constraintsOf),
        WDeg(midori.variablesToAssign, 0L, IntDomainMin(), midori.constraintsOf)
    )
    val sBoxes = midori.sBoxes
    solver.plugMonitor(EnumFilter(model, sBoxes, 3))
    while (solver.solve()) {

        val DX = midori.ΔX
        val DY = midori.ΔY
        val DK = arrayOf(midori.ΔK)
        val DZ = midori.ΔZ

        val midoriStep2 = MidoriStep2(128, 3, 3 , DX, DY, DZ, DK)

        val step2Solver = midoriStep2.m.solver
        step2Solver.findOptimalSolution(midoriStep2.objective, Model.MINIMIZE)
        while (step2Solver.solve()) {
            println("Ok")
        }


    }

}