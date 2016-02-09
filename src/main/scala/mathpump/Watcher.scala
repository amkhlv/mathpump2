package mathpump

import java.io.File
import java.nio.file._

import akka.actor.{ActorRef, Actor}
import org.apache.log4j.{Logger, PropertyConfigurator}

import scala.collection.JavaConversions._
import scala.language.postfixOps
/**
  * Created by andrei on 06/02/16.
  */
class Watcher(sndr: ActorRef) extends Actor {
  val logger = Logger.getLogger("WATCHER")
  PropertyConfigurator.configure("log4j.properties");
  val fsys = Paths.get(".").getFileSystem()

  override def preStart() {
    logger.info("=== entering mainloop ===")
    self ! Continue
  }

  def receive = {
    case Continue => {
      //Thread.sleep(1000);
      logger.info("continuing")
      val watcher = fsys.newWatchService()
      Paths.get(outDirName).register(watcher,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY)
      val watchKey = watcher.take()
      for (event <- watchKey.pollEvents) {
        val evCont = event.context()
        //In the case of ENTRY_CREATE, ENTRY_DELETE, and ENTRY_MODIFY events
        //the context is a Path that is the relative path between the directory registered with the watch service,
        //and the entry that is created, deleted, or modified.
        val evKind = event.kind()
        logger.info("Detected event in context: " + evCont + " of the kind: " + evKind)
        if (
          (evKind == StandardWatchEventKinds.ENTRY_CREATE) ||
            (evKind == StandardWatchEventKinds.ENTRY_MODIFY)
        ) {
          logger.info("Sending message to SNDR about " + evKind.toString + " of: " + evCont.toString)
          sndr ! NotificationOfFilesystemEvent(
            evKind,
            evCont match {
              case p: Path => p
              case _ => throw new RuntimeException("event context not a path!")
            }
          )
        }

        else if (event.context().toString() == "stopwatcher") {
          // This is a hook to stop this Watcher
          // To stop the watcher, we have to create the file called stopwatcher
          val signalFile = new File(outDirName + "/stopwatcher")
          if (signalFile.exists()) {
            //this is just to double check
            signalFile.delete()
            logger.info("Detected signal file; sending WatcherRequestsShutdown to Commander")
            sndr ! WatcherRequestsShutdown
            happy = false
          }
        }
      }
      watcher.close()
      if (happy) self ! Continue
    }
  }

  override def postStop() {
    logger.info("I am gone!")
  }

}
