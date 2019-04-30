package refactor

sealed class Cell {

    abstract var top: Cell
    abstract var left: Cell
    abstract var right: Cell
    abstract var bottom: Cell

}

class Root : Cell() {
    override var top = this as Cell
    override var left = this as Cell
    override var right = this as Cell
    override var bottom = this as Cell
}

interface Removable {
    fun remove()
}

interface Restorable {
    fun restore()
}

sealed class Header(
    val index: Int
) : Cell(), Removable, Restorable

class Row private constructor(
    index: Int,
    override var top: Cell
) : Header(index) {
    constructor(top: Root) : this(0, top as Cell)
    constructor(top: Row) : this(top.index + 1, top as Cell)

    override var bottom = top.bottom
    override var left = this as Cell
    override var right = this as Cell

    init {
        top.bottom = this
        bottom.top = this
    }

    override fun remove() {
        var cell: Cell = this
        do {
            cell.top.bottom = cell.bottom
            cell.bottom.top = cell.top
            cell = cell.right
        } while (cell != this)
    }

    override fun restore() {
        var cell: Cell = this
        do {
            cell.top.bottom = cell
            cell.bottom.top = cell
            cell = cell.left
        } while (cell != this)
    }
}

class Column private constructor(
    index: Int,
    override var left: Cell
) : Header(index) {
    constructor(left: Root) : this(0, left as Cell)
    constructor(left: Column) : this(left.index + 1, left as Cell)

    override var right = left.right
    override var top = this as Cell
    override var bottom = this as Cell

    init {
        left.right = this
        right.left = this
    }

    override fun remove() {
        var cell: Cell = this
        do {
            cell.left.right = cell.right
            cell.right.left = cell.left
            cell = cell.bottom
        } while (cell != this)
    }

    override fun restore() {
        var cell: Cell = this
        do {
            cell.left.right = cell
            cell.right.left = cell
            cell = cell.top
        } while (cell != this)
    }
}

class Data private constructor(
    val equation: Int,
    val variable: Int,
    left: Cell?,
    top: Cell?
) : Cell() {

    constructor(equation: Int, variable: Int, left: Row, top: Column): this(equation, variable, left as Cell, top as Cell)
    constructor(equation: Int, variable: Int, left: Data, top: Column): this(equation, variable, left as Cell, top as Cell)
    constructor(equation: Int, variable: Int, left: Row, top: Data): this(equation, variable, left as Cell, top as Cell)
    constructor(equation: Int, variable: Int, left: Data, top: Data): this(equation, variable, left as Cell, top as Cell)

    override var top = top ?: this
    override var left = left ?: this
    override var right = this.left.right
    override var bottom = this.top.bottom

    init {
        this.top.bottom = this
        this.bottom.top = this
        this.left.right = this
        this.right.left = this
    }

}