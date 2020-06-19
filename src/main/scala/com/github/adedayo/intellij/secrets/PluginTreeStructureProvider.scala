package com.github.adedayo.intellij.secrets

import java.nio.file.Paths
import java.util

import com.github.adedayo.checkmate.CheckMateRunner
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.projectView.{TreeStructureProvider, ViewSettings}
import com.intellij.ide.util.treeView.AbstractTreeNode

import scala.jdk.CollectionConverters._

/**
 * @author Adedayo Adetoye
 */
class PluginTreeStructureProvider extends TreeStructureProvider {

  val icon = AllIcons.General.Error

  override def modify(parent: AbstractTreeNode[_], children: util.Collection[AbstractTreeNode[_]], settings: ViewSettings): util.Collection[AbstractTreeNode[_]] = {

    children.asScala.filter(_.isInstanceOf[PsiFileNode]).map(_.asInstanceOf[PsiFileNode]).foreach(child => {

      if (child != null && child.getVirtualFile != null && child.getVirtualFile.getName != null) {
        val name = child.getVirtualFile.getCanonicalPath
        CheckMateRunner.getSensitiveDescription(name) match {
          case Some(desc) => child.getPresentation.setTooltip(desc)
          case None =>
        }
      }
    })

    children
  }

  override def getData(selected: util.Collection[AbstractTreeNode[_]], dataName: String): AnyRef = null
}
