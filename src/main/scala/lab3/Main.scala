package lab3

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import scopt.{OParser, Read}

sealed trait Mode
object Mode {
  case object Server extends Mode
  case object Client extends Mode
}

case class Params(
    mode: Mode = Mode.Server,
    port: Int = 9000,
    maxConnections: Option[Int] = Some(200),
    host: Option[String] = None
)

object Lab3 extends App {

  implicit val modeRead: Read[Mode] = Read.reads {
    case "server" => Mode.Server
    case "client" => Mode.Client
  }

  val builder = OParser.builder[Params]
  val argsParser = {
    import builder._
    OParser.sequence(
      programName("Lab3"),
      opt[Mode]('m', "mode")
        .action((m, p) => p.copy(mode = m))
        .text("Mode")
        .required(),
      opt[Int]('p', "port")
        .action((port, p) => p.copy(port = port))
        .text(
          "For server mode - tcp port to lisen. For client - server port to connect to"
        )
        .required(),
      opt[Option[Int]]('n', "max-connections")
        .action((mc, p) => p.copy(maxConnections = mc))
        .text(
          "Number of maximum allowed connections. Only makes sense for the server mode"
        ),
      opt[Option[String]]('h', "host")
        .action((h, p) => p.copy(host = h))
        .text(
          "Hostname of the server to connect to. Only makes sense for the client mode"
        )
    )
  }

  implicit val ec =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  OParser.parse(argsParser, args, Params()) match {
    case Some(params) =>
      params.mode match {
        case Mode.Server =>
          val server = new Server(params.port, params.maxConnections.get)
          Await.result(server.serve(), Duration.Inf)
        case Mode.Client =>
          val client = new Client(params.host.get, params.port)
          Await.result(client.connect, Duration.Inf)
      }
    case _ =>
      ()
  }

}
