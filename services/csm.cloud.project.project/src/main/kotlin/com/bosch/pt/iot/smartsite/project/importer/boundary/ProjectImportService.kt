/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_ALREADY_RUNNING
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_EXISTING_DATA
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_MALICIOUS_FILE
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_IMPORT
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureQueryService
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.external.boundary.ExternalIdService
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisColumn
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisResult
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisStatistics
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.UploadResult
import com.bosch.pt.iot.smartsite.project.importer.boundary.resolver.EntityResolver
import com.bosch.pt.iot.smartsite.project.importer.control.ProjectReader
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.IN_PROGRESS
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.PLANNING
import com.bosch.pt.iot.smartsite.project.importer.model.dio.CraftDio.Companion.PLACEHOLDER_CRAFT_NAME
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.SAFE
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.batch.CreateMilestoneBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.query.MilestoneQueryService
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.CreateProjectCraftCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.query.ProjectCraftListQueryService
import com.bosch.pt.iot.smartsite.project.projectcraft.query.ProjectCraftQueryService
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationService
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.task.command.api.CreateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.batch.CreateTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.AcceptTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.StartTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.query.TaskQueryService
import com.bosch.pt.iot.smartsite.project.task.shared.dto.SaveTaskBatchDto
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleBatchDto
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch.CreateTaskScheduleBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.batch.CreateWorkAreaBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.query.WorkAreaListQueryService
import com.bosch.pt.iot.smartsite.project.workarea.query.WorkAreaQueryService
import com.bosch.pt.iot.smartsite.project.workday.command.api.UpdateWorkdayConfigurationCommand
import com.bosch.pt.iot.smartsite.project.workday.command.handler.UpdateWorkdayConfigurationCommandHandler
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import datadog.trace.api.Trace
import jakarta.persistence.EntityManager
import java.io.InputStream
import java.time.DayOfWeek.MONDAY
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import net.sf.mpxj.FieldType
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.TaskField
import net.sf.mpxj.reader.UniversalProjectReader
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectImportService(
    private val blobStorageRepository: ImportBlobStorageRepository,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val createMilestoneBatchCommandHandler: CreateMilestoneBatchCommandHandler,
    private val createProjectCraftCommandHandler: CreateProjectCraftCommandHandler,
    private val createTaskBatchCommandHandler: CreateTaskBatchCommandHandler,
    private val createWorkAreaBatchCommandHandler: CreateWorkAreaBatchCommandHandler,
    private val createTaskScheduleBatchCommandHandler: CreateTaskScheduleBatchCommandHandler,
    private val entityManager: EntityManager,
    private val externalIdService: ExternalIdService,
    private val featureQueryService: FeatureQueryService,
    private val messageSource: MessageSource,
    private val milestoneQueryService: MilestoneQueryService,
    private val participantQueryService: ParticipantQueryService,
    private val projectCraftQueryService: ProjectCraftQueryService,
    private val projectCraftListQueryService: ProjectCraftListQueryService,
    private val projectImportRepository: ProjectImportRepository,
    private val projectQueryService: ProjectQueryService,
    private val projectReader: ProjectReader,
    private val relationService: RelationService,
    private val taskQueryService: TaskQueryService,
    private val workAreaListQueryService: WorkAreaListQueryService,
    private val workAreaQueryService: WorkAreaQueryService,
    private val startTaskCommandHandler: StartTaskCommandHandler,
    private val acceptTaskCommandHandler: AcceptTaskCommandHandler,
    private val updateWorkdayConfigurationCommandHandler: UpdateWorkdayConfigurationCommandHandler,
    private val workdayConfigurationRepository: WorkdayConfigurationRepository
) {

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  open fun findImportObject(projectIdentifier: ProjectId): ProjectImport? =
      projectImportRepository.findOneByProjectIdentifier(projectIdentifier)

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun save(projectImport: ProjectImport): ProjectImport =
      projectImportRepository.save(projectImport)

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  open fun existsByJobId(jobId: UUID): Boolean = projectImportRepository.existsByJobId(jobId)

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  open fun isImportPossible(project: Project): Boolean {
    val featureIsActiveForUser =
        featureQueryService.isFeatureEnabled(PROJECT_IMPORT, project.identifier)
    val userIsCsm = isUserCsm(checkNotNull(project.identifier))
    if (!featureIsActiveForUser || !userIsCsm) {
      return false
    }

    val existingWorkAreas = workAreaQueryService.countByProjectIdentifier(project.identifier)
    val existingCrafts = projectCraftQueryService.countByProjectIdentifier(project.identifier)
    val existingTasks = taskQueryService.countByProjectIdentifier(project.identifier)
    val existingMilestones = milestoneQueryService.countByProjectIdentifier(project.identifier)
    // Relations and task schedules cannot exist without tasks / milestones, therefore they haven't
    // to be checked.

    return existingWorkAreas == 0L &&
        existingCrafts == 0L &&
        existingTasks == 0L &&
        existingMilestones == 0L
  }

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#projectIdentifier)")
  @Suppress("ThrowsCount")
  open fun upload(
      projectIdentifier: ProjectId,
      file: ByteArray,
      fileName: String,
      mimeType: String
  ): UploadResult {

    val project = requireNotNull(projectQueryService.findOneByIdentifier(projectIdentifier))
    var projectImport = projectImportRepository.findOneByProjectIdentifier(projectIdentifier)

    // Check constraints
    if (!isImportPossible(project)) {
      throw PreconditionViolationException(IMPORT_IMPOSSIBLE_EXISTING_DATA)
    }

    // Reject the upload if an import is currently being executed asynchronously
    if (projectImport != null && projectImport.status == IN_PROGRESS) {
      throw PreconditionViolationException(IMPORT_IMPOSSIBLE_ALREADY_RUNNING)
    }

    // Delete previous file if already exists
    if (projectImport != null) {
      blobStorageRepository.deleteIfExists(projectImport.blobName)
    }

    // Save blob in blob store
    val blobName = "projects/${project.identifier}/${randomUUID()}"
    val blob = blobStorageRepository.save(blobName, file, fileName, mimeType, mutableMapOf())

    val malwareScanResult = blobStorageRepository.getMalwareScanResultBlocking(blob.blobName)
    if (malwareScanResult != SAFE) {
      LOGGER.warn(
          "Unsafe malware scan result detected for project ${project.identifier} " +
              "with result $malwareScanResult for file $fileName")

      // Delete blob
      blobStorageRepository.deleteIfExists(blob.blobName)

      // abort import with error to user
      throw PreconditionViolationException(IMPORT_IMPOSSIBLE_MALICIOUS_FILE)
    }

    // Uploaded file is safe, move to correct storage account
    blobStorageRepository.moveFromQuarantine(blobName)

    // Create / update project import entity
    if (projectImport == null) {
      projectImport = ProjectImport(projectIdentifier, blob.blobName, PLANNING, LocalDateTime.now())
    } else {
      projectImport.blobName = blob.blobName
      projectImport.status = PLANNING
    }

    // Read columns from file
    val columns: List<ImportColumn>
    try {
      val projectFile = readProjectFile(checkNotNull(blobStorageRepository.read(blobName)))
      columns = projectReader.readColumns(projectFile)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      blobStorageRepository.deleteIfExists(blob.blobName)
      throw e
    }

    // Flush it to get the update version
    val updatedImport = projectImportRepository.saveAndFlush(projectImport)
    return UploadResult(columns, requireNotNull(updatedImport.version))
  }

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#projectIdentifier)")
  open fun analyze(
      projectIdentifier: ProjectId,
      readWorkingAreasHierarchically: Boolean,
      craftColumn: AnalysisColumn?,
      workAreaColumn: AnalysisColumn?,
      eTag: ETag
  ): AnalysisResult {
    val project = requireNotNull(projectQueryService.findOneByIdentifier(projectIdentifier))
    val projectImport =
        requireNotNull(projectImportRepository.findOneByProjectIdentifier(projectIdentifier))

    // Check optimistic locking
    eTag.verify(requireNotNull(projectImport.version))

    // Check constraints
    if (!isImportPossible(project)) {
      throw PreconditionViolationException(IMPORT_IMPOSSIBLE_EXISTING_DATA)
    }

    if (projectImport.status == IN_PROGRESS) {
      throw PreconditionViolationException(IMPORT_IMPOSSIBLE_ALREADY_RUNNING)
    }

    // Download blob and analyze the file
    val blob = requireNotNull(blobStorageRepository.find(projectImport.blobName))
    val projectFile = readProjectFile(blob.data.inputStream())
    val result =
        analyzeFile(
            project, projectFile, readWorkingAreasHierarchically, craftColumn, workAreaColumn)

    // Update the import entity
    projectImport.readWorkingAreasHierarchically = readWorkingAreasHierarchically
    projectImport.craftColumn = result.craftColumn?.name
    projectImport.craftColumnFieldType = result.craftColumn?.fieldType?.name
    projectImport.workAreaColumn = result.workAreaColumn?.name
    projectImport.workAreaColumnFieldType = result.workAreaColumn?.fieldType?.name

    // Flush it to get the updated version
    val updatedImport = projectImportRepository.saveAndFlush(projectImport)
    return result.apply { this.version = requireNotNull(updatedImport.version) }
  }

  // This method id called asynchronously by the job service
  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#project.identifier)")
  open fun import(
      project: Project,
      projectFile: ProjectFile,
      readWorkingAreasHierarchically: Boolean,
      craftColumn: String?,
      craftColumnFieldType: String?,
      workAreaColumn: String?,
      workAreaColumnFieldType: String?
  ) {

    // Define an entity resolver to get a persisted entity
    // to reference it in other entities
    val entities = mutableMapOf<DataImportObjectIdentifier, Any>()
    val entityResolver = EntityResolver(entities)

    // Define a context where identifiers of other imported objects can be looked up
    val importContext = ImportContext(mutableMapOf(), mutableMapOf(), entityResolver)

    // Read all objects to import
    val importModel =
        projectReader.read(
            project,
            projectFile,
            importContext,
            readWorkingAreasHierarchically,
            craftColumn,
            craftColumnFieldType,
            workAreaColumn,
            workAreaColumnFieldType)

    // Save data to database
    if (importModel.tasks.isNotEmpty() ||
        importModel.milestones.isNotEmpty() ||
        importModel.workAreas.isNotEmpty()) {
      entities[importModel.projectIdentifier] = project
      val idType =
          when (projectFile.projectProperties.fileType) {
            SupportedFileTypes.MSPDI.name,
            SupportedFileTypes.MPP.name -> ExternalIdType.MS_PROJECT
            SupportedFileTypes.PMXML.name,
            SupportedFileTypes.XER.name -> ExternalIdType.P6
            SupportedFileTypes.PP.name -> ExternalIdType.PP
            else -> error("Unsupported file type: ${projectFile.projectProperties.fileType}")
          }

      // Read current workday configuration from db and from file
      val workdayConfiguration =
          checkNotNull(
              workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(
                  project.identifier))

      val projectId = project.identifier
      businessTransactionManager.doImportInBusinessTransaction(projectId) {
        val externalIds = mutableListOf<ExternalId>()
        externalIds.addAll(importModel.workAreas.map { it.toExternalId(projectId, idType) })
        externalIds.addAll(importModel.tasks.map { it.toExternalId(projectId, idType) })
        externalIds.addAll(importModel.milestones.map { it.toExternalId(projectId, idType) })
        saveExternalIds(externalIds)
        entityManager.flushAndClear() // clear persistence context for improved performance

        updateWorkdayConfiguration(workdayConfiguration, project, importModel, projectFile)

        importWorkAreas(project, importModel, importContext)
        importCrafts(project, importModel, importContext)
        val tasks = importModel.tasks.map { it.toTargetType(importContext) }
        importTasks(project, tasks)
        importSchedules(importModel.taskSchedules.map { it.toTargetType(importContext) })
        updateTaskStatus(tasks)
        entityManager.flushAndClear() // clear persistence context for improved performance

        importMilestones(project, importModel.milestones.map { it.toTargetType(importContext) })
        importRelations(project, importModel.relations.map { it.toTargetType(importContext) })
      }
    }
  }

  @Trace
  @Transactional(propagation = SUPPORTS)
  @NoPreAuthorize
  open fun readProjectFile(inputStream: InputStream): ProjectFile =
      UniversalProjectReader().read(inputStream).also {
        if (it == null || !ALLOWED_FILE_TYPES.contains(it.projectProperties.fileType)) {
          throw PreconditionViolationException(IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE)
        }
      }

  private fun analyzeFile(
      project: Project,
      projectFile: ProjectFile,
      readWorkingAreasHierarchically: Boolean,
      craftColumn: AnalysisColumn?,
      workAreaColumn: AnalysisColumn?
  ): AnalysisResult {
    val columns = projectReader.readColumns(projectFile)
    val craftImportColumn = craftColumn?.resolveImportColumn(columns)
    val workAreaImportColumn = workAreaColumn?.resolveImportColumn(columns)

    val entities = mutableMapOf<DataImportObjectIdentifier, Any>()
    val entityResolver = EntityResolver(entities)
    val importContext = ImportContext(mutableMapOf(), mutableMapOf(), entityResolver)
    val importModel =
        projectReader.read(
            project,
            projectFile,
            importContext,
            readWorkingAreasHierarchically,
            craftImportColumn?.name,
            craftImportColumn?.fieldType?.name,
            workAreaImportColumn?.name,
            workAreaImportColumn?.fieldType?.name)

    val validationResults = importModel.validate(projectFile, importContext, messageSource)

    val craftCount =
        if (importModel.crafts.size == 1 &&
            importModel.crafts.first().name == PLACEHOLDER_CRAFT_NAME)
            0
        else importModel.crafts.size

    return AnalysisResult(
        validationResults,
        AnalysisStatistics(
            importModel.workAreas.size,
            craftCount,
            importModel.tasks.size,
            importModel.milestones.size,
            importModel.relations.size),
        craftImportColumn,
        workAreaImportColumn)
  }

  private fun updateWorkdayConfiguration(
      workdayConfiguration: WorkdayConfiguration,
      project: Project,
      importModel: ImportModel,
      projectFile: ProjectFile
  ) {
    if (workdayConfiguration.workingDays != importModel.workdays.toMutableSet() ||
        workdayConfiguration.holidays != importModel.holidays.toMutableSet()) {
      val startOfWeek = projectFile.projectProperties.weekStartDay ?: MONDAY
      updateWorkdayConfigurationCommandHandler.handle(
          UpdateWorkdayConfigurationCommand(
              workdayConfiguration.version,
              project.identifier,
              startOfWeek,
              importModel.workdays,
              importModel.holidays,
              importModel.hasWorkOnNonWorkDays))
    }
  }

  private fun importWorkAreas(
      project: Project,
      importModel: ImportModel,
      importContext: ImportContext
  ) {
    // The project import service now includes a version that starts at the current work area list
    // version and is incremented for each added workArea. This addition ensures clean handlers and
    // eliminates the need to handle WorkAreaLists without a version in the command handlers.
    val workAreaList =
        workAreaListQueryService.findOneWithDetailsByProjectIdentifier(project.identifier)

    val createWorkAreaCommands =
        importModel.workAreas.mapIndexed { index, workArea ->
          workArea
              .toTargetType(importContext)
              .copy(workAreaListVersion = workAreaList.version + index.toLong())
        }

    if (createWorkAreaCommands.isNotEmpty()) {
      createWorkAreaBatchCommandHandler.handle(project.identifier, createWorkAreaCommands)
    }
  }

  private fun importCrafts(
      project: Project,
      importModel: ImportModel,
      importContext: ImportContext
  ) {
    // The project import service now includes a version that starts at the current project craft
    // list version and is incremented for each added project craft. This addition ensures clean
    // handlers and eliminates the need to handle ProjectCraftLists without a version in the command
    // handlers.
    val projectCraftList = projectCraftListQueryService.findOneByProject(project.identifier)

    importModel.crafts.mapIndexed { index, projectCraft ->
      val createProjectCraftCommand =
          projectCraft
              .toTargetType(importContext)
              .copy(projectCraftListVersion = projectCraftList.version + index.toLong())

      verifySameProject(project, createProjectCraftCommand)
      createProjectCraftCommandHandler.handle(createProjectCraftCommand)
    }
  }

  private fun verifySameProject(project: Project, projectCraft: CreateProjectCraftCommand) {
    require(project.identifier == projectCraft.projectIdentifier) {
      "Project crafts cannot be created for a foreign project"
    }
  }

  private fun importTasks(project: Project, tasks: List<SaveTaskBatchDto>) {
    if (tasks.isNotEmpty()) {
      tasks
          .map {
            CreateTaskCommand(
                identifier = it.id,
                projectIdentifier = project.identifier,
                name = it.name,
                description = it.description,
                location = it.location,
                projectCraftIdentifier = it.projectCraftIdentifier,
                assigneeIdentifier = it.assigneeIdentifier,
                workAreaIdentifier = it.workAreaIdentifier,
                status = DRAFT)
          }
          .let { createTaskBatchCommandHandler.handle(it) }
    }
  }

  /** It's required to send update message after creating the initial schedule. */
  private fun updateTaskStatus(tasks: List<SaveTaskBatchDto>) {
    val invalidStatus =
        TaskStatusEnum.values().toMutableList().apply {
          remove(ACCEPTED)
          remove(DRAFT)
          remove(STARTED)
        }

    require(tasks.none { invalidStatus.contains(it.status) }) {
      "Tasks with invalid status for import found"
    }

    // Tasks that are in progress
    tasks.filter { it.status == STARTED }.forEach { startTaskCommandHandler.handle(it.id) }

    // Tasks that are done
    tasks.filter { it.status == ACCEPTED }.forEach { acceptTaskCommandHandler.handle(it.id) }
  }

  private fun importSchedules(schedules: List<SaveTaskScheduleBatchDto>) {
    if (schedules.isNotEmpty()) {
      val createTaskScheduleCommands =
          schedules.map {
            CreateTaskScheduleCommand(
                identifier = it.identifier ?: TaskScheduleId(),
                taskIdentifier = it.taskIdentifier,
                start = it.start,
                end = it.end,
                slots = it.slots)
          }
      createTaskScheduleBatchCommandHandler.handle(createTaskScheduleCommands).returnUnit()
    }
  }

  private fun importMilestones(project: Project, commands: List<CreateMilestoneCommand>) {
    createMilestoneBatchCommandHandler.handle(project.identifier, commands)
  }

  private fun importRelations(project: Project, relations: List<RelationDto>) {
    if (relations.isNotEmpty()) relationService.createBatch(relations, project.identifier)
  }

  private fun saveExternalIds(externalIds: List<ExternalId>) {
    if (externalIds.isNotEmpty()) externalIdService.saveAll(externalIds)
  }

  private fun AnalysisColumn.resolveImportColumn(columns: List<ImportColumn>): ImportColumn =
      when (columnType) {
        ImportColumnType.ACTIVITY_CODE,
        ImportColumnType.RESOURCE ->
            columns.firstOrNull { it.name == name && it.columnType == columnType }
        ImportColumnType.USER_DEFINED_FIELD ->
            columns.firstOrNull { it.fieldType?.name() == name && it.columnType == columnType }
        else ->
            TaskField.values()
                .firstOrNull { (it as FieldType).name() == name }
                ?.let { fieldType ->
                  columns.firstOrNull {
                    it.fieldType == fieldType &&
                        it.fieldType.name() == name &&
                        it.columnType == columnType
                  }
                }
      } ?: throw PreconditionViolationException(errorMessageKey)

  private fun isUserCsm(projectId: ProjectId): Boolean {
    val userId =
        checkNotNull(SecurityContextHelper.getInstance().getCurrentUser().identifier).asUserId()
    return participantQueryService
        .findAllParticipants(setOf(userId), setOf(projectId))[userId]
        ?.get(projectId)
        ?.role == ParticipantRoleEnum.CSM
  }

  private fun EntityManager.flushAndClear() =
      this.apply {
        flush()
        clear()
      }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ProjectImportService::class.java)

    val ALLOWED_FILE_TYPES = SupportedFileTypes.values().map { it.toString() }
  }
}
