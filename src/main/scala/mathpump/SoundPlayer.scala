package mathpump

import java.net.URL

import akka.actor.Actor
import org.apache.log4j.{Logger, PropertyConfigurator}
/**
  * Created by andrei on 05/02/16.
  */
class SoundPlayer extends Actor  {
  val logger = Logger.getLogger("SOUND")
  PropertyConfigurator.configure("log4j.properties");

  val audioFile: Map[Sound, URL] = Map(
    BeepOnError -> getClass.getResource("/sounds/pluck2.wav"),
    BeepOnPatch -> getClass.getResource("/sounds/drum-1.5.wav"),
    BeepOnReceipt -> getClass.getResource("/sounds/drum-1.wav"),
    BeepOnAnomaly -> getClass.getResource("/sounds/pluck.wav")
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
