package justin.db

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import com.typesafe.config.ConfigFactory

final class MultiDcSpecConfig(crossDcConnections: Int = 1) extends MultiNodeConfig {
  val first  = role("first")
  val second = role("second")

  private[this] val allRoles  = List(first, second)
  private[this] val clusterName = "MultiDcSpec"
  private[this] val commonBaseConfig = ConfigFactory.parseString(
    s"""
       |akka.loglevel = INFO
       |akka.log-dead-letters = off
       |akka.log-dead-letters-during-shutdown = off
       |akka.remote.log-remote-lifecycle-events = off
    """.stripMargin
  )
  private[this] def commonNodeConfig(id: Int) = ConfigFactory.parseString(
    s"""
       |justin.system = $clusterName
       |justin.node-id = $id
       |justin.http.port = ${9000 + id}
       |akka.cluster.role.storagenode.min-nr-of-members = ${allRoles.size}
       |akka.remote.netty.tcp.port = 0
       |akka.remote.netty.tcp.hostname = "localhost"
       |akka.remote.netty.tcp.bind-hostname = "0.0.0.0"
       |akka.remote.netty.tcp.bind-port = 0
       |akka.cluster.http.management.port = ${19999 + id}
       |akka.cluster.multi-data-center.cross-data-center-connections = $crossDcConnections
       |akka.cluster.multi-data-center.self-data-center = "dc$id"
    """.stripMargin
  )

  commonConfig(commonBaseConfig.withFallback(JustinDBConfig.init.config))

  allRoles.zipWithIndex.foreach { case (roleName, id) =>
    nodeConfig(roleName)(commonNodeConfig(id))
  }
}

final class MultiDcMultiJvm1 extends MultiDcSpec
final class MultiDcMultiJvm2 extends MultiDcSpec

abstract class MultiDcSpec(config: MultiDcSpecConfig)
  extends MultiNodeSpec(config)
    with MultiNodeClusterSpec {

  def this() = this(new MultiDcSpecConfig())

  "A cluster with multiple data centers" must {
    "be able to form" in {
      val config = new JustinDBConfig(system.settings.config)
      val justinDB = JustinDB.init(config)

      enterBarrier("justindb-cluster-up")
    }
  }
}
