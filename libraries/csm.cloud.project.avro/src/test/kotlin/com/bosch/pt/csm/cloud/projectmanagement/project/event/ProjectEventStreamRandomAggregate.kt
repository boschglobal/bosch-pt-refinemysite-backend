/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

@file:JvmName("ProjectEventStreamRandomAggregate")

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.test.randomLong
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAddressAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Deprecated("to be removed")
fun randomProject(
    block: ((ProjectAggregateAvro) -> Unit)? = null,
    event: ProjectEventEnumAvro = ProjectEventEnumAvro.CREATED
): ProjectEventAvro.Builder {
  val project =
      ProjectAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .setAuditingInformation(randomAuditing())
          .setStart(
              LocalDate.now()
                  .plusDays(randomLong(0L, 30L))
                  .atStartOfDay(ZoneOffset.UTC)
                  .toInstant()
                  .toEpochMilli())
          .setEnd(
              LocalDate.now()
                  .plusDays(randomLong(30L, 60L))
                  .atStartOfDay(ZoneOffset.UTC)
                  .toInstant()
                  .toEpochMilli())
          .setProjectNumber(randomString())
          .setTitle(randomString())
          .setProjectAddress(randomProjectAddress())
          .build()
          .also { block?.invoke(it) }

  return ProjectEventAvro.newBuilder().setAggregate(project).setName(event)
}

@Deprecated("to be removed")
fun randomProjectAddress(block: ((ProjectAddressAvro) -> Unit)? = null): ProjectAddressAvro =
    ProjectAddressAvro.newBuilder()
        .setStreet("Default test street")
        .setHouseNumber("1")
        .setZipCode("12345")
        .setCity("Default test town")
        .build()
        .also { block?.invoke(it) }

@Deprecated("to be removed")
fun randomProjectPicture(
    block: ((ProjectPictureAggregateAvro) -> Unit)? = null,
    event: ProjectPictureEventEnumAvro = ProjectPictureEventEnumAvro.CREATED
): ProjectPictureEventAvro.Builder {
  val projectPicture =
      ProjectPictureAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECTPICTURE.value))
          .setAuditingInformation(randomAuditing())
          .setFileSize(1000)
          .setFullAvailable(false)
          .setSmallAvailable(false)
          .setWidth(768)
          .setHeight(1024)
          .setProjectBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .build()
          .also { block?.invoke(it) }

  return ProjectPictureEventAvro.newBuilder().setAggregate(projectPicture).setName(event)
}

@Deprecated("to be removed")
fun randomWorkArea(
    block: ((WorkAreaAggregateAvro) -> Unit)? = null,
    event: WorkAreaEventEnumAvro = WorkAreaEventEnumAvro.CREATED
): WorkAreaEventAvro.Builder {
  val workArea =
      WorkAreaAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.WORKAREA.value))
          .setAuditingInformation(randomAuditing())
          .setName(randomString())
          .build()
          .also { block?.invoke(it) }

  return WorkAreaEventAvro.newBuilder().setAggregate(workArea).setName(event)
}

@Deprecated("to be removed")
fun randomWorkAreaList(
    block: ((WorkAreaListAggregateAvro) -> Unit)? = null,
    event: WorkAreaListEventEnumAvro = WorkAreaListEventEnumAvro.CREATED
): WorkAreaListEventAvro.Builder {
  val workAreaList =
      WorkAreaListAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.WORKAREALIST.value))
          .setAuditingInformation(randomAuditing())
          .setProjectBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .setWorkAreas(
              mutableListOf(
                  defaultIdentifier(ProjectmanagementAggregateTypeEnum.WORKAREA.value).build()))
          .build()
          .also { block?.invoke(it) }

  return WorkAreaListEventAvro.newBuilder().setAggregate(workAreaList).setName(event)
}

