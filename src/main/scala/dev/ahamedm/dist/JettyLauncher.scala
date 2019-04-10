package dev.ahamedm.dist

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

object JettyLauncher {

  def main(args: Array[String]) {

    val port = if(System.getProperty("server_port") != null) System.getProperty("server_port").toInt else 8080

    println(s"Server Starting with PORT...... $port")

    val server = new Server(port)
    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)

    server.start
    server.join
  }

}
