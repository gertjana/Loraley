package service

import java.io.File
import java.nio.file.{FileSystems,Paths}
import com.typesafe.config.ConfigFactory
import org.slf4s.Logging

class Configuration(optFile:Option[String]) extends Logging {

  def config = optFile match {
    case Some(file) => {
      val pathOfFile = Paths.get("").toAbsolutePath.toString + FileSystems.getDefault.getSeparator + file

      val f = new File(pathOfFile)
      println(f)
      if (f.exists()) {
        log.info(s"Loading Configuration: $pathOfFile")
        ConfigFactory.parseFile(f)
      }
      else {
        log.warn(s"Alternate configuration $file not found")
        ConfigFactory.load()
      }
    }
    case _ => ConfigFactory.load()
  }
}
