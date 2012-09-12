package org.scalaide.play2.templateeditor.compiler

import play.templates.ScalaTemplateCompiler
import play.templates.ScalaTemplateCompiler._
import java.io.File
import play.templates.GeneratedSource
import play.templates.TemplateCompilationError
import scalax.file.Path
import org.scalaide.play2.PlayProject

object CompilerUsing {
  val templateCompiler = ScalaTemplateCompiler
  val additionalImports = """import play.templates._
import play.templates.TemplateMagic._
    
    
import play.api.templates._
import play.api.templates.PlayMagic._
import models._
import controllers._
import play.api.i18n._
import play.api.mvc._
import play.api.data._
import views.html._"""

  def compileTemplateToScalaVirtual(content: String, source: File, playProject: PlayProject) = {
    val sourcePath = playProject.sourceDir.getAbsolutePath()
    if (source.getAbsolutePath().indexOf(sourcePath) == -1)
      throw new Exception("Template files must locate in '" + sourcePath + "' or its subfolders!")
    try {
      templateCompiler.compileVirtual(content, source, playProject.sourceDir, "play.api.templates.Html", "play.api.templates.HtmlFormat", additionalImports)
    } catch {
      case e @ TemplateCompilationError(source: File, message: String, line: Int, column: Int) =>
        val offset = PositionHelper.convertLineColumnToOffset(content, line, column)
        throw new TemplateToScalaCompilationError(source, message, offset, line, column)
    }
  }

  def main(args: Array[String]): Unit = {
    val playProject = PlayProject(null)
    //    val result = compile("a1.scala.html", playProject)
    //    val result = compileTemplateToScala(new File("/Users/shaikhha/Documents/workspace-new/asd/a1.scala.html"), playProject)
    val file1 = new File("/Users/shaikhha/Documents/workspace-new/asd/app/views/a/a1.scala.html")
    val result = compileTemplateToScalaVirtual(Path(file1).slurpString, file1, playProject)
    //    val result2 = compile("a2.scala.html", playProject)
    println(result.matrix)
    println(PositionHelper.mapSourcePosition(result.matrix, 58))
    //    println(result.content)
    //    println(result2.matrix)
    TemplateAsFunctionCompiler.CompilerInstance.compiler.askShutdown
  }

}

case class TemplateToScalaCompilationError(source: File, message: String, offset: Int, line: Int, column: Int) extends RuntimeException(message) {
  override def toString = source.getName + ": " + message + offset + " " + line + "." + column
}

object PositionHelper {
  def convertLineColumnToOffset(source: File, line: Int, column: Int): Int = {
    convertLineColumnToOffset(Path(source).slurpString, line, column)
  }

  def convertLineColumnToOffset(content: String, line: Int, column: Int): Int = {
    // splitting the string will cause some problems
    var offset = 0
    for (i <- 1 until line) {
      offset = content.indexOf("\n", offset) + 1
    }
    offset += column - 1
    offset
  }

  def mapSourcePosition(matrix: Seq[(Int, Int)], sourcePosition: Int): Int = {
    val sortedMatrix = matrix.sortBy(_._2)
    sortedMatrix.indexWhere(p => p._2 > sourcePosition) match {
      case 0 => 0
      case i if i > 0 => {
        val pos = sortedMatrix(i - 1)
        pos._1 + (sourcePosition - pos._2)
      }
      case _ => {
        val pos = sortedMatrix.takeRight(1)(0)
        pos._1 + (sourcePosition - pos._2)
      }
    }
  }
}