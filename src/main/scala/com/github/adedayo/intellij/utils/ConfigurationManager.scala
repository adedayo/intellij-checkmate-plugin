package com.github.adedayo.intellij.utils

import java.nio.file.Paths

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.adedayo.intellij.secrets.ExclusionDefinition

/**
 * @author Adedayo Adetoye
 */
object ConfigurationManager {

  val mapper = new ObjectMapper(new YAMLFactory)
  mapper.registerModule(DefaultScalaModule)
  mapper.enable(SerializationFeature.INDENT_OUTPUT)
  mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

  def loadExclusions(location: String): ExclusionDefinition = {
    val instance = new ExclusionDefinition
    val file = Paths.get(location, ExclusionDefinition.fileName).toFile
    if (!file.exists()) {
      file.getParentFile.mkdirs()
      instance.setLocation(location)
      instance
    } else {
      val config = mapper.readValue(file, classOf[ExclusionDefinition])
      config.setLocation(location)
      config
    }
  }
}
