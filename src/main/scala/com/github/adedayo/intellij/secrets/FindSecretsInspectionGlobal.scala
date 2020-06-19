package com.github.adedayo.intellij.secrets

import java.nio.file.Paths

import com.github.adedayo.checkmate.CheckMateRunner
import com.github.adedayo.intellij.utils.{ConfigurationManager, TooltipUtils}
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection._
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.{PsiBinaryFile, PsiFile, PsiManager}
import com.intellij.util.Processor

class FindSecretsInspectionGlobal extends GlobalInspectionTool {

  val globalSuppressWarning = new GlobalSuppressWarning
  val localInspection = new FindSecretsInspectionLocal

  override def getStaticDescription: String = "Find secrets in code and configuration files"

  override def getSharedLocalInspectionTool: FindSecretsInspectionLocal = localInspection


  override def runInspection(scope: AnalysisScope, manager: InspectionManager, globalContext: GlobalInspectionContext, problemDescriptionsProcessor: ProblemDescriptionsProcessor): Unit = {
    var files = Map.empty[String, PsiFile]
    val psiManager = PsiManager.getInstance(manager.getProject)
    val docManager = FileDocumentManager.getInstance
    val dir = LocalConfig.getConfigDirectory(manager.getProject)
    val exclusions = ConfigurationManager.loadExclusions(dir)
    val refManager = globalContext.getRefManager

    scope.accept(new Processor[VirtualFile] {
      override def process(f: VirtualFile): Boolean = {
        if (!f.isDirectory) {
          files += (f.getCanonicalPath -> psiManager.findFile(f))
        }
        true
      }
    })

    val excPath = Paths.get(LocalConfig.getConfigDirectory(manager.getProject), ExclusionDefinition.fileName)
    val diags = CheckMateRunner.run(files.keys.toList, exclusionPath = excPath.toString)
    diags.foreach(diag => {
      val problemHighlightType = diag.justification.headline.confidence match {
        case "High" => ProblemHighlightType.GENERIC_ERROR
        case "Medium" => ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        case "Low" => ProblemHighlightType.LIKE_UNUSED_SYMBOL
        case _ => ProblemHighlightType.LIKE_UNUSED_SYMBOL
      }

      var startOffset = 0
      var endOffset = 0
      var text = ""
      val file = files(diag.location)
      val vFile = files(diag.location).getVirtualFile
      val isOnTheFly = true
      val document = docManager.getDocument(vFile)
      if (document != null) {
        startOffset = document.getLineStartOffset(diag.highlightRange.start.line)
        endOffset = document.getLineStartOffset(diag.highlightRange.end.line)
      }
      val textRange = new TextRange(startOffset + diag.highlightRange.start.character, endOffset + diag.highlightRange.end.character)

      if (document != null)
        text = document.getText(textRange)

      val problem = if (file.isInstanceOf[PsiBinaryFile]) {
        manager.createProblemDescriptor(text)
      } else {
        val path = vFile.getCanonicalPath
        val prob = if (exclusions.knownSecretSuppress.contains(path) && exclusions.knownSecretSuppress(path).map(_.secret).intersect(Set(text)).nonEmpty) {
          val intersect = exclusions.knownSecretSuppress(path).filter(_.secret == text)
          val header = s"""Yup, we know about this secret (so sorry &#x1F926;)! It was flagged by <b>${intersect.map(_.riskTaker).mkString(" and ")}</b>. It will be removed!<br><hr>"""
          manager.createProblemDescriptor(file, textRange,
            TooltipUtils.justify(diag.justification, header), ProblemHighlightType.LIKE_DEPRECATED, isOnTheFly, new FileSuppressWarning(file), new LocalSuppressWarning(file), new KnownSecretSuppress(file))
        } else {
          manager.createProblemDescriptor(file,
            textRange,
            TooltipUtils.justify(diag.justification), problemHighlightType, isOnTheFly, globalSuppressWarning, new FileSuppressWarning(file), new LocalSuppressWarning(file), new KnownSecretSuppress(file))
        }
        prob
      }
      problemDescriptionsProcessor.addProblemElement(refManager.getReference(file), problem)
    })

    super.runInspection(scope, manager, globalContext, problemDescriptionsProcessor)
  }
}
