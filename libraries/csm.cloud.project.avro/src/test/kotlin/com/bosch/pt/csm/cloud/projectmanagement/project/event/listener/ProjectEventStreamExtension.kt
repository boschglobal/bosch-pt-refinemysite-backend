/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.messageattachment.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomComment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomProjectPicture
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import io.mockk.mockk
import io.mockk.verify
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

fun ProjectEventListener.submitProject(
    existingProject: ProjectAggregateAvro? = null,
    event: ProjectEventEnumAvro = ProjectEventEnumAvro.CREATED,
    vararg blocks: ((ProjectAggregateAvro) -> Unit)?
): ProjectAggregateAvro {
  val project = existingProject.buildEventAvro(event, *blocks)

  val rootContextIdentifierAvro = project.getAggregate().getAggregateIdentifier()
  val key =
      AggregateEventMessageKey(
          rootContextIdentifierAvro.buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(project, key).getAggregate()
}

fun ProjectEventListener.submitProjectPicture(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingProjectPicture: ProjectPictureAggregateAvro? = null,
    event: ProjectPictureEventEnumAvro = ProjectPictureEventEnumAvro.CREATED,
    vararg blocks: ((ProjectPictureAggregateAvro) -> Unit)?
): ProjectPictureAggregateAvro {
  val projectPicture = existingProjectPicture.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          projectPicture.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(projectPicture, key).getAggregate()
}

fun ProjectEventListener.submitWorkArea(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingWorkArea: WorkAreaAggregateAvro? = null,
    event: WorkAreaEventEnumAvro = WorkAreaEventEnumAvro.CREATED,
    vararg blocks: ((WorkAreaAggregateAvro) -> Unit)?
): WorkAreaAggregateAvro {
  val workArea = existingWorkArea.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          workArea.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(workArea, key).getAggregate()
}

fun ProjectEventListener.submitWorkAreaList(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingWorkAreaList: WorkAreaListAggregateAvro? = null,
    event: WorkAreaListEventEnumAvro = WorkAreaListEventEnumAvro.CREATED,
    vararg blocks: ((WorkAreaListAggregateAvro) -> Unit)?
): WorkAreaListAggregateAvro {
  val workAreaList = existingWorkAreaList.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          workAreaList.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(workAreaList, key).getAggregate()
}

fun ProjectEventListener.submitMilestone(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingMilestone: MilestoneAggregateAvro? = null,
    event: MilestoneEventEnumAvro = MilestoneEventEnumAvro.CREATED,
    vararg blocks: ((MilestoneAggregateAvro) -> Unit)?
): MilestoneAggregateAvro {
  val milestone = existingMilestone.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          milestone.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(milestone, key).getAggregate()
}

fun ProjectEventListener.submitMilestoneList(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingMilestoneList: MilestoneListAggregateAvro? = null,
    event: MilestoneListEventEnumAvro = MilestoneListEventEnumAvro.CREATED,
    vararg blocks: ((MilestoneListAggregateAvro) -> Unit)?
): MilestoneListAggregateAvro {
  val milestoneList = existingMilestoneList.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          milestoneList.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(milestoneList, key).getAggregate()
}

fun ProjectEventListener.submitProjectCraftG2(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingProjectCraft: ProjectCraftAggregateG2Avro? = null,
    event: ProjectCraftEventEnumAvro = ProjectCraftEventEnumAvro.CREATED,
    vararg blocks: ((ProjectCraftAggregateG2Avro) -> Unit)?
): ProjectCraftAggregateG2Avro {
  val projectCraft = existingProjectCraft.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          projectCraft.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(projectCraft, key).getAggregate()
}

fun ProjectEventListener.submitParticipantG3(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingParticipant: ParticipantAggregateG3Avro? = null,
    event: ParticipantEventEnumAvro = ParticipantEventEnumAvro.CREATED,
    vararg blocks: ((ParticipantAggregateG3Avro) -> Unit)?
): ParticipantAggregateG3Avro {
  val participant = existingParticipant.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          participant.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(participant, key).getAggregate()
}

fun ProjectEventListener.submitTask(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingTask: TaskAggregateAvro? = null,
    event: TaskEventEnumAvro = TaskEventEnumAvro.CREATED,
    vararg blocks: ((TaskAggregateAvro) -> Unit)?
): TaskAggregateAvro {
  val task = existingTask.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          task.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(task, key).getAggregate()
}

fun ProjectEventListener.submitTaskAction(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingTaskAction: TaskActionSelectionAggregateAvro? = null,
    event: TaskActionSelectionEventEnumAvro = TaskActionSelectionEventEnumAvro.CREATED,
    vararg blocks: ((TaskActionSelectionAggregateAvro) -> Unit)?
): TaskActionSelectionAggregateAvro {
  val taskAction = existingTaskAction.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          taskAction.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(taskAction, key).getAggregate()
}

fun ProjectEventListener.submitSchedule(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingSchedule: TaskScheduleAggregateAvro? = null,
    event: TaskScheduleEventEnumAvro = TaskScheduleEventEnumAvro.CREATED,
    vararg blocks: ((TaskScheduleAggregateAvro) -> Unit)?
): TaskScheduleAggregateAvro {
  val schedule = existingSchedule.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          schedule.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(schedule, key).getAggregate()
}

fun ProjectEventListener.submitDayCardG2(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingDayCard: DayCardAggregateG2Avro? = null,
    event: DayCardEventEnumAvro = DayCardEventEnumAvro.CREATED,
    vararg blocks: ((DayCardAggregateG2Avro) -> Unit)?
): DayCardAggregateG2Avro {
  val dayCard = existingDayCard.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          dayCard.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(dayCard, key).getAggregate()
}

fun ProjectEventListener.submitTaskAttachment(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingTaskAttachment: TaskAttachmentAggregateAvro? = null,
    event: TaskAttachmentEventEnumAvro = TaskAttachmentEventEnumAvro.CREATED,
    vararg blocks: ((TaskAttachmentAggregateAvro) -> Unit)?
): TaskAttachmentAggregateAvro {
  val taskAttachment = existingTaskAttachment.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          taskAttachment.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(taskAttachment, key).getAggregate()
}

fun ProjectEventListener.submitTopicG2(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingTopic: TopicAggregateG2Avro? = null,
    event: TopicEventEnumAvro = TopicEventEnumAvro.CREATED,
    vararg blocks: ((TopicAggregateG2Avro) -> Unit)?
): TopicAggregateG2Avro {
  val topic = existingTopic.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          topic.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(topic, key).getAggregate()!!
}

fun ProjectEventListener.submitComment(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingComment: MessageAggregateAvro? = null,
    event: MessageEventEnumAvro = MessageEventEnumAvro.CREATED,
    vararg blocks: ((MessageAggregateAvro) -> Unit)?
): MessageAggregateAvro {
  val comment = existingComment.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          comment.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(comment, key).getAggregate()
}

fun ProjectEventListener.submitTopicAttachment(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingTopicAttachment: TopicAttachmentAggregateAvro? = null,
    eventName: TopicAttachmentEventEnumAvro = TopicAttachmentEventEnumAvro.CREATED,
    vararg topicAttachmentAggregateOperations: ((TopicAttachmentAggregateAvro) -> Unit)?
): TopicAttachmentAggregateAvro {
  val topicAttachment =
      existingTopicAttachment.buildEventAvro(eventName, *topicAttachmentAggregateOperations)
  val key =
      AggregateEventMessageKey(
          topicAttachment.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(topicAttachment, key).getAggregate()
}

fun ProjectEventListener.submitMessageAttachment(
    rootContextIdentifierAvro: AggregateIdentifierAvro,
    existingMessageAttachment: MessageAttachmentAggregateAvro? = null,
    eventName: MessageAttachmentEventEnumAvro = MessageAttachmentEventEnumAvro.CREATED,
    vararg messageAttachmentAggregateOperations: ((MessageAttachmentAggregateAvro) -> Unit)?
): MessageAttachmentAggregateAvro {
  val messageAttachment =
      existingMessageAttachment.buildEventAvro(eventName, *messageAttachmentAggregateOperations)
  val key =
      AggregateEventMessageKey(
          messageAttachment.getAggregate().buildAggregateIdentifier(),
          rootContextIdentifierAvro.getIdentifier().toUUID())

  return submitProjectEvent(messageAttachment, key).getAggregate()
}

fun <V : SpecificRecordBase?> ProjectEventListener.submitProjectEvent(
    value: V,
    key: EventMessageKey
): V = submitEvent(value, key, ::listenToProjectEvents)

@Suppress("unused")
fun <V : SpecificRecordBase?> ProjectEventListener.submitEvent(
    value: V,
    key: EventMessageKey,
    listener: (ConsumerRecord<EventMessageKey, SpecificRecordBase?>, Acknowledgment) -> Unit
): V {
  mockk<Acknowledgment>(relaxed = true).apply {
    listener(ConsumerRecord("", 0, 0, key, value), this)
    verify { acknowledge() }
  }
  return value
}

private fun ProjectAggregateAvro?.buildEventAvro(
    event: ProjectEventEnumAvro,
    vararg blocks: ((ProjectAggregateAvro) -> Unit)?
): ProjectEventAvro =
    (this?.let { ProjectEventAvro(event, this) } ?: randomProject(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun ProjectPictureAggregateAvro?.buildEventAvro(
    event: ProjectPictureEventEnumAvro,
    vararg blocks: ((ProjectPictureAggregateAvro) -> Unit)?
): ProjectPictureEventAvro =
    (this?.let { ProjectPictureEventAvro(event, this) }
            ?: randomProjectPicture(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun WorkAreaAggregateAvro?.buildEventAvro(
    event: WorkAreaEventEnumAvro,
    vararg blocks: ((WorkAreaAggregateAvro) -> Unit)?
): WorkAreaEventAvro =
    (this?.let { WorkAreaEventAvro(event, this) } ?: randomWorkArea(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun WorkAreaListAggregateAvro?.buildEventAvro(
    event: WorkAreaListEventEnumAvro,
    vararg blocks: ((WorkAreaListAggregateAvro) -> Unit)?
): WorkAreaListEventAvro =
    (this?.let { WorkAreaListEventAvro(event, this) } ?: randomWorkAreaList(null, event).build())
        .apply { for (block in blocks) block?.invoke(getAggregate()) }

private fun ProjectCraftAggregateG2Avro?.buildEventAvro(
    event: ProjectCraftEventEnumAvro,
    vararg blocks: ((ProjectCraftAggregateG2Avro) -> Unit)?
): ProjectCraftEventG2Avro =
    (this?.let { ProjectCraftEventG2Avro(event, this) }
            ?: randomProjectCraftG2(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun MilestoneAggregateAvro?.buildEventAvro(
    event: MilestoneEventEnumAvro,
    vararg blocks: ((MilestoneAggregateAvro) -> Unit)?
): MilestoneEventAvro =
    (this?.let { MilestoneEventAvro(event, this) } ?: randomMilestone(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun MilestoneListAggregateAvro?.buildEventAvro(
    event: MilestoneListEventEnumAvro,
    vararg blocks: ((MilestoneListAggregateAvro) -> Unit)?
): MilestoneListEventAvro =
    (this?.let { MilestoneListEventAvro(event, this) } ?: randomMilestoneList(null, event).build())
        .apply { for (block in blocks) block?.invoke(getAggregate()) }

private fun ParticipantAggregateG3Avro?.buildEventAvro(
    event: ParticipantEventEnumAvro,
    vararg blocks: ((ParticipantAggregateG3Avro) -> Unit)?
): ParticipantEventG3Avro =
    (this?.let { ParticipantEventG3Avro(event, this) } ?: randomParticipantG3(null, event).build())
        .apply { for (block in blocks) block?.invoke(getAggregate()) }

private fun TaskAggregateAvro?.buildEventAvro(
    event: TaskEventEnumAvro,
    vararg blocks: ((TaskAggregateAvro) -> Unit)?
): TaskEventAvro =
    (this?.let { TaskEventAvro(event, this) } ?: randomTask(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun TaskActionSelectionAggregateAvro?.buildEventAvro(
    event: TaskActionSelectionEventEnumAvro,
    vararg blocks: ((TaskActionSelectionAggregateAvro) -> Unit)?
): TaskActionSelectionEventAvro =
    (this?.let { TaskActionSelectionEventAvro(event, this) }
            ?: randomTaskAction(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun TaskScheduleAggregateAvro?.buildEventAvro(
    event: TaskScheduleEventEnumAvro,
    vararg blocks: ((TaskScheduleAggregateAvro) -> Unit)?
): TaskScheduleEventAvro =
    (this?.let { TaskScheduleEventAvro(event, this) } ?: randomSchedule(null, event).build())
        .apply { for (block in blocks) block?.invoke(getAggregate()) }

private fun DayCardAggregateG2Avro?.buildEventAvro(
    event: DayCardEventEnumAvro,
    vararg blocks: ((DayCardAggregateG2Avro) -> Unit)?
): DayCardEventG2Avro =
    (this?.let { DayCardEventG2Avro(event, this) } ?: randomDayCardG2(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun TaskAttachmentAggregateAvro?.buildEventAvro(
    event: TaskAttachmentEventEnumAvro,
    vararg blocks: ((TaskAttachmentAggregateAvro) -> Unit)?
): TaskAttachmentEventAvro =
    (this?.let { TaskAttachmentEventAvro(event, this) }
            ?: randomTaskAttachment(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun TopicAggregateG2Avro?.buildEventAvro(
    event: TopicEventEnumAvro,
    vararg blocks: ((TopicAggregateG2Avro) -> Unit)?
): TopicEventG2Avro =
    (this?.let { TopicEventG2Avro(event, this) } ?: randomTopicG2(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun MessageAggregateAvro?.buildEventAvro(
    event: MessageEventEnumAvro,
    vararg blocks: ((MessageAggregateAvro) -> Unit)?
): MessageEventAvro =
    (this?.let { MessageEventAvro(event, this) } ?: randomComment(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun TopicAttachmentAggregateAvro?.buildEventAvro(
    event: TopicAttachmentEventEnumAvro,
    vararg blocks: ((TopicAttachmentAggregateAvro) -> Unit)?
): TopicAttachmentEventAvro =
    (this?.let { TopicAttachmentEventAvro(event, this) }
            ?: randomTopicAttachment(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }

private fun MessageAttachmentAggregateAvro?.buildEventAvro(
    event: MessageAttachmentEventEnumAvro,
    vararg blocks: ((MessageAttachmentAggregateAvro) -> Unit)?
): MessageAttachmentEventAvro =
    (this?.let { MessageAttachmentEventAvro(event, this) }
            ?: randomMessageAttachment(null, event).build()).apply {
      for (block in blocks) block?.invoke(getAggregate())
    }
