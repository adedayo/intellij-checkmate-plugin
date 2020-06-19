package com.github.adedayo.intellij.secrets

import java.nio.file.{Files, Paths}

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties}
import com.github.adedayo.intellij.utils.ConfigurationManager

import scala.collection.mutable

object ExclusionDefinition {
  val fileName = "SecretsFinderExclusions.yaml"
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ExclusionDefinition {

  //These specify regular expressions of matching strings that should be ignored as secrets anywhere they are found
  val GloballyExcludedRegExs = mutable.Set.empty[String]
  //These specify strings that should be ignored as secrets anywhere they are found
  val GloballyExcludedStrings = mutable.Set.empty[String]
  //These specify regular expression that ignore files whose paths match
  val PathExclusionRegExs = mutable.Set.empty[String]
  //These specify sets of strings that should be excluded in a given file. That is filepath -> Set(strings)
  val PerFileExcludedStrings = mutable.Map.empty[String, mutable.Set[String]]
  //These specify sets of regular expressions that if matched on a path matched by the filepath key should be ignored. That is filepath_regex -> Set(regex)
  //This is a quite versatile construct and can model the four above
  val PathRegexExcludedRegExs = mutable.Map.empty[String, mutable.Set[String]]

  //This is used to signal technical debt secret removal
  val knownSecretSuppress = mutable.Map.empty[String, mutable.Set[AcceptRisk]]

  //directory where this whitelist is stored
  @JsonIgnore
  private var location: String = ""

  def setLocation(loc: String): Unit = location = loc

  def save(): Unit = {
    val loc = Paths.get(location)
    val out = Paths.get(location, ExclusionDefinition.fileName).toFile
    if (Files.exists(loc)) {
      if (Files.isDirectory(loc)) {
        ConfigurationManager.mapper.writeValue(out, this)
      } else {
        throw new RuntimeException(s"A file ${loc.toString} already exists! Must be a directory.")
      }
    } else {
      Files.createDirectories(loc)
      ConfigurationManager.mapper.writeValue(out, this)
    }
  }

  def addKnownSecret(filePath: String, text: String): Unit = {
    val accepted = AcceptRisk(text, System.getProperty("user.name", "Undefined User"))
    if (knownSecretSuppress.contains(filePath))
      knownSecretSuppress(filePath) += accepted
    else
      knownSecretSuppress += (filePath -> mutable.Set(accepted))
    save()
  }

  def addPathExclusionPattern(pathPattern: String): Unit = {
    PathExclusionRegExs += pathPattern
    save()
  }

  def addPerFileGloballyExcludedString(filePath: String, text: String): Unit = {
    if (PerFileExcludedStrings.contains(filePath))
      PerFileExcludedStrings(filePath) += text
    else
      PerFileExcludedStrings(filePath) = mutable.Set(text)
    save()
  }

  def addGloballyExcludedString(text: String): Unit = {
    GloballyExcludedStrings += text
    save()
  }


}

case class AcceptRisk(secret: String, riskTaker: String)