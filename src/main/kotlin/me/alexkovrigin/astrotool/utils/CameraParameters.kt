package me.alexkovrigin.astrotool.utils

import javafx.scene.image.Image
import org.locationtech.jts.geom.Coordinate

data class CameraParameters(
    val imageWidth: Double,
    val imageHeight: Double,
    val azimuths: Map<Int, Double>,
    val heights: Map<Int, Double>
) {

    private val azk: List<Int>
        get() = azimuths.toSortedMap().keys.toList()
    private val azv: List<Double>
        get() = azimuths.toSortedMap().values.toList().map { it + 176.9 - 28.168 }
    private val hsk: List<Int>
        get() = heights.toSortedMap().keys.toList().map { it + 20 }
    private val hsv: List<Double>
        get() = heights.toSortedMap().values.toList()

    fun isValidImage(image: Image): Boolean {
        return image.width == imageWidth && image.height == imageHeight
    }

    fun validateImage(image: Image) {
        if (!isValidImage(image))
            throw AssertionError("Incorrect image size (expected: ${imageWidth}x${imageHeight}, got ${image.width}x${image.height}")
    }

    private fun getXToAzimuth(x: Double): Pair<Double, Double> {
        val p = getNearestTwoIndexes(azk, x.toInt())
        return linearTransform(azk, azv, p)
    }
    private fun getAzToX(x: Double): Pair<Double, Double> {
        val p = getNearestTwoIndexes(azv, x)
        return linearTransform(azv, azk, p)
    }
    private fun getYToHeight(y: Double): Pair<Double, Double> {
        val p = getNearestTwoIndexes(hsk, y.toInt())
        return linearTransform(hsk, hsv, p)
    }
    private fun getHeightToY(y: Double): Pair<Double, Double> {
        val p = getNearestTwoIndexes(hsv, y)
        return linearTransform(hsv, hsk, p)
    }

    fun getStarCoordinate(coordinate: Coordinate): StarCoordinate {
        return StarCoordinate(linearValue(coordinate.x, getXToAzimuth(coordinate.x)), linearValue(coordinate.y, getYToHeight(coordinate.y)))
    }

    fun getCoordinate(starCoordinate: StarCoordinate): Coordinate {
        return Coordinate(linearValue(starCoordinate.azimuth, getAzToX(starCoordinate.azimuth)), linearValue(starCoordinate.height, getHeightToY(starCoordinate.height)))
    }

    companion object {
        private fun linearTransform(x1: Double, x2: Double, y1: Double, y2: Double) : Pair<Double, Double> {
            val k = (y2 - y1) / (x2 - x1)
            val b = y1 - x1 * k
            return k to b
        }

        private fun linearTransform(a: List<Number>, b: List<Number>, pair: Pair<Int, Int>)
            = linearTransform(
            a[pair.first].toDouble(),
            a[pair.second].toDouble(),
            b[pair.first].toDouble(),
            b[pair.second].toDouble()
        )

        private fun linearValue(x: Double, params: Pair<Double, Double>) = x * params.first + params.second

        private fun <T: Comparable<T>> getNearestTwoIndexes(list: Collection<T>, value: T): Pair<Int, Int> {
            require(list.size >= 2)
            var idx = list.toList().indexOfLast {it <= value} // FIXME: troubles with zero
            if (idx == -1)
                idx = 0
            if (idx == list.size - 1)
                idx--
            return idx to idx + 1
        }
    }
}