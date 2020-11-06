package me.alexkovrigin.astrotool.viewer

import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import org.locationtech.jts.geom.*
import java.lang.Double.min
import java.util.*
import kotlin.math.max

class Renderer {
    private var wrappers: MutableList<GeometryWrapper> = ArrayList()
    private val center: Coordinate = Coordinate(0.0, 0.0)

    var scale = 0.0
    fun getCenter(): Coordinate {
        return Coordinate(center)
    }

    fun setCenter(x: Double, y: Double) {
        center.x = x
        center.y = y
    }

    fun setCenter(c: Coordinate) {
        center.x = c.x
        center.y = c.y
    }

    /**
     * Rescale viewport with pivot as center
     * @param pivot
     * @param scale_delta
     * @return
     */
    fun rescale(pivot: Coordinate, scale_delta: Double) {
        center.x -= pivot.x
        center.y -= pivot.y
        scale *= scale_delta
        center.x *= scale_delta
        center.y *= scale_delta
        center.x += pivot.x
        center.y += pivot.y
    }

    fun add(gw: GeometryWrapper) {
        wrappers.add(gw)
    }

    fun addAll(geometry: Collection<GeometryWrapper>) {
        wrappers.addAll(geometry)
    }

    fun clear() {
        wrappers.clear()
    }

    fun screenToLocal(c: Coordinate, screenWidth: Double, screenHeight: Double) {
        val correctionScale = min(screenWidth, screenHeight)
        val correctionShift = Coordinate(screenWidth * 0.5, screenHeight * 0.5)
        c.y = screenHeight - c.y
        transform(c, correctionShift, correctionScale)
        reverseTransform(c, center, scale)
    }

    fun localToScreen(c: Coordinate, screenWidth: Double, screenHeight: Double) {
        val correctionScale = min(screenWidth, screenHeight)
        val correctionShift = Coordinate(screenWidth * 0.5, screenHeight * 0.5)
        correctionTransform(c, correctionShift, correctionScale)
        c.y = screenHeight - c.y
    }

    /**
     * Transform point's x and y to range -1,1 (onscreen coordinates)
     * @param c
     * @param center
     * @param scale
     */
    private fun transform(c: Coordinate, center: Coordinate, scale: Double) {
        c.x = (c.x - center.x) / scale
        c.y = (c.y - center.y) / scale
    }

    /**
     * Transform point's x and y from -1,1 (onscreen coordinates) to local
     * @param c
     * @param center
     * @param scale
     */
    private fun reverseTransform(c: Coordinate, center: Coordinate, scale: Double) {
        c.x = c.x * scale + center.x
        c.y = c.y * scale + center.y
    }

    fun fit() {
        val bbox = Envelope()
        for (g in wrappers) {
            bbox.expandToInclude(g.geometry.envelopeInternal)
        }
        if (bbox.centre() != null) {
            this.setCenter(bbox.centre())
        }
        scale = max(bbox.width, bbox.height)
    }

    private fun render(
        geoWrappers: List<GeometryWrapper>, graphicsContext: GraphicsContext,
        canvasWidth: Double, canvasHeight: Double
    ) {
        val correctionScale = min(canvasWidth, canvasHeight)
        val correctionShift = Coordinate(canvasWidth * 0.5, canvasHeight * 0.5)
        for (wrapper in geoWrappers) {
            wrapper.image?.let {
                renderImage(
                    it,
                    graphicsContext,
                    wrapper.geometry.envelopeInternal,
                    correctionShift,
                    correctionScale,
                    canvasHeight
                )
            }
            val geometry = wrapper.geometry
            val width = wrapper.width
            val color = wrapper.color
            renderGeometry(geometry, graphicsContext, width, color, correctionShift, correctionScale, canvasHeight)
        }
    }

    private fun renderImage(
        image: Image, graphicsContext: GraphicsContext, envelope: Envelope,
        correctionShift: Coordinate, correctionScale: Double, canvasHeight: Double
    ) {
        val pos1 = envelope.run { Coordinate(minX, minY) }
        val pos3 = envelope.run { Coordinate(maxX, maxY) }
        val pos2 = envelope.run { Coordinate(minX, maxY) }
        correctionTransform(pos1, correctionShift, correctionScale)
        correctionTransform(pos3, correctionShift, correctionScale)
        correctionTransform(pos2, correctionShift, correctionScale)
        val newImage = Image(image.url, (pos3.x - pos1.x), (pos3.y - pos1.y), false, false)
        graphicsContext.drawImage(newImage, pos2.x, canvasHeight - pos2.y)
    }

    private fun renderGeometry(
        geometry: Geometry, graphicsContext: GraphicsContext, width: Double, color: Color,
        correctionShift: Coordinate, correctionScale: Double, canvasHeight: Double
    ) {
        graphicsContext.lineWidth = width
        graphicsContext.stroke = color
        val string: LineString = when (geometry) {
            is LineString -> geometry
            is Polygon -> geometry.exteriorRing
            else -> null
        } ?: return
        val coords = string.coordinateSequence
        val c1 = Coordinate(coords.getCoordinate(0))
        correctionTransform(c1, correctionShift, correctionScale)
        if (coords.size() == 1) {
            graphicsContext.fill = color
            graphicsContext.fillOval(c1.x, canvasHeight - c1.y, width, width)
            return
        }
        val c2 = Coordinate()
        for (i in 1 until coords.size()) {
            c2.x = coords.getX(i)
            c2.y = coords.getY(i)
            correctionTransform(c2, correctionShift, correctionScale)
            graphicsContext.strokeLine(c1.x, canvasHeight - c1.y, c2.x, canvasHeight - c2.y)
            c1.x = c2.x
            c1.y = c2.y
        }
    }

    fun render(graphicsContext: GraphicsContext, canvasWidth: Double, canvasHeight: Double) {
        graphicsContext.fill = Color.WHITE
        graphicsContext.fillRect(0.0, 0.0, canvasWidth, canvasHeight)
        render(wrappers, graphicsContext, canvasWidth, canvasHeight)
    }

    fun render(geometry: Geometry, graphicsContext: GraphicsContext, canvasWidth: Double, canvasHeight: Double) {
        val correctionScale = min(canvasWidth, canvasHeight)
        val correctionShift = Coordinate(canvasWidth * 0.5, canvasHeight * 0.5)
        val width = 0.2
        val color = Color.BLACK
        renderGeometry(geometry, graphicsContext, width, color, correctionShift, correctionScale, canvasHeight)
    }

    private fun correctionTransform(c: Coordinate, correctionShift: Coordinate, correctionScale: Double) {
        transform(c, center, scale)
        reverseTransform(c, correctionShift, correctionScale)
    }
}