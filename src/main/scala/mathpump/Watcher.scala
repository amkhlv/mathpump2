package mathpump

import java.nio.file._

import akka.actor.ActorRef
import org.apache.log4j.{Logger, PropertyConfigurator}

import scala.collection.JavaConversions._
import scala.language.postfixOps
/**
  * Created by andrei on 06/02/16.
  */
class Watcher(sndr: ActorRef) {
  val logger = Logger.getLogger("WATCHER")
  PropertyConfigurator.configure("log4j.properties");
  var happy = true;
  val fsys = Paths.get(".").getFileSystem()
  def run = {
    while (happy) {
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
        if (event.context().toString() == stopWatcherFileName) {
          // This is a hook to stop this Watcher
          // To stop the watcher, we create a file with a special name
          val signalFile =  Paths.get(outDirName, stopWatcherFileName).toFile
          if (signalFile.exists()) {
            //this is just to double check
            signalFile.delete()
            happy = false
            logger.info("Detected (and deleted) the signal file; sending WatcherRequestsShutdown to Commander")
            sndr ! WatcherRequestsShutdown
          }
        } else {
          val evKind = event.kind()
          logger.info("Detected event in context: " + evCont + " of the kind: " + evKind)
          if ((evKind == StandardWatchEventKinds.ENTRY_CREATE) || (evKind == StandardWatchEventKinds.ENTRY_MODIFY)) {
            logger.info("Sending message to SNDR about " + evKind.toString + " of: " + evCont.toString)
            sndr ! NotificationOfFilesystemEvent(
              evKind,
              evCont match {
                case p: Path => p
                case _ => throw new RuntimeException("event context not a path!")
              }
            )
          }
        }
      }
      watcher.close()
    }
    WatcherRequestsShutdown
  }
}
