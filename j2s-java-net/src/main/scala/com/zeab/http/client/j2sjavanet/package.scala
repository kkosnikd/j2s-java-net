package com.zeab.http.client

//Imports
import com.zeab.http.seed.{HttpError, HttpResponse, HttpSeed}
//Java
import java.net.{HttpURLConnection, URL}
import java.nio.charset.CodingErrorAction
//Scala
import scala.annotation.tailrec
import scala.io.Codec
import scala.io.Source.fromInputStream
import scala.util.{Failure, Success, Try}

package object j2sjavanet {

  trait HttpClient {

    case class Seed(httpSeed: HttpSeed) {
      def toHttpResponse: Either[HttpError, HttpResponse] = httpResponse(httpSeed.url, httpSeed.method, httpSeed.body, httpSeed.headers, httpSeed.metaData)
    }

    private def httpResponse(url: String, method: String, body: Option[String] = None, headers: Option[Map[String, String]] = None, metaData: Option[Map[String, String]] = None): Either[HttpError, HttpResponse] = {
      openConnection(url) match {
        case Right(openConn) =>
          addConnectionTimeout(openConn, metaData) match {
            case Right(connTimeoutConn) =>
              addRequestTimeout(connTimeoutConn, metaData) match {
                case Right(reqTimeoutConn) =>
                  addMethod(reqTimeoutConn, method) match {
                    case Right(methodConn) =>
                      addHeaders(methodConn, headers) match {
                        case Right(headerConn) =>
                          addBody(headerConn, body) match {
                            case Right(bodyConn) =>
                              getResponse(bodyConn, url, method, body, headers, metaData)
                            case Left(ex) => Left(HttpError(ex.toString, url, method, body, headers, metaData))
                          }
                        case Left(ex) => Left(HttpError(ex.toString, url, method, body, headers, metaData))
                      }
                    case Left(ex) => Left(HttpError(ex.toString, url, method, body, headers, metaData))
                  }
                case Left(ex) => Left(HttpError(ex.toString, url, method, body, headers, metaData))
              }
            case Left(ex) => Left(HttpError(ex.toString, url, method, body, headers, metaData))
          }
        case Left(ex) => Left(HttpError(ex.toString, url, method, body, headers, metaData))
      }
    }

    private def openConnection(url: String): Either[Throwable, HttpURLConnection] = {
      Try(new URL(url).openConnection.asInstanceOf[HttpURLConnection]) match {
        case Success(value) =>
          Right(value)
        case Failure(ex) =>
          Left(ex)
      }
    }

    private def addMethod(openConn: HttpURLConnection, method: String): Either[Throwable, HttpURLConnection] = {
      Try(openConn.setRequestMethod(method), openConn) match {
        case Success(connectPacket) =>
          val (_, openConn) = connectPacket
          Right(openConn)
        case Failure(ex) =>
          Left(ex)
      }
    }

    private def addHeaders(openConn: HttpURLConnection, headers: Option[Map[String, String]]): Either[Throwable, HttpURLConnection] = {
      @tailrec
      def worker(openConn: HttpURLConnection, headers: Map[String, String], headerConn: Either[Throwable, HttpURLConnection]): Either[Throwable, HttpURLConnection] = {
        headers.size match {
          case 0 =>
            headerConn
          case _ =>
            val (key, value) = headers.headOption.getOrElse("" -> "")
            val headerConn = Try(openConn.setRequestProperty(key, value), openConn) match {
              case Success(connectPacket) =>
                val (_, openConn) = connectPacket
                Right(openConn)
              case Failure(ex) =>
                Left(ex)
            }
            worker(openConn, headers.drop(1), headerConn)
        }
      }
      worker(openConn, headers.getOrElse(Map.empty), Right(openConn))
    }

    private def addBody(openConn: HttpURLConnection, body: Option[String]): Either[Throwable, HttpURLConnection] = {
      val charSet = "UTF-8"
      body match {
        case Some(body) =>
          Try(openConn.setDoOutput(true), openConn) match {
            case Success(connectPacket) =>
              val (_, openConn) = connectPacket
              Try(openConn.getOutputStream.write(body.getBytes(charSet)), openConn) match {
                case Success(connectPacket) =>
                  val (_, openConn) = connectPacket
                  Try(openConn.getOutputStream.close(), openConn) match {
                    case Success(connectPacket) =>
                      val (_, openConn) = connectPacket
                      Right(openConn)
                    case Failure(ex) =>
                      Left(ex)
                  }
                case Failure(ex) =>
                  Left(ex)
              }
            case Failure(ex) =>
              Left(ex)
          }
        case None =>
          Right(openConn)
      }
    }

    private def addConnectionTimeout(openConn: HttpURLConnection, metaData: Option[Map[String, String]]): Either[Throwable, HttpURLConnection] = {
      //TODO Grab from metadata
      val connectTimeout = 5
      Try(openConn.setConnectTimeout(connectTimeout * 1000), openConn) match {
        case Success(connectPacket) =>
          val (_, openConn) = connectPacket
          Right(openConn)
        case Failure(ex) =>
          Left(ex)
      }
    }

    private def addRequestTimeout(openConn: HttpURLConnection, metaData: Option[Map[String, String]]): Either[Throwable, HttpURLConnection] = {
      //TODO Grab from metadata
      val readTimeout = 60
      Try(openConn.setReadTimeout(readTimeout * 1000), openConn) match {
        case Success(connectPacket) =>
          val (_, openConn) = connectPacket
          Right(openConn)
        case Failure(ex) =>
          Left(ex)
      }
    }

    private def getResponse(openConn: HttpURLConnection, url: String, method: String, body: Option[String] = None, headers: Option[Map[String, String]] = None, metaData: Option[Map[String, String]] = None): Either[HttpError, HttpResponse] = {
      val charset = "UTF-8"
      implicit val codec: Codec = Codec(charset)
      codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
      codec.onMalformedInput(CodingErrorAction.REPLACE)
      Try(openConn.getInputStream) match {
        case Success(stream) =>
          val responseBody = fromInputStream(stream).mkString
          Right(HttpResponse(url, method, body, headers, metaData, openConn.getResponseCode, Some(responseBody), Some(openConn.getHeaderFields.toString), 0))
        case Failure(ex) =>
          //Since 4x and 5x are exceptions were catching them here and returning them as valid responses
          if (ex.getMessage.contains("Server returned HTTP response code:")) {
            Right(HttpResponse(url, method, body, headers, metaData, openConn.getResponseCode, Some(ex.toString), Some(openConn.getHeaderFields.toString), 0))
          }
          else {
            Left(HttpError(ex.toString, url, method, body, headers, metaData))
          }
      }
    }

    def authorization = ???
  }

  object HttpClient extends HttpClient

}
