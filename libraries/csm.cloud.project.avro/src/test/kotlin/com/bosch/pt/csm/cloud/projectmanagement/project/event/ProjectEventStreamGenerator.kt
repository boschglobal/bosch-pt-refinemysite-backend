/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitComment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitProjectPicture
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import java.time.Instant
import java.time.LocalDate
import org.apache.avro.specific.SpecificRecordBase

@Deprecated("to be removed")
class ProjectEventStreamGenerator(
    private val timeLineGenerator: TimeLineGenerator,
    private val eventListener: ProjectEventListener,
    private val context: MutableMap<String, SpecificRecordBase>
) : AbstractEventStreamGenerator(context) {

  fun submitProject(
      name: String = "project",
      userName: String = "user",
      eventName: ProjectEventEnumAvro = ProjectEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((ProjectAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(name)

    val defaultAggregateModification: ((ProjectAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
    }

    context[name] =
        eventListener.submitProject(
            project, eventName, defaultAggregateModification, aggregateModifications)
    return this
  }

  fun submitProjectPicture(
      name: String = "projectPicture",
      projectName: String = "project",
      userName: String = "user",
      eventName: ProjectPictureEventEnumAvro = ProjectPictureEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((ProjectPictureAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val projectPicture = get<ProjectPictureAggregateAvro>(name)

    val defaultAggregateModification: ((ProjectPictureAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply { setProject(project.getAggregateIdentifier()) }
    }

    context[name] =
        eventListener.submitProjectPicture(
            project.getAggregateIdentifier(),
            projectPicture,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitParticipantG3(
      name: String = "participant",
      projectName: String = "project",
      companyName: String? = "company",
      userName: String? = "user",
      // userReference can be used to simulate compacted users (when simulating a reprocessing case)
      userReference: AggregateIdentifierAvro? = null,
      auditUserName: String = "user",
      eventName: ParticipantEventEnumAvro = ParticipantEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((ParticipantAggregateG3Avro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val company: SpecificRecordBase? = companyName?.let { get(companyName) }
    val user: SpecificRecordBase? = userName?.let { get(userName) }
    val participant = get<ParticipantAggregateG3Avro?>(name)

    val defaultAggregateModification: ((ParticipantAggregateG3Avro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, auditUserName, time)
      it.apply {
        setProject(project.getAggregateIdentifier())
        company?.apply { setCompany(getAggregateIdentifier(company)) }
        user?.apply { setUser(getAggregateIdentifier(user)) }
        userReference?.apply { setUser(this) }
      }
    }

    context[name] =
        eventListener.submitParticipantG3(
            project.getAggregateIdentifier(),
            participant,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitWorkArea(
      name: String = "workArea",
      projectName: String = "project",
      workAreaName: String? = null,
      userName: String = "user",
      eventName: WorkAreaEventEnumAvro = WorkAreaEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((WorkAreaAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val workArea = get<WorkAreaAggregateAvro?>(name)

    val defaultAggregateModification: ((WorkAreaAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply { workAreaName?.let(::setName) }
    }

    context[name] =
        eventListener.submitWorkArea(
            project.getAggregateIdentifier(),
            workArea,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitWorkAreaList(
      name: String = "workAreaList",
      projectName: String = "project",
      workArea: String? = "workArea",
      userName: String = "user",
      eventName: WorkAreaListEventEnumAvro = WorkAreaListEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((WorkAreaListAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val workAreaList = get<WorkAreaListAggregateAvro?>(name)
    val workAreaItem: WorkAreaAggregateAvro? = workArea?.let { get(workArea) }

    val defaultAggregateModification: ((WorkAreaListAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setProject(project.getAggregateIdentifier())
        workAreaItem?.apply { it.setWorkAreas(mutableListOf(getAggregateIdentifier())) }
      }
    }

    context[name] =
        eventListener.submitWorkAreaList(
            project.getAggregateIdentifier(),
            workAreaList,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitMilestone(
      name: String = "milestone",
      projectName: String = "project",
      craftName: String? = null,
      workAreaName: String? = null,
      userName: String = "user",
      eventName: MilestoneEventEnumAvro = MilestoneEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((MilestoneAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val milestone = get<MilestoneAggregateAvro>(name)
    val project = get<ProjectAggregateAvro>(projectName)
    val craft = craftName?.let { get<ProjectCraftAggregateG2Avro>(it) }
    val workArea = workAreaName?.let { get<WorkAreaAggregateAvro>(it) }

    val defaultAggregateModification: ((MilestoneAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setProject(project.getAggregateIdentifier())
        craftName?.let { setCraft(craft!!.getAggregateIdentifier()) }
        workArea?.let { setWorkarea(workArea.getAggregateIdentifier()) }
      }
    }

    context[name] =
        eventListener.submitMilestone(
            project.getAggregateIdentifier(),
            milestone,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitMilestoneList(
      name: String = "milestoneList",
      projectName: String = "project",
      date: LocalDate? = null,
      header: Boolean? = null,
      workAreaName: String? = null,
      milestoneNames: List<String> = listOf("milestone"),
      userName: String = "user",
      eventName: MilestoneListEventEnumAvro = MilestoneListEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((MilestoneListAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val milestoneList = get<MilestoneListAggregateAvro>(name)
    val project = get<ProjectAggregateAvro>(projectName)
    val workArea = workAreaName?.let { get<WorkAreaAggregateAvro>(it) }

    val defaultAggregateModification: ((MilestoneListAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setProject(project.getAggregateIdentifier())
        date?.let { (setDate(date.toEpochMilli())) }
        header?.let { setHeader(header) }
        workArea?.let { setWorkarea(workArea.getAggregateIdentifier()) }
        if (milestoneNames.isNotEmpty()) {
          it.setMilestones(
              milestoneNames
                  .map { milestoneName -> get<MilestoneAggregateAvro>(milestoneName) }
                  .map { milestone -> milestone.getAggregateIdentifier() })
        }
      }
    }

    context[name] =
        eventListener.submitMilestoneList(
            project.getAggregateIdentifier(),
            milestoneList,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitProjectCraftG2(
      name: String = "projectCraft",
      projectName: String = "project",
      craftName: String? = null,
      color: String? = null,
      userName: String = "user",
      eventName: ProjectCraftEventEnumAvro = ProjectCraftEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((ProjectCraftAggregateG2Avro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val projectCraft = get<ProjectCraftAggregateG2Avro?>(name)

    val defaultAggregateModification: ((ProjectCraftAggregateG2Avro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setProject(project.getAggregateIdentifier())
        craftName?.apply(::setName)
        color?.apply(::setColor)
      }
    }

    context[name] =
        eventListener.submitProjectCraftG2(
            project.getAggregateIdentifier(),
            projectCraft,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitTask(
      name: String = "task",
      projectName: String = "project",
      assigneeName: String? = "participant",
      projectCraftName: String? = null,
      workAreaName: String? = null,
      userName: String = "user",
      eventName: TaskEventEnumAvro = TaskEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((TaskAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val task = get<TaskAggregateAvro?>(name)

    val defaultAggregateModification: ((TaskAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setProject(project.getAggregateIdentifier())
        assigneeName?.apply {
          when (val participant = context[this]) {
            is ParticipantAggregateG3Avro -> setAssignee(participant.getAggregateIdentifier())
            else -> throw IllegalArgumentException("Unexpected participant version found")
          }
        }
        projectCraftName?.apply {
          setCraft(get<ProjectCraftAggregateG2Avro>(this).getAggregateIdentifier())
        }
        workAreaName?.apply {
          setWorkarea(get<WorkAreaAggregateAvro>(this).getAggregateIdentifier())
        }
      }
    }

    context[name] =
        eventListener.submitTask(
            project.getAggregateIdentifier(),
            task,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitTaskAction(
      name: String = "taskAction",
      projectName: String = "project",
      taskName: String = "task",
      userName: String = "user",
      taskActionName: TaskActionEnumAvro? = null,
      eventName: TaskActionSelectionEventEnumAvro = TaskActionSelectionEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((TaskActionSelectionAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val task = get<TaskAggregateAvro>(taskName)
    val taskAction = get<TaskActionSelectionAggregateAvro?>(name)

    val defaultAggregateModification: ((TaskActionSelectionAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setTask(task.getAggregateIdentifier())
        if (taskActionName != null) {
          setActions(listOf(taskActionName))
        }
      }
    }

    context[name] =
        eventListener.submitTaskAction(
            project.getAggregateIdentifier(),
            taskAction,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitTaskActions(
      name: String = "taskAction",
      projectName: String = "project",
      taskName: String = "task",
      userName: String = "user",
      taskActionName: List<TaskActionEnumAvro>? = emptyList(),
      eventName: TaskActionSelectionEventEnumAvro = TaskActionSelectionEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((TaskActionSelectionAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val task = get<TaskAggregateAvro>(taskName)
    val taskAction = get<TaskActionSelectionAggregateAvro?>(name)

    val defaultAggregateModification: ((TaskActionSelectionAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setTask(task.getAggregateIdentifier())
        if (taskActionName!!.isNotEmpty()) {
          setActions(taskActionName)
        }
      }
    }

    context[name] =
        eventListener.submitTaskAction(
            project.getAggregateIdentifier(),
            taskAction,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitSchedule(
      name: String = "schedule",
      projectName: String = "project",
      taskName: String = "task",
      dayCardName: String? = null,
      userName: String = "user",
      eventName: TaskScheduleEventEnumAvro = TaskScheduleEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((TaskScheduleAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {

    val project = get<ProjectAggregateAvro>(projectName)
    val task = get<TaskAggregateAvro>(taskName)
    val schedule = get<TaskScheduleAggregateAvro?>(name)
    var dayCardIdentifier: AggregateIdentifierAvro? = null
    if (context.containsKey(dayCardName)) {
      val dayCard = context[dayCardName] as DayCardAggregateG2Avro
      dayCardIdentifier = dayCard.getAggregateIdentifier()
    }

    val defaultAggregateModification: ((TaskScheduleAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setTask(task.getAggregateIdentifier())
        dayCardIdentifier?.apply {
          val slots = ArrayList(getSlots())
          slots.add(
              slot { slot ->
                slot.setDate(getStart())
                slot.setDayCard(dayCardIdentifier)
              })
          setSlots(slots)
        }
      }
    }

    context[name] =
        eventListener.submitSchedule(
            project.getAggregateIdentifier(),
            schedule,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitDayCardG2(
      name: String = "dayCard",
      projectName: String = "project",
      taskName: String = "task",
      userName: String = "user",
      eventName: DayCardEventEnumAvro = DayCardEventEnumAvro.CANCELLED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((DayCardAggregateG2Avro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val task = get<TaskAggregateAvro>(taskName)
    val dayCard = get<DayCardAggregateG2Avro?>(name)

    val defaultAggregateModification: ((DayCardAggregateG2Avro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.setTask(task.getAggregateIdentifier())
    }

    context[name] =
        eventListener.submitDayCardG2(
            project.getAggregateIdentifier(),
            dayCard,
            eventName,
            defaultAggregateModification,
            aggregateModifications)
    return this
  }

  fun submitTaskAttachment(
      name: String = "taskAttachment",
      projectName: String = "project",
      taskName: String = "task",
      userName: String = "user",
      eventName: TaskAttachmentEventEnumAvro = TaskAttachmentEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((TaskAttachmentAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val task = get<TaskAggregateAvro>(taskName)
    val taskAttachment = get<TaskAttachmentAggregateAvro?>(name)

    val defaultAggregateModifications: ((TaskAttachmentAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.setTask(task.getAggregateIdentifier())
    }

    context[name] =
        eventListener.submitTaskAttachment(
            project.getAggregateIdentifier(),
            taskAttachment,
            eventName,
            defaultAggregateModifications,
            aggregateModifications)
    return this
  }

  fun submitTopicG2(
      name: String = "topic",
      projectName: String = "project",
      taskName: String = "task",
      userName: String = "user",
      description: String? = randomString(),
      eventName: TopicEventEnumAvro = TopicEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((TopicAggregateG2Avro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val task = get<TaskAggregateAvro>(taskName)
    val topic = get<TopicAggregateG2Avro?>(name)

    val defaultAggregateModifications: ((TopicAggregateG2Avro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setDescription(description)
        setTask(task.getAggregateIdentifier())
      }
    }

    context[name] =
        eventListener.submitTopicG2(
            project.getAggregateIdentifier(),
            topic,
            eventName,
            defaultAggregateModifications,
            aggregateModifications)
    return this
  }

  fun submitComment(
      name: String = "comment",
      projectName: String = "project",
      topicName: String = "topic",
      userName: String = "user",
      content: String? = randomString(),
      eventName: MessageEventEnumAvro = MessageEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((MessageAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val topic = get<TopicAggregateG2Avro>(topicName)
    val comment = get<MessageAggregateAvro?>(name)

    val defaultAggregateModifications: ((MessageAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.apply {
        setContent(content)
        setTopic(topic.getAggregateIdentifier())
      }
    }

    context[name] =
        eventListener.submitComment(
            project.getAggregateIdentifier(),
            comment,
            eventName,
            defaultAggregateModifications,
            aggregateModifications)
    return this
  }

  fun submitTopicAttachment(
      name: String = "topicAttachment",
      projectName: String = "project",
      topicName: String = "topic",
      userName: String = "user",
      eventName: TopicAttachmentEventEnumAvro = TopicAttachmentEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((TopicAttachmentAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val topic = get<TopicAggregateG2Avro>(topicName)
    val topicAttachment = get<TopicAttachmentAggregateAvro?>(name)

    val defaultAggregateModifications: ((TopicAttachmentAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.setTopic(topic.getAggregateIdentifier())
    }

    context[name] =
        eventListener.submitTopicAttachment(
            rootContextIdentifierAvro = project.getAggregateIdentifier(),
            existingTopicAttachment = topicAttachment,
            eventName = eventName,
            topicAttachmentAggregateOperations =
                arrayOf(defaultAggregateModifications, aggregateModifications))
    return this
  }

  fun submitCommentAttachment(
      name: String = "commentAttachment",
      projectName: String = "project",
      commentName: String = "comment",
      userName: String = "user",
      eventName: MessageAttachmentEventEnumAvro = MessageAttachmentEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((MessageAttachmentAggregateAvro) -> Unit)? = null
  ): ProjectEventStreamGenerator {
    val project = get<ProjectAggregateAvro>(projectName)
    val comment = get<MessageAggregateAvro>(commentName)
    val messageAttachment = get<MessageAttachmentAggregateAvro?>(name)

    val defaultAggregateModifications: ((MessageAttachmentAggregateAvro) -> Unit)? = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)
      it.setMessage(comment.getAggregateIdentifier())
    }

    context[name] =
        eventListener.submitMessageAttachment(
            rootContextIdentifierAvro = project.getAggregateIdentifier(),
            existingMessageAttachment = messageAttachment,
            eventName = eventName,
            messageAttachmentAggregateOperations =
                arrayOf(defaultAggregateModifications, aggregateModifications))
    return this
  }
}
