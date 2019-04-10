import dev.ahamedm.dist.ReplicatedServerImpl
import dev.ahamedm.dist.http.HttpReplicatedServerEndpoints
import javax.servlet.ServletContext
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    println("Endpoints mounted to CONTEXT....")
    context mount (new HttpReplicatedServerEndpoints, "/*")

    ReplicatedServerImpl.init()
  }

}
