package me.alexkovrigin.astrotool.utils

import org.locationtech.jts.geom.*
import org.locationtech.jts.math.Vector2D
import org.locationtech.jts.operation.distance.DistanceOp
import org.locationtech.jts.util.GeometricShapeFactory
import kotlin.math.*

/**
 * Class, containing static 2D geometry functions
 */
object GeomUtils {

    private fun crossProduct(v0x: Double, v0y: Double, v1x: Double, v1y: Double): Double {
        return v0x * v1y - v0y * v1x
    }

    /**
     * unsigned area of triangle
     * @param c0
     * @param c1
     * @param c2
     * @return
     */
    fun area(c0: Coordinate, c1: Coordinate, c2: Coordinate): Double {
        val v0x = c2.x - c0.x
        val v0y = c2.y - c0.y
        val v1x = c1.x - c0.x
        val v1y = c1.y - c0.y
        return abs(crossProduct(v0x, v0y, v1x, v1y))
    }

    /**
     * Returns length fraction of closest point along [LineString] lineString to [Coordinate] c
     * @param c
     * @param lineString
     * @return length fraction
     */
    fun projectionFactor(c: Coordinate, lineString: LineString): Double {
        val lineSegment = LineSegment()
        var minDist = c.distance(lineString.getCoordinateN(0))
        var minLength = 0.0
        var totalLength = 0.0
        for (i in 1 until lineString.numPoints) {
            lineSegment.p0 = lineString.getCoordinateN(i - 1)
            lineSegment.p1 = lineString.getCoordinateN(i)
            val segmentLength = lineSegment.length
            val distanceFromSegment = lineSegment.distance(c)
            if (distanceFromSegment < minDist) {
                minDist = distanceFromSegment
                minLength = totalLength + segmentLength * lineSegment.projectionFactor(c)
            }
            totalLength += segmentLength
        }
        return minLength / totalLength
    }

    fun clamp(value: Double, from: Double, to: Double): Double {
        return max(from, min(to, value))
    }

    fun clamp(value: Int, from: Int, to: Int): Int {
        return max(from, min(to, value))
    }

    fun clamp(value: Short, from: Short, to: Short): Short {
        return max(from.toInt(), min(to.toInt(), value.toInt())).toShort()
    }

    /**
     * map value from range (inMin-inMax) to range (outMin-outMax)
     * @param value value to be mapped
     * @param inMin first interval minimum value
     * @param inMax first interval maximum value
     * @param outMin second interval minimum value
     * @param outMax second interval maximum value
     * @return outMin + (value - inMin)*(outMax - outMin)/(inMax - inMin)
     */
    fun map(
        value: Double,
        inMin: Double,
        inMax: Double,
        outMin: Double,
        outMax: Double
    ): Double {
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin)
    }

}

val NULL_VECTOR = Vector2D(0.0, 0.0)

operator fun Vector2D.plus(v: Vector2D): Vector2D = add(v)
operator fun Vector2D.minus(v: Vector2D): Vector2D = subtract(v)
operator fun Vector2D.times(d: Double): Vector2D = multiply(d)
operator fun Vector2D.div(d: Double): Vector2D = divide(d)
/** Dot product*/
operator fun Vector2D.times(v: Vector2D) = dot(v)
/** (Pseudo) cross product**/
fun Vector2D.cross(v: Vector2D) = x * v.y - v.x * y
fun Vector2D.cosAngle(v: Vector2D) = normalize().dot(v.normalize())
fun Vector2D.projectionOn(v: Vector2D) = multiply(cosAngle(v))

fun GeometryFactory.createCircle(center: Coordinate, radius: Double = 1.0, numberOfPoints: Int = 32): Polygon {
    val shapeFactory = GeometricShapeFactory(this)
    shapeFactory.setNumPoints(numberOfPoints)
    shapeFactory.setCentre(center)
    shapeFactory.setSize(radius * 2)
    return shapeFactory.createCircle()
}

fun Geometry.forEachSubGeometry(block: (subGeometry: Geometry) -> Unit) {
    for (i in 0 until numGeometries) {
        block(getGeometryN(i))
    }
}

fun MultiLineString.toLineStringList(): List<LineString> =
    (0 until numGeometries).map { getGeometryN(it) as LineString }

fun Geometry.closestTo(coordinate: Coordinate, gf: GeometryFactory = GeometryFactory()) : Coordinate {
    if (isEmpty)
        return Coordinate(1e20, 1e20)
    return DistanceOp.nearestPoints(this, gf.createPoint(coordinate))[0]
}

fun Geometry.distanceToClosest(coordinate: Coordinate, gf: GeometryFactory = GeometryFactory()) : Double {
    if (isEmpty)
        return 1e20 * sqrt(2.0)
    return DistanceOp.distance(this, gf.createPoint(coordinate))
}