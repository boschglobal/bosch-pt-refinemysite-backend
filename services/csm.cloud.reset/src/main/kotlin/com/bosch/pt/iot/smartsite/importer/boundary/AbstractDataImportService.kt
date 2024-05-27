/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.importer.boundary

import com.bosch.pt.iot.smartsite.dataimport.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.dataimport.attachment.service.TaskAttachmentImportService
import com.bosch.pt.iot.smartsite.dataimport.common.service.FaultTolerantImportService
import com.bosch.pt.iot.smartsite.dataimport.company.model.Company
import com.bosch.pt.iot.smartsite.dataimport.company.service.CompanyImportService
import com.bosch.pt.iot.smartsite.dataimport.employee.model.Employee
import com.bosch.pt.iot.smartsite.dataimport.employee.service.EmployeeImportService
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.model.FeatureToggle
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.service.FeatureToggleImportService
import com.bosch.pt.iot.smartsite.dataimport.project.model.Milestone
import com.bosch.pt.iot.smartsite.dataimport.project.model.Project
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectCraft
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectParticipant
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectPicture
import com.bosch.pt.iot.smartsite.dataimport.project.model.WorkArea
import com.bosch.pt.iot.smartsite.dataimport.project.service.MilestoneImportService
import com.bosch.pt.iot.smartsite.dataimport.project.service.ProjectCraftImportService
import com.bosch.pt.iot.smartsite.dataimport.project.service.ProjectImportService
import com.bosch.pt.iot.smartsite.dataimport.project.service.ProjectParticipantImportService
import com.bosch.pt.iot.smartsite.dataimport.project.service.ProjectPictureImportService
import com.bosch.pt.iot.smartsite.dataimport.project.service.WorkAreaImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCard
import com.bosch.pt.iot.smartsite.dataimport.task.model.Message
import com.bosch.pt.iot.smartsite.dataimport.task.model.Task
import com.bosch.pt.iot.smartsite.dataimport.task.model.Topic
import com.bosch.pt.iot.smartsite.dataimport.task.service.DayCardImportService
import com.bosch.pt.iot.smartsite.dataimport.task.service.MessageImportService
import com.bosch.pt.iot.smartsite.dataimport.task.service.TaskImportService
import com.bosch.pt.iot.smartsite.dataimport.task.service.TopicImportService
import com.bosch.pt.iot.smartsite.dataimport.user.model.Craft
import com.bosch.pt.iot.smartsite.dataimport.user.model.Document
import com.bosch.pt.iot.smartsite.dataimport.user.model.ProfilePicture
import com.bosch.pt.iot.smartsite.dataimport.user.model.User
import com.bosch.pt.iot.smartsite.dataimport.user.service.CraftImportService
import com.bosch.pt.iot.smartsite.dataimport.user.service.DocumentsImportService
import com.bosch.pt.iot.smartsite.dataimport.user.service.ProfilePictureImportService
import com.bosch.pt.iot.smartsite.dataimport.user.service.UserImportService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.importer.boundary.json.JsonResourceReader
import com.bosch.pt.iot.smartsite.importer.boundary.resource.BlobResourceResolver
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ClasspathResourceScanner
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.company
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.craft
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.daycard
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.employee
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.featuretoggle
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.message
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.milestone
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.profilepicture
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.project
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.projectcraft
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.projectparticipant
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.projectpicture
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.taskattachment
import com.fasterxml.jackson.core.type.TypeReference
import java.time.LocalDate
import java.util.concurrent.ExecutionException
import java.util.concurrent.ForkJoinPool
import javax.annotation.PostConstruct
import org.apache.kafka.common.utils.Utils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.core.io.Resource

open class AbstractDataImportService {

