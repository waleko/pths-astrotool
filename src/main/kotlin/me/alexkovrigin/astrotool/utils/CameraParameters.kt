package me.alexkovrigin.astrotool.utils

import javafx.scene.image.Image
import org.locationtech.jts.geom.Coordinate

data class CameraParameters(
    val imageWidth: Double,
    val imageHeight: Double,
    val azimuths: Map<Int, Double>,
    val heights: Map<Int, Double>
) {
    fun isValidImage(image: Image): Boolean {
        return image.width == imageWidth && image.height == imageHeight
    }

    fun validateImage(image: Image) {
        if (!isValidImage(image))
            throw AssertionError("Incorrect image size (expected: ${imageWidth}x${imageHeight}, got ${image.width}x${image.height}")
    }

    fun getStarCoordinates(coordinate: Coordinate): StarCoordinate {
        TODO()
//        return StarCoordinate(azimuths.values.random(), heights.values.random())
    }
}