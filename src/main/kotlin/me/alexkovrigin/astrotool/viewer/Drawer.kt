package me.alexkovrigin.astrotool.viewer

import javafx.scene.paint.Color
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import ru.kotlin.ogps.ocad.parser.isolines.IIsoline
import ru.kotlin.ogps.ocad.parser.isolines.IsolineContainer
import ru.kotlin.ogps.ocad.parser.utils.Constants
import ru.kotlin.ogps.ocad.parser.utils.Constants.DEFAULT_ROAD_SYMBOLS
import ru.kotlin.ogps.ocad.parser.utils.Constants.DEFAULT_STOP_SYMBOLS
import ru.kotlin.ogps.ocad.parser.utils.LineStringInterpolatedPointIterator
import java.util.*

class Drawer(var gf: GeometryFactory) {

    /**
     * Converts IIsoline Container to geometry
     * @param isolines
     * @return
     */
    fun draw(isolines: IsolineContainer): List<GeometryWrapper> {
        val geom = ArrayList<GeometryWrapper>()
        for (line in isolines) {

            val isGpxTrack = line.type in 290701..290702

            // hide non-roads
            if (!DEFAULT_ROAD_SYMBOLS.contains(line.type) && !DEFAULT_STOP_SYMBOLS.contains(line.type) && !isGpxTrack)
                continue

            val ls = line.lineString

            val col = when (line.type) {
                106002 -> Color.GRAY
                in 103000..103999 -> Color.GRAY
                in 101000..101999 -> Color.BROWN
                in 102000..102999 -> Color.BROWN
                in 106000..106999 -> Color.BROWN
                in 300000..320999 -> Color.BLUE
                in 500000..520999 -> Color.BLACK
                in 800000..810999 -> Color.GREEN
                // corrected route
                290701 -> Color.RED
                // original route
                290702 -> Color.AQUA
                else -> Color.GRAY
            }

            val width = Constants.DRAWING_LINE_WIDTH * (if(isGpxTrack) 2 else 1)

            geom.add(GeometryWrapper(ls, col, /*line.type * */width))
            geom.add(GeometryWrapper(gf.createPoint(ls.getCoordinateN(0)), col, width))
        }
        return geom
    }

    fun drawGeometry(geometries: Collection<LineString>, color: Color): List<GeometryWrapper> {
        val gws: MutableList<GeometryWrapper> = ArrayList()
        for (g in geometries) {
            gws.add(GeometryWrapper(g, color, 1.0))
        }
        return gws
    }

    fun draw(geometry: Geometry, color: Color, width: Int): GeometryWrapper {
        return GeometryWrapper(geometry, color, width.toDouble())
    }

    private fun interpolatedLine(ls: LineString, step: Double): LineString {
        val it = LineStringInterpolatedPointIterator(ls, step, 0.0)
        val coordsList = LinkedList<Coordinate>()
        while (it.hasNext()) {
            val c = it.next()
            coordsList.add(c)
        }
        return gf.createLineString(coordsList.toTypedArray())
    }

    fun draw(line: IIsoline, color: Color, width: Double): List<GeometryWrapper> {
        val geom = ArrayList<GeometryWrapper>()
        val ls = line.lineString
        geom.add(
            GeometryWrapper(ls, color, line.type / 100000 * Constants.DRAWING_LINE_WIDTH * width
            )
        )
        val d = (line.type / 100000).toDouble() * Constants.DRAWING_POINT_WIDTH
        geom.add(GeometryWrapper(gf.createPoint(ls.getCoordinateN(0)), color, d * width))
        return geom
    }

}