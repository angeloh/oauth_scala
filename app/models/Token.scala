package models

import play.api.mvc._
import play.api.libs.json.Json
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats._

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC

import scala.concurrent.Future
import scala.util.{Failure, Success}

import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._

/**
  * A token class
  *
  *@param _id The BSON object id of the token
  *@param expiresAt the time until it expires
  */
  

case class Token(
  _id: BSONObjectID, 
  whenCreated  : Option[DateTime],
  whenUpdated  : Option[DateTime],
  accessToken  : String,
  refreshToken : String,
  expiresAt    : String
)

object Token {

  val mongoDriver = new MongoDriver
  val mongoConnection = mongoDriver.connection(List("localhost"))
  val mongoDb = mongoConnection("matt")
  val collection : BSONCollection = mongoDb.collection("tokens")

  implicit object TokenBSONAccessor extends BSONDocumentReader[Token] with BSONDocumentWriter[Token] {
    def read(doc: BSONDocument): Token = {
      Token(
        doc.getAs[BSONObjectID]("_id").get,
        doc.getAs[BSONDateTime]("whenCreated").map(dt => new DateTime(dt.value)),
        doc.getAs[BSONDateTime]("whenUpdated").map(dt => new DateTime(dt.value)),
        doc.getAs[BSONString]("accessToken").get.value,
        doc.getAs[BSONString]("refreshToken").get.value,
        doc.getAs[BSONString]("expiresAt").get.value
      )
    }
    def write(token: Token) = {
      val bson = BSONDocument(
        "_id" -> BSONObjectID.generate,
        "whenCreated"  -> token.whenCreated.map(dt => BSONDateTime(dt.getMillis)),
        "whenUpdated"  -> token.whenUpdated.map(dt => BSONDateTime(dt.getMillis)),
        "accessToken"  -> BSONString(token.accessToken),
        "refreshToken" -> BSONString(token.refreshToken),
        "expiresAt"    -> BSONString(token.expiresAt)
        )
      bson
    }
  }

  def insert(response: StringBuffer) = {
    val attrs = parseResponse(response.toString)
    collection.insert(new Token(
        BSONObjectID.generate,
        whenCreated  = Some(DateTime.now(UTC)),
        whenUpdated  = None,
        attrs("accessToken"),
        attrs("refreshToken"),
        attrs("expiresAt")
      )
    )
  }

  def parseResponse(response: String) : Map[String, String] = {
    val responseData = scrubString(response)
    return Map(
      "accessToken"  -> parseValue(responseData, "access"),
      "refreshToken" -> parseValue(responseData, "refresh"),
      "expiresAt"    -> parseValue(responseData, "expires")
    )
  }

  def parseValue(data: Array[Array[String]], key: String) : String = {
    return data.find(_(0).contains(key)).get(1) 
  }

  def scrubString(string: String) : Array[Array[String]] = {
    return string.replace(" ","").replace("null","").replace("\"","").split(",").map(_.split(":"))
  }
  
}