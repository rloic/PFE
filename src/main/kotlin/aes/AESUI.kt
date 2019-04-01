package aes

import com.github.rloic.aes.EnumFilter
import com.github.rloic.aes.GlobalXOR
import com.github.rloic.aes.KeyBits
import com.github.rloic.aes.KeyBits.AES128.AES_128
import com.github.rloic.aes.KeyBits.AES192.AES_192
import com.github.rloic.aes.KeyBits.AES256.AES_256
import com.github.rloic.strategy.CustomDomOverWDeg
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin

fun main() {

    val experiments = listOf(
        Experiment(AES_128, 3, 5),
        Experiment(AES_192, 3, 1),
        Experiment(AES_192, 4, 4),
        Experiment(AES_192, 5, 5),
        Experiment(AES_256, 3, 1),
        Experiment(AES_256, 4, 3),
        Experiment(AES_256, 5, 3),
        Experiment(AES_256, 6, 5),
        Experiment(AES_256, 7, 5)
    )

    for ((key, rounds, objStep) in experiments) {
        println("$key-$rounds-$objStep")
        val aesGlobal = GlobalXOR(rounds, objStep, key)
        val solver = aesGlobal.m.solver
        solver.plugMonitor(EnumFilter(aesGlobal.m, aesGlobal.sBoxes, objStep))
        solver.setSearch(
            CustomDomOverWDeg(aesGlobal.sBoxes, 0L, IntDomainMin())
        )
        while (solver.solve()) {}
        solver.printShortStatistics()
    }

}

data class Experiment(
    val keyBits: KeyBits,
    val rounds: Int,
    val objStep: Int
)