@Deprecated("to be removed")
fun randomMilestone(
    block: ((MilestoneAggregateAvro) -> Unit)? = null,
    event: MilestoneEventEnumAvro = MilestoneEventEnumAvro.CREATED
): MilestoneEventAvro.Builder {
  val milestone =
      MilestoneAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.MILESTONE.value))
          .setAuditingInformation(randomAuditing())
          .setProjectBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .setType(MilestoneTypeEnumAvro.PROJECT)
          .setHeader(true)
          .setName(randomString())
          .setDate(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
          .build()
          .also { block?.invoke(it) }

  return MilestoneEventAvro.newBuilder().setAggregate(milestone).setName(event)
}

@Deprecated("to be removed")
fun randomMilestoneList(
    block: ((MilestoneListAggregateAvro) -> Unit)? = null,
    event: MilestoneListEventEnumAvro = MilestoneListEventEnumAvro.CREATED
): MilestoneListEventAvro.Builder {
  val milestoneList =
      MilestoneListAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.MILESTONELIST.value))
          .setAuditingInformation(randomAuditing())
          .setProjectBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .setDate(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
          .setHeader(true)
          .setMilestones(
              mutableListOf(
                  defaultIdentifier(ProjectmanagementAggregateTypeEnum.MILESTONE.value).build()))
          .build()
          .also { block?.invoke(it) }

  return MilestoneListEventAvro.newBuilder().setAggregate(milestoneList).setName(event)
}

@Deprecated("to be removed")
fun randomProjectCraftG2(
    block: ((ProjectCraftAggregateG2Avro) -> Unit)? = null,
    event: ProjectCraftEventEnumAvro = ProjectCraftEventEnumAvro.CREATED
): ProjectCraftEventG2Avro.Builder {
  val projectCraft =
      ProjectCraftAggregateG2Avro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECTCRAFT.value))
          .setAuditingInformation(randomAuditing())
          .setProjectBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .setColor(randomString())
          .setName(randomString())
          .build()
          .also { block?.invoke(it) }

  return ProjectCraftEventG2Avro.newBuilder().setAggregate(projectCraft).setName(event)
}

@Deprecated("to be removed")
fun randomParticipantG3(
    block: ((ParticipantAggregateG3Avro) -> Unit)? = null,
    event: ParticipantEventEnumAvro = ParticipantEventEnumAvro.CREATED
): ParticipantEventG3Avro.Builder {
  val participant =
      ParticipantAggregateG3Avro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.PARTICIPANT.value))
          .setAuditingInformation(randomAuditing())
          .setProjectBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .setRole(ParticipantRoleEnumAvro.CSM)
          .setStatus(ParticipantStatusEnumAvro.ACTIVE)
          .build()
          .also { block?.invoke(it) }

  return ParticipantEventG3Avro.newBuilder().setAggregate(participant).setName(event)
}

@Deprecated("to be removed")
fun randomTask(
    block: ((TaskAggregateAvro) -> Unit)? = null,
    event: TaskEventEnumAvro = TaskEventEnumAvro.CREATED
): TaskEventAvro.Builder {
  val date = LocalDate.now()
  return TaskEventAvro.newBuilder()
      .setAggregate(
          TaskAggregateAvro.newBuilder()
              .setAggregateIdentifierBuilder(
                  defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASK.value))
              .setAuditingInformation(randomAuditing())
              .setAssigneeBuilder(
                  defaultIdentifier(ProjectmanagementAggregateTypeEnum.PARTICIPANT.value))
              .setCraftBuilder(defaultIdentifier("CRAFT"))
              .setDescription(randomString())
              .setEditDate(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
              .setLocation(randomString())
              .setProjectBuilder(
                  defaultIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
              .setStatus(TaskStatusEnumAvro.OPEN)
              .setWorkareaBuilder(
                  defaultIdentifier(ProjectmanagementAggregateTypeEnum.WORKAREA.value))
              .setName(randomString())
              .build()
              .also { block?.invoke(it) })
      .setName(event)
}

@Deprecated("to be removed")
fun randomTaskAction(
    block: ((TaskActionSelectionAggregateAvro) -> Unit)? = null,
    event: TaskActionSelectionEventEnumAvro = TaskActionSelectionEventEnumAvro.CREATED
): TaskActionSelectionEventAvro.Builder {
  return TaskActionSelectionEventAvro.newBuilder()
      .setAggregate(
          TaskActionSelectionAggregateAvro.newBuilder()
              .setAggregateIdentifierBuilder(
                  defaultIdentifier((ProjectmanagementAggregateTypeEnum.TASKACTION.value)))
              .setAuditingInformation(randomAuditing())
              .setActions(listOf(TaskActionEnumAvro.EQUIPMENT))
              .setTaskBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASK.value))
              .build()
              .also { block?.invoke(it) })
      .setName(event)
}

