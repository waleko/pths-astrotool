package me.alexkovrigin.astrotool.isolines

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString

interface IIsoline {
    val type: Int
    var height: Double
    val isClosed: Boolean
    val isSteep: Boolean
    val isHalf: Boolean
    var isEdgeToEdge: Boolean
    var id: Int
    val geometry: Geometry
    val lineString: LineString
    val factory: GeometryFactory?
}

fun Collection<IIsoline>.toIsolineContainer(gf: GeometryFactory = GeometryFactory()) = IsolineContainer(gf, this)