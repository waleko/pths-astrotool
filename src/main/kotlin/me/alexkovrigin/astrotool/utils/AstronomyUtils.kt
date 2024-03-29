package me.alexkovrigin.astrotool.utils

import me.alexkovrigin.astrotool.viewer.GeometryWrapper
import org.locationtech.jts.geom.Coordinate

object AstronomyUtils {
    fun mouseToStarCoordinate(
        position: Coordinate,
        gw: GeometryWrapper,
        cameraParameters: CameraParameters
    ): StarCoordinate {
        val image = gw.image ?: error("No image in GeometryWrapper")
        cameraParameters.validateImage(image)
        return cameraParameters.getStarCoordinate(position)
    }

    fun starCoordinateToMouse(
        starCoordinate: StarCoordinate,
        gw: GeometryWrapper,
        cameraParameters: CameraParameters
    ): Coordinate {
        val image = gw.image ?: error("No image in GeometryWrapper")
        cameraParameters.validateImage(image)
        return cameraParameters.getCoordinate(starCoordinate)
    }
}