package homework.chat


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}



  trait Command extends CborSerializable
case class SendGroupMessage(contents: String) extends Command
case class SendPrivateMessage(toNickname: String, message: String) extends Command
case class GetChatHistory(nickname: String) extends Command


case class GroupMessage(fromNickname: String, contents: String) extends Command //отправь сообщение всем

//case class EnterRoom(fullAddress: String, nickname: String) extends Command

case class PrivateMessage(fromNickname: String, message: String) extends Command

case class ListingResponse(listing: Receptionist.Listing) extends Command
case class UserOnline(nickname: String, actorRef: ActorRef[Command]) extends Command


object ChatBehavior {

  //  var chatController: MyChatController = _

  private case class ActorState(
                                 nickname: String,
                                 port: Int,
                                 chatController: MyChatController,
                                 members: Set[ActorRef[Command]] = Set.empty,
                                 chatUsers: List[ForeignUser] = List.empty
                               )

  private case class ForeignUser(nickname: String, actorRef: ActorRef[Command], chatHistory: String = "")

  val ChatServiceKey: ServiceKey[Command] = ServiceKey[Command]("ChatServiceKey")
  //  var membersList: Set[ActorRef[Command]] = Set.empty

  def apply(nickname: String, port: Int, controller: MyChatController): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    //    chatController = controller
    val listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](ListingResponse.apply)
    ctx.system.receptionist ! Receptionist.subscribe(ChatServiceKey, listingResponseAdapter)
    ctx.system.receptionist ! Receptionist.register(ChatServiceKey, ctx.self)
    behavior(ActorState(nickname, port, controller), ctx)
  }

  /*
  ForeignUser (nickname, actorRef, chatHistory)

  UserOnline  (nickname, actorRef)

   */

  def behavior(state: ActorState, ctx: ActorContext[Command]): Behaviors.Receive[Command] =
    Behaviors.receiveMessage[Command] { message =>

      message match {
        case SendGroupMessage(contents) => //todo общий нейминг переменных и полей
          println(s"send message: $contents")
          state.members.foreach(_ ! GroupMessage(state.nickname, contents))
          Behaviors.same

        case SendPrivateMessage(toNickname, message) =>
          /*
          обновлеям историю сообщений и отправляем сообщение
           */
          val foreignUser = state.chatUsers.find(user => user.nickname == toNickname)
          val updatedChatUsers = if (foreignUser.isDefined) {
            foreignUser.foreach(_.actorRef ! PrivateMessage(state.nickname, message))
            state.chatUsers.map { user =>
              if (user.nickname == toNickname) {
                user.copy(chatHistory = user.chatHistory + "\n" + message)
              } else {
                user
              }
            }
          } else {
            state.chatUsers
          }
          behavior(state.copy(chatUsers = updatedChatUsers), ctx)
        case GetChatHistory(nickname) =>
          state.chatUsers.find(_.nickname == nickname)
            .foreach(user => state.chatController.showHistory(user.chatHistory))
          Behaviors.same

        case UserOnline(nickname, actorRef) =>
          // todo написать нормально
          /*
          Обновляем информацию о пользователе (username)
           */
          val user = state.chatUsers.find(user => user.actorRef == actorRef)
          val updatedChatUsers =
            if (user.isDefined) {
              state.chatUsers.map { user =>
                if (user.actorRef == actorRef) {
                  user.copy(nickname = nickname)
                } else {
                  user
                }
              }
            } else {
              state.chatUsers :+ ForeignUser(nickname, actorRef)
            }
          println("update user: " + updatedChatUsers) //todo
          behavior(state.copy(chatUsers = updatedChatUsers), ctx)

        case ListingResponse(ChatServiceKey.Listing(list)) =>
          //          membersList = list
          val onlineUsers = list.filter(_.ref != ctx.self)
          val resultState = state.copy(members = onlineUsers)
          onlineUsers.foreach(actorRef => actorRef ! UserOnline(state.nickname, ctx.self))
          println(s"List of members(${list.size}) changed: ${list.mkString(", ")}")
          behavior(resultState, ctx)

        case GroupMessage(nickname, contents) =>
          state.chatController.showV(nickname, contents)
          println(s"$nickname say $contents")
          Behaviors.same
        case PrivateMessage(fromNickname, message) =>
          val foreignUser = state.chatUsers.find(user => user.nickname == fromNickname)
          val updatedChatUsers = if (foreignUser.isDefined) {
            state.chatUsers.map { user =>
              if (user.nickname == fromNickname) {
                user.copy(chatHistory = user.chatHistory + s"\n[$fromNickname] " + message)
              } else {
                user
              }
            }
          } else {
            state.chatUsers
          }
          state.chatController.showV(fromNickname, message) //todo
          println("private message received: " + updatedChatUsers)
          behavior(state.copy(chatUsers = updatedChatUsers), ctx)

        case otherCommand =>
          println(s"Skip: $otherCommand")
          Behaviors.same
      }

    }
}
