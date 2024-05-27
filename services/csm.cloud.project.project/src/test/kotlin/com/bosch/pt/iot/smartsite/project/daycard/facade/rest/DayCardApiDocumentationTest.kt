/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedIdentifier
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedUpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKSCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getTaskIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.APPROVE_DAYCARDS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.APPROVE_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.CANCEL_DAYCARD_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.CANCEL_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.COMPLETE_DAYCARDS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.COMPLETE_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.DAYCARDS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.DAYCARD_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.PATH_VARIABLE_DAY_CARD_ID
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.RESET_DAYCARDS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.RESET_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.CancelDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.CancelMultipleDayCardsResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.SaveDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.UpdateDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_APPROVE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_CANCEL_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_COMPLETE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_DELETE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_RESET_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_UPDATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.BAD_WEATHER
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.CONCESSION_NOT_RECOGNIZED
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.AbstractTaskScheduleApiDocumentationTest.Companion.DESCRIPTION_LINK_UPDATE_TASKSCHEDULE
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_UPDATE_TASKSCHEDULE
import java.math.BigDecimal.valueOf
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.ETAG
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class DayCardApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val ifMatchHeaderScheduleSnippet =
      requestHeaders(
          headerWithName(IF_MATCH).description("If-Match header with ETag of task schedule"))

  private val ifMatchHeaderDayCardSnippet =
      requestHeaders(headerWithName(IF_MATCH).description("If-Match header with ETag"))

  private val saveDayCardConstrainedFields = ConstrainedFields(SaveDayCardResource::class.java)

  private val updateDayCardConstrainedFields = ConstrainedFields(UpdateDayCardResource::class.java)

  private val cancelDayCardResourceConstrainedFields =
      ConstrainedFields(CancelDayCardResource::class.java)

  private val removeDayCardsConstrainedFields = ConstrainedFields(BatchRequestResource::class.java)

  private val saveDayCardRequestFieldsSnippet =
      requestFields(
          saveDayCardConstrainedFields
              .withPath("title")
              .description("The title of the day card")
              .type(STRING),
          saveDayCardConstrainedFields
              .withPath("manpower")
              .description("The man power of the day card")
              .type(NUMBER),
          saveDayCardConstrainedFields
              .withPath("date")
              .description("The date of the day card")
              .type("Date"),
          saveDayCardConstrainedFields
              .withPath("notes")
              .description("Notes of the day card")
              .type(STRING))
  private val updateDayCardRequestFieldsSnippet =
      requestFields(
          updateDayCardConstrainedFields
              .withPath("title")
              .description("The title of the day card")
              .type(STRING),
          updateDayCardConstrainedFields
              .withPath("manpower")
              .description("The man power of the day card")
              .type(NUMBER),
          updateDayCardConstrainedFields
              .withPath("notes")
              .description("Notes of the day card")
              .type(STRING))
  private val saveDayCardReasonResourceRequestFieldsSnippet =
      requestFields(
          cancelDayCardResourceConstrainedFields
              .withPath("reason")
              .description(REASON_DESCRIPTION)
              .type(STRING))

  private val saveDayCardResponseFields =
      responseFields(
          fieldWithPath("version").description("Version of the task schedule"),
          fieldWithPath("id").description("ID of the task schedule"),
          fieldWithPath("start").description("The start date of the task"),
          fieldWithPath("end").description("The end date of the task"),
          fieldWithPath("slots[]").description("The slots of the task schedule"),
          fieldWithPath("slots[].dayCard.displayName")
              .description("Title of the day card of the slot")
              .type(STRING)
              .optional(),
          fieldWithPath("slots[].dayCard.id")
              .description("ID of the day card of the slot")
              .type(STRING)
              .optional(),
          fieldWithPath("slots[].date")
              .description("The date of the day card of the slot")
              .type(STRING)
              .optional(),
          fieldWithPath("task.displayName")
              .description("The display name of the task the schedule belongs to"),
          fieldWithPath("task.id").description("ID of the task the schedule belongs to"),
          fieldWithPath("createdBy.displayName")
              .description("Name of the creator of the task schedule"),
          fieldWithPath("createdBy.id").description("ID of the creator of the task schedule"),
          fieldWithPath("createdDate").description("Date of the task schedule creation"),
          fieldWithPath("lastModifiedBy.displayName")
              .description("Name of the user of last modification"),
          fieldWithPath("lastModifiedBy.id").description("ID of the user of last modification"),
          fieldWithPath("lastModifiedDate").description("Date of the last modification"),
          subsectionWithPath("_links").ignored(),
          subsectionWithPath("_embedded.dayCards")
              .optional()
              .description("Embedded day cards")
              .type(OBJECT))

  private val dayCardResponseFieldDescriptions =
      listOf(
          *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
          fieldWithPath("title").description("The title of the day card"),
          fieldWithPath("manpower").description("The manpower of the day card"),
          fieldWithPath("notes").description("The notes of the day card").type(STRING).optional(),
          fieldWithPath("status").description("The status of the day card"),
          fieldWithPath("reason").description(REASON_DESCRIPTION).type(OBJECT).optional(),
          fieldWithPath("reason.key").description(REASON_KEY_DESCRIPTION).type(STRING).optional(),
          fieldWithPath("reason.name").description(REASON_NAME_DESCRIPTION).type(STRING).optional(),
          fieldWithPath("task.displayName")
              .description("The display name of the task the day card belongs to"),
          fieldWithPath("task.id").description("ID of the task the day card belongs to"),
          subsectionWithPath("_links").ignored())

  private val dayCardResponseFields = responseFields(dayCardResponseFieldDescriptions)
  private val dayCardListResponseFields =
      responseFields()
          .andWithPrefix("items[].", dayCardResponseFieldDescriptions)
          .and(subsectionWithPath("_links").ignored().optional())

  private val taskIdentifier by lazy { getIdentifier("task1") }
  private val dayCard1task1 by lazy { getIdentifier("dayCard1task1").asDayCardId() }
  private val dayCard2task1 by lazy { getIdentifier("dayCard2task1").asDayCardId() }
  private val dayCard3task1 by lazy { getIdentifier("dayCard3task1").asDayCardId() }
  private val dayCard1task2 by lazy { getIdentifier("dayCard1task2").asDayCardId() }
  private val dayCard2task2 by lazy { getIdentifier("dayCard2task2").asDayCardId() }
  private val userTest by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val startDate = now()

  private val dayCardDoneBatchResourceRequest by lazy {
    VersionedUpdateBatchRequestResource(
        setOf(
            VersionedIdentifier(dayCard2task1.toUuid(), 0),
            VersionedIdentifier(dayCard3task1.toUuid(), 0)))
  }
  private val dayCardOpenBatchResourceRequest by lazy {
    VersionedUpdateBatchRequestResource(
        setOf(
            VersionedIdentifier(dayCard1task2.toUuid(), 0),
            VersionedIdentifier(dayCard2task2.toUuid(), 0)))
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task1") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "taskSchedule1") {
          it.start = startDate.toEpochMilli()
          it.end = startDate.plusDays(4).toEpochMilli()
        }
        .submitDayCardG2(asReference = "dayCard1task1") { it.status = OPEN }
        .submitDayCardG2(asReference = "dayCard2task1") { it.status = DONE }
        .submitDayCardG2(asReference = "dayCard3task1") { it.status = DONE }
        .submitTask(asReference = "task2") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "taskSchedule2")
        .submitDayCardG2(asReference = "dayCard1task2") { it.status = OPEN }
        .submitDayCardG2(asReference = "dayCard2task2") { it.status = OPEN }

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `for adding a new day card to the task schedule`() {
    val saveDayCardResource =
        SaveDayCardResource("Day Card", valueOf(2), "Some notes...", startDate.plusDays(3))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(DAYCARD_BY_TASK_ID_ENDPOINT), taskIdentifier),
                saveDayCardResource,
                0))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG, "\"1\""),
            header()
                .string(
                    LOCATION,
                    Matchers.matchesRegex(
                        ".*${
                  latestVersionOf(
                    "/projects/tasks/$taskIdentifier/schedule/daycards/[-a-z0-9]{36}$"
                  )
                }")),
            *hasIdentifierAndVersion(version = 1),
            *isCreatedBy(userTest),
            *isLastModifiedBy(userTest),
        )
        .andDo(
            document(
                "day-card/document-add-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task the day card belongs to")),
                ifMatchHeaderScheduleSnippet,
                saveDayCardRequestFieldsSnippet,
                responseHeaders(
                    headerWithName(LOCATION).description("Location of created day card resource")),
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE)),
                saveDayCardResponseFields))

    projectEventStoreUtils.verifyContainsInSequence(
        DayCardEventG2Avro::class.java, TaskScheduleEventAvro::class.java)

    val taskSchedule =
        repositories.findTaskScheduleWithDetails(
            getIdentifier("taskSchedule1").asTaskScheduleId())!!

    projectEventStoreUtils
        .verifyContainsAndGet(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.CREATED, 1, false)
        .first()
        .aggregate
        .also { aggregate ->
          val dayCard = taskSchedule.slots!!.first().dayCard
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, ProjectmanagementAggregateTypeEnum.DAYCARD, userTest)
          assertThat(aggregate.manpower).isEqualByComparingTo(dayCard.manpower)
          assertThat(aggregate.notes).isEqualTo(dayCard.notes)
          assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
          assertThat(aggregate.title).isEqualTo(dayCard.title)
          assertThat(aggregate.getTaskIdentifier()).isEqualTo(taskIdentifier)
          if (aggregate.reason != null) {
            assertThat(aggregate.reason.name).isEqualTo(dayCard.reason!!.name)
          }
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.UPDATED, 1, false)
        .first()
        .aggregate
        .also { aggregate ->
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, taskSchedule, TASKSCHEDULE)
          assertThat(aggregate.slots.size).isEqualTo(1)
          assertThat(aggregate.slots.first().dayCard.identifier)
              .isEqualTo(taskSchedule.slots!!.first().dayCard.identifier.toString())
          assertThat(aggregate.getTaskIdentifier()).isEqualTo(taskIdentifier)
        }
  }

  @Test
  fun `for finding a single day card by its identifier`() {
    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf(DAYCARD_BY_DAYCARD_ID_ENDPOINT), dayCard1task1)))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-get-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_DAY_CARD_ID).description("ID of the day card")),
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the created day card, needed for possible updates of the day card")),
                links(
                    linkWithRel(LINK_RESET_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_RESET_DAYCARD),
                    linkWithRel(LINK_APPROVE_DAYCARD).description(DESCRIPTION_LINK_APPROVE_DAYCARD),
                    linkWithRel(LINK_CANCEL_DAYCARD).description(DESCRIPTION_LINK_CANCEL_DAYCARD),
                    linkWithRel(LINK_UPDATE_DAYCARD).description(DESCRIPTION_LINK_UPDATE_DAYCARD),
                    linkWithRel(LINK_COMPLETE_DAYCARD)
                        .description(DESCRIPTION_LINK_COMPLETE_DAYCARD),
                    linkWithRel(LINK_DELETE_DAYCARD).description(DESCRIPTION_LINK_DELETE_DAYCARD)),
                dayCardResponseFields))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `for finding multiple day cards by their identifiers`() {
    val batchRequestResource =
        BatchRequestResource(setOf(dayCard2task1.toUuid(), dayCard3task1.toUuid()))

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(DAYCARDS_ENDPOINT)), batchRequestResource, null))
        .andExpect(status().isOk)
        .andDo(
            document(
                "day-card/document-get-multiple-day-cards",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(DAYCARD, DAYCARD),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                dayCardListResponseFields))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `for updating a day card`() {
    val updateDayCardResource =
        UpdateDayCardResource("Day Card Updated", valueOf(3), "Some more notes...")

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf(DAYCARD_BY_DAYCARD_ID_ENDPOINT), dayCard1task1),
                updateDayCardResource,
                0))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-update-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_DAY_CARD_ID).description("ID of the day card")),
                ifMatchHeaderDayCardSnippet,
                updateDayCardRequestFieldsSnippet,
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the updated day card, needed for further updates of the day card")),
                links(
                    linkWithRel(LINK_UPDATE_DAYCARD).description(DESCRIPTION_LINK_UPDATE_DAYCARD),
                    linkWithRel(LINK_DELETE_DAYCARD).description(DESCRIPTION_LINK_DELETE_DAYCARD),
                    linkWithRel(LINK_CANCEL_DAYCARD).description(DESCRIPTION_LINK_CANCEL_DAYCARD),
                    linkWithRel(LINK_APPROVE_DAYCARD).description(DESCRIPTION_LINK_APPROVE_DAYCARD),
                    linkWithRel(LINK_COMPLETE_DAYCARD)
                        .description(DESCRIPTION_LINK_COMPLETE_DAYCARD)),
                dayCardResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(DayCardEventG2Avro::class.java, DayCardEventEnumAvro.UPDATED)
        .aggregate
        .also { aggregate ->
          val dayCard = repositories.dayCardRepository.findEntityByIdentifier(dayCard1task1)!!
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
          assertThat(aggregate.manpower).isEqualByComparingTo(dayCard.manpower)
          assertThat(aggregate.notes).isEqualTo(dayCard.notes)
          assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
          assertThat(aggregate.title).isEqualTo(dayCard.title)
          assertThat(aggregate.getTaskIdentifier()).isEqualTo(taskIdentifier)
          if (aggregate.reason != null) {
            assertThat(aggregate.reason.name).isEqualTo(dayCard.reason!!.name)
          }
        }
  }

  @Test
  fun `for deleting a single day card of a task schedule`() {
    val dayCard = repositories.dayCardRepository.findEntityByIdentifier(dayCard1task1)!!

    mockMvc
        .perform(
            requestBuilder(
                delete(latestVersionOf(DAYCARD_BY_DAYCARD_ID_ENDPOINT), dayCard1task1), null, 0))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-remove-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_DAY_CARD_ID)
                        .description("ID of the day card to be removed")),
                ifMatchHeaderScheduleSnippet,
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE)),
                saveDayCardResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(DayCardEventG2Avro::class.java, DayCardEventEnumAvro.DELETED)
        .aggregate
        .also { aggregate ->
          validateDeletedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD, userTest)
        }
  }

  @Test
  fun `for deleting multiple day cards of a task schedule`() {
    val batchRequestResource = BatchRequestResource(setOf(dayCard1task1.toUuid()))
    val dayCard = repositories.dayCardRepository.findEntityByIdentifier(dayCard1task1)!!

    mockMvc
        .perform(
            requestBuilder(delete(latestVersionOf(DAYCARDS_ENDPOINT)), batchRequestResource, 0))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-remove-day-cards",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(DAYCARD, DAYCARD),
                ifMatchHeaderScheduleSnippet,
                requestFields(
                    removeDayCardsConstrainedFields
                        .withPath("ids")
                        .description("IDs of the day cards to be removed")),
                links(
                    linkWithRel(LINK_CREATE_DAYCARD)
                        .optional()
                        .description(DESCRIPTION_LINK_CREATE_DAYCARD),
                    linkWithRel(LINK_UPDATE_TASKSCHEDULE)
                        .optional()
                        .description(DESCRIPTION_LINK_UPDATE_TASKSCHEDULE)),
                saveDayCardResponseFields))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        DayCardEventG2Avro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.DELETED, 1, false)
        .first()
        .aggregate
        .also { aggregate ->
          validateDeletedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD, userTest)
        }
  }

  @Test
  fun `for cancellation of a single day card`() {
    val cancelDayCardResource = CancelDayCardResource(CONCESSION_NOT_RECOGNIZED)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(CANCEL_DAYCARD_BY_DAYCARD_ID_ENDPOINT), dayCard1task1),
                cancelDayCardResource,
                0))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-cancel-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_DAY_CARD_ID).description("ID of the day card")),
                ifMatchHeaderDayCardSnippet,
                saveDayCardReasonResourceRequestFieldsSnippet,
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the updated day card, needed for further updates of the day card")),
                links(linkWithRel(LINK_RESET_DAYCARD).description(DESCRIPTION_LINK_RESET_DAYCARD)),
                dayCardResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(DayCardEventG2Avro::class.java, DayCardEventEnumAvro.CANCELLED)
        .aggregate
        .also { aggregate ->
          val dayCard = repositories.dayCardRepository.findEntityByIdentifier(dayCard1task1)!!
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
          assertThat(aggregate.reason.name).isEqualTo(dayCard.reason!!.name)
          assertThat(aggregate.reason.name).isEqualTo(cancelDayCardResource.reason.name)
          assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
          assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.NOTDONE.name)
        }
  }

  @Test
  fun `for cancellation of multiple day cards`() {
    val request = CancelMultipleDayCardsResource(dayCardDoneBatchResourceRequest.items, BAD_WEATHER)
    val cancelMultipleDayCardsResourceFieldDescriptors =
        VERSIONED_BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS.toMutableList().apply {
          add(
              ConstrainedFields(CancelMultipleDayCardsResource::class.java)
                  .withPath("reason")
                  .description(REASON_DESCRIPTION)
                  .type(STRING))
        }
    val cancelMultipleDayCardsResourceFields =
        requestFields(cancelMultipleDayCardsResourceFieldDescriptors)

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(CANCEL_DAYCARD_BATCH_ENDPOINT)), request, null))
        .andExpect(status().isOk)
        .andDo(
            document(
                "day-card/document-cancel-multiple-day-cards",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(DAYCARD, DAYCARD),
                cancelMultipleDayCardsResourceFields,
                dayCardListResponseFields))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        DayCardEventG2Avro::class.java,
        DayCardEventG2Avro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.CANCELLED, 2, false)
        .also { events ->
          for (event in events) {
            val aggregate = event.aggregate
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isIn(request.items.map { it.id.toString() })

            val dayCard =
                repositories.dayCardRepository.findEntityByIdentifier(
                    aggregate.aggregateIdentifier.identifier.asDayCardId())!!
            validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
            assertThat(aggregate.reason.name).isEqualTo(dayCard.reason!!.name)
            assertThat(aggregate.reason.name).isEqualTo(request.reason.name)
            assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
            assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.NOTDONE.name)
          }
        }
  }

  @Test
  fun `for completing a single day card`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(COMPLETE_DAYCARD_BY_DAYCARD_ID_ENDPOINT), dayCard1task2),
                null,
                0))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-complete-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_DAY_CARD_ID).description("ID of the day card")),
                ifMatchHeaderDayCardSnippet,
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the updated day card, needed for further updates of the day card")),
                links(
                    linkWithRel(LINK_CANCEL_DAYCARD).description(DESCRIPTION_LINK_CANCEL_DAYCARD),
                    linkWithRel(LINK_APPROVE_DAYCARD).description(DESCRIPTION_LINK_APPROVE_DAYCARD),
                    linkWithRel(LINK_RESET_DAYCARD).description(DESCRIPTION_LINK_RESET_DAYCARD)),
                dayCardResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(DayCardEventG2Avro::class.java, DayCardEventEnumAvro.COMPLETED)
        .aggregate
        .also { aggregate ->
          val dayCard = repositories.dayCardRepository.findEntityByIdentifier(dayCard1task2)!!
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
          assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
          assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.DONE.name)
        }
  }

  @Test
  fun `for completing multiple day cards`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(COMPLETE_DAYCARDS_BATCH_ENDPOINT)),
                dayCardOpenBatchResourceRequest,
                null))
        .andExpect(status().isOk)
        .andDo(
            document(
                "day-card/document-complete-multiple-day-cards",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(DAYCARD, DAYCARD),
                requestFields(VERSIONED_BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                dayCardListResponseFields))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        DayCardEventG2Avro::class.java,
        DayCardEventG2Avro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.COMPLETED, 2, false)
        .also { events ->
          for (event in events) {
            val aggregate = event.aggregate
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isIn(dayCardOpenBatchResourceRequest.items.map { it.id.toString() })

            val dayCard =
                repositories.dayCardRepository.findEntityByIdentifier(
                    aggregate.aggregateIdentifier.identifier.asDayCardId())!!
            validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
            assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
            assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.DONE.name)
          }
        }
  }

  @Test
  fun `for approving a single card`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(APPROVE_DAYCARD_BY_DAYCARD_ID_ENDPOINT), dayCard1task1),
                null,
                0))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-approve-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_DAY_CARD_ID).description("ID of the day card")),
                ifMatchHeaderDayCardSnippet,
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the updated day card, needed for further updates of the day card")),
                links(linkWithRel(LINK_RESET_DAYCARD).description(DESCRIPTION_LINK_RESET_DAYCARD)),
                dayCardResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(DayCardEventG2Avro::class.java, DayCardEventEnumAvro.APPROVED)
        .aggregate
        .also { aggregate ->
          val dayCard = repositories.dayCardRepository.findEntityByIdentifier(dayCard1task1)!!
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
          assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
          assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.APPROVED.name)
        }
  }

  @Test
  fun `for approving multiple day cards`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(APPROVE_DAYCARDS_BATCH_ENDPOINT)),
                dayCardOpenBatchResourceRequest,
                null))
        .andExpect(status().isOk)
        .andDo(
            document(
                "day-card/document-approve-multiple-day-cards",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(DAYCARD, DAYCARD),
                requestFields(VERSIONED_BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                dayCardListResponseFields.and(subsectionWithPath("_links").ignored().optional())))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        DayCardEventG2Avro::class.java,
        DayCardEventG2Avro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.APPROVED, 2, false)
        .also { events ->
          for (event in events) {
            val aggregate = event.aggregate
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isIn(dayCardOpenBatchResourceRequest.items.map { it.id.toString() })

            val dayCard =
                repositories.dayCardRepository.findEntityByIdentifier(
                    aggregate.aggregateIdentifier.identifier.asDayCardId())!!
            validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
            assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
            assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.APPROVED.name)
          }
        }
  }

  @Test
  fun `for resetting a single day card`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(RESET_DAYCARD_BY_DAYCARD_ID_ENDPOINT), dayCard2task1),
                null,
                0))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "day-card/document-reset-day-card",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_DAY_CARD_ID).description("ID of the day card")),
                ifMatchHeaderDayCardSnippet,
                responseHeaders(
                    headerWithName(ETAG)
                        .description(
                            "Entity tag of the updated day card, needed for further updates of the day card")),
                links(
                    linkWithRel(LINK_UPDATE_DAYCARD).description(DESCRIPTION_LINK_UPDATE_DAYCARD),
                    linkWithRel(LINK_DELETE_DAYCARD).description(DESCRIPTION_LINK_DELETE_DAYCARD),
                    linkWithRel(LINK_CANCEL_DAYCARD).description(DESCRIPTION_LINK_CANCEL_DAYCARD),
                    linkWithRel(LINK_APPROVE_DAYCARD).description(DESCRIPTION_LINK_APPROVE_DAYCARD),
                    linkWithRel(LINK_COMPLETE_DAYCARD)
                        .description(DESCRIPTION_LINK_COMPLETE_DAYCARD)),
                dayCardResponseFields))

    projectEventStoreUtils
        .verifyContainsAndGet(DayCardEventG2Avro::class.java, DayCardEventEnumAvro.RESET)
        .aggregate
        .also { aggregate ->
          val dayCard = repositories.dayCardRepository.findEntityByIdentifier(dayCard2task1)!!
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
          assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
          assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.OPEN.name)
        }
  }

  @Test
  fun `for resetting multiple day cards`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(RESET_DAYCARDS_BATCH_ENDPOINT)),
                dayCardDoneBatchResourceRequest,
                null))
        .andExpect(status().isOk)
        .andDo(
            document(
                "day-card/document-reset-multiple-day-cards",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(DAYCARD, DAYCARD),
                requestFields(VERSIONED_BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                dayCardListResponseFields))

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        DayCardEventG2Avro::class.java,
        DayCardEventG2Avro::class.java,
        BatchOperationFinishedEventAvro::class.java)

    projectEventStoreUtils
        .verifyContainsAndGet(DayCardEventG2Avro::class.java, DayCardEventEnumAvro.RESET, 2, false)
        .also { events ->
          for (event in events) {
            val aggregate = event.aggregate
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isIn(dayCardDoneBatchResourceRequest.items.map { it.id.toString() })

            val dayCard =
                repositories.dayCardRepository.findEntityByIdentifier(
                    aggregate.aggregateIdentifier.identifier.asDayCardId())!!
            validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
                aggregate, dayCard, ProjectmanagementAggregateTypeEnum.DAYCARD)
            assertThat(aggregate.status.name).isEqualTo(dayCard.status.name)
            assertThat(aggregate.status.name).isEqualTo(DayCardStatusEnum.OPEN.name)
          }
        }
  }

  companion object {

    const val DESCRIPTION_LINK_CREATE_DAYCARD =
        "Link to <<api-guide-project-context-tasks.adoc#resources-add-day-card" +
            ",add a day card to task schedule>>."

    const val DESCRIPTION_LINK_UPDATE_DAYCARD =
        "Link to <<api-guide-project-context-tasks.adoc#resources-update-day-card" +
            ",update the day card>>."

    const val DESCRIPTION_LINK_DELETE_DAYCARD =
        "Link to <<api-guide-project-context-tasks.adoc#resources-remove-day-card" +
            ",delete the day card>>."

    const val DESCRIPTION_LINK_CANCEL_DAYCARD =
        "Link to <<api-guide-project-context-tasks.adoc#resources-update-day-card-notdone" +
            ",update the day card status to not done>>."

    const val DESCRIPTION_LINK_COMPLETE_DAYCARD =
        "Link to <<api-guide-project-context-tasks.adoc#resources-update-day-card-done" +
            ",update the day card status to done>>."

    const val DESCRIPTION_LINK_APPROVE_DAYCARD =
        "Link to <<api-guide-project-context-tasks.adoc#resources-update-day-card-approve" +
            ",update the day card status to approve>>."

    const val DESCRIPTION_LINK_RESET_DAYCARD =
        "Link to <<api-guide-project-context-tasks.adoc#resources-update-day-card-reset" +
            ",reset the status of the day card>>."

    const val REASON_DESCRIPTION = "A tuple with the reason for the cancellation of the day card"

    const val REASON_KEY_DESCRIPTION = "The identifier key of the cancellation reason"

    const val REASON_NAME_DESCRIPTION = "The display value of the cancellation reason"
  }
}
