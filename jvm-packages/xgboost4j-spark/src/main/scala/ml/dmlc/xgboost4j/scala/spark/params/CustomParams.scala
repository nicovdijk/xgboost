/*
 Copyright (c) 2014 by Contributors

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package ml.dmlc.xgboost4j.scala.spark.params

import ml.dmlc.xgboost4j.scala.{EvalTrait, ObjectiveTrait}
import ml.dmlc.xgboost4j.scala.spark.TrackerConf
import org.json4s.JsonAST.JField
import org.json4s.{DefaultFormats, Extraction, FullTypeHints, JValue, NoTypeHints, TypeHints}
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import org.json4s.jackson.Serialization

import org.apache.spark.ml.param.{Param, ParamPair, Params}

object TypeHintsUtil {
  /**
   * Get the TypeHints according to the value
   * @param value the instance of customized obj/eval
   * @return if value is null,
   *            return NoTypeHints
   *         else return the FullTypeHints.
   *
   *         The FullTypeHints will save the full class name into the "jsonClass" of the json,
   *         so we can find jsonClass and turn it to FullTypeHints when deserializetion.
   */
  def getTypeHints(value: Any): TypeHints = {
    var typeHints: TypeHints = NoTypeHints
    if (value != null) { // XGBoost will save the default values
      typeHints = FullTypeHints(List(value.getClass))
    }
    typeHints
  }

  /**
   * Extract TypeHints from the saved jsonClass field
   * @param json
   * @return TypeHints
   */
  def extractTypeHint(json: JValue): TypeHints = {
    val jsonClassField = json findField {
      case JField("jsonClass", _) => true
      case _ => false
    }

    jsonClassField.map { field =>
      implicit val formats = DefaultFormats
      val className = field._2.extract[String]
      FullTypeHints(List(Utils.classForName(className)))
    }.getOrElse(NoTypeHints)
  }
}

class CustomEvalParam(
    parent: Params,
    name: String,
    doc: String) extends Param[EvalTrait](parent, name, doc) {

  /** Creates a param pair with the given value (for Java). */
  override def w(value: EvalTrait): ParamPair[EvalTrait] = super.w(value)

  override def jsonEncode(value: EvalTrait): String = {
    implicit val format = Serialization.formats(TypeHintsUtil.getTypeHints(value))
    compact(render(Extraction.decompose(value)))
  }

  override def jsonDecode(json: String): EvalTrait = {
    val js = parse(json)
    implicit val formats = DefaultFormats.withHints(TypeHintsUtil.extractTypeHint(js))
    js.extract[EvalTrait]
  }
}

class CustomObjParam(
    parent: Params,
    name: String,
    doc: String) extends Param[ObjectiveTrait](parent, name, doc) {

  /** Creates a param pair with the given value (for Java). */
  override def w(value: ObjectiveTrait): ParamPair[ObjectiveTrait] = super.w(value)

  override def jsonEncode(value: ObjectiveTrait): String = {
    implicit val format = Serialization.formats(TypeHintsUtil.getTypeHints(value))
    compact(render(Extraction.decompose(value)))
  }

  override def jsonDecode(json: String): ObjectiveTrait = {
    val js = parse(json)
    implicit val formats = DefaultFormats.withHints(TypeHintsUtil.extractTypeHint(js))
    js.extract[ObjectiveTrait]
  }

}

class TrackerConfParam(
    parent: Params,
    name: String,
    doc: String) extends Param[TrackerConf](parent, name, doc) {

  /** Creates a param pair with the given value (for Java). */
  override def w(value: TrackerConf): ParamPair[TrackerConf] = super.w(value)

  override def jsonEncode(value: TrackerConf): String = {
    import org.json4s.jackson.Serialization
    implicit val formats = Serialization.formats(NoTypeHints)
    compact(render(Extraction.decompose(value)))
  }

  override def jsonDecode(json: String): TrackerConf = {
    implicit val formats = DefaultFormats
    val parsedValue = parse(json)
    parsedValue.extract[TrackerConf]
  }
}
