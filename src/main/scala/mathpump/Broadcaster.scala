package mathpump

/**
  * Created by andrei on 12/02/16.
  */
trait Broadcaster {
     def broadcast(recipients: List[String], obj: Parcel): Unit

}
