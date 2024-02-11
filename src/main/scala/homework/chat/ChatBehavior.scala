package homework.chat


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}



  trait Command extends CborSerializable

  case class ChatMessageAll(nickname: String, contents: String) extends Command //отправь сообщение всем

  case class UserMessage(contents: String) extends Command

  //case class EnterRoom(fullAddress: String, nickname: String) extends Command

  case class ListingResponse(listing: Receptionist.Listing) extends Command


object ChatBehavior {

  var chatController: MyChatController = _

  case class ActorState(nickname: String, port: Int, members: Set[ActorRef[Command]])

  val ChatServiceKey: ServiceKey[Command] = ServiceKey[Command]("ChatServiceKey")
  var membersList: Set[ActorRef[Command]] = Set.empty

  def apply(nickname: String, port: Int, controller: MyChatController): Behavior[Command] = Behaviors.setup[Command] { ctx =>
    chatController = controller
    val listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](ListingResponse.apply)
    ctx.system.receptionist ! Receptionist.subscribe(ChatServiceKey, listingResponseAdapter)
    ctx.system.receptionist ! Receptionist.register(ChatServiceKey, ctx.self)
    behavior(ActorState(nickname, port, Set.empty), ctx)
  }

  def behavior(state: ActorState, ctx: ActorContext[Command]): Behaviors.Receive[Command] =
    Behaviors.receiveMessage[Command] { message =>
      var resultState = state
      message match {
        case UserMessage(contents) =>
          println(s"send message: $contents")
          state.members.foreach(_ ! ChatMessageAll(state.nickname, contents))
          Behaviors.same

        case ListingResponse(ChatServiceKey.Listing(list)) =>
          membersList = list
          resultState = state.copy(members = list.filter(_.ref != ctx.self))
          println(s"List of members(${list.size}) changed: ${list.mkString(", ")}")

        case ChatMessageAll(nickname, contents) =>
          chatController.showV(nickname, contents)
          println(s"$nickname say $contents")
          Behaviors.same

        case m => println(s"Skip: $m")
      }
      behavior(resultState, ctx)
    }
}
