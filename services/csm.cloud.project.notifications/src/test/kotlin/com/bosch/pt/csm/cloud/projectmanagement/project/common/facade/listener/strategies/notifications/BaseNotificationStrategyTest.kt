/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_COMMENT_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_DAY_CARD_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_DAY_CARD_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_ASSIGNED_TO_YOU
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TOPIC_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.event.model.LocalizableMessage
import com.bosch.pt.csm.cloud.projectmanagement.event.model.LocalizableText
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.notification.NotificationVerificationComponent
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.ObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.PlaceholderValueDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.MobileDetails
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.test.NotificationSummaryGenerator.buildSummary
import com.bosch.pt.csm.cloud.projectmanagement.test.RestUtils.requestBuilder
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.fasterxml.jackson.databind.ObjectMapper
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

open class BaseNotificationStrategyTest : BaseNotificationTest() {

  val crUserAggregate by lazy { context[CR_USER] as UserAggregateAvro }
  val csmUserAggregate by lazy { context[CSM_USER] as UserAggregateAvro }
  val fmUserAggregate by lazy { context[FM_USER] as UserAggregateAvro }
  val otherFmUserAggregate by lazy { context[OTHER_FM_USER] as UserAggregateAvro }
  val otherCompanyFmUserAggregate by lazy { context[OTHER_COMPANY_FM_USER] as UserAggregateAvro }

  val csmParticipantAggregate by lazy { context[CSM_PARTICIPANT] as ParticipantAggregateG3Avro }
  val crParticipantAggregate by lazy { context[CR_PARTICIPANT] as ParticipantAggregateG3Avro }
  val fmParticipantAggregate by lazy { context[FM_PARTICIPANT] as ParticipantAggregateG3Avro }
  val otherFmParticipantAggregate by lazy {
    context[OTHER_FM_PARTICIPANT] as ParticipantAggregateG3Avro
  }
  val otherCompanyCrParticipantAggregate by lazy {
    context[OTHER_COMPANY_CR_PARTICIPANT] as ParticipantAggregateG3Avro
  }
  val otherCompanyFmParticipantAggregate by lazy {
    context[OTHER_COMPANY_FM_PARTICIPANT] as ParticipantAggregateG3Avro
  }

  val projectCraft1 by lazy { context[PROJECT_CRAFT_1] as ProjectCraftAggregateG2Avro }
  val projectCraft2 by lazy { context[PROJECT_CRAFT_2] as ProjectCraftAggregateG2Avro }

  val workArea1 by lazy { context[WORK_AREA_1] as WorkAreaAggregateAvro }
  val workArea2 by lazy { context[WORK_AREA_2] as WorkAreaAggregateAvro }

  val projectAggregate by lazy { context[PROJECT] as ProjectAggregateAvro }

  val taskAggregate by lazy { context["task"] as TaskAggregateAvro }

  val dayCardAggregateG2 by lazy { context["dayCard"] as DayCardAggregateG2Avro }

  val topicAggregateG2 by lazy { context["topic"] as TopicAggregateG2Avro }

  val messageAggregate by lazy { context["message"] as MessageAggregateAvro }

  val taskScheduleStartDateInMillis =
      Instant.now().plusMillis(1000 * 60 * 60 * 24 * 5).toEpochMilli()
  val taskScheduleEndDateInMillis =
      Instant.now().plusMillis(1000 * 60 * 60 * 24 * 10).toEpochMilli()

  @Autowired lateinit var objectMapper: ObjectMapper

  @Autowired lateinit var notificationVerificationComponent: NotificationVerificationComponent

  @Autowired lateinit var webApplicationContext: WebApplicationContext

  @Autowired lateinit var messageSource: MessageSource

  @Autowired lateinit var apiVersionProperties: ApiVersionProperties

  lateinit var mockMvc: MockMvc

