package mathpump

/**
  * Created by andrei on 06/02/16.
  */


import java.nio.file._

import akka.actor.{Actor, ActorRef}
import org.apache.log4j.{Logger, PropertyConfigurator}

import scala.annotation.tailrec
import scala.language.postfixOps
import scalafx.application.Platform
import scalafx.scene.control.Label
import scalafx.scene.paint.Color

class Transmitter(beeper: ActorRef, delivery: Broadcaster, statusLabel: Map[String, Label]) extends Actor {
  val logger = Logger.getLogger("TRANSMITTER")
  PropertyConfigurator.configure("log4j.properties");


  var oldFileContents: Map[Path, String] = Map()

  def updateOldFileContents(fpath: Path, x: String) = {
    if (oldFileContents.keys.toList.contains(fpath)) {
      oldFileContents = oldFileContents.updated(fpath, x)
    }
    else {
      oldFileContents = oldFileContents + (fpath -> x)
    }
  }
  // The variable pendingReceiptsFrom is a Map:
  // (recipient  -> the Set of files for which the receipt is pending from this recipient)
  var pendingReceipts: Map[String, Set[String]] = (them.keys map {
    nm => (nm, Set[String]())
  }) toMap

  def allEmpty[T](m: Map[T, Set[T]]) = (true /: m.keys)(_ && m(_).isEmpty)

  var filesWeNeedToSend: Set[Path] = Set()
  var patchesWeNeedToSend: Set[Path] = Set()

  def sendFile(fpath: Path) = {
    val fname = fpath.getFileName.toString
    logger.info("Going to send whole file " + fname)
    val lines = Misc.readFromFilePath(fpath)
    logger.info("Finished reading file " + fname)
    delivery.broadcast(them.keys toList, new ParcelTextFile(lines, myName, fname))
    pendingReceipts = pendingReceipts.mapValues(x => x + fname)
    filesWeNeedToSend = filesWeNeedToSend - fpath
    logger.info("Done sending contents of " + fname)
    updateOldFileContents(fpath, lines)
    Platform.runLater{
      new Runnable {
        override def run(): Unit = {
          for (k <- statusLabel.keys)  {
            statusLabel(k).setTextFill(Color.web("#ee0000"));
            statusLabel(k).setText("waiting for receipt for FILE");
          };
          ()
        }}}}

  def sendPatch(fpath: Path) = {
    val fname = fpath.getFileName.toString
    logger.info("Going to only send a patch to " + fname)
    val lines = Misc.readFromFilePath(fpath)
    val dmp = new diff_match_patch
    val patch = dmp.patch_make(oldFileContents(fpath), lines)
    val patchString = dmp.patch_toText(patch)
    delivery.broadcast(them.keys toList, new ParcelPatch(patchString, myName, fname))
    pendingReceipts = pendingReceipts.mapValues(x => x + fname)
    patchesWeNeedToSend = patchesWeNeedToSend - fpath
    logger.info("Done sending patch to file " + fname)
    updateOldFileContents(fpath, lines)
    Platform.runLater{
      new Runnable {
        override def run(): Unit = {
          for (k <- statusLabel.keys)  {
            statusLabel(k).setTextFill(Color.web("#ee0000"));
            statusLabel(k).setText("waiting for receipt for PATCH");
          };
          ()
        }}}}

  def sendAllFilesInOutDir() {
    val listOfFiles = (new java.io.File(outDirName)).listFiles
    logger.info("sendAllFilesInOutDir: " + listOfFiles.toString)
    for (f <- listOfFiles.filter(_ isFile)) {
      if (Misc.notIgnored(f.getName)) sendFile(f.toPath)
    }
  }

