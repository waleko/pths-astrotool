package me.alexkovrigin.astrotool.viewer

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
import me.alexkovrigin.astrotool.isolines.IsolineContainer
import org.locationtech.jts.geom.*
import org.locationtech.jts.util.GeometricShapeFactory
import java.io.*
import java.net.URL
import java.util.*
import kotlin.math.pow

class Controller : Initializable {

    private val gf = GeometryFactory()
    var lines = IsolineContainer(gf)
    var border: LinearRing? = null
    private val renderer = Renderer()
    private var drawer = Drawer(gf)
    private var mousePosition = Coordinate(0.0, 0.0)
    private var lastOpenedDir: File = File(".")

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
                // TODO load picture
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
            infoLabel.text += "; closest is (${position.x}, ${position.y})"

            redraw()
            renderer.add(GeometryWrapper(createCircle(position, td), Color.RED, 1.0))
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