  private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  @BeforeEach
  override fun setup() {
    super.setup()
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitParticipantG3(asReference = CR_PARTICIPANT_INACTIVE) {
          it.user = getByReference(CR_USER_INACTIVE)
          it.role = CR
        }
        .submitParticipantG3(
            asReference = CR_PARTICIPANT_INACTIVE, eventType = ParticipantEventEnumAvro.DEACTIVATED)
        .submitCrParticipant()
        .submitParticipantG3(asReference = FM_PARTICIPANT_INACTIVE) {
          it.user = getByReference(FM_USER_INACTIVE)
          it.role = FM
        }
        .submitParticipantG3(
            asReference = FM_PARTICIPANT_INACTIVE, eventType = ParticipantEventEnumAvro.DEACTIVATED)
        .submitFmParticipant()
        .submitParticipantG3(asReference = OTHER_FM_PARTICIPANT) {
          it.user = getByReference(OTHER_FM_USER)
          it.role = FM
        }
        .setLastIdentifierForType(
            CompanymanagementAggregateTypeEnum.COMPANY.value, getByReference(COMPANY_2))
        .submitParticipantG3(asReference = OTHER_COMPANY_CR_PARTICIPANT) {
          it.user = getByReference(OTHER_COMPANY_CR_USER)
          it.role = CR
        }
        .submitParticipantG3(asReference = OTHER_COMPANY_FM_PARTICIPANT) {
          it.user = getByReference(OTHER_COMPANY_FM_USER)
          it.role = FM
        }
        .submitParticipantG3(asReference = "compacted-user-participant") {
          it.user = compactedUserIdentifier
          it.role = FM
        }
        .submitParticipantG3(
            asReference = "compacted-user-participant",
            eventType = ParticipantEventEnumAvro.DEACTIVATED)
        .submitWorkArea(asReference = WORK_AREA_1)
        .submitWorkArea(asReference = WORK_AREA_1, eventType = WorkAreaEventEnumAvro.UPDATED)
        .submitWorkArea(asReference = WORK_AREA_2)
        .submitWorkArea(asReference = WORK_AREA_2, eventType = WorkAreaEventEnumAvro.UPDATED)
        .submitWorkAreaList {
          it.project = getByReference(PROJECT)
          it.workAreas = listOf(getByReference(WORK_AREA_1), getByReference(WORK_AREA_2))
        }
        .submitProjectCraftG2(asReference = PROJECT_CRAFT_1)
        .submitProjectCraftG2(
            asReference = PROJECT_CRAFT_1, eventType = ProjectCraftEventEnumAvro.UPDATED)
        .submitProjectCraftG2(asReference = PROJECT_CRAFT_2)
        .submitProjectCraftG2(
            asReference = PROJECT_CRAFT_2, eventType = ProjectCraftEventEnumAvro.UPDATED)
  }

  companion object {
    const val PROJECT_CRAFT_1: String = "projectCraft1"
    const val PROJECT_CRAFT_2: String = "projectCraft2"
    const val WORK_AREA_1: String = "workArea1"
    const val WORK_AREA_2: String = "workArea2"
  }

  fun checkNotifications(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      project: ProjectAggregateAvro,
      task: TaskAggregateAvro,
      objectReference: ObjectReference,
      summary: NotificationSummaryDto,
      details: String?,
      expectedNumberOfResults: Int = 1,
      indexOfNotificationToVerify: Int = 0
  ) {
    // Get notification and verify result
    doWithAuthorization(requestUser) {
      val resultActions =
          mockMvc
              .perform(
                  requestBuilder(
                      get(
                          latestVersionOf("/projects/notifications?before={before}&limit={limit}"),
                          df.format(Date(Instant.now().plusMillis(1).toEpochMilli())),
                          expectedNumberOfResults + 1),
                      objectMapper))
              .andExpect(status().isOk)
              .andExpect(jsonPath("$.items.length()").value(expectedNumberOfResults))

      notificationVerificationComponent.verifyId(
          resultActions, notification, indexOfNotificationToVerify)
      notificationVerificationComponent.verifyInsertDate(
          resultActions, notification, indexOfNotificationToVerify)
      notificationVerificationComponent.verifyRead(
          resultActions, false, indexOfNotificationToVerify)
      notificationVerificationComponent.verifyActor(
          resultActions,
          "${actorUser.getFirstName()} ${actorUser.getLastName()}",
          actorParticipant.getAggregateIdentifier().getIdentifier(),
          ".*/default-profile-picture.png",
          indexOfNotificationToVerify)
      notificationVerificationComponent.verifyDetails(
          resultActions, details, indexOfNotificationToVerify)
      notificationVerificationComponent.verifyContextProject(
          resultActions,
          project.getAggregateIdentifier().getIdentifier(),
          project.getTitle(),
          indexOfNotificationToVerify)
      notificationVerificationComponent.verifyContextTask(
          resultActions,
          task.getAggregateIdentifier().getIdentifier(),
          task.getName(),
          indexOfNotificationToVerify)
      notificationVerificationComponent.verifyObject(
          resultActions, objectReference, indexOfNotificationToVerify)
      notificationVerificationComponent.verifySummary(
          resultActions, summary, indexOfNotificationToVerify)
      notificationVerificationComponent.verifyLink(
          resultActions,
          "\$.items[0]._links.read.href",
          ".*" + latestVersionOf("/projects/notifications/[0-9a-z-]+/read"))
    }
  }

  fun checkNotificationForTaskUpdatedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      details: String,
      taskAggregate: TaskAggregateAvro
  ) {
    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = taskAggregate.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_TASK_UPDATED, actorParticipant, actorUser),
        details = details)
  }

  fun checkNotificationForTaskAssignedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      assigneeUser: UserAggregateAvro? = null,
      assigneeParticipant: ParticipantAggregateG3Avro? = null
  ) {
    val actorParticipantIdentifier = actorParticipant.getIdentifier()
    val actorDisplayName = "${actorUser.getFirstName()} ${actorUser.getLastName()}"

    if (assigneeUser == null && assigneeParticipant == null) {
          NotificationSummaryDto(
              NOTIFICATION_SUMMARY_TASK_ASSIGNED_TO_YOU,
              mapOf(
                  "originator" to
                      PlaceholderValueDto(
                          "PARTICIPANT", actorParticipantIdentifier, actorDisplayName)))
        } else {
          val assigneeParticipantIdentifier = assigneeParticipant!!.getIdentifier()
          val assigneeDisplayName = "${assigneeUser!!.getFirstName()} ${assigneeUser.getLastName()}"

          NotificationSummaryDto(
              NOTIFICATION_SUMMARY_TASK_ASSIGNED,
              mapOf(
                  "originator" to
                      PlaceholderValueDto(
                          "PARTICIPANT", actorParticipantIdentifier, actorDisplayName),
                  "assignee" to
                      PlaceholderValueDto(
                          "PARTICIPANT", assigneeParticipantIdentifier, assigneeDisplayName)))
        }
        .also {
          checkNotifications(
              notification = notification,
              requestUser = requestUser,
              actorUser = actorUser,
              actorParticipant = actorParticipant,
              project = projectAggregate,
              task = taskAggregate,
              objectReference = taskAggregate.buildObjectReference(),
              summary = it,
              details = null)
        }
  }

  fun checkNotificationForScheduleUpdatedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      details: String,
      taskAggregate: TaskAggregateAvro,
      scheduleAggregate: TaskScheduleAggregateAvro
  ) {
    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = scheduleAggregate.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_TASK_UPDATED, actorParticipant, actorUser),
        details = details)
  }

  fun checkNotificationForTaskAttachmentCreatedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      taskAttachmentAggregate: TaskAttachmentAggregateAvro,
      details: String,
      expectedNumberOfResults: Int = 1,
      indexOfNotificationToVerify: Int = 0
  ) =
      checkNotifications(
          notification = notification,
          requestUser = requestUser,
          actorUser = actorUser,
          actorParticipant = actorParticipant,
          project = projectAggregate,
          task = taskAggregate,
          objectReference = taskAttachmentAggregate.buildObjectReference(),
          summary = buildSummary(NOTIFICATION_SUMMARY_TASK_UPDATED, actorParticipant, actorUser),
          details = details,
          expectedNumberOfResults = expectedNumberOfResults,
          indexOfNotificationToVerify = indexOfNotificationToVerify)

  fun checkNotificationForDayCardCreatedEventG2(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro
  ) {
    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = dayCardAggregateG2.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_DAY_CARD_CREATED, actorParticipant, actorUser),
        details = dayCardAggregateG2.getTitle().let { "\"${dayCardAggregateG2.getTitle()}\"" })
  }

  fun checkNotificationForDayCardStatusChangedEventG2(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro
  ) {
    val status = translate("DayCardStatusEnum_${dayCardAggregateG2.getStatus()}")

    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = dayCardAggregateG2.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_DAY_CARD_UPDATED, actorParticipant, actorUser),
        details =
            if (dayCardAggregateG2.getStatus() == DayCardStatusEnumAvro.NOTDONE) {
              val reason =
                  translate(projectAggregate.getIdentifier(), dayCardAggregateG2.getReason())
              "Status \"$status: $reason\""
            } else "Status \"$status\"")
  }

  fun checkNotificationForDayCardUpdateEventG2(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      details: String
  ) {
    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = dayCardAggregateG2.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_DAY_CARD_UPDATED, actorParticipant, actorUser),
        details = details)
  }

  fun checkNotificationForTopicCreatedEventG2(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro
  ) {
    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = topicAggregateG2.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_TOPIC_CREATED, actorParticipant, actorUser),
        details =
            topicAggregateG2.getDescription()?.let { "\"${topicAggregateG2.getDescription()}\"" })
  }

  fun checkNotificationForCommentCreatedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro
  ) {
    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = messageAggregate.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_COMMENT_CREATED, actorParticipant, actorUser),
        details = messageAggregate.getContent()?.let { "\"${messageAggregate.getContent()}\"" })
  }

  fun translate(messageKey: String): String =
      messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale())

  private fun translate(projectIdentifier: UUID, reason: DayCardReasonNotDoneEnumAvro): String {
    val rfv =
        repositories.rfvCustomizationRepository.findLatestCachedByProjectIdentifierAndReason(
            projectIdentifier, DayCardReasonEnum.valueOf(reason.name))
    return if (rfv?.name == null) {
      translate("DayCardReasonEnum_${reason.name}")
    } else {
      rfv.name!!
    }
  }

  fun pushNotificationForTaskAssignedBy(originatingUser: UUID) =
      mobileDetailsFor(
          "TASK_ASSIGNED",
          bodyArgs = listOf(taskAggregate.name),
          metadata =
              mapOf(
                  "projectId" to projectAggregate.aggregateIdentifier.identifier,
                  "taskId" to taskAggregate.aggregateIdentifier.identifier),
          originatingUser = originatingUser,
          minVersion = "1.6.0")

  fun mobileDetailsFor(
      type: String,
      bodyArgs: List<String>,
      metadata: Map<String, String>,
      originatingUser: UUID,
      minVersion: String = "1.5.0"
  ) =
      MobileDetails(
          LocalizableMessage(
              title = LocalizableText("PN_TITLE_$type"),
              body = LocalizableText("PN_BODY_$type", bodyArgs)),
          data = metadata + ("type" to type),
          originatingUser,
          minVersion)

  fun latestVersionOf(path: String) = "/v${apiVersionProperties.version.max}$path"
}