  @Autowired protected lateinit var authenticationService: AuthenticationService
  @Autowired protected lateinit var companyImportService: CompanyImportService
  @Autowired protected lateinit var documentsImportService: DocumentsImportService
  @Autowired protected lateinit var employeeImportService: EmployeeImportService
  @Autowired protected lateinit var featureToggleImportService: FeatureToggleImportService
  @Autowired protected lateinit var projectImportService: ProjectImportService
  @Autowired protected lateinit var projectCraftImportService: ProjectCraftImportService
  @Autowired protected lateinit var workAreaImportService: WorkAreaImportService
  @Autowired protected lateinit var milestoneImportService: MilestoneImportService
  @Autowired protected lateinit var projectParticipantImportService: ProjectParticipantImportService
  @Autowired protected lateinit var projectAttachmentImportService: ProjectPictureImportService
  @Autowired protected lateinit var topicImportService: TopicImportService
  @Autowired protected lateinit var messageImportService: MessageImportService
  @Autowired protected lateinit var craftImportService: CraftImportService
  @Autowired protected lateinit var taskImportService: TaskImportService
  @Autowired protected lateinit var dayCardImportService: DayCardImportService
  @Autowired protected lateinit var taskAttachmentImportService: TaskAttachmentImportService
  @Autowired protected lateinit var userImportService: UserImportService
  @Autowired protected lateinit var profilePictureImportService: ProfilePictureImportService
  @Autowired protected lateinit var faultTolerantImportService: FaultTolerantImportService
  @Autowired protected lateinit var blobResourceResolver: BlobResourceResolver
  @Autowired protected lateinit var idRepository: IdRepository
  @Autowired protected lateinit var environment: Environment

  @Value("\${csm-cloud-project.url}") private lateinit var csmCloudProjectUrl: String
  @Value("\${csm-cloud-user.url}") private lateinit var csmCloudUserUrl: String
  @Value("\${import.threads.max}") private val maxThreads: Int = 0

  private lateinit var pool: ForkJoinPool

  @PostConstruct
  fun init() {
    pool = ForkJoinPool(maxThreads)
  }

