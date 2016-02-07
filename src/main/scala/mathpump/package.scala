import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}


/**
  * Created by andrei on 04/02/16.
  */
package object mathpump {
  var happy = true
  val config = ConfigFactory.load()
  val myName = config.getString("me.name")
  val myPassword = config.getString("me.password")
  val outDirName = config.getString("me.dir")
  val them: Map[String, Map[String, String]] = Map((for (c <- config.getConfigList("them").toArray()) yield {
    val nm: String = c match {
      case conf: Config => conf.getString("name")
    }
    val dir: String = c match {
      case conf: Config => conf.getString("dir")
    }
    (nm -> Map("dir" -> dir))
  }):_*)
  val rabbitURL = config.getString("rabbitURL")
  val rabbitPort = config.getInt("rabbitPort")
  val vhost = config.getString("vhost")
  val rabbitVerifyCertificates = config.getBoolean("rabbitVerifyCertificates")
  val trustStore: String = config.getString("trustStore")
  val trustPassphrase: String = config.getString("trustStorePassphrase")
  val beepless: Boolean = config.getBoolean("silent")
  val headless: Boolean = config.getBoolean("headless")


  val system = ActorSystem("mathpump")

}
