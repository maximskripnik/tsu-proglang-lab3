package lab3

import cats.data.EitherT
import cats.implicits._
import scala.io.BufferedSource
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.net.{ServerSocket, Socket}
import java.io.PrintStream
import scala.util.{Success, Failure}
import scala.util.matching.Regex
import lab3.Util

class Server(port: Int, maxConnections: Int, timeout: Int = 20000) {

  var currentConnections = 0

  def serve()(implicit ex: ExecutionContext): Future[Unit] = {
    for {
      socket <- Future(new ServerSocket(port))
      _ = println("Started server")
      _ <- serve(socket)
    } yield ()
  }

  private def serve(
      socket: ServerSocket
  )(implicit ex: ExecutionContext): Future[Unit] =
    if (currentConnections < maxConnections)
      for {
        _ <- acceptNewConnection(socket)
        _ <- serve(socket)
      } yield ()
    else
      for {
        _ <- Future(Thread.sleep(2000))
        res <- serve(socket)
      } yield ()

  private def acceptNewConnection(
      socket: ServerSocket
  )(implicit ec: ExecutionContext): Future[Socket] = {
    val connectionF = Future(socket.accept())
    connectionF.onComplete {
      case Success(connection) =>
        connection.setSoTimeout(timeout)
        currentConnections += 1
        println(
          s"Accepted new connection '${connection.getRemoteSocketAddress()}'. " +
            s"Number of current connections: $currentConnections"
        )
      case Failure(_) => ()
    }

    type EitherTString[R] = EitherT[Future, String, R]

    val servingResult = for {
      connection <- EitherT.right[String](connectionF)
      session <- EitherT(handShake(connection))
      _ <- session.iterateForeverM[EitherTString, Unit] { session =>
        for {
          rawMessage <- EitherT.right[String](Util.readMessage(session.in))
          pair <- EitherT.fromEither[Future](Server.parseMessage(rawMessage))
          (key, msg) = pair
          newKey = session.protector.nextSessionKey(key)
          _ <- EitherT.right[String](
            Util.sendMessage(session.out, s"KEY:$newKey MSG:$msg")
          )
        } yield session
      }
    } yield ()

    servingResult
      .foldF(
        error =>
          for {
            connection <- connectionF
            out = new PrintStream(connection.getOutputStream())
            _ <- Util.sendMessage(out, error)
            _ <- closeConnection(connection)
          } yield (),
        _ =>
          for {
            connection <- connectionF
            _ <- closeConnection(connection)
          } yield ()
      )
      .recoverWith {
        case ex =>
          for {
            connection <- connectionF
            _ <- closeConnection(connection)
          } yield ()
      }

    connectionF
  }

  private def handShake(
      connection: Socket
  )(implicit ec: ExecutionContext): Future[Either[String, Session]] = {
    val in = new BufferedSource(connection.getInputStream()).getLines()
    val out = new PrintStream(connection.getOutputStream())

    val result = for {
      message <- EitherT.right[String](Util.readMessage(in))
      re = raw"HASH:(\d+) KEY:(\d+)".r
      pair <- EitherT.fromEither[Future](message match {
        case re(hash, key) => (hash, key).asRight
        case _ =>
          "ERROR: BADHANDSHAKE DETAILS: SYNTAX 'HASH:<DIGITAL_HASH> KEY:<DIGITAL_KEY>'".asLeft
      })
      (hash, initialKey) = pair
      protector <- EitherT.fromEither[Future](SessionProtector(hash))
      key = protector.nextSessionKey(initialKey)
      _ <- EitherT.right[String](Util.sendMessage(out, s"KEY:$key"))
    } yield Session(in, out, protector)

    result.value
  }

  private def closeConnection(
      connection: Socket
  )(implicit ec: ExecutionContext): Future[Unit] = {
    val result = Future(connection.close())
    result.onComplete {
      case Success(_) =>
        currentConnections -= 1
        println(
          s"Closed connection '${connection.getRemoteSocketAddress()}'. " +
            s"Number of current connections: $currentConnections"
        )
      case Failure(_) =>
        ()
    }
    result
  }

}

object Server {

  def parseMessage(message: String): Either[String, (String, String)] = {
    val re = raw"KEY:(\d+) MSG:(.*)".r
    message match {
      case re(key, msg) => (key, msg).asRight
      case _ =>
        "ERROR: BADMSG DETAILS: SYNTAX 'KEY:<DIGITAL_KEY> MSG:<YOUR MESSAGE>'".asLeft
    }
  }

}
