package kenu.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Gabbler extends AbstractBehavior<Gabbler.GabblerCommand> {

  interface GabblerCommand {}

  public static final class SessionGranted implements GabblerCommand {

    public final ActorRef<ChatRoom.Session.PostMessage> forTellPostMessageToSessionActor;

    public SessionGranted(ActorRef<ChatRoom.Session.PostMessage> forTellPostMessageToSessionActor) {
      this.forTellPostMessageToSessionActor = forTellPostMessageToSessionActor;
    }
  }

  public static final class MessagePosted implements GabblerCommand {

    public final String screenName;
    public final String message;

    public MessagePosted(String screenName, String message) {
      this.screenName = screenName;
      this.message = message;
    }
  }

  private Gabbler(ActorContext<GabblerCommand> context) {
    super(context);
  }

  public static Behavior<Gabbler.GabblerCommand> create() {
    return Behaviors.setup(Gabbler::new);
  }

  @Override
  public Receive<GabblerCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(SessionGranted.class, this::onSessionGranted)
        .onMessage(MessagePosted.class, this::onMessagePosted)
        .build();
  }

  private Behavior<GabblerCommand> onSessionGranted(SessionGranted sessionGranted) {
    sessionGranted.forTellPostMessageToSessionActor
        .tell(new ChatRoom.Session.PostMessage("Hello, World!"));
    return Behaviors.same();
  }

  private Behavior<GabblerCommand> onMessagePosted(MessagePosted messagePosted) {
    getContext().getLog().info("message has been posted by '{}': {}", messagePosted.screenName,
        messagePosted.message);
    return Behaviors.same();
  }
}