  fun importData(dataset: String, rootDate: LocalDate) {

    LOGGER.info("Importing dataset {} ...", dataset)
    LOGGER.info("Using {} as root date ...", rootDate)
    LOGGER.info("Using user API url {} ...", csmCloudUserUrl)
    LOGGER.info("Using project API url {} ...", csmCloudProjectUrl)

    val resources: Map<ResourceTypeEnum, Resource> = ClasspathResourceScanner.scan(dataset)
    if (resources.isEmpty()) {
      LOGGER.error("The specified dataset does not contain any files")
      return
    }

    // Reset the id repository
    idRepository.reset()
    LOGGER.info("Logging in admin user ...")
    authenticationService.loginAdmin()
    LOGGER.info("Logging in users ...")

    LOGGER.info("Importing dataset ...")

    val users = getUsers(resources)

    // If the dataset doesn't have users configured, use the users from the master data dataset
    if (users.isEmpty()) {
      LOGGER.info("Using users from dataset 'masterdata' to login.")
      login(
          getResources(
              ClasspathResourceScanner.scan("masterdata"),
              ResourceTypeEnum.user,
              object : TypeReference<Collection<User>>() {}))
    } else {
      login(users)
    }

    LOGGER.info("Load existing crafts")
    craftImportService.resetCraftData()
    craftImportService.loadExistingCrafts()
    LOGGER.info("Import crafts")
    executeParallel {
      getCrafts(resources).parallelStream().forEach {
        faultTolerantImportService.importData(it) { craftImportService.importData(it) }
      }
    }

    LOGGER.info("Load existing users")
    userImportService.resetUserData()
    userImportService.loadExistingUsers()
    LOGGER.info("Import users")
    executeParallel {
      users.parallelStream().forEach {
        faultTolerantImportService.importData(it) { userImportService.importData(it) }
      }
    }

    LOGGER.info("Download user profile pictures (if required)")
    val profilePictures = getProfilePictures(resources)
    profilePictures.forEach { blobResourceResolver.getBlobResource(dataset + "/" + it.path) }

    LOGGER.info("Import users profile pictures")
    executeParallel {
      profilePictures.parallelStream().forEach {
        if (userImportService.getExistingUserId(it.userId) != null) {
          LOGGER.warn("Skipped profile picture of existing user (id: " + it.userId + ")")
          return@forEach
        }
        it.resource = blobResourceResolver.getBlobResource(dataset + "/" + it.path)

        faultTolerantImportService.importData(it) { profilePictureImportService.importData(it) }
      }
    }

    LOGGER.info("Import documents")
    val documents = getDocuments(resources)
    if (documents.isEmpty()) LOGGER.info("No documents to import in this dataset.")
    executeParallel {
      documents.forEach {
        faultTolerantImportService.importData(it) { documentsImportService.importData(it) }
      }
    }

    if (users.isNotEmpty()) {
      LOGGER.info("Waiting until user data is replicated to the company and project service...")
      Utils.sleep(SERVICE_REPLICATION_WAITING_TIME)
    }

    LOGGER.info("Load existing companies")
    companyImportService.resetCompanyData()
    companyImportService.loadExistingCompanies()
    val companies = getCompanies(resources)

    LOGGER.info("Import companies")
    executeParallel {
      companies.parallelStream().forEach {
        faultTolerantImportService.importData(it) { companyImportService.importData(it) }
      }
    }

    LOGGER.info("Load existing employees")
    employeeImportService.resetEmployeeData()
    employeeImportService.loadExistingEmployees()

    LOGGER.info("Import employees")
    val employees = getEmployees(resources)
    executeParallel {
      employees.parallelStream().forEach {
        faultTolerantImportService.importData(it) { employeeImportService.importData(it) }
      }
    }

    if (companies.isNotEmpty()) {
      LOGGER.info("Waiting until company data is replicated to the project service...")
      Utils.sleep(SERVICE_REPLICATION_WAITING_TIME)
    }

    LOGGER.info("Import projects")
    val projects = getProjects(resources)
    executeParallel {
      projects.parallelStream().forEach {
        faultTolerantImportService.importData(it, rootDate) { t, u ->
          projectImportService.importData(t, u)
        }
      }
    }

    LOGGER.info("Import project crafts")
    val projectCraftsByProjectId = getProjectCrafts(resources).groupBy { it.projectId }
    executeParallel {
      projectCraftsByProjectId.forEach {
        it.value
            .sortedBy { it.etag.toInt() }
            .forEach {
              faultTolerantImportService.importData(it) { projectCraftImportService.importData(it) }
            }
      }
    }

    LOGGER.info("Import work areas")
    val workAreasByProjectId = getWorkAreas(resources).groupBy { it.projectId }
    executeParallel {
      workAreasByProjectId.forEach {
        it.value
            .sortedBy { it.etag.toInt() }
            .forEach {
              faultTolerantImportService.importData(it) { workAreaImportService.importData(it) }
            }
      }
    }

    LOGGER.info("Import project participants")
    // The first participant becomes the company representative. So we can only insert
    // representative in parallel with different projects, otherwise we get a HTTP status code 500.
    val partMap: MutableMap<String, MutableList<ProjectParticipant>> = HashMap()
    getParticipants(resources).forEach {
      partMap.computeIfAbsent(it.projectId) { ArrayList() }.add(it)
    }

    executeParallel {
      partMap.values
          .parallelStream()
          .flatMap { it.stream() }
          .forEach {
            faultTolerantImportService.importData(it) {
              projectParticipantImportService.importData(it)
            }
          }
    }

    LOGGER.info("Import milestones")
    val milestones = getMilestones(resources)
    executeParallel {
      milestones.forEach {
        faultTolerantImportService.importData(it) { milestoneImportService.importData(it) }
      }
    }

    LOGGER.info("Download project pictures (if required)")
    val projectPictures = getProjectPictures(resources)
    projectPictures.forEach { blobResourceResolver.getBlobResource(dataset + "/" + it.path) }

    LOGGER.info("Import project pictures")
    executeParallel {
      projectPictures.parallelStream().forEach {
        it.resource = blobResourceResolver.getBlobResource(dataset + "/" + it.path)

        faultTolerantImportService.importData(it) { projectAttachmentImportService.importData(it) }
      }
    }

    LOGGER.info("Import tasks and schedule")
    val tasks = getTasks(resources).associateBy { it.id }

    executeParallel {
      tasks.values.parallelStream().forEach {
        faultTolerantImportService.importData(it, rootDate) { t: Task, rootDate: LocalDate ->
          taskImportService.importData(t, rootDate)
        }
      }
    }

    LOGGER.info("Import day card")
    // Creating day cards leads to an update of it's related task schedule.
    // So we have to aggregate the day cards by task and in the insert of then add an incremental
    // the etag.
    val dayCardsByTaskMap = getDayCards(resources).groupBy { tasks[it.taskId]!! }

    val dayCardsPerTask =
        dayCardsByTaskMap.entries.map { entry ->
          val task = entry.key
          val dayCards = entry.value
          entry.value.map {
            it.copy(
                task = task,
                etag = dayCards.indexOf(it).toString(),
                createWithUserId = it.createWithUserId)
          }
        }

    executeParallel {
      dayCardsPerTask.parallelStream().forEach {
        it.forEach {
          faultTolerantImportService.importData(it, rootDate) { d: DayCard, rootDate: LocalDate ->
            dayCardImportService.importData(d, rootDate)
          }
        }
      }
    }

    LOGGER.info("Import topics")
    // Creating a topic leads to an update of it's related task. So we can only insert
    // topics in parallel with different tasks, otherwise we get an optimistic lock error.
    val topicMap: MutableMap<String?, MutableList<Topic>> = HashMap()
    getTopics(resources).forEach { topic: Topic ->
      topicMap.computeIfAbsent(topic.taskId) { ArrayList() }.add(topic)
    }

    executeParallel {
      topicMap.values
          .parallelStream()
          .flatMap { it.stream() }
          .forEach {
            faultTolerantImportService.importData(it) { topicImportService.importData(it) }
          }
    }

    LOGGER.info("Import messages")
    // Creating a messages leads to an update of it's related topic. So we can only insert
    // messages in parallel with different topics, otherwise we get an optimistic lock error.
    val messageMap: MutableMap<String?, MutableList<Message>> = HashMap()
    getMessages(resources).forEach {
      messageMap.computeIfAbsent(it.topicId) { ArrayList() }.add(it)
    }

    executeParallel {
      messageMap.values
          .parallelStream()
          .flatMap { it.stream() }
          .forEach {
            faultTolerantImportService.importData(it) { messageImportService.importData(it) }
          }
    }

    LOGGER.info("Download task attachments (if required)")
    val attachments = getAttachments(resources)
    attachments.forEach { blobResourceResolver.getBlobResource("$dataset/${it.path}") }

    LOGGER.info("Import task attachments")
    executeParallel {
      attachments.parallelStream().forEach {
        it.resource = blobResourceResolver.getBlobResource(dataset + "/" + it.path)

        faultTolerantImportService.importData(it) { taskAttachmentImportService.importData(it) }
      }
    }

    LOGGER.info("Load existing feature toggles")
    featureToggleImportService.resetFeatureToggleData()
    val ignoreFailing = environment.acceptsProfiles(Profiles.of("local"))
    faultTolerantImportService.run(
        ignoreFailing, "Couldn't load existing feature toggles via api") {
          featureToggleImportService.loadExistingFeatureToggles()
        }
    LOGGER.info("Import feature toggles")
    val featureFlags = getFeaturetoggles(resources)
    if (featureFlags.isEmpty()) LOGGER.info("No feature toggles in this dataset.")
    executeParallel {
      featureFlags.parallelStream().forEach {
        faultTolerantImportService.run(
            ignoreFailing, "Feature toggle '${it.name}' couldn't be imported. Skipping it.") {
              featureToggleImportService.importData(it)
            }
      }
    }

    LOGGER.info("Data import finished")
  }

