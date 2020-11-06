package me.alexkovrigin.astrotool.utils

import javafx.scene.image.Image
import org.locationtech.jts.geom.Coordinate

data class CameraParameters(
    val imageWidth: Double,
    val imageHeight: Double,
    val azimuths: Map<Int, Double>,
    val heights: Map<Int, Double>
) {

    private val x_az
        get() = linearTransform(
        azimuths.keys.min()!!.toDouble(),
        azimuths.keys.max()!!.toDouble(),
        azimuths.values.min()!!.toDouble(),
        azimuths.values.max()!!.toDouble()
    )

    private val az_x
        get() = linearTransform(
        azimuths.values.min()!!.toDouble(),
        azimuths.values.max()!!.toDouble(),
        azimuths.keys.min()!!.toDouble(),
        azimuths.keys.max()!!.toDouble()
    )

    private val y_he
        get() = linearTransform(
        heights.keys.min()!!.toDouble(),
        heights.keys.max()!!.toDouble(),
        heights.values.min()!!.toDouble(),
        heights.values.max()!!.toDouble()
    )

    private val he_y         
        get() = linearTransform(
        heights.values.min()!!.toDouble(),
        heights.values.max()!!.toDouble(),
        heights.keys.min()!!.toDouble(),
        heights.keys.max()!!.toDouble()
    )

    fun isValidImage(image: Image): Boolean {
        return image.width == imageWidth && image.height == imageHeight
    }

    fun validateImage(image: Image) {
        if (!isValidImage(image))
            throw AssertionError("Incorrect image size (expected: ${imageWidth}x${imageHeight}, got ${image.width}x${image.height}")
    }

    fun getStarCoordinate(coordinate: Coordinate): StarCoordinate {
        return StarCoordinate(linearValue(coordinate.x, x_az), linearValue(coordinate.y, y_he))
    }

    fun getCoordinate(starCoordinate: StarCoordinate): Coordinate {
        return Coordinate(linearValue(starCoordinate.azimuth, az_x), linearValue(starCoordinate.height, he_y))
    }

    companion object {
        private fun linearTransform(x1: Double, x2: Double, y1: Double, y2: Double) : Pair<Double, Double> {
            val k = (y2 - y1) / (x2 - x1)
            val b = y1 - x1 * k
            return k to b
        }

        private fun linearValue(x: Double, params: Pair<Double, Double>) = x * params.first + params.second
    }
}