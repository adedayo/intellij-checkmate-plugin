package com.github.adedayo.intellij.utils

import com.github.adedayo.checkmate.model.Justification
import com.intellij.util.ui.UIUtil

/**
 * @author Adedayo Adetoye
 */
object TooltipUtils {
  def html(value: String): String = s"""<html>$value</html>"""

  def justify(just: Justification, header:String=""):String = {

    html(
      s"""$header<b>Problem:</b> ${just.headline.description}. <b>Confidence Level:</b> ${decode(just.headline.confidence)}. <br><hr>
         |<b>Analysis</b><ul>${analyse(just)}</ul>""".stripMargin)
  }

  def analyse(justification: Justification):String = {
    val analyses = for(reason <- justification.reasons) yield {
      s"""<li>${reason.description}. ${decode(reason.confidence)} confidence"""
    }
    analyses.mkString("\n")
  }

  def decode(ratings: String): String = ratings match {
    case "High" => red("High")
    case "Medium" => amber("Medium")
    case "Low" => blue("Low")
    case _ => blue("Low")
  }

  def red(data:String): String = {
    val color = if (UIUtil.isUnderDarcula) "FF6B68" else "red"
    s"<font color='$color'><b>$data</b></font>"
  }

  def amber(data:String): String = {
    val color = if (UIUtil.isUnderDarcula) "FF6B68" else "orange"
    s"<font color='$color'><b>$data</b></font>"
  }

  def blue(data:String): String = {
    val color = if (UIUtil.isUnderDarcula) "FF6B68" else "gray"
    s"<font color='$color'><b>$data</b></font>"
  }

}
