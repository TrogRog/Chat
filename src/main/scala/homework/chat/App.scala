package homework.chat


import akka.actor.typed.ActorSystem
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.{Modality, Stage}


object App {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[App], args: _*)
  }
}

case class Credentials(username: String, port: Int)

class App extends Application {
  var system: ActorSystem[AnyRef] = _
  override def start(stage: Stage): Unit = {

    val credentials = showLoginWindow()

    println("---------")
    println(credentials)

    credentials.foreach { credentials =>
      val fxmlLoader = new FXMLLoader(classOf[MyChatController].getResource("/MyChat.fxml"))
      val scene = new Scene(fxmlLoader.load, 700, 400)
      val controller: MyChatController = fxmlLoader.getController
      stage.setTitle("MyChat")
      stage.setScene(scene)
      stage.show()
      controller.memberName = credentials.username
      system = ChatCluster.startup(credentials.username, credentials.port, controller)

    }


  }

  override def stop(): Unit = {
    super.stop()
    Option(system).foreach(_.terminate())
  }

  private def showLoginWindow(): Option[Credentials] = {
    val fxmlLoader = new FXMLLoader(classOf[LogController].getResource("/log.fxml"))
    val scene = new Scene(fxmlLoader.load, 700, 400)
    val controller: LogController = fxmlLoader.getController
    val stage = new Stage
    stage.initModality(Modality.APPLICATION_MODAL)
    stage.setTitle("MyChat")
    stage.setScene(scene)
    stage.showAndWait()
    controller.getCredentials
  }
}