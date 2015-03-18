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
  /**
  * Set Up Google App Credentials
  */
  def prepareGoogleDrive(accessToken: String): Drive = {
    val credential = new GoogleCredential.Builder()
      .setJsonFactory(jsonFactory)
      .setTransport(httpTransport)
      .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
      .build();
    credential.setAccessToken(accessToken);
    new Drive.Builder(httpTransport, jsonFactory, credential).build()
  }

  /**
   * Upload To Google Drive
   */
  // def uploadToGoogleDrive(accessToken: String, fileToUpload: java.io.File, fileName: String, contentType: String): String = {
  //   val service = prepareGoogleDrive(accessToken)
  //   //Insert a file
  //   val body = new File
  //   body.setTitle(fileName)
  //   body.setDescription(fileName)
  //   body.setMimeType(contentType)
  //   val fileContent: java.io.File = fileToUpload
  //   val mediaContent = new FileContent(contentType, fileContent)
  //   //Inserting the files
  //   val file = service.files.insert(body, mediaContent).execute()
  //   file.getId
 
  // }
  /**
   * Get All Files From Google Drive
   */
 
  def getAllDocumentsFromGoogleDocs(code: String) = {
    val service = prepareGoogleDrive(code)
    val result = scala.collection.mutable.ListBuffer.empty[File]
    val request = service.files.list

    do {
      val files = request.execute
      val retrievedFiles = files.getItems
      retrievedFiles.forEach( file => result ++= List[File](file))
      request.setPageToken(files.getNextPageToken)
    } while (request.getPageToken() != null && request.getPageToken().length() > 0)

    result.toList map {
      case a => (a.getOriginalFilename, a.getAlternateLink)
    }
  }
  
}