package dev.ahamedm.dist.http

import dev.ahamedm.dist.LogEntry
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json._
import org.slf4j.LoggerFactory

case class AppendLogRequest(term:Int,leaderId:Int,prevLogIndex:Int,prevLogTerm:Int,entries:Seq[LogEntry],leaderCommitIndex:Int)
case class AppendLogResponse(term:Int,success:Boolean)
case class RequestVoteRequest(term:Int,candidateId:Int,lastLogIndex:Int,lastLogTerm:Int)
case class RequestVoteResponse(term:Int,voted:Boolean)

import dev.ahamedm.dist.{ReplicatedServerImpl => server}

class HttpReplicatedServerEndpoints extends ScalatraServlet  with JacksonJsonSupport{
  protected implicit lazy val jsonFormats: Formats = DefaultFormats
  val logger =  LoggerFactory.getLogger(getClass)

  before() {
    contentType = formats("json")
  }

  post("/appendLogEntries") {
    logger.info("HTTP Endpoint: appendLogEntries enter")
    val parsedBody = parse(request.body)
    val app_request:AppendLogRequest=parsedBody.extract[AppendLogRequest]
    server.appendEntries(app_request.term,app_request.leaderId,app_request.prevLogIndex,app_request.prevLogTerm,app_request.entries,app_request.leaderCommitIndex)
    logger.info("HTTP Endpoint: appendLogEntries exit")
  }

  post( "/heartbeat") {
    logger.info("HTTP Endpoint: heartbeat enter")
    val parsedBody = parse(request.body)
    val app_request:AppendLogRequest=parsedBody.extract[AppendLogRequest]
    val (responseTerm,responseState) = server.appendEntries(app_request.term,app_request.leaderId,app_request.prevLogIndex,app_request.prevLogTerm,app_request.leaderCommitIndex)

    logger.info("HTTP Endpoint: appendLogEntries exit")
    AppendLogResponse(responseTerm,responseState)



  }

  post ( "/requestVote") {
    logger.info("HTTP Endpoint: requestVote entry")
    val parsedBody = parse(request.body)
    val app_request:RequestVoteRequest=parsedBody.extract[RequestVoteRequest]
    val (responseTerm,responseState) =server.requestVote(app_request.term,app_request.candidateId,app_request.lastLogIndex,app_request.lastLogTerm)
    logger.info(s"HTTP Endpoint: requestVote exit ... $responseTerm .... $responseState")
    RequestVoteResponse(responseTerm,responseState)
  }

}
