package lab3

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.io.PrintStream

object Util {

  def sendMessage(out: PrintStream, message: String)(
      implicit ec: ExecutionContext
  ): Future[Unit] =
    Future {
      out.print(message + "\n")
      out.flush()
      println(s"Sent message: '$message'")
    }

  def readMessage(
      in: Iterator[String]
  )(implicit ec: ExecutionContext): Future[String] =
    Future {
      val message = in.next()
      println(s"Received message: '$message'")
      message
    }

}
