package kenu.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Guardian extends AbstractBehavior<String> {

  private Guardian(ActorContext<String> context) {
    super(context);
  }

  public static Behavior<String> create() {
    return Behaviors.setup(Guardian::new);
  }

  @Override
  public Receive<String> createReceive() {
    return newReceiveBuilder().onMessageEquals("start", this::onMessage).build();
  }

  private Behavior<String> onMessage() {
    ActorContext<String> context = getContext();
    ActorRef<Gabbler.GabblerCommand> gabblerActor = context.spawn(Gabbler.create(), "gabbler");
    ActorRef<ChatRoom.ChatRoomCommand> chatRoomActor = context.spawn(ChatRoom.create(), "chatRoom");
    chatRoomActor.tell(new ChatRoom.GetSession("gabbler", gabblerActor));
    return Behaviors.same();
  }
}
