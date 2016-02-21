package mathpump

/**
  * Created by andrei on 06/02/16.
  */

import java.nio.charset.StandardCharsets
import java.nio.file.Files._
import java.nio.file.{Files, Path, Paths}
import scala.collection.JavaConversions._
import scala.util.matching.Regex

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

  def resourceFile(x: String): java.io.File = Paths.get(getClass.getResource(x).getPath).toFile

  def notIgnored(x: String): Boolean = ignoredFilenamePatterns.toList.map {
            case r: Regex => r.findFirstMatchIn(x) match {
              case Some(y) => false
              case None => true
            }}.fold(true)((a,b) => a && b)

}
