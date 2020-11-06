package me.alexkovrigin.astrotool

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class Viewer : Application() {
    private lateinit var stage: Stage

    override fun start(primaryStage: Stage) {
        stage = primaryStage
        with(stage) {
            val root = FXMLLoader().load<Parent>(javaClass.getResourceAsStream("/fxml/mainWindow.fxml"))
            scene = Scene(root).apply {
                stylesheets.add(javaClass.getResource("/fxml/mainWindow.css").toExternalForm())
            }
            minHeight = 200.0
            minWidth = 200.0
            title = "Astrotool"
            show()
        }
    }

}

fun main(args: Array<String>) {
    Application.launch(Viewer::class.java, *args)
}