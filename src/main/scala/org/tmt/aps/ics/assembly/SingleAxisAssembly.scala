package org.tmt.aps.ics.assembly

import java.io.File

import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import org.tmt.aps.ics.assembly.AssemblyContext.{SingleAxisCalculationConfig, SingleAxisControlConfig}
import csw.services.alarms.AlarmService
import csw.services.ccs.AssemblyMessages.{DiagnosticMode, OperationsMode}
import csw.services.ccs.SequentialExecutor.{ExecuteOne, StartTheSequence}
import csw.services.ccs.Validation.ValidationList
import csw.services.ccs.Validation._
import csw.services.ccs.{AssemblyController, SequentialExecutor, Validation}
import csw.services.cs.akka.ConfigServiceClient
import csw.services.events.{EventService, TelemetryService}
import csw.services.loc.LocationService._
import csw.services.loc._
import csw.services.pkg.Component.AssemblyInfo
import csw.services.pkg.{Assembly, Supervisor}
import csw.util.config.Configurations.SetupConfigArg

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Top Level Actor for Single Axis Assembly
 *
 * SingleAxisAssembly starts up the component doing the following:
 * creating all needed actors,
 * handling initialization,
 * participating in lifecycle with Supervisor,
 * handles locations for distribution throughout component
 * receives comamnds and forwards them to the CommandHandler by extending the AssemblyController
 */
class SingleAxisAssembly(val info: AssemblyInfo, supervisor: ActorRef) extends Assembly with AssemblyController {

  import Supervisor._
  import SingleAxisAssembly._

  private var galilHCD: Option[ActorRef] = badHCDReference

  private var eventService: Option[EventService] = badEventService

  private var telemetryService: Option[TelemetryService] = badTelemetryService

  private var alarmService: Option[AlarmService] = badAlarmService

  private val trackerSubscriber = context.actorOf(LocationSubscriberActor.props)

  private var diagPublsher: ActorRef = _

  private var commandHandler: ActorRef = _

  implicit val ac: AssemblyContext = initialize()

  // Gets the assembly configuration from the config service or resource file and uses it to
  // initialize the assembly
  def initialize(): AssemblyContext = {
    try {
      // The following line could fail, if the config service is not running or the file is not found
      val (calculationConfig, controlConfig) = Await.result(getAssemblyConfigs, 5.seconds)
      val assemblyContext = AssemblyContext(info, calculationConfig, controlConfig)

      // Start tracking the components we command
      log.info("Connections: " + info.connections)
      trackerSubscriber ! LocationSubscriberActor.Subscribe

      // This actor handles all telemetry and system event publishing
      val eventPublisher = context.actorOf(SingleAxisPublisher.props(assemblyContext, None))

      // Setup command handler for assembly - note that CommandHandler connects directly to galilHCD here, not state receiver
      commandHandler = context.actorOf(SingleAxisCommandHandler.props(assemblyContext, galilHCD, Some(eventPublisher)))
      /*
      // This sets up the diagnostic data publisher - setting Var here
      diagPublsher = context.actorOf(DiagPublisher.props(assemblyContext, galilHCD, Some(eventPublisher)))
			*/

      // This tracks the HCD
      LocationSubscriberActor.trackConnections(info.connections, trackerSubscriber)
      // This tracks required services
      LocationSubscriberActor.trackConnection(EventService.eventServiceConnection(), trackerSubscriber)
      LocationSubscriberActor.trackConnection(TelemetryService.telemetryServiceConnection(), trackerSubscriber)
      LocationSubscriberActor.trackConnection(AlarmService.alarmServiceConnection(), trackerSubscriber)

      supervisor ! Initialized
      assemblyContext
    } catch {
      case ex: Exception =>
        supervisor ! InitializeFailure(ex.getMessage)
        null
    }
  }

  def receive: Receive = initializingReceive

  /**
   * This contains only commands that can be received during intialization
   *
   * @return Receive is a partial function
   */
  def initializingReceive: Receive = locationReceive orElse {

    case Running =>
      // When Running is received, transition to running Receive
      log.info("becoming runningReceive")
      // Set the operational cmd state to "ready" according to spec-this is propagated to other actors
      //state(cmd = cmdReady)

      log.info("COMMAND HANDLER = " + commandHandler)

      context.become(runningReceive)
    case x => log.error(s"Unexpected message in SingleAxisAssembly:initializingReceive: $x")
  }

  /**
   * This Receive partial function processes changes to the services and galilHCD
   * Ideally, we would wait for all services before sending Started, but it's not done yet
   */
  def locationReceive: Receive = {
    case location: Location =>
      location match {

        case l: ResolvedAkkaLocation =>
          log.info(s"Got actorRef: ${l.actorRef}")
          galilHCD = l.actorRef
          // When the HCD is located, Started is sent to Supervisor
          checkServicesResolved

        case h: ResolvedHttpLocation =>
          log.info(s"HTTP Service Damn it: ${h.connection}")

        case t: ResolvedTcpLocation =>
          log.info(s"Received TCP Location: ${t.connection}")
          // Verify that it is the event service
          if (t.connection == EventService.eventServiceConnection()) {
            log.info(s"Assembly received ES connection: $t")
            // Setting var here!
            eventService = Some(EventService.get(t.host, t.port))
            log.info(s"Event Service at: $eventService")
            //checkServicesResolved
          }
          if (t.connection == TelemetryService.telemetryServiceConnection()) {
            log.info(s"Assembly received TS connection: $t")
            // Setting var here!
            telemetryService = Some(TelemetryService.get(t.host, t.port))
            log.info(s"Telemetry Service at: $telemetryService")
            //checkServicesResolved
          }
          if (t.connection == AlarmService.alarmServiceConnection()) {
            implicit val timeout = Timeout(10.seconds)
            log.info(s"Assembly received AS connection: $t")
            // Setting var here!
            alarmService = Some(AlarmService.get(t.host, t.port))
            log.info(s"Alarm Service at: $alarmService")
            //checkServicesResolved
          }

        case u: Unresolved =>
          log.info(s"Unresolved: ${u.connection}")
          //if (u.connection == EventService.eventServiceConnection()) eventService = badEventService
          if (u.connection.componentId == ac.hcdComponentId) galilHCD = badHCDReference

        case ut: UnTrackedLocation =>
          log.info(s"UnTracked: ${ut.connection}")
      }
  }

