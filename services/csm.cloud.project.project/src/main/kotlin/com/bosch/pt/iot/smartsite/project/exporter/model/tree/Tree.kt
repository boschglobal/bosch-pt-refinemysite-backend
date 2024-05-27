/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.tree

import net.sf.mpxj.Relation as MpxjRelation
import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.exporter.api.MilestoneExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.Description
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.MILESTONE
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.TASK
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType.WORKAREA
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.regex.Pattern
import kotlin.math.max
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.RelationType.FINISH_START
import net.sf.mpxj.TaskField
import org.springframework.context.MessageSource

@Suppress("LongParameterList", "TooManyFunctions")
class Tree(
    private val projectFile: ProjectFile,
    private val project: Project,
    existingExternalIds: List<ExternalId>,
    private val idType: ExternalIdType,
    private val allowWorkOnNonWorkingDays: Boolean,
    private val requestedTaskSchedulingType: TaskExportSchedulingType,
    private val requestedMilestoneSchedulingType: MilestoneExportSchedulingType,
) {

  val externalIds = mapExternalIdsByObjectIdentifier(existingExternalIds)
  private val nodes: MutableList<AbstractNode> = mutableListOf()
  private val rootNode: ProjectNode = ProjectNode(project.identifier, project)
  private val nodeMap: MutableMap<WorkAreaId, WorkAreaNode> = mutableMapOf()
  private val relations: MutableList<Relation> = mutableListOf()
  private var maxUniqueId = getMaxUniqueId(externalIds)
  private var maxWbsId = getMaxWbsId(externalIds)
  private var maxFileId = getMaxFileId(externalIds)

  fun addWorkArea(workArea: WorkArea) {
    val workAreaId = workArea.identifier
    val node = WorkAreaNode(workAreaId, workArea, getExternalId(WORKAREA, workAreaId.identifier))

    // Add to hierarchy, as placeholder if parent wasn't inserted yet
    addNodeToTree(workArea.parent, node)
  }

  fun addTask(task: Task) = addTask(task, emptyList(), emptyList())

  fun addTask(task: Task, notes: List<Description>, dayCards: List<DayCard>) {
    val taskIdentifier = checkNotNull(task.identifier)
    val node =
        TaskNode(
            taskIdentifier,
            task,
            getExternalId(TASK, taskIdentifier.identifier),
            allowWorkOnNonWorkingDays,
            requestedTaskSchedulingType,
            notes,
            dayCards)
    addNodeToTree(task.workArea?.identifier, node)
  }

  fun addMilestone(milestone: Milestone) {
    val milestoneId = checkNotNull(milestone.identifier)
    val node =
        MilestoneNode(
            identifier = milestoneId,
            milestone = milestone,
            externalId = getExternalId(MILESTONE, milestoneId.identifier),
            requestedSchedulingType = requestedMilestoneSchedulingType,
        )
    addNodeToTree(milestone.workArea?.identifier, node)
  }

  fun addRelation(relation: Relation) {
    check(relation.type == FINISH_TO_START)
    check(relation.project == project)
    relations.add(relation)
  }

  fun write(messageSource: MessageSource): List<ExternalId> {
    assertValidExportStructure()

    // Register custom field for crafts
    registerCustomFields(projectFile)

    return rootNode.write(projectFile, null, messageSource).also {
      val tasksByGuid = projectFile.tasks.associateBy { it.guid }

      relations.forEach { relation ->
        check(relation.type == FINISH_TO_START)
        val source = nodes.single { it.identifier.identifier == relation.source.identifier }
        val target = nodes.single { it.identifier.identifier == relation.target.identifier }

        val sourceTask = checkNotNull(tasksByGuid[checkNotNull(source.externalId).guid])
        val targetTask = checkNotNull(tasksByGuid[checkNotNull(target.externalId).guid])

        targetTask.addPredecessor(sourceTask, FINISH_START, null)
        sourceTask.successors.add(MpxjRelation(sourceTask, targetTask, FINISH_START, null))
      }
    }
  }

  private fun assertValidExportStructure() {
    val workAreaNodes = nodes.filterIsInstance<WorkAreaNode>()

    fun workAreaNodesFromHierarchy(node: AbstractNode?, workAreas: MutableList<WorkAreaNode>) {
      if (node is WorkAreaNode) {
        workAreas.add(node)
      }
      node?.children?.forEach { workAreaNodesFromHierarchy(it, workAreas) }
    }
    val workAreaNodesFromHierarchy =
        mutableListOf<WorkAreaNode>().apply { workAreaNodesFromHierarchy(rootNode, this) }

    require(workAreaNodes.size == workAreaNodesFromHierarchy.size) {
      "Discrepancy between work area nodes: $workAreaNodes and work areas from structure: $workAreaNodesFromHierarchy"
    }
  }

  private fun registerCustomFields(projectFile: ProjectFile) =
      projectFile.customFields
          .getOrCreate(TaskField.TEXT1)
          .also { it.alias = FIELD_ALIAS_CRAFT }
          .returnUnit()

  private fun mapExternalIdsByObjectIdentifier(externalIds: List<ExternalId>) =
      externalIds.associateBy { it.objectIdentifier }.toMutableMap()

  private fun getMaxWbsId(externalIds: Map<UUID, ExternalId>): Int {
    val wbsNumberPattern = Pattern.compile("[^0-9.]*([0-9]).*")

    fun getMaxId(value: String?): Int =
        if (value != null) {
          val matcher = wbsNumberPattern.matcher(value)
          if (matcher.find()) {
            matcher.group(1).toInt()
          } else 0
        } else 0

    val maxActivityId = externalIds.values.maxOfOrNull { id -> getMaxId(id.activityId) } ?: 0
    val maxWbsId = externalIds.values.maxOfOrNull { id -> getMaxId(id.wbs) } ?: 0

    return max(maxActivityId, maxWbsId)
  }

  private fun getMaxUniqueId(externalIds: Map<UUID, ExternalId>): Int =
      externalIds.maxOfOrNull { checkNotNull(it.value.fileUniqueId) } ?: 0

  private fun getMaxFileId(externalIds: Map<UUID, ExternalId>): Int =
      externalIds.maxOfOrNull { checkNotNull(it.value.fileId) } ?: 1

  private fun addNodeToTree(parent: WorkAreaId?, node: AbstractNode) {

    fun insertToNodeMap(node: WorkAreaNode) {
      val placeholder = nodeMap[node.identifier]
      if (placeholder == null) {
        nodeMap[node.identifier] = node
      } else {
        node.children.addAll(placeholder.children)
        nodeMap[node.identifier] = node
      }
    }

    fun addTo(rootNode: ProjectNode, node: AbstractNode) {
      rootNode.children.add(node)
      if (node is WorkAreaNode) {
        insertToNodeMap(node)
      }
    }

    fun addTo(parentNode: WorkAreaNode, node: AbstractNode) {
      if (node is WorkAreaNode) {
        parentNode.children.add(node)
        insertToNodeMap(node)
      } else {
        parentNode.children.add(node)
      }
    }

    // Add node to list of nodes (required for relation element resolution)
    nodes.add(node)

    // Add node to tree
    if (parent == null) {
      addTo(rootNode, node)
    } else {
      var parentNode = nodeMap[parent]
      if (parentNode == null) {
        // Add parent as placeholder if it is not added yet
        parentNode = WorkAreaNode(parent, null, null).also { insertToNodeMap(it) }
      }
      addTo(parentNode, node)
    }
  }

  private fun nextFileId() = ++maxFileId
  private fun nextUniqueId() = ++maxUniqueId
  private fun nextWorkAreaId() = ++maxWbsId

  private fun newExternalId(objectType: ObjectType, objectIdentifier: UUID) =
      ExternalId(
          identifier = randomUUID(),
          projectId = project.identifier,
          idType = this@Tree.idType,
          objectIdentifier = objectIdentifier,
          objectType = objectType,
          guid = objectIdentifier,
          uniqueId = nextUniqueId(),
          fileId = nextFileId(),
          activityId = null,
          wbs = if (objectType == WORKAREA) nextWorkAreaId().toString() else null)

  fun getExternalId(objectType: ObjectType, objectIdentifier: UUID) =
      externalIds[objectIdentifier]
          ?: newExternalId(objectType, objectIdentifier).also { externalIds[objectIdentifier] = it }

  // Only for debugging purpose
  @ExcludeFromCodeCoverage
  @Suppress("unused")
  fun printTree() {

    fun AbstractNode.print(indent: Int) {
      println("${" ".repeat(indent)}$this")
      this.children.sortNodes().forEach { it.print(indent + 2) }
    }

    rootNode.print(0)
  }

  companion object {
    fun getExternalIdType(projectFile: ProjectFile) =
        when (projectFile.projectProperties.fileType) {
          SupportedFileTypes.MSPDI.name -> ExternalIdType.MS_PROJECT
          SupportedFileTypes.PMXML.name -> ExternalIdType.P6
          SupportedFileTypes.PP.name -> ExternalIdType.PP
          else -> error("Unsupported export file type")
        }
  }
}