  protected open fun getAttachments(resources: Map<ResourceTypeEnum, Resource>): List<Attachment> =
      getResources(resources, taskattachment, object : TypeReference<List<Attachment>>() {})

  protected open fun getCompanies(resources: Map<ResourceTypeEnum, Resource>): List<Company> =
      getResources(resources, company, object : TypeReference<List<Company>>() {})

  protected open fun getCrafts(resources: Map<ResourceTypeEnum, Resource>): List<Craft> =
      getResources(resources, craft, object : TypeReference<List<Craft>>() {})

  protected open fun getEmployees(resources: Map<ResourceTypeEnum, Resource>): List<Employee> =
      getResources(resources, employee, object : TypeReference<List<Employee>>() {})

  protected open fun getFeaturetoggles(
      resources: Map<ResourceTypeEnum, Resource>
  ): List<FeatureToggle> =
      getResources(resources, featuretoggle, object : TypeReference<List<FeatureToggle>>() {})

  protected open fun getDayCards(resources: Map<ResourceTypeEnum, Resource>): List<DayCard> =
      getResources(resources, daycard, object : TypeReference<List<DayCard>>() {})

  protected open fun getMessages(resources: Map<ResourceTypeEnum, Resource>): List<Message> =
      getResources(resources, message, object : TypeReference<List<Message>>() {})

