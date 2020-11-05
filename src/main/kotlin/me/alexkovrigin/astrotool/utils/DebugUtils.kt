package me.alexkovrigin.astrotool.utils

/**
 * Used in debug. Should be removed on release
 */
object DebugUtils {
    // Isoline id counter. (constructor of isoline increments this value by id and uses new value as isoline ID)
    var isolineLastId = -1
    var skipExternalSimplification = false
    var skipBuildingMap = false
    var showDebugInfo = true
    var showTracks = true
    var printMouseCoordinateOnClick = true
}