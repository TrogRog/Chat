package homework.chat


import javafx.event.ActionEvent
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.{Button, TextField}
import javafx.stage.Stage

import java.net.URL
import java.util.ResourceBundle


class LogController {


  //  @FXML private val resources: ResourceBundle = null
  //  @FXML private val location: URL = null
  @FXML private var logSeedNode: TextField = _
  @FXML private var nameLog: TextField = _
  //  var ok: Boolean = false
  var credentials: Option[Credentials] = None


  @FXML private[chat] def authLogButton(event: ActionEvent): Unit = {

    credentials = Some(Credentials(nameLog.getText, logSeedNode.getText.toInt))
    //    ok = true
    event.getSource.asInstanceOf[Button].getScene.getWindow.hide()
  }

  def getCredentials: Option[Credentials] = {
    credentials
    //    if (ok) Some(Credentials(nameLog.getText, logSeedNode.getText.toInt))
    //    else None
  }

  //  @FXML private[chat] def initialize(): Unit = {
  //
  //  }
}














