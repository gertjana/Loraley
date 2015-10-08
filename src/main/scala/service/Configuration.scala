package service

import java.io.File
import java.nio.file.{FileSystems,Paths}


import akka.event.Logging
import com.typesafe.config.ConfigFactory

class Configuration(optFile:Option[String]) {

  def config = optFile match {
    case Some(file) => {
      val pathOfFile = Paths.get("").toAbsolutePath + FileSystems.getDefault.getSeparator + file
      val f = new File(pathOfFile)
      if (f.exists()) {
        println(s"Loading Configuration: $pathOfFile")
        ConfigFactory.parseFile(f)
      }
      else {
        println(s"Alternate configuration $file not found")
        ConfigFactory.load()
      }
    }
    case _ => ConfigFactory.load()
  }
}
