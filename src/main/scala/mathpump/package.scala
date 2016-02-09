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
  case class PersonConfig(dir: String, width: Int, height: Int)
  val them: Map[String, PersonConfig] = Map((for (c <- config.getConfigList("them").toArray()) yield {
    val (nm: String , dir: String, width: Int, height: Int) = c match {
      case conf: Config => (
        conf.getString("name"),
        conf.getString("dir"),
        conf.getInt("width"),
        conf.getInt("height")
        )
    }
    (nm -> PersonConfig(dir = dir, width = width, height = height))
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
