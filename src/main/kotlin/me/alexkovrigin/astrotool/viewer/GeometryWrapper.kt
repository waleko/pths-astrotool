package me.alexkovrigin.astrotool.viewer

import javafx.scene.image.Image
import javafx.scene.paint.Color
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory

data class GeometryWrapper(
    var geometry: Geometry,
    var color: Color = Color.BLACK,
    var width: Double = 0.2,
    var image: Image? = null
)

fun Image.toWrapper(pos: Coordinate, gf: GeometryFactory = GeometryFactory())
        = GeometryWrapper(gf.createLinearRing(
    arrayOf(
        pos.copy(),
        Coordinate(pos.x + this.width, pos.y),
        Coordinate(pos.x + this.width, pos.y + this.height),
        Coordinate(pos.x, pos.y + this.height),
        pos.copy()
    )
), image = this)
