package com.github.adedayo.intellij.secrets

import com.github.adedayo.intellij.utils.ConfigurationManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInspection.{LocalQuickFix, ProblemDescriptor}
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.project.Project
import com.intellij.psi.{PsiDocumentManager, PsiFile}

object LocalConfig {
  def getConfigDirectory(project: Project): String = {
    PathMacroManager.getInstance(project).expandPath("$PROJECT_DIR$") + "/.checkmate"
  }
}

class GlobalSuppressWarning extends LocalQuickFix {
  override def getFamilyName: String = "Disable warning for this string everywhere"

  override def getName: String = "Disable warning for this string everywhere"

  override def applyFix(project: Project, d: ProblemDescriptor): Unit = {
    val file = d.getPsiElement.getContainingFile
    val dir = LocalConfig.getConfigDirectory(project)
    val docManager = PsiDocumentManager.getInstance(project)
    val document = docManager.getDocument(file)
    val text = document.getText(d.getTextRangeInElement)
    val whitelists = ConfigurationManager.loadExclusions(dir)
    whitelists.addGloballyExcludedString(text)
    DaemonCodeAnalyzer.getInstance(project).restart(file)
  }
}


class LocalSuppressWarning(file: PsiFile) extends LocalQuickFix {
  override def getFamilyName: String = "Disable warning for this string in this file"

  override def getName: String = "Disable warning for this string in this file"

  override def applyFix(project: Project, d: ProblemDescriptor): Unit = {
    //    val file = d.getPsiElement.getContainingFile
    val docManager = PsiDocumentManager.getInstance(project)
    val document = docManager.getDocument(file)
    val text = document.getText(d.getTextRangeInElement)
    val dir = LocalConfig.getConfigDirectory(project)
    val whitelists = ConfigurationManager.loadExclusions(dir)
    val filePath = file.getVirtualFile.getCanonicalPath.stripPrefix(dir)
    whitelists.addPerFileGloballyExcludedString(filePath, text)
    DaemonCodeAnalyzer.getInstance(project).restart(file)
  }
}

class FileSuppressWarning(file: PsiFile) extends LocalQuickFix {
  override def getFamilyName: String = "Ignore this file in the future"

  override def getName: String = "Ignore this file in the future"

  override def applyFix(project: Project, d: ProblemDescriptor): Unit = {
    //    val file = d.getPsiElement.getContainingFile
    val dir = LocalConfig.getConfigDirectory(project)
    val whitelists = ConfigurationManager.loadExclusions(dir)
    val filePath = file.getVirtualFile.getCanonicalPath.stripPrefix(dir)
    whitelists.addPathExclusionPattern(filePath)
    DaemonCodeAnalyzer.getInstance(project).restart(file)
  }
}


class KnownSecretSuppress(file: PsiFile) extends LocalQuickFix {
  override def getFamilyName: String = "Yeah, we know about this secret! Promise to remove it"

  override def getName: String = "Yeah, we know about this secret! Promise to remove it"

  override def applyFix(project: Project, d: ProblemDescriptor): Unit = {
    //    val file = d.getPsiElement.getContainingFile
    val docManager = PsiDocumentManager.getInstance(project)
    val document = docManager.getDocument(file)
    val text = document.getText(d.getTextRangeInElement)
    val dir = LocalConfig.getConfigDirectory(project)
    val whitelists = ConfigurationManager.loadExclusions(dir)
    val filePath = file.getVirtualFile.getCanonicalPath.stripPrefix(dir)
    whitelists.addKnownSecret(filePath, text)
    DaemonCodeAnalyzer.getInstance(project).restart(file)
  }
}

