package org.allenai.ari.solvers.textilp.utils

import java.net.{InetSocketAddress, URLEncoder}

import org.allenai.ari.solvers.textilp.{Paragraph, Question}

import scala.io.Source
import play.api.libs.json._

object WikiUtils {
  def extractRelevantCategories(category: String): Seq[String] = {
    val wikiCategoryQueryUrl = s"https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:${URLEncoder.encode(category, "UTF-8")}&cmlimit=100&format=json"
    val html = Source.fromURL(wikiCategoryQueryUrl)
    val jsonString = html.mkString
    val json = Json.parse(jsonString)
    (json \\ "title").map { jsValue =>
      jsValue.as[JsString]
    }.collect{case jsValue if !jsValue.value.contains("List of") && !jsValue.value.contains("Template:") => jsValue.value}
  }

  // the Bishop
  def extractCategoryOfWikipage(wikipageMention: String): Seq[String] = {
    val wikiCategoryQueryUrl = s"https://en.wikipedia.org/w/api.php?format=json&action=query&prop=categories&titles=${URLEncoder.encode(wikipageMention, "UTF-8")}"
    val html = Source.fromURL(wikiCategoryQueryUrl)
    val jsonString = html.mkString
    // println(jsonString)
    val json = Json.parse(jsonString)
    (json \\ "title").map { jsValue =>
      jsValue.as[JsString]
    }.collect{case jsValue if jsValue.value.contains("Category:") => jsValue.value}
  }

  def extractCategoryOfWikipageRecursively(wikipageMentions: Seq[String], depth: Int): Seq[String] = {
    if(depth > 0) {
      val mentionsDepthLower = wikipageMentions.flatMap{ m => extractCategoryOfWikipage(m) }
      extractCategoryOfWikipageRecursively(mentionsDepthLower.toSet.union(wikipageMentions.toSet).toSeq, depth - 1)
    }
    else {
      wikipageMentions
    }
  }

  // this function gets the Wikipedia id of the page and returns its WikiData id
  def getWikiDataId(title: String): Option[String] = {
    val wikiCategoryQueryUrl = s"https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&titles=${URLEncoder.encode(title, "UTF-8")}"
    val html = Source.fromURL(wikiCategoryQueryUrl)
    val jsonString = html.mkString
    val json = Json.parse(jsonString)
    (json \\ "wikibase_item").map { jsValue =>
      jsValue.as[JsString]
    }.map{ _.value }.headOption
  }

  // important WikiData relations
  object WikiDataProperties {
    val instanceOf = "P31"
    val subclassOf = "P279"

    val person = "Q5"
    val country = "Q6256"
    val soverignState = "Q3624078"
    val memberOfUN = "Q160016"


    // location?
    // time?
    // color

  }

//  def wikiAskQueryWithMaxRepetition(qStr: String, pStr: String, property: String, maxRepetition: Int): Boolean = {
//    val result = (1 to maxRepetition).exists(wikiAskQuery(qStr, pStr, property, _))
//    println(s"$qStr -> $property -> $pStr: $result")
//    result
//  }

  def wikiAskQuery(qStr: String, pStr: String, property: String, mostOccurrences: Int): Boolean = {
    val qStrId = getWikiDataId(qStr)
    val pStrId = getWikiDataId(pStr)
    val result = if(qStrId.isDefined && pStrId.isDefined) {
      val qStrIdEncoded = URLEncoder.encode(qStrId.get, "UTF-8")
      val pStrIdEncoded = URLEncoder.encode(pStrId.get, "UTF-8")
      val propertyEncoded = URLEncoder.encode(property, "UTF-8")
      require(mostOccurrences > 0)
      // no "?" for the first one
      val path = "wdt:" + propertyEncoded + "/" + (1 until mostOccurrences).map(_ => "wdt:" + propertyEncoded + "?").mkString("/")
      val wikiCategoryQueryUrl = s"https://query.wikidata.org/sparql?format=json&query=ASK%20{%20wd:$qStrIdEncoded%20$path%20wd:$pStrIdEncoded%20}"
      println("qStr: " + qStr + " / pStr: " + pStr + " / query: " + wikiCategoryQueryUrl)
      val html = Source.fromURL(wikiCategoryQueryUrl)
      val jsonString = html.mkString
      val json = Json.parse(jsonString)
      (json \ "boolean").as[JsBoolean].value
    }
    else {
      false
    }
//    println(s"$qStr -> $property -> $pStr: $result")
    result
  }

  def getWikiBaseCandidatesForQuestion(question: Question): Seq[String] = {
    Seq.empty
  }

//  def wikiDataDistance(ent1: String, ent2: String, property: String): Int = {
//
//  }
}