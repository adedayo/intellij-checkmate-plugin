package com.github.adedayo.intellij.secrets

import com.github.adedayo.checkmate.CheckMateRunner
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * @author Adedayo Adetoye
 */
class DangerousFileTypeFactory extends FileTypeIdentifiableByVirtualFile {

  def isMyFileType(file: VirtualFile): Boolean = {
    val name: String = file.getCanonicalPath
    CheckMateRunner.getSensitiveDescription(name) match {
      case Some(_) => true
      case None => false
    }
  }

  def getName: String = DangerousFileType.NAME

  def getDescription: String = DangerousFileType.NAME

  def getDefaultExtension: String = ""

  def getIcon: Icon = AllIcons.General.Error

  def isBinary: Boolean = false

  def isReadOnly: Boolean = false

  def getCharset(file: VirtualFile, content: Array[Byte]): String = "UTF-8"
}


object DangerousFileType {
  val NAME = "Security-sensitive files"
}


