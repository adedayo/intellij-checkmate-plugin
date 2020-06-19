package com.github.adedayo.intellij.utils

import java.util

import com.github.adedayo.intellij.secrets.FindSecretsInspectionLocal
import com.intellij.analysis.dialog.{CustomScopeItem, ModelScopeItem, ModuleScopeItem, ProjectScopeItem}
import com.intellij.analysis.{AnalysisUIOptions, BaseAnalysisActionDialog}
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.actions.RunInspectionIntention
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.{PsiDocumentManager, PsiManager}
import com.intellij.util.Processor

/**
 * @author Adedayo Adetoye
 */

object InspectionRunner {

  def runInspection(name: String, e: AnActionEvent, reportPath: String): Unit = {
    e.getProject
    val project: Project = e.getProject
    if (project == null) return

    PsiDocumentManager.getInstance(project).commitAllDocuments()
    val psiManager = PsiManager.getInstance(project)

    val psiElement = CommonDataKeys.PSI_ELEMENT.getData(e.getDataContext)
    val psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext)
    val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext)
    val managerEx = InspectionManager.getInstance(project).asInstanceOf[InspectionManagerEx]
    val module = if (virtualFile != null) ModuleUtilCore.findModuleForFile(virtualFile, project) else null

    val sc = new util.ArrayList[ModelScopeItem]()
    val projectScope = new ProjectScopeItem(project)
    sc.add(projectScope)
    if (module != null) {
      sc.add(new ModuleScopeItem(module))
    }

    sc.add(new CustomScopeItem(project, psiElement))

    val dialog = new BaseAnalysisActionDialog("CheckMate Security Inspection",
      "Find Secrets Inspection",
      project,
      sc, AnalysisUIOptions.getInstance(project), true, true)

    if (!dialog.showAndGet()) {
      return
    }
    
    val scope = dialog.getScope(projectScope.getScope)
    val element = if (psiFile == null) psiElement else psiFile
    val profile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile
    val toolWrapper = profile.getInspectionTool(name, element)

    scope.accept(new Processor[VirtualFile] {
      val localInspection = new FindSecretsInspectionLocal

      override def process(f: VirtualFile): Boolean = {
        if (!f.isDirectory) {
          val psiFile = psiManager.findFile(f)
          if (psiFile != null) {
            localInspection.checkFile(psiFile, managerEx, true)
          }
        }
        true
      }
    })

    RunInspectionIntention.rerunInspection(toolWrapper, managerEx, scope, element)
  }

}