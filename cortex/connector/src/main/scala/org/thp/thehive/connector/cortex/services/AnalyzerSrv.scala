package org.thp.thehive.connector.cortex.services

import javax.inject.{Inject, Singleton}
import org.thp.cortex.dto.v0.OutputCortexWorker
import org.thp.scalligraph.auth.AuthContext
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class AnalyzerSrv @Inject()(connector: Connector, serviceHelper: ServiceHelper, implicit val ec: ExecutionContext) {

  lazy val logger = Logger(getClass)

  /**
    * Lists the Cortex analyzers from all CortexClients
    *
    * @return
    */
  def listAnalyzer(range: Option[String])(implicit authContext: AuthContext): Future[Map[OutputCortexWorker, Seq[String]]] =
    Future
      .traverse(serviceHelper.availableCortexClients(connector.clients, authContext.organisation)) { client =>
        client
          .listAnalyser(range)
          .transform {
            case Success(analyzers) => Success(analyzers.map(_ -> client.name))
            case Failure(error) =>
              logger.error(s"List Cortex analyzers fails on ${client.name}", error)
              Success(Nil)
          }
      }
      .map(serviceHelper.flattenList(_, _ => true))

  def listAnalyzerByType(dataType: String)(implicit authContext: AuthContext): Future[Map[OutputCortexWorker, Seq[String]]] =
    Future
      .traverse(serviceHelper.availableCortexClients(connector.clients, authContext.organisation)) { client =>
        client
          .listAnalyzersByType(dataType)
          .transform {
            case Success(analyzers) => Success(analyzers.map(_ -> client.name))
            case Failure(error) =>
              logger.error(s"List Cortex analyzers by dataType fails on ${client.name}", error)
              Success(Nil)
          }
      }
      .map(serviceHelper.flattenList(_, _ => true))

  def getAnalyzer(id: String)(implicit authContext: AuthContext): Future[(OutputCortexWorker, Seq[String])] =
    Future
      .traverse(serviceHelper.availableCortexClients(connector.clients, authContext.organisation)) { client =>
        client
          .getAnalyzer(id)
          .map(_ -> client.name)
      }
      .map(
        analyzerByClients =>
          analyzerByClients
            .groupBy(_._1.name)
            .values // Seq[Seq[(worker, cortexId)]]
            .map(a => a.head._1 -> a.map(_._2).toSeq) // Map[worker, Seq[CortexId] ]
            .toMap
            .head
      )
}
