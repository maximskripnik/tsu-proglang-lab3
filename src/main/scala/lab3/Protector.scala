package lab3

import cats._
import cats.implicits._

import scala.util.Random

class SessionProtector private (private val hashString: List[Byte]) {

  import SessionProtector._

  def nextSessionKey(sessionKey: String): String = {
    val sessionKeyInBytes = sessionKey.toList.map(_.asDigit.toByte)
    val hashResult = sessionKeyInBytes.foldLeft(0L) { (soFar, byte) =>
      soFar + digitsToLong(calculateHash(sessionKeyInBytes, byte))
    }
    (List.fill[Byte](10)(0) ++ longToDigits(hashResult).take(10))
      .takeRight(10)
      .mkString("")
  }

  private def calculateHash(sessionKey: List[Byte], digit: Byte): List[Byte] =
    digit match {
      case 1 =>
        (List[Byte](0, 0) ++ longToDigits(
          digitsToLong(sessionKey.take(5)) % 97
        )).dropRight(2)
      case 2 =>
        (1 until sessionKey.length).foldLeft(List.empty[Byte]) {
          (soFar, index) =>
            soFar :+ sessionKey(sessionKey.length - index)
        } :+ sessionKey.head
      case 3 =>
        sessionKey.takeRight(5) ++ sessionKey.take(5)
      case 4 =>
        val num = (1 until 9).foldLeft(0L) { (soFar, index) =>
          soFar + sessionKey(index) + 41
        }
        longToDigits(num)
      case 5 =>
        val num = sessionKey.foldLeft(0L) { (soFar, byte) =>
          val ch = (byte.toString.head.toInt ^ 43).toChar
          val digits =
            if (ch.isDigit) List(ch.asDigit.toByte) else longToDigits(ch.toInt)
          soFar + digitsToLong(digits)
        }
        longToDigits(num)
      case x =>
        longToDigits(digitsToLong(sessionKey) + x)
    }

}

object SessionProtector {
  def apply(hashString: String): Either[String, SessionProtector] =
    for {
      _ <- Either.cond(hashString.nonEmpty, (), "Hash code is empty")
      reversedDigits <- hashString.toList.foldM(List.empty[Byte]) {
        case (soFar, ch) =>
          if (ch.isDigit) (ch.toByte :: soFar).asRight
          else s"""Hash code contains non digit letter "$ch"""".asLeft
      }
      digits = reversedDigits.reverse
    } yield new SessionProtector(digits)

  def getSessionKey(): String =
    (1 to 10)
      .foldLeft(List.empty[Byte]) { (soFar, _) =>
        longToDigits((9 * Random.nextDouble()).toInt + 1).head.toByte :: soFar
      }
      .mkString("")

  def getHashString(): String =
    (1 to 5)
      .foldLeft(List.empty[Byte]) { (soFar, _) =>
        ((6 * Random.nextDouble()).toInt + 1).toByte :: soFar
      }
      .mkString("")

  private def digitsToLong(digits: List[Byte]): Long =
    digits.foldLeft(0L)(10 * _ + _)

  private def longToDigits(x: Long): List[Byte] =
    x.toString.toList.map(_.asDigit.toByte)
}
