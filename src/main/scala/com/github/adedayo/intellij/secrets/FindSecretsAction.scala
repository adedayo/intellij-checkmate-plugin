package com.github.adedayo.intellij.secrets

import com.github.adedayo.intellij.utils.InspectionRunner
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.components.PathMacroManager

class FindSecretsAction extends AnAction {
  val inspectionName = "FindSecretsInspectionLocal"

  def actionPerformed(e: AnActionEvent): Unit = {
    val configDir = PathMacroManager.getInstance(e.getProject).expandPath("$PROJECT_DIR$") + "/.checkmate"
    InspectionRunner.runInspection(inspectionName, e, configDir)
  }
}
