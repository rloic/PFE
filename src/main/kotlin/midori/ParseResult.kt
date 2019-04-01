package midori

import java.io.File

fun main() {
    val lines = File("midori_results.txt").readLines()

    val parts = mutableListOf<MutableList<String>>()
    var part = mutableListOf<String>()

    for (line in lines) {
        if (line.startsWith("***")) {
            if (part.isNotEmpty()) {
                parts += part
            }
            part = mutableListOf<String>()
        } else {
            part .add(line)
        }
    }
    if (part.isNotEmpty()) {
        parts += part
    }

    for(exp in parts) {
        parseExp(exp)
    }

}

fun parseExp(lines: List<String>) {
    val roundsAndObjStep = lines[0].substringAfter('(').substringBefore(')')
    val experiment = roundsAndObjStep.replace("rounds=([0-9]+), objStep=([0-9]+)".toRegex(), "$1-$2")
    for (i in 1..lines.size) {
        val components = lines[i].split(", ")
        val model = components[0]
        print("$model => ")
        if (lines[i].endsWith("timeout")) {
            println(Result.Timeout)
        } else {
            println(
                Result.Valid(
                    components[1].split(' ')[0],
                    components[2].split(' ')[2],
                    components[3].split(' ')[0]
                )
            )
        }
    }
}

sealed class Result {
    data class Valid(
        val solutions: String,
        val time: String,
        val nodes: String
    ) : Result()

    object Timeout: Result()
}