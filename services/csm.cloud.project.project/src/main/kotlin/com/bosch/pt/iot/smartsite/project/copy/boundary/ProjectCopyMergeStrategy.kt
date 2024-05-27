/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.iot.smartsite.project.copy.boundary.GenericImporter.MergeStrategy
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ActiveParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectCraftDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.WorkAreaDto
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId

class ProjectCopyMergeStrategy : MergeStrategy {

  private val idMapperFactory = IdMapperFactory()

  override fun merge(source: ProjectDto, target: ProjectDto): ProjectDto =
      source
          .mergeParticipantsInto(target)
          .mergeCrafts()
          .mergeWorkAreas()
          .mergeMilestones()
          .mergeTasks()

  private fun ProjectDto.mergeParticipantsInto(targetProject: ProjectDto): ProjectDto {
    val initialTargetParticipantUsers = targetProject.participants.map { it.userId }.toSet()
    val initialParticipantMapping =
        this.participants
            .filter { source -> source.userId in initialTargetParticipantUsers }
            .associate {
              it.identifier to
                  targetProject.participants
                      .first { target -> target.userId == it.userId }
                      .identifier
            }
    val participantIdMapper =
        idMapperFactory.createGeneratingMapper(initialParticipantMapping) { ParticipantId() }

    return this.copy(
        participants =
            this.participants
                .filterIsInstance<ActiveParticipantDto>()
                .filter { it.userId !in initialTargetParticipantUsers }
                .map { it.copy(identifier = participantIdMapper.map(it.identifier)) }
                .toSet(),
        tasks =
            this.tasks.map {
              it.copy(
                  assignee =
                      if (it.assignee != null) participantIdMapper.map(it.assignee) else null)
            })
  }

  private fun ProjectDto.mergeCrafts(): ProjectDto {
    val projectCraftIdMapper =
        if (this.projectCrafts.isNotEmpty()) {
          idMapperFactory.createGeneratingMapper { ProjectCraftId() }
        } else {
          idMapperFactory.createConstantMapper(placeholderProjectCraft.identifier)
        }
    return this.copy(
        projectCrafts =
            if (this.projectCrafts.isNotEmpty())
                this.projectCrafts.map {
                  it.copy(identifier = projectCraftIdMapper.map(it.identifier))
                }
            else listOf(placeholderProjectCraft),
        milestones =
            this.milestones.map {
              if (it.projectCraft != null)
                  it.copy(projectCraft = projectCraftIdMapper.map(it.projectCraft))
              else it
            },
        tasks =
            this.tasks.map { it.copy(projectCraft = projectCraftIdMapper.map(it.projectCraft)) })
  }

  private fun ProjectDto.mergeWorkAreas(): ProjectDto {
    val workAreaIdMapper =
        if (this.workAreas.isNotEmpty()) {
          idMapperFactory.createGeneratingMapper { WorkAreaId() }
        } else {
          idMapperFactory.createConstantMapper(placeholderWorkArea.identifier)
        }
    return this.copy(
        workAreas =
            if (this.workAreas.isNotEmpty())
                this.workAreas.map { it.copy(identifier = workAreaIdMapper.map(it.identifier)) }
            else if (this.tasks.isNotEmpty() || this.milestones.isNotEmpty())
                listOf(placeholderWorkArea)
            else emptyList(),
        milestones =
            this.milestones.map {
              if (it.workArea != null) it.copy(workArea = workAreaIdMapper.map(it.workArea)) else it
            },
        tasks =
            this.tasks.map {
              if (it.workArea != null) it.copy(workArea = workAreaIdMapper.map(it.workArea)) else it
            })
  }

  private fun ProjectDto.mergeTasks(): ProjectDto {
    val taskIdMapper = idMapperFactory.createGeneratingMapper { TaskId() }
    val dayCardIdMapper = idMapperFactory.createGeneratingMapper { DayCardId() }
    val topicIdMapper = idMapperFactory.createGeneratingMapper { TopicId() }
    val messageIdMapper = idMapperFactory.createGeneratingMapper { MessageId() }
    return this.copy(
        tasks =
            this.tasks.map {
              it.copy(
                  identifier = taskIdMapper.map(it.identifier),
                  dayCards =
                      it.dayCards.map { dayCard ->
                        dayCard.copy(identifier = dayCardIdMapper.map(dayCard.identifier))
                      },
                  topics =
                      it.topics.map { topic ->
                        topic.copy(
                            identifier = topicIdMapper.map(topic.identifier),
                            messages =
                                topic.messages.map { message ->
                                  message.copy(identifier = messageIdMapper.map(message.identifier))
                                })
                      })
            },
        relations =
            this.relations.map {
              it.copy(
                  source = mapRelationElementIdTask(it.source, taskIdMapper),
                  target = mapRelationElementIdTask(it.target, taskIdMapper))
            })
  }

  private fun ProjectDto.mergeMilestones(): ProjectDto {
    val milestoneMapper = idMapperFactory.createGeneratingMapper { MilestoneId() }
    return this.copy(
        milestones =
            this.milestones.map { it.copy(identifier = milestoneMapper.map(it.identifier)) },
        relations =
            this.relations.map {
              it.copy(
                  source = mapRelationElementIdMilestone(it.source, milestoneMapper),
                  target = mapRelationElementIdMilestone(it.target, milestoneMapper))
            })
  }

  private fun mapRelationElementIdTask(
      relation: RelationElementDto,
      taskMapper: IdMapper<TaskId>,
  ) =
      relation.copy(
          id =
              when (relation.type) {
                RelationElementTypeEnum.TASK -> taskMapper.map(relation.id.asTaskId()).toUuid()
                else -> relation.id
              })

  private fun mapRelationElementIdMilestone(
      relation: RelationElementDto,
      milestoneMapper: IdMapper<MilestoneId>
  ) =
      relation.copy(
          id =
              when (relation.type) {
                RelationElementTypeEnum.MILESTONE ->
                    milestoneMapper.map(MilestoneId(relation.id)).toUuid()
                else -> relation.id
              })

  private val placeholderProjectCraft: ProjectCraftDto by lazy {
    ProjectCraftDto(ProjectCraftId(), "Placeholder Project Craft", "#d9c200")
  }

  private val placeholderWorkArea: WorkAreaDto by lazy {
    WorkAreaDto(WorkAreaId(), "Placeholder Working Area")
  }

  /**
   * Mapping all entity pairs - original entity ID -> copied entity ID<br> Example
   * GeneratingIdMapper: Task A with TaskSchedule B is copied to Task A1 and TaskSchedule B1 The
   * task mapper maps A -> A1; The taskSchedule mapper B -> B1 for further children to be mapped
   * correctly
   */
  interface IdMapper<T> {
    fun map(identifier: T): T
  }

  class IdMapperFactory {

    fun <T> createGeneratingMapper(
        initialMapping: Map<T, T> = emptyMap(),
        generator: () -> T
    ): IdMapper<T> = GeneratingIdMapper(initialMapping.toMutableMap(), generator)

    fun <T> createConstantMapper(constant: T): IdMapper<T> = ConstantIdMapper(constant)

    private inner class ConstantIdMapper<T>(private val constant: T) : IdMapper<T> {
      override fun map(identifier: T): T = constant
    }

    private inner class GeneratingIdMapper<T>(
        private val identifierMap: MutableMap<T, T> = mutableMapOf(),
        private val generator: () -> T
    ) : IdMapper<T> {
      override fun map(identifier: T): T = identifierMap.getOrPut(identifier, generator)
    }
  }
}
