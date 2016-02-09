package mathpump

import java.nio.file.Paths
import java.util

import akka.actor.{ActorSystem, PoisonPill, Props}
import com.typesafe.config.ConfigFactory
import org.apache.log4j.{Logger, PropertyConfigurator}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.web.{WebEngine, WebView}
import scalafx.stage.Stage

/**
  * Created by andrei on 03/02/16.
  */
class SVGViewer extends JFXApp {
  val browser: Map[String, WebView] =
    ( for (nm <- them.keys) yield (nm -> new WebView()) ) toMap
  val webEngine: Map[String, WebEngine] = ( for (nm <- them.keys) yield (nm -> browser(nm).engine)) toMap ;
  val fxec = JavaFXExecutionContext.javaFxExecutionContext

  def startMain = {
    val backgroundThread = new Thread {
      setDaemon(true)
      override def run = runMain
    }
    backgroundThread.run
  }
  def runMain = {
    val logger = Logger.getLogger("RECEIVER")
    PropertyConfigurator.configure("log4j.properties");
    val customConf = ConfigFactory.parseString("akka { log-dead-letters-during-shutdown = off } ")
    val system = ActorSystem("MathPump", customConf)
    val beeper =  system.actorOf(Props[SoundPlayer], name="beeper")
    val sndr = system.actorOf(Props(new Transmitter(beeper)), name = "transmitter")
    val wtcr = system.actorOf(Props(new Watcher(sndr)), name = "watcher")

    val delivery = PostOffice

    def mainThread(): Unit = Future {
      var toUpdate: Either[(String,String),Signal] = Right(Continue)
      //thread will block here:
      logger.info("Waitin on PostOffice delivery")
      val message = delivery.listen()
      message match {
        case p: ParcelTextFile => {
          logger.info("Got text file: " + p.filename + " from " + p.from)
          val path = Paths.get(them(p.from).dir, p.filename)
          Misc.writeToFilePath(path, p.cont)
          toUpdate = Left((p.from, "file://" + Paths.get(them(p.from).dir, p.filename).toAbsolutePath().toString))
          logger.info("Sending acknowledgment to " + p.from)
          delivery.broadcast(List(p.from), new ParcelReceipt("OK", myName, p.filename))
          beeper ! BeepOnPatch
        }
        case p: ParcelPatch => {
          logger.info("Got patch: " + p.patch)
          val dmp = new diff_match_patch
          val patch = new util.LinkedList(dmp.patch_fromText(p.patch))
          val lines = Misc.readFromFilePath(Paths.get(them(p.from).dir, p.filename))
          val resultOfApplyingPatch = dmp.patch_apply(patch, lines).toList
          val newLines = resultOfApplyingPatch.head match {
            case x: String => x
          }
          val diagnostics = resultOfApplyingPatch.tail.head match {
            case xs: Array[Boolean] => xs.toList
          }
          logger.info("   ======= Patch applied =======   " + diagnostics.toString())
          if (diagnostics contains false) {
            logger.error("************ PATCH ERROR => Requesting to resend the whole file ************")
            delivery.broadcast(List(p.from), new ParcelReceipt("PleaseResend", myName, p.filename))
          } else {
            Misc.writeToFilePath(Paths.get(them(p.from).dir, p.filename), newLines)
            logger.info("to load: " + "file://" + Paths.get(them(p.from).dir, p.filename).toAbsolutePath().toString)
            toUpdate = Left((p.from, "file://" + Paths.get(them(p.from).dir, p.filename).toAbsolutePath().toString))
            logger.info("BeepOnPatch :)")
            beeper ! BeepOnPatch
            logger.info("Sending acknowledgment to " + p.from)
            delivery.broadcast(List(p.from), new ParcelReceipt("OK", myName, p.filename))
          }
          //TODO: file_was_patched
        }
        case p: ParcelReceipt => {
          logger.info("BeepOnReceipt :)")
          beeper ! BeepOnReceipt
          logger.info("Received receipt from " + p.from + " with status " + p.status + " of file " + p.filename)
          sndr ! p  //notify the sender of the receipt of acknowledgment
        }
        case Stop => {
          logger.info("I got the Stop signal from RabbitMQ; exiting")
          if (! delivery.close()) {
            beeper ! BeepOnError
          }
          beeper ! PoisonPill
          sndr ! PoisonPill
          toUpdate = Right(Stop)
        }
        case Ignore => println("PASS")
        case e: ChannelWasClosed => {
          beeper ! BeepOnError
          logger.error("lost incoming channel; exiting")
          if (! delivery.close()) {
            beeper ! BeepOnError
          }
          beeper ! PoisonPill
          sndr ! PoisonPill
          toUpdate = Right(Stop)
        }
      }
      toUpdate
    }(ExecutionContext.global).onSuccess {
      case Left((who, url)) => {
        webEngine(who).load(url);
        mainThread()
      }
      case Right(Stop) => {()}
      case Right(Continue) => {
        mainThread()
      }
    }(fxec)
    //let us now start it:
    mainThread()
  }
  val btn : Button = new Button("press to START mathpump") {
    onAction = (e: ActionEvent) => {
      btn.text = "press to STOP mathpump"
      startMain
    }
  }

  stage = new PrimaryStage {
    title = "MATHACOC: " + myName
    scene = new Scene {
      content = List(
        btn
      )
    }
  }

  val svgStage : Map[String, Stage] = (
    for (nm <- them.keys) yield {
      browser(nm).setPrefHeight(them(nm).height - 20)
      browser(nm).setPrefWidth(them(nm).width - 20)
      (nm -> new Stage {
        title = nm + " pumped in:"
        width = them(nm).width
        height = them(nm).height
        scene = new Scene {
          content = List(
            browser(nm)
          )
        }
      })
    }
    ) toMap    ;

  for (nm <- them.keys) svgStage(nm).show()

}
