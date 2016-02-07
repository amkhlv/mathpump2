package mathpump

import java.nio.file.{WatchEvent, Path}

/**
  * Created by andrei on 05/02/16.
  */

abstract class Signal {

}


object Stop extends Signal with Parcel with Serializable

object Ignore extends Signal with Parcel with Serializable

object Fix extends Signal with Parcel with Serializable

object Continue extends Signal
object Shutdown extends Signal

object WatcherRequestsShutdown extends Signal
object ReceiverGotStopSignal extends Signal
object SenderResigns extends Signal

case class NotificationOfFilesystemEvent[T](k: WatchEvent.Kind[T], x: Path)
