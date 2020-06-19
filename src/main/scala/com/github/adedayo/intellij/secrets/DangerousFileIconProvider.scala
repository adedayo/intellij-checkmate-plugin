package com.github.adedayo.intellij.secrets

import com.github.adedayo.checkmate.CheckMateRunner
import com.intellij.icons.AllIcons
import com.intellij.ide.IconProvider
import com.intellij.psi.{PsiElement, PsiFile}
import com.intellij.ui.LayeredIcon
import javax.swing.Icon

/**
 * @author Adedayo Adetoye
 */
class DangerousFileIconProvider extends IconProvider {
  val icon = AllIcons.General.Error

  override def getIcon(element: PsiElement, flags: Int): Icon = {
    val containingFile: PsiFile = element.getContainingFile
    var dangerous = false
    var baseIcon: Icon = null
    if (containingFile != null) {
      val name: String = containingFile.getVirtualFile.getCanonicalPath
      baseIcon = containingFile.getFileType.getIcon
      CheckMateRunner.getSensitiveDescription(name) match {
        case Some(_) => dangerous = true
        case None =>
      }
    }
    if (dangerous && baseIcon != null) LayeredIcon.create(baseIcon, icon)
    else null
  }
}