@Deprecated("to be removed")
fun randomSchedule(
    block: ((TaskScheduleAggregateAvro) -> Unit)? = null,
    event: TaskScheduleEventEnumAvro = TaskScheduleEventEnumAvro.CREATED
): TaskScheduleEventAvro.Builder {
  val date = LocalDate.now()
  return TaskScheduleEventAvro.newBuilder()
      .setAggregate(
          TaskScheduleAggregateAvro.newBuilder()
              .setAggregateIdentifierBuilder(
                  defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASKSCHEDULE.value))
              .setAuditingInformation(randomAuditing())
              .setEnd(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
              .setSlots(listOf())
              .setStart(date.atStartOfDay().minusDays(5).toInstant(ZoneOffset.UTC).toEpochMilli())
              .setTaskBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASK.value))
              .build()
              .also { block?.invoke(it) })
      .setName(event)
}

@Deprecated("to be removed")
fun randomDayCardG2(
    block: ((DayCardAggregateG2Avro) -> Unit)? = null,
    event: DayCardEventEnumAvro = DayCardEventEnumAvro.CREATED
): DayCardEventG2Avro.Builder =
    DayCardEventG2Avro.newBuilder()
        .setAggregate(
            DayCardAggregateG2Avro.newBuilder()
                .setAggregateIdentifierBuilder(
                    defaultIdentifier(ProjectmanagementAggregateTypeEnum.DAYCARD.value))
                .setAuditingInformation(randomAuditing())
                .setManpower(1.0f.toBigDecimal())
                .setNotes(randomString())
                .setReason(DayCardReasonNotDoneEnumAvro.BAD_WEATHER)
                .setStatus(DayCardStatusEnumAvro.NOTDONE)
                .setTaskBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASK.value))
                .setTitle(randomString())
                .build()
                .also { block?.invoke(it) })
        .setName(event)

@Deprecated("to be removed")
fun randomTaskAttachment(
    block: ((TaskAttachmentAggregateAvro) -> Unit)? = null,
    event: TaskAttachmentEventEnumAvro = TaskAttachmentEventEnumAvro.CREATED
): TaskAttachmentEventAvro.Builder =
    TaskAttachmentEventAvro.newBuilder()
        .setAggregate(
            TaskAttachmentAggregateAvro.newBuilder()
                .setAggregateIdentifierBuilder(
                    defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASKATTACHMENT.value))
                .setAuditingInformation(randomAuditing())
                .setAttachmentBuilder(
                    AttachmentAvro.newBuilder()
                        .setCaptureDate(Instant.now().toEpochMilli())
                        .setFileName("myPicture.jpg")
                        .setFileSize(1000)
                        .setFullAvailable(false)
                        .setSmallAvailable(false)
                        .setWidth(768)
                        .setHeight(1024))
                .setTaskBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASK.value))
                .build()
                .also { block?.invoke(it) })
        .setName(event)

@Deprecated("to be removed")
fun randomTopicG2(
    block: ((TopicAggregateG2Avro) -> Unit)? = null,
    event: TopicEventEnumAvro = TopicEventEnumAvro.CREATED
): TopicEventG2Avro.Builder =
    TopicEventG2Avro.newBuilder()
        .setAggregate(
            TopicAggregateG2Avro.newBuilder()
                .setAggregateIdentifierBuilder(
                    defaultIdentifier(ProjectmanagementAggregateTypeEnum.TOPIC.value))
                .setAuditingInformation(randomAuditing())
                .setDescription(randomString())
                .setCriticality(TopicCriticalityEnumAvro.UNCRITICAL)
                .setTaskBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.TASK.value))
                .build()
                .also { block?.invoke(it) })
        .setName(event)

