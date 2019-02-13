#!/usr/bin/env kscript

import java.io.File

fun main(args: Array<String>) {
   diff(args[0], args[1])
}

fun diff(fileA: String, fileB: String) {
   val linesOfFileA = File(fileA).readLines()
      .map(::clean)
      .sorted()
   val linesOfFileB = File(fileB).readLines()
      .map(::clean)
      .sorted()

   println(linesOfFileA == linesOfFileB)
}

fun clean(line: String) = line.replace(" ", "")