  protected open fun getMilestones(resources: Map<ResourceTypeEnum, Resource>): List<Milestone> =
      getResources(resources, milestone, object : TypeReference<List<Milestone>>() {})

  protected open fun getParticipants(
      resources: Map<ResourceTypeEnum, Resource>
  ): List<ProjectParticipant> =
      getResources(
          resources, projectparticipant, object : TypeReference<List<ProjectParticipant>>() {})

  protected open fun getProfilePictures(
      resources: Map<ResourceTypeEnum, Resource>
  ): List<ProfilePicture> =
      getResources(resources, profilepicture, object : TypeReference<List<ProfilePicture>>() {})

  protected open fun getProjects(resources: Map<ResourceTypeEnum, Resource>): List<Project> =
      getResources(resources, project, object : TypeReference<List<Project>>() {})

  protected open fun getProjectCrafts(
      resources: Map<ResourceTypeEnum, Resource>
  ): List<ProjectCraft> =
      getResources(resources, projectcraft, object : TypeReference<List<ProjectCraft>>() {})

  protected open fun getProjectPictures(
      resources: Map<ResourceTypeEnum, Resource>
  ): List<ProjectPicture> =
      getResources(resources, projectpicture, object : TypeReference<List<ProjectPicture>>() {})

  protected open fun getTasks(resources: Map<ResourceTypeEnum, Resource>): List<Task> =
      getResources(resources, ResourceTypeEnum.task, object : TypeReference<List<Task>>() {})

  protected open fun getTopics(resources: Map<ResourceTypeEnum, Resource>): List<Topic> =
      getResources(resources, ResourceTypeEnum.topic, object : TypeReference<List<Topic>>() {})

  protected open fun getUsers(resources: Map<ResourceTypeEnum, Resource>): List<User> =
      getResources(resources, ResourceTypeEnum.user, object : TypeReference<List<User>>() {})

  private fun getDocuments(resources: Map<ResourceTypeEnum, Resource>): List<Document> =
      getResources(
          resources, ResourceTypeEnum.document, object : TypeReference<List<Document>>() {})

  protected open fun getWorkAreas(resources: Map<ResourceTypeEnum, Resource>): List<WorkArea> =
      getResources(
          resources, ResourceTypeEnum.workarea, object : TypeReference<List<WorkArea>>() {})

  private fun executeParallel(runnable: Runnable) {
    try {
      pool.submit(runnable).get()
    } catch (e: InterruptedException) {
      throw IllegalStateException(e)
    } catch (e: ExecutionException) {
      throw IllegalStateException(e)
    }
  }

  private fun login(users: Collection<User>) {
    executeParallel {
      users.parallelStream().forEach {
        authenticationService.loginUser(it.id, it.email, it.password)
      }
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(DynamicDataImportService::class.java)

    private const val SERVICE_REPLICATION_WAITING_TIME = 30000L

    @JvmStatic
    fun <T : Collection<*>?> getResources(
        resources: Map<ResourceTypeEnum, Resource?>,
        type: ResourceTypeEnum,
        clazz: TypeReference<T>
    ): T {
      return if (resources[type] != null) {
        JsonResourceReader.read(resources[type]!!, clazz)
      } else {
        LOGGER.info("No files found in this dataset for this resource type.")

        @Suppress("UNCHECKED_CAST")
        // @formatter:off
        emptyList<Any>() as T
        // @formatter:on
      }
    }
  }
}
