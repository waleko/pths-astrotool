package me.alexkovrigin.astrotool.viewer

import javafx.scene.image.Image
import javafx.scene.paint.Color
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.io.File

data class GeometryWrapper(
    var geometry: Geometry,
    var color: Color = Color.BLACK,
    var width: Double = 0.2,
    val imageFile: File? = null
) {
    val image: Image?
        get() = if (imageFile != null) Image(imageFile.toURL().toString()) else null
}

fun File.toWrapper(pos: Coordinate, gf: GeometryFactory = GeometryFactory()): GeometryWrapper {
    val image = Image(this.toURL().toString())
    return GeometryWrapper(gf.createLinearRing(
        arrayOf(
            pos.copy(),
            Coordinate(pos.x + image.width, pos.y),
            Coordinate(pos.x + image.width, pos.y + image.height),
            Coordinate(pos.x, pos.y + image.height),
            pos.copy()
        )
    ), imageFile = this)
}
