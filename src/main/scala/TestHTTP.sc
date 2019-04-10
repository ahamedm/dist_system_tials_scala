import java.nio.charset.Charset


import scala.concurrent.ExecutionContext.Implicits.global
import dispatch.{Http, url}
import dispatch._
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{read => jsonRead, write => jsonWrite}


implicit val formats = DefaultFormats


case class LogEntry(index:Long,term:Long,recordedTS:Long,cmd:String)
case class AppendLogRequest(term:Int,leaderId:Int,prevLogIndex:Int,prevLogTerm:Int,entries:Seq[LogEntry],leaderCommitIndex:Int)
case class AppendLogResponse(term:Int,success:Boolean)
case class RequestVoteRequest(term:Int,candidateId:Int,lastLogIndex:Int,lastLogTerm:Int)
case class RequestVoteResponse(term:Int,voted:Boolean)

val hostname="localhost:9084"
val svc = url(s"http://${hostname}/requestVote").POST.
        setContentType("application/json",Charset.forName("UTF-8"))
      .setBody(jsonWrite(RequestVoteRequest(5, 1, 0, 4)))

val str = Http.default(svc OK as.String)
val optRes=for(s <- str) yield jsonRead[RequestVoteResponse](s)
val res:RequestVoteResponse=optRes.completeOption.getOrElse(RequestVoteResponse(-1,false))
println(s"Response from Server...is ${res.voted}, ${res.term}")
