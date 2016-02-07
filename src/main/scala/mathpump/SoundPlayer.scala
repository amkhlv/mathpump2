package mathpump

import java.io.File
import java.nio.file.Paths
import akka.actor.Actor
import org.apache.log4j.{Logger, PropertyConfigurator}
/**
  * Created by andrei on 05/02/16.
  */
class SoundPlayer extends Actor  {
  val logger = Logger.getLogger("SOUND")
  PropertyConfigurator.configure("log4j.properties");

  val audioFile: Map[Sound, File] = Map(
    BeepOnError -> Paths.get("hooks", "ton.wav").toFile,
    BeepOnPatch -> Paths.get("hooks", "drum-1.5.wav").toFile,
    BeepOnReceipt -> Paths.get("hooks", "drum-1.wav").toFile,
    BeepOnAnomaly -> Paths.get("hooks", "90125__pierrecartoons1979__click-tiny.wav").toFile
  )
  def receive = {
    case x:Sound => {
      if (beepless) {
        logger.info("Not beeping because silent")
      } else {
        logger.info("playing sound: " + x.toString)
        (new SoundClip).play(audioFile(x))
      }
    }
  }


  override def postStop() {
    logger.info("I am gone!")
  }

}
