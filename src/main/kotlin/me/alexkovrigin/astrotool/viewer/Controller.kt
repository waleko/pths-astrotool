package me.alexkovrigin.astrotool.viewer

import io.jenetics.jpx.GPX
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.FileChooser
import org.locationtech.jts.geom.*
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import org.locationtech.jts.util.GeometricShapeFactory
import me.alexkovrigin.astrotool.OcadParser
import ru.kotlin.ogps.ocad.parser.binary.OcadVertex
import ru.kotlin.ogps.ocad.parser.binary.TOcadObject
import ru.kotlin.ogps.ocad.parser.corrector.*
import ru.kotlin.ogps.ocad.parser.isolines.IIsoline
import ru.kotlin.ogps.ocad.parser.isolines.Isoline
import ru.kotlin.ogps.ocad.parser.isolines.IsolineContainer
import ru.kotlin.ogps.ocad.parser.utils.Constants.DEFAULT_ROAD_SYMBOLS
import ru.kotlin.ogps.ocad.parser.utils.GeographyUtils
import ru.kotlin.ogps.ocad.parser.utils.GeomUtils.rotateCoordinate
import java.io.*
import java.lang.AssertionError
import java.net.URL
import java.util.*
import kotlin.math.PI
import kotlin.math.pow

class Controller : Initializable {

    private val gf = GeometryFactory()
    var lines = IsolineContainer(gf)
    private var ocadParser: OcadParser = OcadParser()
    var border: LinearRing? = null
    private val renderer = Renderer()
    private var drawer = Drawer(gf)
    private var mousePosition = Coordinate(0.0, 0.0)
    private var lastOpenedDir: File = File(".")
    private var coordinateCorrector = CoordinateCorrector(lines, gf)

    @FXML
    lateinit var gridPane: GridPane

    @FXML
    lateinit var statusText: Text

    @FXML
    lateinit var progressBar: ProgressBar

    @FXML
    lateinit var display: Canvas

    @FXML
    lateinit var displayAnchorPane: AnchorPane

    @FXML
    lateinit var infoLabel: Label

    private fun getLineRingForBorder(obj: TOcadObject, gf: GeometryFactory): LinearRing? {
        val vertices: ArrayList<OcadVertex> = obj.vertices
        if (vertices.isNotEmpty()) {
            val firstVertex: OcadVertex = vertices[0]
            val lastVertex: OcadVertex = vertices[vertices.size - 1]
            if (firstVertex != lastVertex) {
                vertices.add(firstVertex)
            }
            val coordinates: Array<Coordinate> = vertices.toArray(arrayOfNulls<OcadVertex>(vertices.size))
            return gf.createLinearRing(coordinates)
        }
        return null
    }

    private fun redraw() {
        val geometry: List<GeometryWrapper> = drawer.draw(lines)
        renderer.clear()
        renderer.addAll(geometry)
        border?.let {
            renderer.add(drawer.draw(it, Color.RED, 2))
        }
    }

    private fun render() {
        val gc = display.graphicsContext2D
        renderer.render(gc, display.width, display.height)
    }

    fun openOcadAction(actionEvent: ActionEvent) {
        val fileChooser = FileChooser()
        fileChooser.title = "Open ocad map"
        fileChooser.initialDirectory = lastOpenedDir
        val ocadFile = fileChooser.showOpenDialog(null)
        if (ocadFile != null) {
            lastOpenedDir = ocadFile.parentFile
            try {
                progressBar.progress = 0.0
                lines = IsolineContainer(gf)
                ocadParser.loadOcad(ocadFile) { done: Int, max: Int ->
                    progressBar.progress = done.toDouble() / max
                }
                val isolines: ArrayList<IIsoline> = ocadParser.linesAsIsolines(gf)
                lines.clear()
                lines.addAll(isolines)
                coordinateCorrector = CoordinateCorrector(lines, gf)

                // TODO: remove
                showTracks()

                border = ocadParser.border?.let { getLineRingForBorder(it, gf) }
                val envelope = lines.envelope
                statusText.text = "Added ${lines.size} isolines. Bounding box: minX=${envelope.minX}, minY=${envelope.minY}, maxX=${envelope.maxX}, maxY=${envelope.maxY}"
                redraw()
                renderer.fit()
                render()
            } catch (e: FileNotFoundException) {
                statusText.text = "File not found"
            } catch (e: Exception) {
                statusText.text = "File load error: " + e.message
                println(e)
            }
        }
    }

