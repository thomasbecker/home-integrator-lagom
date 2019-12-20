import sbt._

object BuildTarget {
  private sealed trait DeploymentRuntime
  private case object ConductR extends DeploymentRuntime
  private case object Kubernetes extends DeploymentRuntime
  private case object Unmanaged extends DeploymentRuntime
  private case object Marathon extends DeploymentRuntime

  private val deploymentRuntime: DeploymentRuntime = sys.props.get("buildTarget") match {
    case Some(v) if v.toLowerCase == "conductr" =>
      ConductR

    case Some(v) if v.toLowerCase == "kubernetes" =>
      Kubernetes

    case Some(v) if v.toLowerCase == "unmanaged" =>
      Unmanaged

    case Some(v) if v.toLowerCase == "marathon" =>
      Marathon

    case Some(v) =>
      sys.error(s"The build target $v is not supported. Available: 'conductr', 'kubernetes', 'unmanaged', 'marathon'")

    case None =>
      ConductR
  }

  val additionalSettings = deploymentRuntime match {
    case Kubernetes =>
      Seq(
        Keys.libraryDependencies ++= Seq(
          Library.serviceLocatorDns
        ),
        Keys.unmanagedResourceDirectories in Compile += Keys.sourceDirectory.value / "main" / "kubernetes-resources"
      )
    case Unmanaged =>
      Seq(
        Keys.unmanagedResourceDirectories in Compile += Keys.sourceDirectory.value / "main" / "unmanaged-resources"
      )
    case Marathon   =>
      Seq(
        Keys.libraryDependencies ++= Seq(
          Library.serviceLocatorDns, Library.constructr, Library.constructrZooKeeper
        ),
        Keys.unmanagedResourceDirectories in Compile += Keys.sourceDirectory.value / "main" / "marathon-resources"
      )
    case ConductR   =>
      Seq.empty
  }

  val dockerRepository: String = deploymentRuntime match {
    case Kubernetes => "home-integrator"
    case Unmanaged => "little:5000"
    case Marathon => "home-integrator-marathon"
    case ConductR   => "home-integrator-conductr"
  }
}
