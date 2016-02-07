package mathpump

/**
  * Created by andrei on 05/02/16.
  */
trait Parcel {

}

class ParcelReceipt(st: String, sender: String, f: String) extends scala.Serializable with Parcel {
  val status = st
  val from = sender
  val filename = f
}

/** t is type of object e.g. "patch", "file"
  * @param content
  */
class ParcelTextFile(content: String, sender: String, f: String) extends scala.Serializable with Parcel {
  def cont = content

  def from = sender

  val filename = f
}

class ParcelPatch(content: String, sender: String, f: String) extends scala.Serializable with Parcel {
  def patch = content

  def from = sender

  val filename = f
}

class ChannelWasClosed extends scala.Serializable with Parcel