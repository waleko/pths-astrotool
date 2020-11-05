package me.alexkovrigin.astrotool.isolines

import org.locationtech.jts.geom.*
import me.alexkovrigin.astrotool.utils.DebugUtils
import java.io.Serializable

class Isoline : IIsoline, Serializable {
    override val lineString: LineString
    override var type: Int
        private set

    override var id: Int
    override var isEdgeToEdge: Boolean
    override var height: Double

    constructor(type: Int, cs: CoordinateSequence?, gf: GeometryFactory?) {
        lineString = LineString(cs, gf)
        id = ++DebugUtils.isolineLastId
        this.type = type
        isEdgeToEdge = false
        height = 0.0
    }

    constructor(other: IIsoline) {
        lineString = LineString(
            other.lineString.coordinateSequence,
            other.lineString.factory
        )
        id = ++DebugUtils.isolineLastId
        type = other.type
        isEdgeToEdge = other.isEdgeToEdge
        height = other.height
    }

    override val isClosed: Boolean
        get() = lineString.isClosed

    override val geometry: Geometry
        get() = lineString

    override val factory: GeometryFactory?
        get() = lineString.factory

    override fun hashCode(): Int {
        val p1: Coordinate = lineString.getCoordinateN(0)
        val p2: Coordinate = lineString.getCoordinateN(lineString.numPoints - 1)
        return (31 * java.lang.Double.hashCode(p1.x) - 51 * java.lang.Double.hashCode(p1.y) +
               (31 * java.lang.Double.hashCode(p2.x) - 51 * java.lang.Double.hashCode(p2.y))) +
                129 * type

    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Isoline) return false
        return equals(other)
    }

    fun equals(other: Isoline): Boolean {
        if (lineString.numPoints != other.lineString.numPoints) return false
        if (type != other.type) return false
        var exactMatch = true
        var reversedMatch = true
        for (i in 0 until lineString.numPoints) {
            if (lineString.getCoordinateN(i) != other.lineString.getCoordinateN(i)) {
                exactMatch = false
                break
            }
        }
        for (i in 0 until lineString.numPoints) {
            if (lineString.getCoordinateN(i) != other.lineString.getCoordinateN(lineString.numPoints - 1 - i)) {
                reversedMatch = false
                break
            }
        }
        return (exactMatch || reversedMatch)
    }

    override fun toString(): String {
        val ls: LineString = lineString
        val end: Int = lineString.numPoints - 1
        val startX: Double = ls.getCoordinateN(0).x
        val startY: Double = ls.getCoordinateN(0).y
        val endX: Double = ls.getCoordinateN(end).x
        val endY: Double = ls.getCoordinateN(end).y
        return "ISOLINE_" + type + "_h=" + height + "(" + startX + "," + startY + " - " + endX + "," + endY + ")"
    }

    override val isSteep: Boolean
        get() = type in 106000..106999 && type != 106002

    override val isHalf: Boolean
        get() = type in 103000..103999

}