package me.alexkovrigin.astrotool.viewer

import javafx.scene.paint.Color
import org.locationtech.jts.geom.Geometry

data class GeometryWrapper(
    var geometry: Geometry,
    var color: Color = Color.BLACK,
    var width: Double = 0.2
)