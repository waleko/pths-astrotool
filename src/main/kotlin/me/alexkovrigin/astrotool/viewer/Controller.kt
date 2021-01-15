package me.alexkovrigin.astrotool.viewer

import com.google.gson.Gson
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
import me.alexkovrigin.astrotool.utils.AstronomyUtils
import me.alexkovrigin.astrotool.utils.CameraParameters
import me.alexkovrigin.astrotool.utils.StarCoordinate
import org.locationtech.jts.geom.*
import org.locationtech.jts.util.GeometricShapeFactory
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.math.min
import kotlin.math.pow

class Controller : Initializable {

    private val gf = GeometryFactory()
    var lines = IsolineContainer(gf)
    var border: LinearRing? = null
    private val renderer = Renderer()
    private var drawer = Drawer(gf)
    private var mousePosition = Coordinate(0.0, 0.0)
    private var lastOpenedDir: File = File(".")
    private val geometryWrappers = mutableListOf<GeometryWrapper>()
    private val geometryWrappers1 = mutableListOf<GeometryWrapper>()
    private var envelope: Envelope = Envelope(Coordinate(0.0, 0.0))
    private val imagesQueue: Queue<File> = LinkedList<File>()

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
        val geometry: List<GeometryWrapper> = drawer.draw(lines) + geometryWrappers + geometryWrappers1
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

    private var awaitingInput = false

    private fun displayNextInQueue() {
        if (awaitingInput)
            return
        geometryWrappers.clear()
        if (imagesQueue.isNotEmpty()) {
            awaitingInput = true
            geometryWrappers.add(imagesQueue.poll().toWrapper(Coordinate(0.0, 0.0)).also {
                envelope = it.geometry.envelopeInternal
            })
        }
        redraw()
        renderer.fit()
        render()
    }

    private fun processImage(imageFile: File) {
        try {
            imagesQueue.add(imageFile)
            displayNextInQueue()
        } catch (e: FileNotFoundException) {
            statusText.text = "File not found"
        } catch (e: Exception) {
            statusText.text = "File load error: " + e.message
            println(e)
        }
    }

    fun openSinglePicture(actionEvent: ActionEvent) {
        val fileChooser = FileChooser()
        fileChooser.title = "Open image"
        fileChooser.initialDirectory = lastOpenedDir
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"))
        val imageFile = fileChooser.showOpenDialog(null)
        if (imageFile != null) {
            imagesQueue.clear()
            awaitingInput = false
            geometryWrappers.clear()
            geometryWrappers1.clear()
            lastOpenedDir = imageFile.parentFile
            processImage(imageFile)
        }
    }

    fun openFolder(actionEvent: ActionEvent) {
        val multiFileChooser = FileChooser()
        multiFileChooser.title = "Select input folder"
        multiFileChooser.initialDirectory = lastOpenedDir
        multiFileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"))
        val selection = multiFileChooser.showOpenMultipleDialog(null)
        if (selection != null && selection.isNotEmpty()) {
            imagesQueue.clear()
            awaitingInput = false
            geometryWrappers.clear()
            geometryWrappers1.clear()
            selection.sortedBy { it.lastModified() }.forEach { processImage(imageFile = it) }
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

        if (envelope.intersects(position)) {
            val radius = 10.0
            redraw()
            renderer.add(GeometryWrapper(createCircle(position, radius), Color.RED, 1.0))
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
        if (!awaitingInput)
            return
        awaitingInput = false

        val position = Coordinate(mousePosition)
        renderer.screenToLocal(position, display.width, display.height)
        val gw = geometryWrappers.first()
        cameraParameters?.let {
            val star = AstronomyUtils.mouseToStarCoordinate(
                position,
                gw,
                it
            )
            val lastModified = gw.imageFile?.lastModified() ?: error("No file")
            val lastModifiedString = Instant.ofEpochMilli(lastModified).toString()
            outputFile.appendText("$lastModifiedString;${star.azimuth};${star.height}\n")
        }
        displayNextInQueue()
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

    private var outputFile = File(System.getProperty("user.home"))
        .resolve("astrotool_${SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(Date())}.csv")

    fun chooseOutputfile(actionEvent: ActionEvent) {
        val fileChooser = FileChooser()
        fileChooser.title = "Select output file location"
        fileChooser.initialDirectory = lastOpenedDir
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV", "*.csv"))
        outputFile = fileChooser.showSaveDialog(null) ?: return
    }

    var cameraParameters: CameraParameters? = null

    fun chooseCameraFile(actionEvent: ActionEvent) {
        val fileChooser = FileChooser()
        fileChooser.title = "Select camera model file"
        fileChooser.initialDirectory = lastOpenedDir
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Camera model JSON file", "*.json"))
        val file = fileChooser.showOpenDialog(null)
        if (file != null) {
            cameraParameters = Gson().fromJson(file.readText(), CameraParameters::class.java)
        }
    }

    fun openCSVPodgon(actionEvent: ActionEvent) {
        val fileChooser = FileChooser()
        fileChooser.title = "Select csv values file"
        fileChooser.initialDirectory = lastOpenedDir
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV", "*.csv"))
        val csvFile = fileChooser.showOpenDialog(null) ?: return
        val coords = csvFile
            .readText()
            .split('\n')
            .map { it.split(';') }
            .filter { line -> line.size == 2 && line.all { it.isNotBlank() } }
            .map {
//                val date = Instant.parse(it[0]).toEpochMilli()
                val star = StarCoordinate(it[0].toDouble(), it[1].toDouble())
                return@map star
            }

        val cnt = coords.size
        geometryWrappers1.clear()
        coords.forEachIndexed { i, star ->
            val coordinate = AstronomyUtils.starCoordinateToMouse(
                star, geometryWrappers.first(), cameraParameters ?: error("No camera parameters")
            )
            val circle = createCircle(coordinate, radius = 10.0)
            val color = Color.hsb(360.0 * i / cnt, 1.0, 1.0)
            val gw = GeometryWrapper(circle, color, 2.0)
            geometryWrappers1.add(gw)
        }
        redraw()
        renderer.fit()
        render()
    }
}