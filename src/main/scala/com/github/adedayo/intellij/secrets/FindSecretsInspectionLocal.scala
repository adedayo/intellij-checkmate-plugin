package com.github.adedayo.intellij.secrets

import java.nio.file.Paths

import com.github.adedayo.checkmate.CheckMateRunner
import com.github.adedayo.intellij.utils.{ConfigurationManager, TooltipUtils}
import com.intellij.codeInspection.{InspectionManager, LocalInspectionTool, ProblemDescriptor, ProblemHighlightType}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

/**
 * Locates secrets in code and configuration files
 *
 * @author Adedayo Adetoye
 */
class FindSecretsInspectionLocal extends LocalInspectionTool {
  private val docManager: FileDocumentManager = FileDocumentManager.getInstance
  private val globalSuppressWarning = new GlobalSuppressWarning

  override def checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] = {
    val wl = Paths.get(LocalConfig.getConfigDirectory(manager.getProject), ExclusionDefinition.fileName)
    val diags = CheckMateRunner.run(paths = List(file.getVirtualFile.getCanonicalPath), exclusionPath = wl.toString)
    val document = docManager.getDocument(file.getVirtualFile)
    val dir = LocalConfig.getConfigDirectory(manager.getProject)
    val exclusions = ConfigurationManager.loadExclusions(dir)
    val issues = for (diag <- diags) yield {
      val problemHighlightType = diag.justification.headline.confidence match {
        case "High" => ProblemHighlightType.GENERIC_ERROR
        case "Medium" => ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        case "Low" => ProblemHighlightType.LIKE_UNUSED_SYMBOL
        case _ => ProblemHighlightType.LIKE_UNUSED_SYMBOL
      }

      var startOffset = 0
      var endOffset = 0
      var text = ""
      if (document != null) {
        startOffset = document.getLineStartOffset(diag.highlightRange.start.line)
        endOffset = document.getLineStartOffset(diag.highlightRange.end.line)
      }
      val textRange = new TextRange(startOffset + diag.highlightRange.start.character, endOffset + diag.highlightRange.end.character)

      if (document != null)
        text = document.getText(textRange)

      val path = file.getVirtualFile.getCanonicalPath.stripPrefix(dir)
      val problem = if (exclusions.knownSecretSuppress.contains(path) && exclusions.knownSecretSuppress(path).map(_.secret).intersect(Set(text)).nonEmpty) {
        val intersect = exclusions.knownSecretSuppress(path).filter(_.secret == text)
        val header = s"""Yup, we know about this secret (so sorry &#x1F926;)! It was flagged by <b>${intersect.map(_.riskTaker).mkString(" and ")}</b>. It will be removed!<br><hr>"""
        manager.createProblemDescriptor(file, textRange,
          TooltipUtils.justify(diag.justification, header), ProblemHighlightType.LIKE_DEPRECATED, isOnTheFly, new FileSuppressWarning(file), new LocalSuppressWarning(file), new KnownSecretSuppress(file))
      } else {
        manager.createProblemDescriptor(file,
          textRange,
          TooltipUtils.justify(diag.justification), problemHighlightType, isOnTheFly, globalSuppressWarning, new FileSuppressWarning(file), new LocalSuppressWarning(file), new KnownSecretSuppress(file))
      }
      problem
    }
    issues
  }

  override def getStaticDescription = "Find secrets in code and configuration files"
}