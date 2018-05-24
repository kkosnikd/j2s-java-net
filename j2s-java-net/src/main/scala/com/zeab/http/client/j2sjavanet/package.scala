package com.zeab.http.client

//Imports
import com.zeab.http.seed._
//Java
import java.net.{HttpURLConnection, URL}
import java.nio.charset.CodingErrorAction
//Scala
import scala.collection.JavaConversions._
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.io.Source.fromInputStream
import scala.util.{Failure, Success, Try}

package object j2sjavanet {

  trait HttpClient {

    //Http Responses
    private def asyncHttpResponse(url: String, method: String, body: Option[String] = None, headers: Option[Map[String, String]] = None, metaData: Option[Map[String, String]] = None)(implicit ec: ExecutionContext): Future[Either[HttpError, HttpResponse]] = {
      Future {
        httpResponse(url, method, body, headers, metaData)
      }
    }

    private def httpResponse(url: String, method: String, body: Option[String] = None, headers: Option[Map[String, String]] = None, metaData: Option[Map[String, String]] = None): Either[HttpError, HttpResponse] = {

      //Calculate authorization if one is required
      val combineHeaders = authorization(url, method, body, headers, metaData).getOrElse(Map.empty) ++ headers.getOrElse(Map.empty)
      val completedHeaders =
        if (combineHeaders.isEmpty) {
          None
        }
        else {
          Some(combineHeaders)
        }

      val shouldRetry = true

      //Open Url Connection
      openConnection(url) match {
        case Right(openConn) =>
          //Add Connection Timeout Value
          addConnectionTimeout(openConn, metaData) match {
            case Right(connTimeoutConn) =>
              //Add Request Timeout Value
              addRequestTimeout(connTimeoutConn, metaData) match {
                case Right(reqTimeoutConn) =>
                  //Add the method
                  addMethod(reqTimeoutConn, method) match {
                    case Right(methodConn) =>
                      //Add the headers
                      addHeaders(methodConn, completedHeaders) match {
                        case Right(headerConn) =>
                          //Add the body
                          addBody(headerConn, body) match {
                            case Right(bodyConn) =>
                              getResponse(bodyConn, url, method, body, completedHeaders, metaData)
                            case Left(ex) => Left(HttpError(ex.toString, url, method, body, completedHeaders, metaData))
                          }
                        case Left(ex) => Left(HttpError(ex.toString, url, method, body, completedHeaders, metaData))
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

    //Internal
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
          Right(
            HttpResponse(
              url,
              method,
              body,
              headers,
              metaData,
              openConn.getResponseCode,
              Some(fromInputStream(stream).mkString),
              Some(formatHeaders(openConn)),
              0))
        //TODO make sure to add the timer stuff in
        case Failure(ex) =>
          //Since 4x and 5x are exceptions were catching them here and returning them as valid responses
          if (ex.getMessage.contains("Server returned HTTP response code:")) {
            Right(
              HttpResponse(
                url,
                method,
                body,
                headers,
                metaData,
                openConn.getResponseCode,
                Some(ex.toString),
                Some(formatHeaders(openConn)),
                0))
            //TODO make sure to add the timer stuff in
          }
          else {
            Left(
              HttpError(
                ex.toString,
                url,
                method,
                body,
                headers,
                metaData))
          }
      }
    }

    //Format the response headers so they are easy to consume
    private def formatHeaders(openConn: HttpURLConnection): Map[String, String] = {
      openConn.getHeaderFields.toMap.map { headers =>
        val (headerKey, headerValues) = headers
        headerKey -> headerValues.toList.mkString(" ")
      }
    }

    //Allow for overriding the authorization to custom one's
    def authorization(url: String, method: String, body: Option[String], headers: Option[Map[String, String]], metaData: Option[Map[String, String]]): Option[Map[String, String]] = HttpHeaders.authorizationBearerHeader(HttpSeedMetaData.bearerCheck(metaData))

    case class Seed(httpSeed: HttpSeed) {
      def toHttpResponse: Either[HttpError, HttpResponse] = httpResponse(httpSeed.url, httpSeed.method, httpSeed.body, httpSeed.headers, httpSeed.metaData)

      def toAsyncHttpResponse(implicit ec: ExecutionContext): Future[Either[HttpError, HttpResponse]] = asyncHttpResponse(httpSeed.url, httpSeed.method, httpSeed.body, httpSeed.headers, httpSeed.metaData)

      def retryHttpResponse(successfulResponseStatus: Int, retryInterval: Int = 1, maxRetry: Int = 5): Either[HttpError, HttpResponse] = {
        def retry(httpResponse1: Either[HttpError, HttpResponse], startTime: Double): Either[HttpError, HttpResponse] = {
          def worker(httpResponse1: Either[HttpError, HttpResponse], startTime: Double, retryCount: Int = 0): Either[HttpError, HttpResponse] = {
            httpResponse1 match {
              case Right(resp) =>
                if (resp.responseStatus == successfulResponseStatus) {
                  Right(resp)
                }
                else {
                  if (retryCount == maxRetry) {
                    Left(HttpError(s"Reached Max Retry $retryCount", resp.requestUrl, resp.requestMethod, resp.requestBody, resp.requestHeaders, resp.requestMetaData))
                  }
                  else {
                    if (((System.nanoTime - startTime) / 1e9) >= retryInterval) {
                      worker(httpResponse(resp.requestUrl, resp.requestMethod, resp.requestBody, resp.requestHeaders, resp.requestMetaData), System.nanoTime, retryCount + 1)
                    }
                    else {
                      worker(httpResponse1, startTime, retryCount)
                    }
                  }
                }
              case Left(ex) => Left(ex)
            }
          }
          worker(httpResponse1, startTime)
        }
        retry(httpResponse(httpSeed.url, httpSeed.method, httpSeed.body, httpSeed.headers, httpSeed.metaData), System.nanoTime)
      }

      

    }
  }

  object HttpClient extends HttpClient

}