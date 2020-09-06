package com.github.adedayo.checkmate

import java.io.{BufferedInputStream, FileOutputStream}
import java.net.URLDecoder
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Paths}
import java.util.jar.JarFile
import java.util.zip.GZIPInputStream

import com.fasterxml.jackson.databind.{MapperFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.adedayo.checkmate.model.{SecurityDiagnostic, SensitiveFile}
import com.intellij.ide.plugins.cl.PluginClassLoader
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils

import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.sys.process._
import scala.util.matching.Regex

object CheckMateRunner {
  val pluginNamePattern = "intellij-checkmate-plugin-"
  private lazy val checkmate: String = extractCheckMateBinary()

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

  private var sensitiveFileExtensions = List.empty[SensitiveFile]
  private var sensitiveFileNames = List.empty[SensitiveFile]
  private var excludedSensitivePatterns: List[Regex] = List.empty[Regex]

  loadSensitiveFiles()

  def run(paths: List[String] = List.empty, exclusionPath: String = ""): Array[SecurityDiagnostic] = {
    if (checkmate.contains("unsupported")) return Array.empty
    val wl = if (exclusionPath.nonEmpty) s"""--exclusion="$exclusionPath"""" else ""
    val codePaths = if (paths.nonEmpty) paths.mkString(" ") else "."
    val in = s"${checkmate} secretSearch --source --json $wl $codePaths" !!

    //    println(in)

    val diags = mapper.readValue(in, classOf[Array[SecurityDiagnostic]]).filter(!_.excluded)
    //        for(diag <- diags) {
    //          println(diag)
    //        }
    diags
  }

  def getSensitiveDescription(path: String): Option[String] = {
    val p = Paths.get(path).getFileName
    if (p == null)
      return None

    val name = p.toString
    val fileExt = name.split('.')
    if (fileExt.length == 1) {

      return sensitiveFileNames.filter(x => x.extension == fileExt.head) match {
        case (f: SensitiveFile) :: _ => Some(f.description)
        case _ => None
      }
    }

    val ext = fileExt.takeRight(1).head

    sensitiveFileExtensions.filter(_.extension == s".$ext") match {
      case (f: SensitiveFile) :: _ =>
        if (isExcludedName(name))
          None
        else
          Some(f.description)
      case _ => None
    }
  }

  private def isExcludedName(name: String): Boolean = {
    val fileName = name.toLowerCase
    excludedSensitivePatterns.exists(_.matches(fileName))
  }

  private def loadSensitiveFiles(): List[SensitiveFile] = {
    if (checkmate.contains("unsupported")) return List.empty
    val in = s"${checkmate} secretSearch --sensitive-files" !!

    val files = mapper.readValue(in, classOf[Array[SensitiveFile]]).map(x => x.copy(description = s"Warning! You may be sharing confidential (${x.description}) data with your code"))

    sensitiveFileExtensions = files.filter(x => x.extension.startsWith(".") && !x.excluded).toList
    sensitiveFileNames = files.filterNot(x => x.extension.startsWith(".") && !x.excluded).toList
    excludedSensitivePatterns = files.filter(x => x.excluded).map(_.extension.r).toList
    files.toList
  }

  private def getCheckMateJarPath: String = getClass.getProtectionDomain.getClassLoader match {
    case plug: PluginClassLoader =>
      plug.getUrls.asScala.map(x => Paths.get(x.toURI).toAbsolutePath).find(_.toString.matches(s".*${pluginNamePattern}.*[.]jar")) match {
        case Some(path) => path.toString
        case None => throw new Exception("CheckMate Jar not found in the PluginClassLoader URLs")
      }
    case x =>
      //Production environment doesn't seem to use PluginClassLoader. Look in the ProtectionDomain
      val pd = getClass.getProtectionDomain
      val codeSource = pd.getCodeSource
      if (codeSource != null && codeSource.getLocation != null) {
        val location = codeSource.getLocation
        if (location.getPath.contains(s"${pluginNamePattern}")) {
          val rx = s".*file:(.*$pluginNamePattern.*[.]jar).*".r
          val dPath = URLDecoder.decode(location.getPath, "UTF-8")
          val path = rx.findAllIn(dPath).group(1)
          if (path != null)
            return path
          else throw new Exception(s"CheckMate path could not be extracted from ${location.getPath}")
        }
      }
      throw new Exception(s"CheckMate has been loaded by a different ClassLoader $x (not PluginClassLoader)" +
        s"Class = ${getClass} ; ProtectionDomain = ${getClass.getProtectionDomain} " +
        s"CodeSource = ${getClass.getProtectionDomain.getCodeSource} ; " +
        s"CodeSourceLocation = ${getClass.getProtectionDomain.getCodeSource.getLocation} ;" +
        s"CodeSourceLocationPath = ${getClass.getProtectionDomain.getCodeSource.getLocation.getPath} ;")
  }

  private def extractCheckMateBinary(): String = {
    //    println("Extracting checkmate on " + System.getProperty("os.name").toLowerCase)
    val osName = System.getProperty("os.name").toLowerCase
    var cMate = s"checkmate_unsupported_${osName}"
    val os = osName match {
      case os if os.contains("mac") => "darwin"
      case os if os.contains("win") => "windows"
      case os if os.contains("nux") => "linux"
      case _ => "unsupported"
    }
    if (os == "unsupported") { //unsupported platform - fail gracefully!
      println("Unsupported CheckMate platform: plugin will not work")
      return s"${cMate}_unsupported_$osName"
    }

    try {
      val jar = new JarFile(getCheckMateJarPath)
      val entries = jar.entries()
      val native = s".*checkmate_.*$os.*[.]tar[.]gz"
      var proceed = true

      while (proceed && entries.hasMoreElements) {
        val entry = entries.nextElement()
        if (entry.getName.toLowerCase.matches(native)) {
          val tis = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(jar.getInputStream(entry))))
          var loop = true
          while (loop) {
            val tarEntry = tis.getNextEntry
            if (tarEntry == null) {
              loop = false
              tis.close()
            } else {
              if (tarEntry.getName.startsWith("checkmate")) {
                val path = Paths.get(System.getProperty("user.home"), ".checkmate", "checkmate")
                val out = path.toFile
                out.getParentFile.mkdirs()
                IOUtils.copy(tis, new FileOutputStream(out))
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"))
                cMate = path.toAbsolutePath.toString
                proceed = false
                jar.close()
              }
            }
          }
        }
      }
      //      println(s"Extracted CheckMate at $cMate")
      cMate
    } catch {
      case x: Throwable =>
        println(s"""CheckMate Exception: ${x.getMessage}\n ${x.getStackTrace.mkString("\n")}""")
        cMate
    }
  }
}

