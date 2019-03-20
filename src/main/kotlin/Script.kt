import java.io.File

fun main() {

    val logs = File("results")
        .listFiles()
        .filter { file ->
            file.name.startsWith("log__")
        }

    val experiences = logs
        .groupBy { parseFileName(it.name) }
        .toList()
        .sortedBy { it.first }

    for ((exp, expLogs) in experiences) {
        print(exp)
        print(",")
        for (expLog in expLogs.sortedBy { it.name }) {
            val lines = expLog.readLines()
            val resultLine = if(lines[lines.lastIndex] == "") lines[lines.lastIndex - 1] else lines[lines.lastIndex]
            print(parseResults(resultLine).toCSV())
        }
        println()
    }

}

class Exp(
    val aes: String,
    val rounds: Int,
    val objStep: Int
)

sealed class Result {
    abstract fun toCSV(): String
}
data class ValidResult(
    val solutions: Int,
    val resolutionTime: Float,
    val nodes: Int,
    val fails: Int
) : Result() {

    override fun toCSV(): String {
        return "valid,$solutions,$resolutionTime,$nodes,$fails,"
    }

}
data class Timeout(
    val time: Int,
    val unit: String
) : Result() {

    override fun toCSV(): String {
        return ",,,,,"
    }

}

fun parseResults(str: String): Result {
    return when {
        str.startsWith("Timeout") -> parseTimeout(str)
        else -> parseValid(str)
    }
}

fun parseTimeout(str: String): Timeout {
    val parts = str.split(' ')
    return Timeout(parts[2].toInt(), parts[3])
}

fun parseValid(str: String): ValidResult {
    val parts = str.split(',')
    val solutions = parts[2].split(' ')[1].toInt()
    val resolutionTime = parts[3].split(' ')[3].toFloat()
    val nodes = parts[5].split(' ')[1].toInt()
    val fails = parts[9].split(' ')[1].toInt()

    return ValidResult(solutions, resolutionTime, nodes, fails)
}

fun parseFileName(str: String): String {
    val expSlug = str
        .substringAfterLast("__")
        .substringBefore(".text")

    return expSlug
}