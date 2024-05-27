/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_WORK_AREA
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import java.util.Locale
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringSubstitutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * We only test one assignment scenario per status change since the recipient determination is
 * always the same and already tested with other types of notifications.
 */
@DisplayName("Notifications must be created")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskUpdatedNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator
        .setLastIdentifierForType(
            ProjectmanagementAggregateTypeEnum.WORKAREA.value, getByReference(WORK_AREA_1))
        .setLastIdentifierForType(
            ProjectmanagementAggregateTypeEnum.PROJECTCRAFT.value, getByReference(PROJECT_CRAFT_1))
        .submitTaskAsFm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `when a single attribute of a task is updated`() {
    val name = "new name"
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UPDATED) { it.name = name }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(Key.TASK_ATTRIBUTE_NAME) + " \"" + name + "\"").replaceFirstChar {
                  if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                },
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(Key.TASK_ATTRIBUTE_NAME) + " \"" + name + "\"").replaceFirstChar {
                  if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                },
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `when only attribute work area of a task is updated`() {
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UPDATED) {
      it.workarea = getByReference(WORK_AREA_2)
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(TASK_ATTRIBUTE_WORK_AREA) + " \"" + workArea2.getName() + "\"")
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(TASK_ATTRIBUTE_WORK_AREA) + " \"" + workArea2.getName() + "\"")
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `when only attribute project craft of a task is updated`() {
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UPDATED) {
      it.craft = getByReference(PROJECT_CRAFT_2)
    }

    val taskAggregate = context["task"] as TaskAggregateAvro

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(TASK_ATTRIBUTE_CRAFT) + " \"" + projectCraft2.getName() + "\"")
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(TASK_ATTRIBUTE_CRAFT) + " \"" + projectCraft2.getName() + "\"")
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `when a single attribute of a task unassigned is updated`() {
    val name = "new name"
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UNASSIGNED) {
      it.assignee = null
    }
    repositories.notificationRepository.deleteAll()
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UPDATED) { it.name = name }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(1)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(Key.TASK_ATTRIBUTE_NAME) + " \"" + name + "\"").replaceFirstChar {
                  if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                },
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `when a single optional attribute of a task is removed`() {
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UPDATED) { it.location = null }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          StringUtils.capitalize(
              StringSubstitutor(
                      mapOf(Pair("attribute", translate(TASK_ATTRIBUTE_LOCATION))), "\${", "}")
                  .replace(translate(Key.NOTIFICATION_DETAILS_ATTRIBUTE_REMOVED)))

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `when attributes of a task handled by the strategy are updated`() {
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UPDATED) {
      it.name = "new name"
      it.description = "new description"
      it.location = "new location"
      it.workarea = getByReference(WORK_AREA_2)
      it.craft = getByReference(PROJECT_CRAFT_2)
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(Key.TASK_ATTRIBUTE_NAME) +
                        ", " +
                        translate(TASK_ATTRIBUTE_CRAFT) +
                        ", " +
                        translate(TASK_ATTRIBUTE_WORK_AREA) +
                        ", " +
                        translate(TASK_ATTRIBUTE_LOCATION) +
                        " " +
                        translate(MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR) +
                        " " +
                        translate(TASK_ATTRIBUTE_DESCRIPTION))
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(Key.TASK_ATTRIBUTE_NAME) +
                        ", " +
                        translate(TASK_ATTRIBUTE_CRAFT) +
                        ", " +
                        translate(TASK_ATTRIBUTE_WORK_AREA) +
                        ", " +
                        translate(TASK_ATTRIBUTE_LOCATION) +
                        " " +
                        translate(MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR) +
                        " " +
                        translate(TASK_ATTRIBUTE_DESCRIPTION))
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `when some attributes of a task handled by the strategy are updated`() {
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.UPDATED) {
      it.name = "new name"
      it.description = "new description"
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(Key.TASK_ATTRIBUTE_NAME) +
                        " " +
                        translate(MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR) +
                        " " +
                        translate(TASK_ATTRIBUTE_DESCRIPTION))
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details =
                (translate(Key.TASK_ATTRIBUTE_NAME) +
                        " " +
                        translate(MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR) +
                        " " +
                        translate(TASK_ATTRIBUTE_DESCRIPTION))
                    .replaceFirstChar {
                      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
            taskAggregate = taskAggregate)
      }
    }
  }
}
