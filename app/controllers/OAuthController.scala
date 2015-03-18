
package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.Play.current
import play.api.libs._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import java.util.concurrent._
import java.util.Arrays // added this for the Arrays.asList()
import java.net._ // ADDING FOR URL
import javax.net.ssl.HttpsURLConnection // Added for instanceOf(HttpsConnection)
import java.io.{DataOutputStream, InputStreamReader, BufferedReader} // Added three objects for use below
import scala.concurrent.stm._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.cache._
import play.api.libs.json._
import play.api.libs.json.Json

// Google OAuth Packages

import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential, GoogleBrowserClientRequestUrl} // Added google growser client
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory // added the '2' after 'jackson' to get object
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File

object OAuthController extends Controller {

  val CLIENT_ID = GoogleClient.getClientID
  val CLIENT_SECRET = GoogleClient.getSecret

  val redirectURI = "http://localhost:9000/driveAuth"

  def authenticateToGoogle = Action { implicit request =>
    val urlToRedirect = new GoogleBrowserClientRequestUrl(
      CLIENT_ID, 
      redirectURI, 
      Arrays.asList(
        "https://www.googleapis.com/auth/plus.login", 
        "https://www.googleapis.com/auth/drive"
      )
    )
    .set("access_type", "offline")
    .set("response_type", "code")
    .build()
    Redirect(urlToRedirect)
  }

  /**
     * Google Oauth2 accessing code and exchanging it for Access & Refresh Token
     */
  def googleDriveAuthentication = Action { implicit request =>
    val code = request.queryString("code").toList(0)
    val url = "https://accounts.google.com/o/oauth2/token"
    val obj = new URL(url)
    val con = obj.openConnection().asInstanceOf[HttpsURLConnection]

    con.setRequestMethod("POST");
    con.setRequestProperty("User-Agent", USER_AGENT);
    con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
    val urlParameters = "code=" + code + "&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET + "&redirect_uri=http://localhost:9000/driveAuth&grant_type=authorization_code&Content-Type=application/x-www-form-urlencoded";
    con.setDoOutput(true)
    val wr = new DataOutputStream(con.getOutputStream)
    wr.writeBytes(urlParameters)
    wr.flush
    wr.close
    val in = new BufferedReader(
      new InputStreamReader(con.getInputStream))
    val response = new StringBuffer
 
    while (in.readLine != null) {
      response.append(in.readLine)
    }
    in.close
    Token.insert(response)
    val files = DriveClient.getAllDocumentsFromGoogleDocs(Token.parseResponse(response.toString)("accessToken"))
    Redirect(routes.PhantomController.index)
  }

}
