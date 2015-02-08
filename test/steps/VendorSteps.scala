package steps

import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.ShouldMatchers
import play.api.libs.json.Json
import support.World.{responseBody, responseCode, _}
import support.{Mongo, Http, World}
import utils.{ErrorMarshalling, VendorMarshalling}

class VendorSteps extends ScalaDsl with EN with ShouldMatchers with VendorMarshalling with ErrorMarshalling {

  val ConsumerTokenPattern = """^[a-f0-9]{64}$""".r

  val statusCodes = Map("CREATED" -> 201, "BAD_REQUEST" -> 400, "FORBIDDEN" -> 403, "CONFLICT" -> 409)

  Before() { scenario =>
    coll = Mongo.createCollection(mongo, "vendors")
  }
  
  After() { scenario =>
    Mongo.dropCollection(coll)
  }
  
  Given( """^the Admin Token "(.*?)" is presented$""") { (token: String) =>
    World.adminToken = token
  }

  Given("""^an Environment Variable "(.*)"$"""){ (envVar: String) =>
    System.getenv("ADMIN_TOKEN") shouldBe envVar
  }

  When("""^the Create Vendor endpoint "(.*)" is posted a request:$"""){ (endpoint: String, json: String) =>
    val (rc, rb) = Http.postJson(endpoint, json.stripMargin, adminToken)
    World.responseCode = rc
    World.responseBody = rb
  }

  Then("""^the returned status is "(.*?)"$"""){ (status: String) =>
    responseCode shouldBe statusCodes(status)
  }

  Then("""^the payload contains a consumerKey of value "(.*?)"$"""){ (value: String) =>
    Json.parse(responseBody).validate[Response].asOpt match {
      case Some(actual) => actual.consumerKey shouldBe value
      case None => fail("No valid response found.")
    }
  }

  Then("""^the payload contains a valid consumerToken$"""){ () =>
    val actual = Json.parse(responseBody).as[Response]
    actual.consumerToken should fullyMatch regex ConsumerTokenPattern
  }

  Then("""the payload contains a statusCode of value (.*)"""){ (status: Int) =>
    Json.parse(responseBody).validate[ErrorMessage].asOpt match {
      case Some(actual) => actual.statusCode shouldBe status
      case None => fail("No valid status code found.")
    }
  }

  Then("""the payload contains message "(.*)""""){ (message: String) =>
    Json.parse(responseBody).validate[ErrorMessage].asOpt match {
      case Some(actual) => actual.message shouldBe message
      case None => fail("No valid message found.")
    }
  }
  
  Then("""the vendor "(.*)" has been persisted"""){ (vendor: String) =>
    Mongo.vendorExists(coll, "groovy")
  }

  Then("""the persisted vendor "(.*)" has consumerKey "(.*)""""){ (vendor: String, consumerKey: String) =>
    Mongo.vendorConsumerKey(coll, vendor) match {
      case Some(key) => key shouldBe consumerKey
      case None => fail("no vendor found")
    }
  }

  Then("""the persisted vendor "(.*)" has a valid consumerToken"""){ (vendor: String) =>
    Mongo.vendorConsumerToken(coll, vendor) match {
      case Some(token) => token should fullyMatch regex ConsumerTokenPattern
      case None => fail("no vendor found")
    }
  }

}