@Deprecated("to be removed")
fun randomComment(
    block: ((MessageAggregateAvro) -> Unit)? = null,
    event: MessageEventEnumAvro = MessageEventEnumAvro.CREATED
): MessageEventAvro.Builder =
    MessageEventAvro.newBuilder()
        .setAggregate(
            MessageAggregateAvro.newBuilder()
                .setAggregateIdentifierBuilder(
                    defaultIdentifier(ProjectmanagementAggregateTypeEnum.MESSAGE.value))
                .setAuditingInformation(randomAuditing())
                .setContent(randomString())
                .setTopicBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.TOPIC.value))
                .build()
                .also { block?.invoke(it) })
        .setName(event)

@Deprecated("to be removed")
fun randomTopicAttachment(
    block: ((TopicAttachmentAggregateAvro) -> Unit)? = null,
    event: TopicAttachmentEventEnumAvro = TopicAttachmentEventEnumAvro.CREATED
): TopicAttachmentEventAvro.Builder =
    TopicAttachmentEventAvro.newBuilder()
        .setAggregate(
            TopicAttachmentAggregateAvro.newBuilder()
                .setAggregateIdentifierBuilder(
                    defaultIdentifier(ProjectmanagementAggregateTypeEnum.TOPICATTACHMENT.value))
                .setAuditingInformation(randomAuditing())
                .setAttachmentBuilder(
                    AttachmentAvro.newBuilder()
                        .setCaptureDate(Instant.now().toEpochMilli())
                        .setFileName("myPicture.jpg")
                        .setFileSize(1000)
                        .setFullAvailable(false)
                        .setSmallAvailable(false)
                        .setWidth(768)
                        .setHeight(1024))
                .setTopicBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.TOPIC.value))
                .build()
                .also { block?.invoke(it) })
        .setName(event)

@Deprecated("to be removed")
fun randomMessageAttachment(
    block: ((MessageAttachmentAggregateAvro) -> Unit)? = null,
    event: MessageAttachmentEventEnumAvro = MessageAttachmentEventEnumAvro.CREATED
): MessageAttachmentEventAvro.Builder =
    MessageAttachmentEventAvro.newBuilder()
        .setAggregate(
            MessageAttachmentAggregateAvro.newBuilder()
                .setAggregateIdentifierBuilder(
                    defaultIdentifier(ProjectmanagementAggregateTypeEnum.MESSAGEATTACHMENT.value))
                .setAuditingInformation(randomAuditing())
                .setAttachmentBuilder(
                    AttachmentAvro.newBuilder()
                        .setCaptureDate(Instant.now().toEpochMilli())
                        .setFileName("myPicture.jpg")
                        .setFileSize(1000)
                        .setFullAvailable(false)
                        .setSmallAvailable(false)
                        .setWidth(768)
                        .setHeight(1024))
                .setMessageBuilder(
                    defaultIdentifier(ProjectmanagementAggregateTypeEnum.MESSAGE.value))
                .build()
                .also { block?.invoke(it) })
        .setName(event)

@Deprecated("to be removed")
fun slot(block: ((TaskScheduleSlotAvro) -> Unit)? = null): TaskScheduleSlotAvro =
    TaskScheduleSlotAvro.newBuilder()
        .setDate(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        .setDayCardBuilder(defaultIdentifier(ProjectmanagementAggregateTypeEnum.DAYCARD.value))
        .build()
        .also { block?.invoke(it) }

@Deprecated("to be removed")
fun randomAuditing(block: ((AuditingInformationAvro) -> Unit)? = null): AuditingInformationAvro =
    AuditingInformationAvro.newBuilder()
        .setCreatedByBuilder(defaultIdentifier())
        .setCreatedDate(randomLong())
        .setLastModifiedByBuilder(defaultIdentifier())
        .setLastModifiedDate(randomLong())
        .build()
        .also { block?.invoke(it) }

@Deprecated("to be removed")
fun defaultIdentifier(type: String = randomString()): AggregateIdentifierAvro.Builder =
    AggregateIdentifierAvro.newBuilder().setIdentifier(randomString()).setType(type).setVersion(0)