  // TODO: have a function that tests all services and if all are resolved, send a start message to the supervisor
  def checkServicesResolved(): Unit = {
    // check all services and if all are resolved, send a started message to Supervisor.
    // only do this once.

    if (galilHCD != badHCDReference) supervisor ! Started
  }

  // Receive partial function used when in Running state
  // def runningReceive: Receive = locationReceive orElse diagReceive orElse controllerReceive orElse lifecycleReceivePF orElse unhandledPF
  def runningReceive: Receive = locationReceive orElse controllerReceive orElse lifecycleReceivePF orElse unhandledPF

  // Receive partial function for handling the diagnostic commands
  /*
  def diagReceive: Receive = {
    case DiagnosticMode(hint) =>
      log.debug(s"Received diagnostic mode: $hint")
      diagPublsher ! DiagPublisher.DiagnosticState
    case OperationsMode => {
      log.debug(s"Received operations mode")
      diagPublsher ! DiagPublisher.OperationsState
    }
  }
  */

  // Receive partial function to handle runtime lifecycle messages
  def lifecycleReceivePF: Receive = {
    case Running =>
    // Already running so ignore
    case RunningOffline =>
      // Here we do anything that we need to do be an offline, which means running and ready but not currently in use
      log.info("Received running offline")
    case DoRestart =>
      log.info("Received dorestart")
    case DoShutdown =>
      log.info("SingleAxisAssembly received doshutdown")
      // Ask our HCD to shutdown, then return complete
      galilHCD.foreach(_ ! DoShutdown)
      supervisor ! ShutdownComplete
    case LifecycleFailureInfo(state: LifecycleState, reason: String) =>
      // This is an error conditin so log it
      log.error(s"SingleAxisAssembly received failed lifecycle state: $state for reason: $reason")
  }

  // Catchall unhandled message receive
  def unhandledPF: Receive = {
    case x => log.error(s"Unexpected message in SingleAxisAssembly:unhandledPF: $x")
  }

  /**
   * Function that overrides AssemblyController setup processes incoming SetupConfigArg messages
   * @param sca received SetupConfgiArg
   * @param commandOriginator the sender of the command
   * @return a validation object that indicates if the received config is valid
   */
  override def setup(sca: SetupConfigArg, commandOriginator: Option[ActorRef]): ValidationList = {
    // Returns validations for all
    val validations: ValidationList = validateSequenceConfigArg(sca)
    if (Validation.isAllValid(validations)) {
      // Create a SequentialExecutor to process all SetupConfigs
      val sequentialExecutor = newSequentialExecutor(sca, commandOriginator)
      log.info("about to send StartTheSequence. commandHandler = " + commandHandler)
      sequentialExecutor ! StartTheSequence(commandHandler)
    }
    validations
  }

  /**
   * Performs the initial validation of the incoming SetupConfigArg
   */
  private def validateSequenceConfigArg(sca: SetupConfigArg): ValidationList = {
    // Are all of the configs really for us and correctly formatted, etc?
    /*
    ConfigValidation.validateSingleAxisSetupConfigArg(sca)
     */
    List[Validation]() // sm - I put this in so it would compile
  }

  // Convenience method to create a new SequentialExecutor
  private def newSequentialExecutor(sca: SetupConfigArg, commandOriginator: Option[ActorRef]): ActorRef =
    context.actorOf(SequentialExecutor.props(sca, commandOriginator))

  // Gets the assembly configurations from the config service, or a resource file, if not found and
  // returns the two parsed objects.
  private def getAssemblyConfigs: Future[(SingleAxisCalculationConfig, SingleAxisControlConfig)] = {
    // This is required by the ConfigServiceClient
    implicit val system = context.system
    import system.dispatcher

    implicit val timeout = Timeout(3.seconds)
    val f = ConfigServiceClient.getConfigFromConfigService(singleAxisConfigFile, resource = Some(resource))
    // parse the future
    f.map(configOpt => (SingleAxisCalculationConfig(configOpt.get), SingleAxisControlConfig(configOpt.get)))
  }
}

/**
 * All assembly messages are indicated here
 */
object SingleAxisAssembly {

  // Get the single axis assembly config file from the config service, or use the given resource file if that doesn't work
  val singleAxisConfigFile = new File("ics/singleAxisAssembly.conf")
  val resource = new File("singleAxisAssembly.conf")

  def props(assemblyInfo: AssemblyInfo, supervisor: ActorRef) = Props(classOf[SingleAxisAssembly], assemblyInfo, supervisor)

  // --------- Keys/Messages used by Multiple Components
  /**
   * The message is used within the Assembly to update actors when the Galil HCD goes up and down and up again
   *
   * @param galilHCD the ActorRef of the galilHCD or None
   */
  case class UpdateGalilHCD(galilHCD: Option[ActorRef])

  private val badEventService: Option[EventService] = None
  private val badTelemetryService: Option[TelemetryService] = None
  private val badAlarmService: Option[AlarmService] = None
  private val badHCDReference = None
}
