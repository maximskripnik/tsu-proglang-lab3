package lab3

import java.net.Socket
import java.io.PrintStream
import scala.io.{BufferedSource, StdIn}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.Try
import cats.implicits._
import cats._
import lab3.Util

class Client(host: String, port: Int) {

  type Key = String

  def connect(implicit ec: ExecutionContext): Future[Unit] =
    for {
      connection <- Future(new Socket(host, port))
      hsResult <- handShake(connection)
      (session, key) = hsResult
      _ <- (key, session).iterateForeverM[Future, Unit] {
        case (key: Key, session: Session) =>
          print("$")
          val text = StdIn.readLine()
          println(s"TEXT $text")
          for {
            _ <- Util.sendMessage(session.out, s"KEY:$key MSG:$text")
            response <- Util.readMessage(session.in)
            parseResult <- Future.fromTry {
              Try(
                Server
                  .parseMessage(response)
                  .getOrElse(
                    throw new RuntimeException(
                      s"Bad response from server: '$response'"
                    )
                  )
              )
            }
            (receivedKey, _) = parseResult
            newKey = session.protector.nextSessionKey(key)
            _ <- Future.fromTry(validateKey(newKey, receivedKey))
          } yield (newKey, session)
      }
    } yield ()

  private def handShake(
      connection: Socket
  )(implicit ec: ExecutionContext): Future[(Session, Key)] = {
    val in = new BufferedSource(connection.getInputStream()).getLines()
    val out = new PrintStream(connection.getOutputStream())
    val hash = SessionProtector.getHashString()
    val initialKey = SessionProtector.getSessionKey()
    println(s"Generated hash '$hash' and initial key '$initialKey'")
    for {
      protector <- Future.fromTry {
        Try(
          SessionProtector(hash).getOrElse(
            throw new RuntimeException(
              s"Could not instantiate protector with hash $hash"
            )
          )
        )
      }
      _ <- Util.sendMessage(out, s"HASH:$hash KEY:$initialKey")
      re = raw"KEY:(\d+)".r
      response <- Util.readMessage(in)
      receivedKey <- Future.fromTry {
        Try(
          response match {
            case re(key) => key
            case _ =>
              throw new RuntimeException(
                s"Bad response from server: '$response'"
              )
          }
        )
      }
      newKey = protector.nextSessionKey(initialKey)
      _ <- Future.fromTry(validateKey(newKey, receivedKey))
      session = Session(
        in = in,
        out = out,
        protector = protector
      )
    } yield (session, newKey)
  }

  private def validateKey(expectedKey: String, receivedKey: String): Try[Unit] =
    Try {
      println(
        s"Validating received key '$receivedKey' against expected key '$expectedKey'"
      )
      if (receivedKey == expectedKey) {
        println("Ok. Key is correct")
      } else {
        throw new RuntimeException(s"Bad key '$receivedKey'")
      }
    }

}
