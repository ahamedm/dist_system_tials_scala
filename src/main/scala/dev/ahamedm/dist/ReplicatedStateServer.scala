package dev.ahamedm.dist


trait ReplicatedStateServer {

  /**
    *
    * @param term Leader's Term
    * @param leaderId So Followers can redirect to Leader
    * @param prevLogIndex index of log entry immediately preceding new ones
    * @param prevLogTerm term of prevLogIndex entry
    * @param entries log entries to store (empty for heartbeat;may send more than one for efficiency)
    * @param leaderCommitIndex leader's commit Index
    * @return term : currentTerm, for leader to update itself ; success: true if follower contained entry matching prevLogIndex and prevLogTerm
    */
  def xappendEntries(term:Int,leaderId:Int,prevLogIndex:Int,prevLogTerm:Int,entries:Seq[LogEntry],leaderCommitIndex:Int):(Int,Boolean)

  /**
    *
    * @param term: Candidate's Term
    * @param candidateId Candidate-Id/ Peer-Id requesting vote
    * @param lastLogIndex Index of candidate’s last log entry
    * @param lastLogTerm Term of candidate’s last log entry
    * @return   term: currentTerm, for candidate to update itself; voteGranted: true means candidate received vote
    */
  def xrequestVote(term:Int,candidateId:Int,lastLogIndex:Int,lastLogTerm:Int):(Int,Boolean)

}
