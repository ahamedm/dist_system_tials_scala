package dev.ahamedm.dist

object ClusterConfiguration {
    val heartbeatInterval:Int=3000
    val initialHeartbeatIntervalBuffer:Int=1000
    val leaderElectionTimeout:Int=15000
    val initLeaderElectionTimeoutBuffer:Int=1000

    lazy val group:String=System.getProperty("server_group")
    lazy val serverId:String=System.getProperty("server_id")

}
