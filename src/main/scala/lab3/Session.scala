package lab3
import scala.io.BufferedSource
import java.io.PrintStream

case class Session(
    in: Iterator[String],
    out: PrintStream,
    protector: SessionProtector
)
