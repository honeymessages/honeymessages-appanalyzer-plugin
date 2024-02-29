package de.tubs.cs.ias.appanalyzer

import de.halcony.appanalyzer.analysis.Analysis
import de.halcony.appanalyzer.analysis.exceptions.SkipThisApp
import de.halcony.appanalyzer.analysis.interaction.{Interface, InterfaceElementInteraction}
import de.halcony.appanalyzer.analysis.plugin.ActorPlugin
import de.halcony.appanalyzer.platform.appium.Appium
import spray.json.JsonParser.ParsingException
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsonParser}
import wvlet.log.LogSupport

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.util.UUID
import scala.annotation.tailrec
import scala.io.StdIn.readLine

class ChatInstrumentation() extends ActorPlugin with LogSupport {

  private var apiEndpointUrl : Option[String] = None
  private var authToken : Option[String] = None
  private val client : HttpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.ALWAYS)
    .build()

  private val NEW_MESSENGER = "/api/messengers/"
  private val REGISTER_EXPERIMENT = "/api/experiments/"
  private val EXPERIMENT_DONE = "/experiments/<token>/finish_manually/"
  private var messenger_id : Option[Int] = None
  private val EXPERIMENTS = List(
      ("with_meta_tags_honeypage","honeypage_link"),
    ("with_honeypage","honeypage_link"),
      ("with_honeymail","honeymail_address"),
    ("with_suspicious_honeypage", "honeypage_link"),
    ("sus","http://us-ps-postaz.top")
  )

  private def newRequestBuilder(endpoint : String, param : (String,String)*) : HttpRequest.Builder = {
    HttpRequest.newBuilder(URI.create(apiEndpointUrl.get + endpoint +
      (if(param.nonEmpty) param.map{case (key,value) => s"$key=$value"}.mkString("?","&","") else "")))
  }

  private[appanalyzer] def authenticate() : Unit = {
    authToken = Some(getAuthenticateToken)
  }

  private[appanalyzer] def getAuthenticateToken : String = {
    s"Token 341318d164c8d1c28592d10085b514af6feefc4f"
  }

  override def setParameter(parameter: Map[String, String]): ActorPlugin = {
    apiEndpointUrl = Some(parameter("domain"))
    this
  }

  override def getDescription: String = "Chat Instrumentation"

  private[appanalyzer] def setMessengerId(messenger : String) : Unit = {
    messenger_id = Some(createOrGetMessenger(messenger))
  }

  private[appanalyzer] def getMessengers : Map[String,Int] = {
    val request = newRequestBuilder(NEW_MESSENGER)
      .setHeader("Authorization",authToken.get)
    val body = client.send(request.build(),HttpResponse.BodyHandlers.ofString()).body()
    val ret = JsonParser(body).asJsObject.fields("results").asInstanceOf[JsArray].elements.map {
      value =>
        val obj = value.asJsObject
        obj.fields("name").asInstanceOf[JsString].value -> obj.fields("id").asInstanceOf[JsNumber].value.toInt
    }.toMap
    info(ret.map(pair => s"${pair._1} => ${pair._2}").mkString("\n"))
    ret
  }

  @tailrec
  final private[appanalyzer] def createOrGetMessenger(messenger : String, tries : Int = 3) : Int = {
    if(tries == 0) throw new RuntimeException("cannot create new messenger, three failed attempts")
    getMessengers.get(messenger) match {
      case Some(value) =>
        info(s"messenger already exists with id $value")
        value
      case None => val data = JsObject(
        "name" -> JsString(messenger + "2"),
        "manual_only" -> JsBoolean(true)
      )
        val request = newRequestBuilder(NEW_MESSENGER)
          .setHeader("Authorization", authToken.get)
          .setHeader("Content-Type", "application/json; charset=utf8")
          .POST(HttpRequest.BodyPublishers.ofString(data.prettyPrint))
          .build()
        val body = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        //println(body)
        try {
          val response = JsonParser(body)
          response.asJsObject.fields("id").asInstanceOf[JsNumber].value.toInt
        } catch {
          case x : ParsingException =>
            error(x.getMessage)
            error(body)
            createOrGetMessenger(messenger,tries - 1)
        }

    }
  }

  private[appanalyzer] def startExperiment(experiment : String, dataPoint : String) : (String,Int) = {
    val data = JsObject(
      "name" -> JsString(UUID.randomUUID().toString),
      "messenger_id" -> JsNumber(messenger_id.get),
      experiment -> JsBoolean(true),
      "manuel" -> JsNumber(1)
    )
    val request = newRequestBuilder(REGISTER_EXPERIMENT)
      .setHeader("Content-Type","application/json; charset=utf8")
      .setHeader("Authorization",authToken.get)
      .POST(
        HttpRequest.BodyPublishers.ofString(data.prettyPrint)
      ).build()
    val response = client.send(request,HttpResponse.BodyHandlers.ofString()).body()
    val json = try {
      JsonParser(response).asJsObject
    } catch {
      case x : Throwable =>
        error(response)
        throw x
    }
    (json.fields(dataPoint).asInstanceOf[JsString].value,json.fields("id").asInstanceOf[JsNumber].value.toInt)
  }

  private[appanalyzer] def confirmExperimentDone(experiment : Int) : Unit = {
      client.send(newRequestBuilder(EXPERIMENT_DONE.replace("<token>",experiment.toString)).build(),
        HttpResponse.BodyHandlers.ofString())
  }

  private def waitForNext(message : String) : Unit = {
    if("skip" == readLine(message + "    ").trim) {
      info("skipping this app for now")
      throw SkipThisApp("user says to skip this app")
    }
  }

  override def action(interface: Interface)(implicit context: Analysis, appium: Appium): Option[InterfaceElementInteraction] = {
    try {
      waitForNext(s"press enter to start traffic collection for ${context.getCurrentApp.toString()}")
      if(context.deviceIsRooted) {
        context.stopTrafficCollection()
        context.startTrafficCollection(None,"Chat Instrumentation")
      } else {
        info("device is not rooted - not switching to traffic collection")
      }
      info(s"conducting honey message experiment for app ${context.getCurrentApp.toString()}")
      authenticate()
      setMessengerId(context.getCurrentApp.toString())
      context.checkIfAppIsStillRunning(true) // check if app is running, if not err out
      EXPERIMENTS.foreach {
        case (experiment,dataPoint) =>
          if(experiment == "sus") {
            appium.setClipboardContent(dataPoint)
          } else {
            val (clipboardContent, experimentId): (String, Int) = startExperiment(experiment, dataPoint)
            info(s"Conducting experiment $experimentId of type $experiment using honey data $clipboardContent")
            appium.setClipboardContent(clipboardContent)
            // wait for user confirmation
            confirmExperimentDone(experimentId)
          }
          waitForNext("press enter to continue to the next experiment")
      }
    } finally {
      context.stopTrafficCollection() // after we are done we can stop the traffic collection
    }
    None // we do not want to store interactions with the app
  }

  override def restartApp: (Boolean, Boolean) = (false,false)

  override def onAppStartup(implicit context: Analysis): Unit = {
    context.setCollectInterfaceElements(false)
    context.startDummyTrafficCollection()
  }
  //context.startTrafficCollection(None,"traffic monitoring for tokens")

}
