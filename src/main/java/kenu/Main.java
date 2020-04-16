package kenu;

import akka.actor.typed.ActorSystem;
import kenu.actors.Guardian;

public class Main {

  public static void main(String[] args) {
    ActorSystem<String> guardianActor = ActorSystem.create(Guardian.create(), "guardian");
    guardianActor.tell("start");
  }
}