    private fun showTracks() {
        val dir = "./sample-events/2017-02-18/gpx/"

        val gpxs = File(dir)
            .walkTopDown()
            .filter { it.isFile && it.extension == "gpx" }
            .map { GPX.read(it.toPath()) }
            .toList()

        // TODO: take forEach instead of first
        gpxs.first().tracks.first().let { track ->
            println("Now processing ${track.name}")

            track.segments.first().let { segment ->
                val absoluteCoords = segment.points.map {
                    GeographyUtils.toUTM(it.latitude.toDegrees(), it.longitude.toDegrees())
                }

                val relativeCoords = absoluteCoords.map {
                    // Get relative coordinates
                    Coordinate(it.first - ocadParser.mapCoordinate.x, it.second - ocadParser.mapCoordinate.y)
                }

                val convertedCoords = relativeCoords.map {
                    rotateCoordinate(it, ocadParser.mapAngle * PI / 180)
                }

                val correctedCoords = coordinateCorrector.correctAll(convertedCoords)
                val correctedIsoline = Isoline(290701, CoordinateArraySequence(correctedCoords.toTypedArray()), gf)
                val baseIsoline = Isoline(290702, CoordinateArraySequence(convertedCoords.toTypedArray()), gf)

                lines.add(correctedIsoline)
                lines.add(baseIsoline)
            }
        }
    }

    fun exitAction(actionEvent: ActionEvent) {
        Platform.exit()
    }

    fun canvasMouseEntered(mouseEvent: MouseEvent) {
        updateMouseInfo(mouseEvent)
    }

    fun canvasMouseMove(mouseEvent: MouseEvent) {
        updateMouseInfo(mouseEvent)
    }

    private fun updateMouseInfo(mouseEvent: MouseEvent) {
        mousePosition.x = mouseEvent.x
        mousePosition.y = mouseEvent.y
        val position = Coordinate(mousePosition)
        renderer.screenToLocal(position, display.width, display.height)
        infoLabel.text = "(${mouseEvent.x.toInt()}, ${mouseEvent.y.toInt()}) - (${position.x}, ${position.y})"

        if (lines.envelope.intersects(position)) {
            val td = 15.0
            val closest = coordinateCorrector.correct(position, terminateDistance = td)
            infoLabel.text += "; closest is (${closest.x}, ${closest.y})"

            redraw()
            renderer.add(GeometryWrapper(createCircle(position, td), Color.GREEN, 1.0))
            renderer.add(GeometryWrapper(createCircle(closest, td), Color.RED, 1.0))
            render()
        }
    }

    private fun createCircle(position: Coordinate, radius: Double = 1.0): Polygon {
        val shapeFactory = GeometricShapeFactory()
        shapeFactory.setNumPoints(32)
        shapeFactory.setCentre(position)
        shapeFactory.setSize(radius * 2)
        return shapeFactory.createCircle()
    }

    @FXML
    fun canvasScroll(event: ScrollEvent) {
        // Rescale map
        val delta = event.deltaY + event.deltaX
        val localMousePos = Coordinate(mousePosition)
        renderer.screenToLocal(localMousePos, display.width, display.height)
        renderer.rescale(localMousePos, 0.995.pow(delta))
        render()
    }

    fun canvasMouseDown(mouseEvent: MouseEvent) {
        // TODO
    }

    fun canvasMouseUp(mouseEvent: MouseEvent) {
        // TODO
    }

    abstract inner class BackgroundTask(private val taskName: String) : Task<String?>() {
        override fun call(): String? {
            callWithProgress { workDone: Int, max: Int ->
                this.updateProgress(workDone.toLong(), max.toLong())
            }
            updateProgress(100, 100)
            return null
        }

        override fun failed() {
            super.failed()
            updateProgress(0, 1)
            statusText.text = "$taskName failed"
        }

        override fun succeeded() {
            super.succeeded()
            updateProgress(1, 1)
        }

        override fun cancelled() {
            super.cancelled()
            updateProgress(0, 1)
            statusText.text = "$taskName cancelled"
        }

        abstract fun callWithProgress(reportProgress: ((done: Int, max: Int) -> Unit)?)

    }

    private fun executeAsBackgroundTask(task: BackgroundTask): Thread? {
        progressBar.progressProperty().unbind()
        progressBar.progressProperty().bind(task.progressProperty())
        val thread = Thread(task)
        thread.start()
        return thread
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // redraw properly on resizing
        val listener = InvalidationListener {
            render()
        }
        display.widthProperty().bind(displayAnchorPane.widthProperty())
        display.heightProperty().bind(displayAnchorPane.heightProperty())
        display.widthProperty().addListener(listener)
        display.heightProperty().addListener(listener)
    }
}