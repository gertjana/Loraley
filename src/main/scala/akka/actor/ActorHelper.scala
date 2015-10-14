package akka.actor

import akka.actor.dungeon.ChildrenContainer
import akka.event.Logging

object ActorHelper {

  //Blantantly copied from Akka sourcecode ;)
  //be very careful using this in production, can cause OOM Exceptions and performance issues
  def printTree(root:ActorRef): String = {
    def printNode(node: ActorRef, indent: String): String = {
      node match {
        case wc: ActorRefWithCell ⇒
          val cell = wc.underlying
          (if (indent.isEmpty) "-> " else indent.dropRight(1) + "⌊-> ") +
            node.path.name + " " + Logging.simpleName(node) + " " +
            (cell match {
              case real: ActorCell ⇒ if (real.actor ne null) real.actor.getClass else "null"
              case _               ⇒ Logging.simpleName(cell)
            }) +
            (cell match {
              case real: ActorCell ⇒ " status=" + real.mailbox.currentStatus
              case _               ⇒ ""
            }) +
            " " + (cell.childrenRefs match {
            case ChildrenContainer.TerminatingChildrenContainer(_, toDie, reason) ⇒
              "Terminating(" + reason + ")" +
                (toDie.toSeq.sorted mkString ("\n" + indent + "   |    toDie: ", "\n" + indent + "   |           ", ""))
            case x @ (ChildrenContainer.TerminatedChildrenContainer | ChildrenContainer.EmptyChildrenContainer) ⇒ x.toString
            case n: ChildrenContainer.NormalChildrenContainer ⇒ n.c.size + " children"
            case x ⇒ Logging.simpleName(x)
          }) +
            (if (cell.childrenRefs.children.isEmpty) "" else "\n") +
            ({
              val children = cell.childrenRefs.children.toSeq.sorted
              val bulk = children.dropRight(1) map (printNode(_, indent + "   |"))
              bulk ++ (children.lastOption map (printNode(_, indent + "    ")))
            } mkString "\n")
        case _ ⇒
          indent + node.path.name + " " + Logging.simpleName(node)
      }
    }
    printNode(root, "")
  }
}
