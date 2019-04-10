package dev.ahamedm.dist

import java.nio.charset.Charset
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import dev.ahamedm.dist.http.{AppendLogRequest, AppendLogResponse, RequestVoteRequest, RequestVoteResponse}
import dev.ahamedm.dist.{ClusterConfiguration => cfg, ReplicatedServerInternalState => istate}
import dispatch._
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{read => jsonRead, write => jsonWrite}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.Breaks._


object ReplicatedServerImpl {

  def logger = LoggerFactory.getLogger("ReplicatedServer")


  //TODO How to restore the log?
  val persistentLog: collection.mutable.Seq[LogEntry] = new collection.mutable.MutableList()

  var serverRole: ClusterNodeRole = istate.enumRoles("follower")

  val timerEx = new ScheduledThreadPoolExecutor(1)

  var peers: collection.mutable.Seq[PeerNode] = cfg.group.split("[;]").map(idstr => PeerNode(idstr.split("#")(0).toInt, idstr.split("#")(1)))

  val heartBeatTask = new Runnable {
    def run(): Unit = beatHeart()
  }

  val campaignForLeaderTask = new Runnable {
    def run(): Unit = campaign()
  }

  //TODO When to schedule heartbeat
  var heartBeatTimer: ScheduledFuture[_] = null //timerEx.scheduleAtFixedRate(heartBeatTask, cfg.initialHeartbeatIntervalBuffer, cfg.heartbeatInterval, TimeUnit.MILLISECONDS)
  var campaignTimer: ScheduledFuture[_] = timerEx.scheduleAtFixedRate(campaignForLeaderTask, cfg.initLeaderElectionTimeoutBuffer + cfg.leaderElectionTimeout, cfg.leaderElectionTimeout, TimeUnit.MILLISECONDS)

  implicit val formats = DefaultFormats

  logger.info("TIMERS scheduled in StateServer....")

  /**
    * Just a dummy method for being an object...
    */
  def init(): Unit = {

  }

  /**
    *
    */
  def resetCampaign(): Unit = {

    logger.info("Resetting Campaign...")
    campaignTimer.cancel(false)
    campaignTimer = timerEx.scheduleAtFixedRate(campaignForLeaderTask, cfg.leaderElectionTimeout, cfg.leaderElectionTimeout, TimeUnit.MILLISECONDS)

  }

  /**
    *
    */
  def beatHeart(): Unit = {

    logger.info("BeatHeart Invoked...")

    //TODO Use HttpClient To Send HeartBeat if Leader
    if (istate.serverRole.role.equals("leader")) {


      peers.filterNot(_.id == istate.serverId).foreach(peer => {

        if (!istate.serverRole.role.equals("leader"))
          break

        logger.info("Sending hearbeat as a leader..")

        val svc = url(s"http://${peer.iaddr}/heartbeat").POST.setContentType("application/json", Charset.forName("UTF-8"))
                  .setBody(jsonWrite(AppendLogRequest(istate.currentTerm, istate.serverId, istate.commitIndex, istate.currentTerm - 1, Seq(), istate.commitIndex)))
        val app_response = Http.default(svc OK as.String)
        for (res <- app_response) {
          logger.info(s"Got the HEARTBEAT response....")
          val typedResponse: AppendLogResponse = jsonRead[AppendLogResponse](res)
          if (typedResponse.term > istate.currentTerm) {
            logger.info("Term less than holding currentTerm, shifting to follower..")

            istate.currentTerm = typedResponse.term
            istate.serverRole = istate.enumRoles("follower")
            resetCampaign()

          }
        }
      })

    }
  }

