package dev.ahamedm.dist

case class ClusterNodeRole(role:String,roleId:Int)

object ReplicatedServerInternalState {

  lazy val serverId:Int=System.getProperty("server_id").toInt


  val enumRoles:Map[String,ClusterNodeRole]=Map("follower"->ClusterNodeRole("follower",3001),
    "candidate"->ClusterNodeRole("candidate",3002),
    "leader"->ClusterNodeRole("leader",3003))

  var commitIndex:Int = 0 //TODO Should be 1 ?
  var lastApplied:Int = 0

  var currentTerm:Int = 0
  var votedFor:Int = 0

  var serverRole:ClusterNodeRole=enumRoles("follower")
  var lastHeartBeatFromLeader:Long=System.currentTimeMillis()


  var nextIndexes:collection.mutable.Map[PeerNode,Int]=collection.mutable.HashMap.empty
  var matchIndexes:collection.mutable.Map[PeerNode,Int]=collection.mutable.HashMap.empty
}
