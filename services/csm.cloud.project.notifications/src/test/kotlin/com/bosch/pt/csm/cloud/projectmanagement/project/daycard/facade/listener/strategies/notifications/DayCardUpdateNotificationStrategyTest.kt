/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ATTRIBUTE_MANPOWER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ATTRIBUTE_NOTES
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ATTRIBUTE_TITLE
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import java.time.LocalDate
import java.util.Locale
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("Notifications must be created on an update of a day card ")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class DayCardUpdateNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createDayCard() {
    eventStreamGenerator
        .submitTaskAsFm()
        .submitTaskSchedule(auditUserReference = FM_USER)
        .submitDayCardG2(auditUserReference = FM_USER)
        .submitTaskSchedule(
            auditUserReference = FM_USER, eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
        }
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `changed by the CSM for the CR and FM`() {
    eventStreamGenerator.submitDayCardG2(eventType = UPDATED, auditUserReference = CSM_USER) {
      it.title = "Updated title"
      it.manpower = 2F.toBigDecimal()
      it.notes = "Updated notes"
    }

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      Assertions.assertThat(notifications).hasSize(2)

      val details =
          "${translate(DAY_CARD_ATTRIBUTE_TITLE)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, " +
              "${translate(DAY_CARD_ATTRIBUTE_MANPOWER)} " +
              "and ${translate(DAY_CARD_ATTRIBUTE_NOTES)}"

      notifications.selectFirstFor(crUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details)
      }
    }
  }

  @Test
  fun `changed by the CR for the CSM and FM`() {
    eventStreamGenerator.submitDayCardG2(eventType = UPDATED, auditUserReference = CR_USER) {
      it.title = "Updated title"
      it.manpower = 2F.toBigDecimal()
      it.notes = "Updated notes"
    }

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      Assertions.assertThat(notifications).hasSize(2)

      val details =
          "${translate(DAY_CARD_ATTRIBUTE_TITLE)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, " +
              "${translate(DAY_CARD_ATTRIBUTE_MANPOWER)} " +
              "and ${translate(DAY_CARD_ATTRIBUTE_NOTES)}"

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = csmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate,
            details = details)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate,
            details = details)
      }
    }
  }

  @Test
  fun `changed by the FM for the CSM and CR`() {
    eventStreamGenerator.submitDayCardG2(eventType = UPDATED, auditUserReference = FM_USER) {
      it.title = "Updated title"
      it.manpower = 2F.toBigDecimal()
      it.notes = "Updated notes"
    }

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      Assertions.assertThat(notifications).hasSize(2)

      val details =
          "${translate(DAY_CARD_ATTRIBUTE_TITLE)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, " +
              "${translate(DAY_CARD_ATTRIBUTE_MANPOWER)} " +
              "and ${translate(DAY_CARD_ATTRIBUTE_NOTES)}"

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = details)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = details)
      }
    }
  }

  @Test
  fun `when a single attribute of a day card is updated`() {
    eventStreamGenerator.submitDayCardG2(eventType = UPDATED, auditUserReference = CSM_USER) {
      it.title = "Updated title"
    }

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      Assertions.assertThat(notifications).hasSize(2)

      val details =
          "${translate(DAY_CARD_ATTRIBUTE_TITLE)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} " +
            "\"Updated title\""

      notifications.selectFirstFor(crUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details)
      }
    }
  }

  @Test
  fun `when two attributes of a day card are updated`() {
    eventStreamGenerator.submitDayCardG2(eventType = UPDATED, auditUserReference = CSM_USER) {
      it.title = "Updated title"
      it.manpower = 2F.toBigDecimal()
    }

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      Assertions.assertThat(notifications).hasSize(2)

      val details =
          "${translate(DAY_CARD_ATTRIBUTE_TITLE)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} " +
              "and ${translate(DAY_CARD_ATTRIBUTE_MANPOWER)}"

      notifications.selectFirstFor(crUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details)
      }
    }
  }

  @Test
  fun `except for deactivated participant`() {
    eventStreamGenerator.submitParticipantG3(
        asReference = CR_PARTICIPANT, eventType = ParticipantEventEnumAvro.DEACTIVATED)
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitDayCardG2(eventType = UPDATED, auditUserReference = CSM_USER) {
      it.title = "Updated title"
      it.manpower = 2F.toBigDecimal()
      it.notes = "Updated notes"
    }

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      Assertions.assertThat(notifications).hasSize(1)

      val details =
          "${translate(DAY_CARD_ATTRIBUTE_TITLE)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, " +
              "${translate(DAY_CARD_ATTRIBUTE_MANPOWER)} " +
              "and ${translate(DAY_CARD_ATTRIBUTE_NOTES)}"

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardUpdateEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details)
      }
    }
  }
}
