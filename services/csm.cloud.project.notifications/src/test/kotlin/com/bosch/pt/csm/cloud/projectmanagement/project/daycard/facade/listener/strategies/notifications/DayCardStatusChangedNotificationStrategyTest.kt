/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCr
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCsm
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@DisplayName("Notifications must be created on day card ")
@SmartSiteSpringBootTest
class DayCardStatusChangedNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createDayCard() {
    eventStreamGenerator
        .submitTaskAsFm()
        .submitTaskSchedule(auditUserReference = FM_USER)
        .submitDayCardG2()
        .submitTaskSchedule(
            auditUserReference = CSM_USER, eventType = TaskScheduleEventEnumAvro.UPDATED) {
              it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
            }
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `cancelled event done by the CSM for the CR and FM`() {
    eventStreamGenerator.submitDayCardG2(eventType = DayCardEventEnumAvro.CANCELLED) {
      it.reason = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
    }

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    val notificationForFm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == fmUser.identifier }

    // Check notification for CR
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)

    // Check notification for FM (being the assignee)
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForFm,
        requestUser = fmUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)
  }

  @Test
  fun `cancelled event done by the FM for the CSM and CR`() {
    eventStreamGenerator.submitDayCardG2(
        eventType = DayCardEventEnumAvro.CANCELLED, auditUserReference = FM_USER) {
          it.reason = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
        }

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    // Check notification for CSM
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)

    // Check notification for CR
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)
  }

  @Test
  fun `cancelled event with custom reason with text done by the FM for the CSM and CR`() {
    eventStreamGenerator
        .submitRfvCustomization {
          it.name = "Custom rfv 1"
          it.key = DayCardReasonNotDoneEnumAvro.CUSTOM1
        }
        .submitDayCardG2(eventType = DayCardEventEnumAvro.CANCELLED, auditUserReference = FM_USER) {
          it.reason = DayCardReasonNotDoneEnumAvro.CUSTOM1
        }

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    // Check notification for CSM
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)

    // Check notification for CR
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)
  }

  @Test
  fun `cancelled event with custom reason without text done by the FM for the CSM and CR`() {
    eventStreamGenerator
        .submitRfvCustomization {
          it.key = DayCardReasonNotDoneEnumAvro.CUSTOM2
          it.active = false
        }
        .submitDayCardG2(eventType = DayCardEventEnumAvro.CANCELLED, auditUserReference = FM_USER) {
          it.reason = DayCardReasonNotDoneEnumAvro.CUSTOM2
        }

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    // Check notification for CSM
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)

    // Check notification for CR
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)
  }

  @Test
  fun `approved event done by CSM for the CR and FM`() {
    eventStreamGenerator.submitDayCardG2(
        eventType = DayCardEventEnumAvro.APPROVED, auditUserReference = CSM_USER) {
          it.status = DayCardStatusEnumAvro.APPROVED
          it.reason = null
        }

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    val notificationForFm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == fmUser.identifier }

    // Check notification for CR
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)

    // Check notification for FM
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForFm,
        requestUser = fmUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)
  }

  @Test
  fun `completed event by assigned FM for the CSM and CR`() {
    eventStreamGenerator.submitDayCardG2(
        eventType = DayCardEventEnumAvro.COMPLETED, auditUserReference = FM_USER) {
          it.status = DayCardStatusEnumAvro.DONE
          it.reason = null
        }

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    // Check notification for CSM
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)

    // Check notification for CR
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)
  }

  @Test
  fun `reset event by CSM for the assigned FM and CR`() {
    eventStreamGenerator.submitDayCardG2(
        eventType = DayCardEventEnumAvro.RESET, auditUserReference = CSM_USER) {
          it.status = DayCardStatusEnumAvro.OPEN
          it.reason = null
        }

    // Check that two notification were created
    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForFm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == fmUser.identifier }

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    // Check notification for CR
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)

    // Check notification for FM
    checkNotificationForDayCardStatusChangedEventG2(
        notification = notificationForFm,
        requestUser = fmUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)
  }

  @Test
  fun `except task is assigned to the CSM itself`() {
    eventStreamGenerator
        .submitTaskAsCsm()
        .submitTaskSchedule()
        .submitDayCardG2()
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
        }
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitDayCardG2(
        eventType = DayCardEventEnumAvro.APPROVED, auditUserReference = CSM_USER) {
          it.status = DayCardStatusEnumAvro.APPROVED
          it.reason = null
        }

    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `except task is assigned to a deactivated participant also being a CR`() {
    eventStreamGenerator
        .submitTaskAsCr()
        .submitTaskSchedule()
        .submitDayCardG2(auditUserReference = CSM_USER)
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now()))
        }
        .submitParticipantG3(
            asReference = CR_PARTICIPANT, eventType = ParticipantEventEnumAvro.DEACTIVATED)

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitDayCardG2(
        eventType = DayCardEventEnumAvro.APPROVED, auditUserReference = CSM_USER) {
          it.status = DayCardStatusEnumAvro.APPROVED
          it.reason = null
        }

    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }
}
