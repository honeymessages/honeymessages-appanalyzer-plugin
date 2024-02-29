package de.tubs.cs.ias.appanalyzer

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class ChatInstrumentationTest extends AnyWordSpec with Matchers {

  val config: Map[String, String] = Map(
    "apiUser" -> "simon",
    "apiPwd" -> "Halogen-Carpenter-container6",
    "domain" -> "https://api.honey.ias-lab.de"
  )

  "authenticate" should {
    "give me a token" in {
      val chatInstrumentation = new ChatInstrumentation()
      chatInstrumentation.setParameter(config)
      val token = chatInstrumentation.getAuthenticateToken
      (token.length > "Token ".length) shouldBe true
    }
  }

  "createOrGetMessenger" should {
    "give me a new messenger" in {
      val chatInstrumentation = new ChatInstrumentation()
      chatInstrumentation.setParameter(config)
      chatInstrumentation.authenticate()
      val uuid = UUID.randomUUID().toString
      val messengerId = chatInstrumentation.createOrGetMessenger(uuid)
      messengerId > 0 shouldBe true
    }
    "give me the same messenger on reregistration" in {
      val chatInstrumentation = new ChatInstrumentation()
      chatInstrumentation.setParameter(config)
      chatInstrumentation.authenticate()
      val uuid = UUID.randomUUID().toString
      val messengerId = chatInstrumentation.createOrGetMessenger(uuid)
      messengerId > 0 shouldBe true
      val next = chatInstrumentation.createOrGetMessenger(uuid)
      messengerId shouldBe next
    }
  }

  "experiment start/stop" should {
    "work for with_honeypage" in {
      val chatInstrumentation = new ChatInstrumentation()
      chatInstrumentation.setParameter(config)
      chatInstrumentation.authenticate()
      val uuid = UUID.randomUUID().toString
      chatInstrumentation.setMessengerId(uuid)
      val (content,id) = chatInstrumentation.startExperiment("with_honeypage","honeypage_link")
      content.nonEmpty shouldBe true
      id > 0 shouldBe true
      chatInstrumentation.confirmExperimentDone(id)
    }
  }

}
