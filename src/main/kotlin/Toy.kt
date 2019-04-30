import Node.Companion.link
import java.lang.IllegalArgumentException

typealias N = Node<Decision, Constraint>

infix fun String.equalsTo(n: Int) = BelongsTo(this, setOf(n))
infix fun String.notEqualsTo(n: Int) = NotBelongsTo(this, setOf(n))
infix fun String.belongsTo(domain: Set<Int>) = BelongsTo(this, domain)
infix fun String.belongsTo(range: IntRange) = BelongsTo(this, range.toSet())
infix fun String.notBelongsTo(domain: Set<Int>) = NotBelongsTo(this, domain)
infix fun String.notBelongsTo(range: IntRange) = NotBelongsTo(this, range.toSet())
infix fun Literal.at(n: Int) = Decision(this, n)

fun main() {

    val c1 = Constraint()
    val c2 = Constraint()
    val c3 = Constraint()
    val c4 = Constraint()
    val c5 = Constraint()
    val c6 = Constraint()

    val explanations = mapOf(
        c1 to Clause("a" notBelongsTo 1..4, "b" notBelongsTo 1..4),
        c2 to Clause("a" notBelongsTo 1..4, "c" notBelongsTo 1..4),
        c3 to Clause("b" notBelongsTo 5..8, "c" notBelongsTo 5..8),
        c4 to Clause("x1" notEqualsTo 0, "x2" notEqualsTo 0),
        c5 to Clause("x1" notEqualsTo 0, "x3" notEqualsTo 0),
        c6 to Clause("x2" notEqualsTo 1, "x3" notEqualsTo 1, "a" equalsTo 1)
    )

    val x1AssignedToFalse = N("x1" equalsTo 0 at 1)
    val x2AssignedToTrue = N("x2" equalsTo 1 at 1)
    val x3AssignedToTrue = N("x3" equalsTo 1 at 1)

    val aAssignedTo1 = N("a" equalsTo 1 at 1)

    val initialDomainOfC = N("c" belongsTo 1..8 at 0)
    val initialDomainOfB = N("b" belongsTo 1..8 at 0)

    val bBelongsTo5_8 = N("b" belongsTo 5..8 at 1)
    val cBelongsTo5_8 = N("c" belongsTo 5..8 at 1)

    c1.link(x1AssignedToFalse, x2AssignedToTrue)
    c1.link(x1AssignedToFalse, x3AssignedToTrue)

    c6.link(x2AssignedToTrue, aAssignedTo1)
    c6.link(x3AssignedToTrue, aAssignedTo1)

    c1.link(aAssignedTo1, bBelongsTo5_8)
    c1.link(initialDomainOfB, bBelongsTo5_8)

    c2.link(aAssignedTo1, cBelongsTo5_8)
    c2.link(initialDomainOfC, cBelongsTo5_8)

    val emptyClause = N("" belongsTo setOf() at 0)

    c3.link(bBelongsTo5_8, emptyClause)
    c3.link(cBelongsTo5_8, emptyClause)

    cspAnalyzeConflict(emptyClause) { node ->
        val constraints = node.inComingEdges.map { it.using }.toSet()
        constraints.map { explanations[it] ?: error("No explanation for this constraint") }
            .reduceRight { clause, acc -> clause + acc }
    }

}

data class Clause(
    private val literals: List<Literal>
) {
    
    constructor(vararg literals: Literal): this(literals.toList())

    override fun toString(): String {
        return literals.joinToString(" V ")
    }

    operator fun plus(other: Clause) = Clause(literals + other.literals)

    fun filter(predicate: (Literal) -> Boolean) = Clause(literals.sortedBy { it.variable }.filter(predicate))

    fun <U> map(transform: (Literal) -> U): List<U> = literals.map(transform)

    fun reduce() = Clause(literals.reduceRight { literal, acc -> literal union acc })

    fun merge(): Clause {
        val literalsByVariable = literals.groupBy { it.variable }
        val simplifiedLiteralsByVariable =
            literalsByVariable.map { it.value.reduceRight { literal, acc -> literal union acc } }
        return Clause(simplifiedLiteralsByVariable)
    }

}

class Constraint

data class Decision(
    val literal: Literal,
    val level: Int
) {

    override fun toString(): String {
        return "$literal@$level"
    }
}

sealed class Literal(
    val variable: String
) {

    abstract fun domain(initialDomain: Set<Int>): Set<Int>
    abstract infix fun union(other: Literal): Literal

}

class BelongsTo(
    variable: String,
    val positiveFilter: Set<Int>
) : Literal(variable) {
    override fun toString(): String {
        return if (positiveFilter.size == 1)
            "$variable==${positiveFilter.first()}"
        else
            "$variable∈$positiveFilter"
    }

    override fun domain(initialDomain: Set<Int>): Set<Int> {
        val result = mutableSetOf<Int>()
        for (n in initialDomain) {
            if (n in positiveFilter) {
                result += n
            }
        }
        return result
    }

    override fun union(other: Literal): Literal {
        if (variable != other.variable) throw IllegalArgumentException()
        return when (other) {
            is BelongsTo -> BelongsTo(variable, positiveFilter + other.positiveFilter)
            is NotBelongsTo -> NotBelongsTo(variable, other.negativeFilter - positiveFilter)
        }
    }
}

