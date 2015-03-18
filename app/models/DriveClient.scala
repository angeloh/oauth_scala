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

import scala.collection.JavaConverters
import java.util.concurrent._
import java.util.Arrays // added this for the Arrays.asList()
import java.net._ // ADDING FOR URL
import javax.net.ssl.HttpsURLConnection // Added for instanceOf(HttpsConnection)
import java.io.{DataOutputStream, InputStreamReader, BufferedReader} // Added three objects for use below
import scala.concurrent.stm._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

// Google OAuth Packages

import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential, GoogleBrowserClientRequestUrl} // Added google growser client
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory // added the '2' after 'jackson' to get object
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList

import com.google.gdata.client.spreadsheet.SpreadsheetService
import com.google.gdata.data.spreadsheet.ListEntry
import com.google.gdata.data.spreadsheet.ListFeed
import com.google.gdata.data.spreadsheet.SpreadsheetEntry
import com.google.gdata.data.spreadsheet.WorksheetEntry
import com.google.gdata.util.ServiceException

object DriveClient {

  val mongoDriver = new MongoDriver
  val mongoConnection = mongoDriver.connection(List("localhost"))
  val mongoDb = mongoConnection("matt")
  val tokenCollection : BSONCollection = mongoDb.collection("tokens")
  val driveCollection : BSONCollection = mongoDb.collection("drive")

  val CLIENT_ID = GoogleClient.getClientID
  val CLIENT_SECRET = GoogleClient.getSecret
  val httpTransport = new NetHttpTransport
  val jsonFactory = new JacksonFactory

  def prepareGoogleDrive(accessToken: String): Drive = {
    val credential = new GoogleCredential.Builder()
      .setJsonFactory(jsonFactory)
      .setTransport(httpTransport)
      .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
      .build();
    credential.setAccessToken(accessToken);
    new Drive.Builder(httpTransport, jsonFactory, credential).build()
  }

  def getAllDocumentsFromGoogleDocs(code: String) = {
    val service = prepareGoogleDrive(code)
    val result = scala.collection.mutable.ListBuffer.empty[List[File]]
    val request = service.files.list

    do {
      val files = request.execute
      val listOfFiles = JavaConverters
        .asScalaBufferConverter(files.getItems)
        .asScala
        .toList
      result ++= List[List[File]](listOfFiles)
      request.setPageToken(files.getNextPageToken)
    } while (request.getPageToken() != null && request.getPageToken().length() > 0)

    result.toList.flatten map {
      case a => (a.getOriginalFilename, a.getAlternateLink, a.getId)
    }
  }

  def getSpreadSheet(id: String) = {
    val url = "https://spreadsheets.google.com/feeds/spreadsheets/" + id
    val service = new SpreadsheetService("OAuth Scala")
    service.setClientCredentials(CLIENT_ID, CLIENT_SECRET)
    service.setAccessToken(token)
    val metafeedUrl = new URL(url)
    val spreadsheet = service.getEntry(metafeedUrl, SpreadsheetEntry.getClass)
    val listFeedUrl = spreadsheet.getWorksheets().get(0).getListFeedUrl
    val feed = service.getFeed(listFeedUrl, ListFeed.getClass)
    for ( entry <- feed.getEntries ) {
      println("new row")
      for ( tag <- entry.getCustomElements.getTags ) {
        println("     "+tag + ": " + entry.getCustomElements.getValue(tag))
      }
    }
  }
  
}