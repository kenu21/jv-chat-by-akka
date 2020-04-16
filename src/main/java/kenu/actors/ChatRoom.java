package kenu.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.sun.javafx.scene.control.skin.ContextMenuSkin;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChatRoom extends AbstractBehavior<ChatRoom.ChatRoomCommand> {

  static class Session extends AbstractBehavior<Session.SessionCommand> {

    interface SessionCommand {}

    public static final class PostMessage implements SessionCommand {

      public final String message;

      public PostMessage(String message) {
        this.message = message;
      }
    }

    public static final class NotifyClient implements SessionCommand {

      final Gabbler.MessagePosted message;

      public NotifyClient(Gabbler.MessagePosted message) {
        this.message = message;
      }
    }

    private final ActorRef<ChatRoomCommand> chatRoomActor;
    private final String screenName;
    private final ActorRef<Gabbler.GabblerCommand> replyTo;

    private Session(ActorContext<SessionCommand> context, ActorRef<ChatRoomCommand> chatRoomActor,
        String screenName, ActorRef<Gabbler.GabblerCommand> replyTo) {
      super(context);
      this.chatRoomActor = chatRoomActor;
      this.screenName = screenName;
      this.replyTo = replyTo;
    }

    static Behavior<SessionCommand> create(
        ActorRef<ChatRoomCommand> chatRoomActor, String screenName,
        ActorRef<Gabbler.GabblerCommand> replyTo) {
      return Behaviors.setup(contex -> new Session(contex, chatRoomActor, screenName, replyTo));
    }

    @Override
    public Receive<SessionCommand> createReceive() {
      return newReceiveBuilder()
          .onMessage(PostMessage.class,
              postMessage -> onPostMessage(chatRoomActor, screenName, postMessage))
          .onMessage(NotifyClient.class, notifyClient -> onNotifyClient(replyTo, notifyClient))
          .build();
    }

    private Behavior<SessionCommand> onPostMessage(
        ActorRef<ChatRoomCommand> chatRoomActor, String screenName, PostMessage postMessage) {
      chatRoomActor.tell(new PublishSessionMessage(screenName, postMessage.message));
      return Behaviors.same();
    }

    private Behavior<SessionCommand> onNotifyClient(
        ActorRef<Gabbler.GabblerCommand> gabblerActor, NotifyClient notification) {
      gabblerActor.tell(notification.message);
      return Behaviors.same();
    }
  }

  interface ChatRoomCommand {}

  public static final class GetSession implements ChatRoomCommand {

    public final String screenName;
    public final ActorRef<Gabbler.GabblerCommand> replyTo;

    public GetSession(String screenName, ActorRef<Gabbler.GabblerCommand> replyTo) {
      this.screenName = screenName;
      this.replyTo = replyTo;
    }
  }

  private static final class PublishSessionMessage implements ChatRoomCommand {

    public final String screenName;
    public final String message;

    private PublishSessionMessage(String screenName, String message) {
      this.screenName = screenName;
      this.message = message;
    }
  }

  private final List<ActorRef<Session.SessionCommand>> sessionActors = new ArrayList<>();

  private ChatRoom(ActorContext<ChatRoomCommand> context) {
    super(context);
  }

  public static Behavior<ChatRoomCommand> create() {
    return Behaviors.setup(ChatRoom::new);
  }

  @Override
  public Receive<ChatRoomCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(GetSession.class, this::onGetSession)
        .onMessage(PublishSessionMessage.class, this::onPublishSessionMessage)
        .build();
  }

  private Behavior<ChatRoomCommand> onGetSession(GetSession getSession)
      throws UnsupportedEncodingException {
    ActorContext<ChatRoomCommand> context = getContext();
    ActorRef<Session.SessionCommand> sessionActor = context.spawn(
        Session.create(context.getSelf(), getSession.screenName, getSession.replyTo),
        URLEncoder.encode(getSession.screenName, StandardCharsets.UTF_8.name()));
    getSession.replyTo.tell(new Gabbler.SessionGranted(sessionActor.narrow()));

    sessionActors.add(sessionActor);
    return Behaviors.same();
  }

  private Behavior<ChatRoomCommand> onPublishSessionMessage(PublishSessionMessage pub) {
    Session.NotifyClient notification =
        new Session.NotifyClient(new Gabbler.MessagePosted(pub.screenName, pub.message));
    sessionActors.forEach(sessionActor -> sessionActor.tell(notification));
    return Behaviors.same();
  }
}
