package xitrum.view

import org.fusesource.scalate.Template
import xitrum.Action

/** Additional utility methods. */
trait ScalateEngineRenderTemplate {
  this: ScalateEngine =>

  def renderTemplateFile(templateFile: String)(implicit currentAction: Action): String =
    renderTemplateFile(templateFile, Map.empty)(currentAction)

  /**
   * Renders Scalate template file.
   *
   * @param templateFile  Absolute file path of template
   * @param options       "date" -> DateFormat, "number" -> NumberFormat
   * @param currentAction Will be imported in the template as "helper"
   */
  def renderTemplateFile(templateFile: String, options: Map[String, Any])(implicit currentAction: Action): String = {
    val (context, buffer, out) = createContext(templateFile, fileEngine, currentAction, options)
    try {
      fileEngine.layout(templateFile, context)
      buffer.toString
    } finally {
      out.close()
    }
  }

  //----------------------------------------------------------------------------

  def renderTemplate(template: Template)(implicit currentAction: Action): String =
    renderTemplate(template, "precompiled_template", Map.empty)(currentAction)

  def renderTemplate(template: Template, options: Map[String, Any])(implicit currentAction: Action): String =
    renderTemplate(template, "precompiled_template", options)(currentAction)

  def renderTemplate(template: Template, templateUri: String)(implicit currentAction: Action): String =
    renderTemplate(template, templateUri, Map.empty)(currentAction)

  /**
   * Renders precompiled Scalate template.
   *
   * @param template      Template object
   * @param templateUri   URI to identify a template
   * @param options       "date" -> DateFormat, "number" -> NumberFormat
   * @param currentAction Will be imported in the template as "helper"
   */
  def renderTemplate(template: Template, templateUri: String, options: Map[String, Any])(implicit currentAction: Action): String = {
    val (context, buffer, out) = createContext(templateUri, fileEngine, currentAction, options)
    try {
      //fileEngine.layout(template, context)
      buffer.toString
    } finally {
      out.close()
    }
  }
}
