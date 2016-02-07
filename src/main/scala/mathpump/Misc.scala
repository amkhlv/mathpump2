package mathpump

/**
  * Created by andrei on 06/02/16.
  */

import java.nio.charset.StandardCharsets
import java.nio.file.Files._
import java.nio.file.{Files, Path}
import scala.collection.JavaConversions._

object Misc {
  val separator = sys.props("line.separator")

  def fileAtPathExists(path: Path): Boolean =
    Files.exists(path)

  def copyPath(orig:Path, dest:Path): Path = copy(orig,dest)

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  def writeToFilePath(path: Path, data: String) =
    printToFile(path.toFile())(p => p.println(data))

  def readFromFilePath(filePath: Path): String =
    readAllLines(filePath, StandardCharsets.UTF_8).mkString(separator)

}
