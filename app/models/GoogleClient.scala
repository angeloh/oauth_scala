package models

object GoogleClient {

  def getSecret : String = System.getenv("CLIENT_SECRET")
  def getClientID : String = System.getenv("CLIENT_ID")

}