  def campaign(): Unit = {
    logger.info("Campaign Invoked...")

    if (istate.serverRole.role.equals("follower")) {

      istate.serverRole = istate.enumRoles("candidate")

      istate.currentTerm += 1

      val votes: Int = peers.filterNot(_.id == istate.serverId).map(peer => {

        if (!istate.serverRole.role.equals("candidate"))
          break

        logger.info(s"Still a candidate for election...${istate.currentTerm}")

        istate.votedFor=istate.serverId

        val svc = url(s"http://${peer.iaddr}/requestVote").POST
                  .setContentType("application/json", Charset.forName("UTF-8"))
                  .setBody(jsonWrite(RequestVoteRequest(istate.currentTerm, istate.serverId, istate.lastApplied, istate.currentTerm - 1)))

        logger.info(s"Getting vote from peer ${peer.iaddr}...")

        val app_response = Http.default(svc OK as.String)

        logger.info("------>Future of app_response")
        logger.info(app_response.print)

        var actVote:RequestVoteResponse=RequestVoteResponse(-1,false)
        val res_str=app_response()

        //  for (res <- app_response)
        actVote=jsonRead[RequestVoteResponse](res_str)

        //val actVote:RequestVoteResponse=optVote.completeOption.get

        logger.info(s"Actual vote got ${actVote.voted}....${actVote.term}")
        actVote

      }).map(a => if (a.voted) 1 else 0).sum

      logger.info(s"Total votes from peers.. ${votes}...")

      if (votes > peers.length / 2) {
        istate.serverRole = istate.enumRoles("leader")
        logger.info(s"Votes Greater than majority so ELECTED...LEADER...")
        beatHeart()
        heartBeatTimer = timerEx.scheduleAtFixedRate(heartBeatTask, cfg.initialHeartbeatIntervalBuffer, cfg.heartbeatInterval, TimeUnit.MILLISECONDS)
      }else{
        istate.serverRole = istate.enumRoles("follower")
      }
    }
  }

  /**
    *
    * @param term              Leader's Term
    * @param leaderId          So Followers can redirect to Leader
    * @param prevLogIndex      index of log entry immediately preceding new ones
    * @param prevLogTerm       term of prevLogIndex entry
    * @param entries           log entries to store (empty for heartbeat;may send more than one for efficiency)
    * @param leaderCommitIndex leader's commit Index
    * @return term : currentTerm, for leader to update itself ; success: true if follower contained entry matching prevLogIndex and prevLogTerm
    */
  def appendEntries(term: Int, leaderId: Int, prevLogIndex: Int, prevLogTerm: Int, entries: Seq[LogEntry], leaderCommitIndex: Int): (Int, Boolean) = {

    logger.info(s"LOG ENTRY..Append Entries invoked...from leader ${leaderId}...")
    (-1, false)
  }


  def appendEntries(rq_term: Int, rq_leaderId: Int, rq_prevLogIndex: Int, rq_prevLogTerm: Int, rq_leaderCommitIndex: Int): (Int, Boolean) = {

    logger.info(s"Append Entries invoked...from leader ${rq_leaderId}...")

    istate.lastHeartBeatFromLeader = System.currentTimeMillis()

    var responseTerm = istate.currentTerm
    val responseState = true //TODO Conditions
    if (rq_term > istate.currentTerm) {
      istate.serverRole = istate.enumRoles("follower")
      istate.currentTerm = rq_term
      responseTerm = rq_term
    } else {
      responseTerm = istate.currentTerm
    }

    resetCampaign()

    (responseTerm, responseState)

  }

  /**
    *
    * @param rq_term         : Candidate's Term
    * @param rq_candidateId  Candidate-Id/ Peer-Id requesting vote
    * @param rq_lastLogIndex Index of candidate’s last log entry
    * @param rq_lastLogTerm  Term of candidate’s last log entry
    * @return term: currentTerm, for candidate to update itself; voteGranted: true means candidate received vote
    */
  def requestVote(rq_term: Int, rq_candidateId: Int, rq_lastLogIndex: Int, rq_lastLogTerm: Int): (Int, Boolean) = {

    var responseTerm = istate.currentTerm
    val responseState = true //TODO Conditions

    if (rq_term > istate.currentTerm) {
      istate.serverRole = istate.enumRoles("follower")
      istate.currentTerm = rq_term
      responseTerm = rq_term
    } else {
      responseTerm = istate.currentTerm
    }
    istate.votedFor=rq_candidateId
    logger.info(s"${istate.serverId} VOTED for $rq_candidateId.........")
    resetCampaign()

    (responseTerm, responseState)

  }
}
