package mathpump

import java.nio.file.Paths
import java.util

import akka.actor.{ActorSystem, PoisonPill, Props}
import com.typesafe.config.ConfigFactory
import org.apache.log4j.{Logger, PropertyConfigurator}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scalafx.Includes._
import scalafx.application.{Platform, JFXApp}
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
  Platform.implicitExit = true;
  val browser: Map[String, WebView] =
    ( for (nm <- them.keys) yield (nm -> new WebView()) ) toMap
  val webEngine: Map[String, WebEngine] = ( for (nm <- them.keys) yield (nm -> browser(nm).engine)) toMap ;
  val fxec = JavaFXExecutionContext.javaFxExecutionContext
  val customConf = ConfigFactory.parseString("akka { log-dead-letters-during-shutdown = off } ")
  val system = ActorSystem("MathPump", customConf)
  val beeper =  system.actorOf(Props[SoundPlayer], name="beeper")
  println("Initializing transmitter...")
  val delivery = PostOffice
  val sndr = system.actorOf(Props(new Transmitter(beeper, delivery)), name = "transmitter")


  def startMain = {
    val backgroundThreadForListeningToRabbit = new Thread {
      setDaemon(true)
      override def run = waitForMessages
    }
    val backgroundThreadForWatchingFiles = new Thread {
      setDaemon(true)
      override def run = watcherProc
    }
    backgroundThreadForWatchingFiles.run
    backgroundThreadForListeningToRabbit.run
  }
  def stopMain = Paths.get(outDirName, stopWatcherFileName).toFile().createNewFile()
  def watcherProc = {
    def mainThread(): Unit = Future{(new Watcher(sndr)).run}(ExecutionContext.global).onSuccess {
      case WatcherRequestsShutdown => { //exiting:
        //close all SVG windows:
        for (nm <- them.keys) svgStage(nm).close()  ;
        //I want to sleep 3 seconds before closing the main window
        //to give time to the other thread to receive the Stop signal and shut down the actor system
        //because onSuccess for the other thread is to be executed on the FX thread
        //so I dont want to destroy the FX thread before this happens
        println("waiting for things to settle down...")
        Thread.sleep(3000)
        println("terminating")
        stage.close()
        System.exit(0)
        ()
      }
      case _ => throw  new RuntimeException("Strange result from Watcher")
    }(fxec)  ;
    mainThread()
  }
  def waitForMessages = {
    val logger = Logger.getLogger("RECEIVER")
    PropertyConfigurator.configure("log4j.properties");
    def mainThread(): Unit = Future {
      var continuation: Either[(String,String),Signal] = Right(Continue)
      //thread will block here:
      logger.info("Waiting on PostOffice delivery")
      val message = delivery.listen()
      message match {
        case p: ParcelTextFile => {
          logger.info("Got text file: " + p.filename + " from " + p.from)
          val path = Paths.get(them(p.from).dir, p.filename)
          Misc.writeToFilePath(path, p.cont)
          continuation = Left((p.from, "file://" + Paths.get(them(p.from).dir, p.filename).toAbsolutePath().toString))
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
            continuation = Left((p.from, "file://" + Paths.get(them(p.from).dir, p.filename).toAbsolutePath().toString))
            logger.info("BeepOnPatch :)")
            beeper ! BeepOnPatch
            logger.info("Sending acknowledgment to " + p.from)
            delivery.broadcast(List(p.from), new ParcelReceipt("OK", myName, p.filename))
          }
        }
        case p: ParcelReceipt => {
          logger.info("BeepOnReceipt :)")
          beeper ! BeepOnReceipt
          logger.info("Received receipt from " + p.from + " with status " + p.status + " of file " + p.filename)
          sndr ! p  //notify the transmitter of the receipt of acknowledgment
        }
        case Stop => {
          logger.info("I got the Stop signal from RabbitMQ; exiting")
          if (! delivery.close()) { beeper ! BeepOnError }
          beeper ! PoisonPill
          sndr ! PoisonPill
          logger.info("shutting down the actor system")
          Thread.sleep(1000)
          system.shutdown()  ;
          continuation = Right(Stop)
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
          logger.info("shutting down the actor system")
          Thread.sleep(1000)
          continuation = Right(Stop)
          System.exit(1)
        }
      }
      continuation
    }(ExecutionContext.global).onSuccess {
      case Left((who, url)) => {
        webEngine(who).load(url);
        mainThread()
      }
      case Right(Stop) => ()
      case Right(Continue) => {
        mainThread()
      }
    }(fxec)
    mainThread()
  }
  val btn : Button = new Button("press to START mathpump") {
    onAction = (e: ActionEvent) => {
      startMain
      btn.text = "press to STOP mathpump"
      for (nm <- them.keys) svgStage(nm).show()
      btn.onAction = {
        (e1: ActionEvent) => {
          btn.text = "---" ;
          stopMain
        }
      }
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
}