  def sendAllWeNeedToSend() {
    logger.info("sendAllWeNeedToSend, files:   " + filesWeNeedToSend.toString)
    if (! filesWeNeedToSend.isEmpty) println("sending FILES: " + filesWeNeedToSend.toString())
    logger.info("sendAllWeNeedToSend, patches: " + patchesWeNeedToSend.toString)
    if (! patchesWeNeedToSend.isEmpty) println("sending PATCHES: " + patchesWeNeedToSend.toString())
    for (path <- filesWeNeedToSend) sendFile(path)
    for (path <- patchesWeNeedToSend) sendPatch(path)
  }

  override def preStart() {
    sendAllFilesInOutDir()
    logger.info("=== entering mainloop ===")
  }

  def receive = {
    case NotificationOfFilesystemEvent(kind, path: Path) => {
      logger.info("Detected filesystem event of the kind: " + kind +
        " on the path: " + Paths.get(outDirName, path.toString))
      if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
        filesWeNeedToSend = filesWeNeedToSend + Paths.get(outDirName, path.toString)
        Thread.sleep(200)
      }
      else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
        Thread.sleep(200)
        val fullpath = Paths.get(outDirName, path.toString)
        @tailrec def file_did_change(j:Int, s:String): Boolean = {
          if ( s == oldFileContents(fullpath) ) {
            if (j > 0) {
              logger.info("--- 100 ms --- 100 ms --- 100ms --- 100ms --- 100ms ---")
              Thread.sleep(100)
              file_did_change(j-1, Misc.readFromFilePath(fullpath))
            } else {
              beeper ! BeepOnAnomaly
              false
            }
          } else true
        }
        if (file_did_change(5, Misc.readFromFilePath(fullpath))) {
          patchesWeNeedToSend = patchesWeNeedToSend + Paths.get(outDirName, path.toString)
        }
      }
      if (!(allEmpty(pendingReceipts)) && (!(patchesWeNeedToSend.isEmpty) || !(filesWeNeedToSend.isEmpty))) {
        logger.info("Not sending anything, because still waiting for receipt from the following recipients: " +
          pendingReceipts.toString())
        println(Console.RED + pendingReceipts.toString() + Console.RESET)
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
          logger.info("But remembering that, once the receipt is received, need to send the whole file " + path)
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
          logger.info("But remembering that, once the receipt is received, need to send a patch for " + path)
        }
      }
      else sendAllWeNeedToSend()
    }
    case x: ParcelReceipt => {
      pendingReceipts = pendingReceipts.updated(x.from, pendingReceipts(x.from) - x.filename)
      if (pendingReceipts(x.from).isEmpty) {
        println(Console.GREEN + "OK" + Console.RESET)
        Platform.runLater{
          new Runnable {
            override def run(): Unit = {
              Platform.runLater{statusLabel(x.from).setTextFill(Color.web("#009900"))}
              Platform.runLater{statusLabel(x.from).setText("current")  } ;
              ()
            }}}}
      if (x.status == "PleaseResend") {
        logger.error("was notified of a PATCH ERROR; will resend the whole file")
        filesWeNeedToSend = filesWeNeedToSend + Paths.get(outDirName, x.filename)
        if (allEmpty(pendingReceipts)) sendAllWeNeedToSend
        else logger.info("not yet sending, because there are receipts pending; but will remember to send when I can")
      }
      else if (x.status != "OK") logger.error("unknown Parcel status")
      else if (allEmpty(pendingReceipts)) sendAllWeNeedToSend
    }
    case Fix => {
      logger.info("--- Fix requested; going to re-send all files ---")
      for (nm <- them.keys) {
        pendingReceipts = pendingReceipts.updated(nm, Set())
      }
      filesWeNeedToSend = Set()
      patchesWeNeedToSend = Set()
      sendAllFilesInOutDir()
    }
    case WatcherRequestsShutdown => {
      logger.info("Sending Stop object to my own Rcvr")
      delivery.broadcast(List(myName), Stop)
    }
    case _ => throw new RuntimeException("Msgr: Unknown message received by Actor Msgr")
  }

  override def postStop() {

    logger.info("I am gone!")
  }

}
