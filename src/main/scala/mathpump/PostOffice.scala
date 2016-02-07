package mathpump

/**
  * Created by andrei on 05/02/16.
  */


import java.io._
import java.security._
import javax.net.ssl._

import com.rabbitmq.client.{Connection, ConnectionFactory, QueueingConsumer}
import org.apache.log4j.{Logger, PropertyConfigurator}

import scala.language.postfixOps

object PostOffice {
  val logger = Logger.getLogger("POSTOFFICE")
  PropertyConfigurator.configure("log4j.properties")

  val maybeConnection: Option[Connection] = {
    val factory: com.rabbitmq.client.ConnectionFactory = new ConnectionFactory();
    factory.setHost(rabbitURL);
    factory.setUsername(myName);
    factory.setPassword(myPassword)
    factory.setPort(rabbitPort);
    factory.setVirtualHost(vhost);
    factory.setConnectionTimeout(7200);
    factory.setRequestedHeartbeat(30);
    if (rabbitVerifyCertificates) {
      //Keystore:
      /*
    val ks = KeyStore.getInstance("PKCS12");
    ks.load(new FileInputStream("certificates/keycert.p12"), keyPassphrase.toCharArray);
    val kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ks, keyPassphrase);
*/
      //Truststore:
      val tks = KeyStore.getInstance("JKS");
      tks.load(new FileInputStream(trustStore), trustPassphrase.toCharArray);
      val tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(tks);

      val c = SSLContext.getInstance("SSLv3");
      c.init(null, tmf.getTrustManagers(), null);

      factory.useSslProtocol(c)
    }
    else factory.useSslProtocol()

    val nc = try {
      Some(factory.newConnection());
    } catch {
      case e: java.net.SocketTimeoutException => {
        println("=== CANNOT CONNECT TO SERVER ===")
        logger.error("=== CANNOT CONNECT TO SERVER ===")
        None
      }
    }
    nc
  }

  val connection: Connection = maybeConnection match {
    case Some(x) => x
    case None => throw new Error("could not connect to server")
  }

  val inChannel = connection.createChannel()
  inChannel.queueDeclare(myName, false, false, false, null)
  val consumer = new QueueingConsumer(inChannel);
  val outChannel: Map[String, com.rabbitmq.client.Channel] = Map((
    for (nm <- myName :: (them.keys toList)) yield {
      logger.info("creating connection for " + nm)
      val ch = connection.createChannel(); ch.queueDeclare(nm, false, false, false, null); (nm -> ch)
    }
    ): _*)


  def close(): Boolean = {
    var result = true
    try {
      inChannel.close()
    } catch {
      case ex: com.rabbitmq.client.AlreadyClosedException => {
        result = false
        println("ERROR: lost inChannel")
        logger.error("lost inChannel")
      }
    }
    for (nm <- myName :: (them.keys toList)) {
      try {
        outChannel(nm).close()
      } catch {
        case ex: com.rabbitmq.client.AlreadyClosedException => {
          result = false
          println("ERROR: lost outChannel")
          logger.error("lost outChannel")
        }
      }
    }
    try {
      connection.close()
    } catch {
      case ex: com.rabbitmq.client.AlreadyClosedException => {
        result = false
        println("ERROR: lost connection")
        logger.error("lost connection")
      }
    }
    result
  }

  def objToMsg(x: Parcel): Array[Byte] = x match {
    case y: ParcelTextFile => {
      val txt = "type:ParcelTextFile\n" +
        "from:" + y.from + "\n" +
        "filename:" + y.filename + "\n" +
        y.cont
      txt.getBytes
    }
    case y: ParcelPatch => {
      val txt = "type:ParcelPatch\n" +
        "from:" + y.from + "\n" +
        "filename:" + y.filename + "\n" +
        y.patch
      txt.getBytes
    }
    case y: ParcelReceipt => {
      val txt = "type:ParcelReceipt\n" +
        "status:" + y.status + "\n" +
        "from:" + y.from + "\n" +
        "filename:" + y.filename
      txt.getBytes
    }
    case Stop => {
      val txt = "type:Stop"
      txt.getBytes
    }
    case u => {
      println(u)
      logger.error("failure of objToMsg")
      throw new Exception("ERROR in objToMsg")
    }
  }

  def msgToObj(f: Array[Byte]) = {
    val lines = (new String(f)).split('\n')
    val objType = {
      val x = lines(0).split(':')
      if (x(0) == "type") x(1) else throw new Exception("ERROR in decoding message")
    }
    objType match {
      case "ParcelReceipt" => {
        /*
      type:ParcelReceipt
      status:OK
      from:andrei
      filename:out.svg
       */
        val st = lines(1).split(':')
        if (st(0) != "status") {
          throw new Exception("ERROR in decoding message")
        }
        val status = st(1)
        val fr = lines(2).split(':')
        if (fr(0) != "from") {
          throw new Exception("ERROR in decoding message")
        }
        val from = fr(1)
        val fn = lines(3).split(':')
        if (fn(0) != "filename") {
          throw new Exception("ERROR in decoding message")
        }
        val filename = fn(1)
        new ParcelReceipt(status, from, filename)
      }
      case "ParcelPatch" => {
        /*
      type:ParcelPatch
      from:andrei
      filename:out.svg
      CONTENT
       */
        val fr = lines(1).split(':')
        if (fr(0) != "from") {
          throw new Exception("ERROR in decoding message")
        }
        val from = fr(1)
        val fn = lines(2).split(':')
        if (fn(0) != "filename") {
          throw new Exception("ERROR in decoding message")
        }
        val filename = fn(1)
        new ParcelPatch(lines.drop(3).mkString("\n"), from, filename)
      }
      case "ParcelTextFile" => {
        /*
      type:ParcelTextFile
      from:andrei
      filename:out.svg
      CONTENT
       */
        val fr = lines(1).split(':')
        if (fr(0) != "from") {
          throw new Exception("ERROR in decoding message")
        }
        val from = fr(1)
        val fn = lines(2).split(':')
        if (fn(0) != "filename") {
          throw new Exception("ERROR in decoding message")
        }
        val filename = fn(1)
        new ParcelTextFile(lines.drop(3).mkString("\n"), from, filename)
      }
      case "Stop" => Stop
      case x => {
        logger.error("****************** Unknown Transmission: " + x)
        Ignore
      }
    }
  }

  def broadcast(recipients: List[String], obj: Parcel) = {
    val diagnosticString = obj match {
      case y: ParcelTextFile => "type:ParcelTextFile from:" + y.from + " filename:" + y.filename
      case y: ParcelPatch => "type:ParcelPatch from:" + y.from + " filename:" + y.filename
      case y: ParcelReceipt => "type:ParcelReceipt status:" + y.status + " from:" + y.from + " filename:" + y.filename
      case Stop => "type:Stop"
      case u => "SOMETHING STRANGE"
    }
    for (name <- recipients) {
      outChannel(name).basicPublish("", name, null, objToMsg(obj));
      logger.info(diagnosticString + " --> Sent to: " + name)
    }
  }

  def listen(): Parcel = {
    logger.info("RabbitMQ is waiting for messages");
    try {
      inChannel.basicConsume(myName, true, consumer);
      val delivery = consumer.nextDelivery();
      msgToObj(delivery.getBody)
    } catch {
      case ex: com.rabbitmq.client.AlreadyClosedException => new ChannelWasClosed
    }

  }

}