class NotBelongsTo(
    variable: String,
    val negativeFilter: Set<Int>
) : Literal(variable) {
    override fun toString(): String {
        return if (negativeFilter.size == 1)
            "$variable!=${negativeFilter.first()}"
        else
            "$variable∉$negativeFilter"
    }

    override fun domain(initialDomain: Set<Int>): Set<Int> {
        val result = mutableSetOf<Int>()
        for (n in initialDomain) {
            if (n !in negativeFilter) {
                result += n
            }
        }
        return result
    }

    override fun union(other: Literal): Literal {
        if (variable != other.variable) throw IllegalArgumentException()
        return when (other) {
            is BelongsTo -> NotBelongsTo(variable, negativeFilter - other.positiveFilter)
            is NotBelongsTo -> NotBelongsTo(variable, negativeFilter + other.negativeFilter)
        }
    }
}

class Node<N, E>(
    val value: N
) {
    private val _outComingEdges = mutableListOf<OutComingEdge<N, E>>()
    val outComingEdges: MutableList<OutComingEdge<N, E>> get() = this._outComingEdges
    private val _inComingEdges = mutableListOf<InComingEdge<N, E>>()
    val inComingEdges: MutableList<InComingEdge<N, E>> get() = this._inComingEdges

    companion object {
        fun <N, E> E.link(from: Node<N, E>, to: Node<N, E>) {
            from._outComingEdges += OutComingEdge(to, this)
            to._inComingEdges += InComingEdge(from, this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node<*, *>

        if (value != other.value) return false
        if (_outComingEdges != other._outComingEdges) return false
        if (_inComingEdges != other._inComingEdges) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + _outComingEdges.hashCode()
        result = 31 * result + _inComingEdges.hashCode()
        return result
    }

    override fun toString(): String {
        return value.toString()
    }
}

data class InComingEdge<N, E>(
    val from: Node<N, E>,
    val using: E
)

data class OutComingEdge<N, E>(
    val to: Node<N, E>,
    val using: E
)

@JvmName("variablesOfNode")
fun variables(nodes: List<Node<Decision, Constraint>>) = nodes.map { u -> variable(literal(u)) }

@JvmName("variablesOfClause")
fun variables(clause: Clause) = clause.map { l -> variable(l) }

fun literal(node: Node<Decision, Constraint>): Literal = node.value.literal
fun variable(literal: Literal): String = literal.variable

fun cspAnalyzeConflict(
    conflictNode: Node<Decision, Constraint>,
    explain: (Node<Decision, Constraint>) -> Clause
) {

    var cl = explain(conflictNode)
    println("cl := $cl")
    var pred = predecessors(conflictNode)
    println("pred := $pred")
    var front = relevant(pred, cl)
    println("front := $front")
    while (!stopCriterionMet(front)) {
        println("-------------------")
        val currNode = lastNode(front)
        println("curr-node := $currNode")
        front = front - currNode
        println("front := $front")
        val expl = explain(currNode)
        println("expl := $expl")
        cl = resolve(cl, expl, variable(literal(currNode)))
        println("cl := $cl")
        pred = predecessors(currNode)
        println("pred := $pred")
        front = distinct(relevant(pred + front, cl))
        println("front := $front")
    }
    println("-------------------")
    println("add($cl)")

}

fun predecessors(node: Node<Decision, Constraint>): List<Node<Decision, Constraint>> =
    node.inComingEdges.map { edge ->
        edge.from
    }

fun relevant(nodes: List<Node<Decision, Constraint>>, clause: Clause): List<Node<Decision, Constraint>> {
    val clauseVariables = variables(clause)
    return nodes.filter { node ->
        variable(literal(node)) in clauseVariables
    }
}

fun stopCriterionMet(front: List<Node<Decision, Constraint>>): Boolean {
    fun maxByDl(nodes: List<Node<Decision, Constraint>>): Pair<Int, List<Node<Decision, Constraint>>> {
        var dl = 0
        val maxDLNodes = mutableListOf<Node<Decision, Constraint>>()

        for (node in nodes) {
            if (node.value.level > dl) {
                dl = node.value.level
                maxDLNodes.clear()
            }
            if (node.value.level == dl) {
                maxDLNodes.add(node)
            }
        }

        return dl to maxDLNodes
    }

    val (dl, maxDLNodes) = maxByDl(front)
    return (maxDLNodes.size == 1 || dl == 0 && front.none { node -> node.inComingEdges.isNotEmpty() })
}

fun lastNode(nodes: List<Node<Decision, Constraint>>) = nodes[nodes.lastIndex]

fun resolve(clause: Clause, expl: Clause, variable: String): Clause {
    val toSolve = (clause + expl).filter { it.variable == variable }
    val simplification = toSolve.reduce()
    val rest = (clause + expl).filter { it.variable != variable }
    return rest.merge() + simplification
}

fun distinct(nodes: List<Node<Decision, Constraint>>): List<Node<Decision, Constraint>> {
    val result = nodes.toMutableList()
    var i = result.lastIndex
    while (i > 0) {
        var j = i - 1
        while (j >= 0) {
            if (result[j] == result[i]) {
                result.removeAt(j)
                i -= 1
            } else {
                j -= 1
            }
        }
        i-= 1
    }
    return result
}