import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContextExecutor

object RestfulAPI extends App {
  implicit val system: ActorSystem = ActorSystem("restful-api")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  var items: List[String] = List("item1", "item2", "item3")

  val routes: Route =
    pathPrefix("items") {
      pathEndOrSingleSlash {
        get {
          complete(items)
        } ~
        post {
          entity(as[String]) { item =>
            items = items :+ item
            complete(StatusCodes.Created, s"Item $item created.")
          }
        }
      } ~
      path(Segment) { id =>
        get {
          complete(items.lift(id.toInt - 1))
        } ~
        put {
          entity(as[String]) { item =>
            items.lift(id.toInt - 1) match {
              case Some(_) =>
                items = items.updated(id.toInt - 1, item)
                complete(s"Item $id updated.")
              case None =>
                complete(StatusCodes.NotFound, s"Item $id not found.")
            }
          }
        } ~
        delete {
          items.lift(id.toInt - 1) match {
            case Some(_) =>
              items = items.filterNot(_ == items(id.toInt - 1))
              complete(s"Item $id deleted.")
            case None =>
              complete(StatusCodes.NotFound, s"Item $id not found.")
          }
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  println("Server started at http://localhost:8080/")

  scala.sys.addShutdownHook {
    bindingFuture.flatMap(_.unbind())
    system.terminate()
  }
}
