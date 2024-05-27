/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest

import com.bosch.pt.csm.cloud.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextKafkaEvent
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.request.CreateFeatureResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureCreatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDisabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureEnabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureWhitelistActivatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectAddedToWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.repository.FeatureRepository
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.request.CreateWhitelistedSubjectResource
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.DISABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.ENABLED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.WHITELIST_ACTIVATED
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeaturetogglemanagementAggregateTypeEnum.FEATURE_TOGGLE
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.addSubjectToWhitelistOfFeature
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.createFeature
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.disableFeature
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import io.mockk.unmockkStatic
import java.time.Clock
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.headers.HeaderDocumentation
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Test and Document Feature Toggle API")
class FeatureApiDocumentationTest : AbstractApiDocumentationTest() {

  @Autowired private lateinit var featureRepository: FeatureRepository

  @Autowired
  private lateinit var featureToggleEventStoreUtils: EventStoreUtils<FeaturetoggleContextKafkaEvent>

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitUserAndActivate("admin") { it.locale = "de_DE" }
        .submitUser("admin") { it.admin = true }

    setAuthentication("admin")
  }

  @AfterEach fun unmockkClock() = unmockkStatic(Clock::class)

  @Test
  fun `verify and document creating a new feature successfully`() {

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/features")),
                content = CreateFeatureResource("projectImport")))
        .andExpectAll(status().isCreated)
        .andDo(
            document(
                "features/document-create-feature",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(CREATE_FEATURE_REQUEST_FIELDS),
                responseHeaders(LOCATION_HEADER_DESCRIPTOR),
                responseFields(FEATURE_RESPONSE_FIELDS_DESCRIPTORS)))

    val feature =
        featureRepository.findByName("projectImport").also {
          assertNotNull(it)
          assertThat(it?.name).isEqualTo("projectImport")
          assertThat(it?.state).isEqualTo(WHITELIST_ACTIVATED)
          assertThat(it?.version).isEqualTo(0L)
        }

    featureToggleEventStoreUtils
        .verifyContainsAndGet(FeatureCreatedEventAvro::class.java, null, 1)
        .also { events ->
          for (i in events.indices) {
            val event = events[i]
            assertThat(event.aggregateIdentifier.identifier)
                .isEqualTo(feature?.identifier.toString())
            assertThat(event.aggregateIdentifier.version).isEqualTo(0L + i)
            assertThat(event.aggregateIdentifier.type).isEqualTo(FEATURE_TOGGLE.name)
            assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
            assertThat(event.auditingInformation.date)
                .isEqualTo(feature?.lastModifiedDate?.get()?.toEpochMilli())
          }
        }
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `verify one cannot create a feature that already exists by name`() {
    eventStreamGenerator.createFeature("projectImport") { it.featureName = "projectImport" }

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/features")),
                content = CreateFeatureResource("projectImport")))
        .andExpectAll(status().isBadRequest)
  }

  @Test
  fun `verify an empty lists of features when there are none`() {
    mockMvc
        .perform(requestBuilder(get(latestVersionOf("/features"))))
        .andExpectAll(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpectAll(jsonPath("items").isArray, jsonPath("items.length()").value(0))
  }

  @Test
  fun `verify and document features list with expected information`() {
    eventStreamGenerator
        .createFeature("projectImport") { it.featureName = "projectImport" }
        .createFeature("bimViewer") { it.featureName = "bimViewer" }
        .createFeature("happyHippos") { it.featureName = "happyHippos" }

    mockMvc
        .perform(requestBuilder(get(latestVersionOf("/features"))))
        .andExpectAll(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("items").isArray,
            jsonPath("items.length()").value(3),
            jsonPath("items[0].name").value("bimViewer"),
            jsonPath("items[1].name").value("happyHippos"),
            jsonPath("items[2].name").value("projectImport"))
        .andDo(
            document(
                "features/document-get-features",
                preprocessResponse(prettyPrint()),
                buildListResponseFields(FEATURE_RESPONSE_FIELDS_DESCRIPTORS)))
  }

  @Test
  fun `verify and document listing feature toggles for a specific subject with expected detail information`() {
    val subjectId = "62063193-710b-405a-932d-17f6c56670a1"

    eventStreamGenerator
        .createFeature("projectImport") { it.featureName = "projectImport" }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = subjectId
          it.type = COMPANY.toString()
        }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = subjectId
          it.type = COMPANY.toString()
        }
        .createFeature("bimViewer") { it.featureName = "bimViewer" }
        .createFeature("happyHippos") { it.featureName = "happyHippos" }
        .addSubjectToWhitelistOfFeature("happyHippos") {
          it.featureName = "happyHippos"
          it.subjectRef = subjectId
          it.type = COMPANY.toString()
        }
        .createFeature("disabledFeature") { it.featureName = "disabledFeature" }
        .disableFeature("disabledFeature") { it.featureName = "disabledFeature" }

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/features/subjects/62063193-710b-405a-932d-17f6c56670a1"))))
        .andExpectAll(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("items").isArray,
            jsonPath("items.length()").value(3),
            jsonPath("items[0].name").value("bimViewer"),
            jsonPath("items[1].name").value("happyHippos"),
            jsonPath("items[2].name").value("projectImport"))
        .andDo(
            document(
                "features/document-get-features-for-subject",
                preprocessResponse(prettyPrint()),
                buildListResponseFields(FEATURE_TOGGLE_SUBJECT_RESPONSE_FIELDS_DESCRIPTORS)))
  }

  fun buildListResponseFields(itemsFieldDescriptors: List<FieldDescriptor>): ResponseFieldsSnippet =
      requireNotNull(
          responseFields(
                  fieldWithPath("items[]").description("List of items").type(ARRAY),
                  subsectionWithPath("_links").ignored())
              .andWithPrefix("items[].", itemsFieldDescriptors))

  @Test
  fun `verify and document enabling a feature successfully`() {
    eventStreamGenerator.createFeature("projectImport") { it.featureName = "projectImport" }

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/features/projectImport/enable"))))
        .andExpectAll(status().isOk)
        .andDo(
            document(
                "features/document-enable-feature",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(FEATURE_RESPONSE_FIELDS_DESCRIPTORS)))

    val feature =
        featureRepository.findByName("projectImport").also {
          assertNotNull(it)
          assertThat(it?.name).isEqualTo("projectImport")
          assertThat(it?.state).isEqualTo(ENABLED)
          assertThat(it?.version).isEqualTo(1L)
        }
    featureToggleEventStoreUtils
        .verifyContainsAndGet(FeatureEnabledEventAvro::class.java, null, 1)
        .also { events ->
          for (i in events.indices) {
            val event = events[i]
            assertThat(event.aggregateIdentifier.version).isEqualTo(1L + i)
            assertThat(event.aggregateIdentifier.type).isEqualTo(FEATURE_TOGGLE.name)
            assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
            assertThat(event.auditingInformation.date)
                .isEqualTo(feature?.lastModifiedDate?.get()?.toEpochMilli())
          }
        }
    featureToggleEventStoreUtils
  }

  @Test
  fun `verify and document disabling a feature successfully`() {
    eventStreamGenerator.createFeature("projectImport") { it.featureName = "projectImport" }

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/features/projectImport/disable"))))
        .andExpectAll(status().isOk)
        .andDo(
            document(
                "features/document-disable-feature",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(FEATURE_RESPONSE_FIELDS_DESCRIPTORS)))

    val feature =
        featureRepository.findByName("projectImport").also {
          assertNotNull(it)
          assertThat(it?.name).isEqualTo("projectImport")
          assertThat(it?.state).isEqualTo(DISABLED)
          assertThat(it?.version).isEqualTo(1L)
        }
    featureToggleEventStoreUtils
        .verifyContainsAndGet(FeatureDisabledEventAvro::class.java, null, 1)
        .also { events ->
          for (i in events.indices) {
            val event = events[i]
            assertThat(event.aggregateIdentifier.version).isEqualTo(1L + i)
            assertThat(event.aggregateIdentifier.type).isEqualTo(FEATURE_TOGGLE.name)
            assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
            assertThat(event.auditingInformation.date)
                .isEqualTo(feature?.lastModifiedDate?.get()?.toEpochMilli())
          }
        }
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `document and verify activating the whitelist on previously disabled feature successfully`() {
    eventStreamGenerator
        .createFeature("projectImport") { it.featureName = "projectImport" }
        .disableFeature("projectImport") { it.featureName = "projectImport" }

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf("/features/projectImport/activate-whitelist"))))
        .andExpectAll(status().isOk)
        .andDo(
            document(
                "features/document-activate-whitelist-for-feature",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(FEATURE_RESPONSE_FIELDS_DESCRIPTORS)))

    val feature =
        featureRepository.findByName("projectImport").also {
          assertNotNull(it)
          assertThat(it?.name).isEqualTo("projectImport")
          assertThat(it?.state).isEqualTo(WHITELIST_ACTIVATED)
          assertThat(it?.version).isEqualTo(2L)
        }
    featureToggleEventStoreUtils
        .verifyContainsAndGet(FeatureWhitelistActivatedEventAvro::class.java, null, 1)
        .also { events ->
          for (i in events.indices) {
            val event = events[i]
            assertThat(event.aggregateIdentifier.identifier)
                .isEqualTo(getIdentifier("projectImport").toString())
            assertThat(event.aggregateIdentifier.version).isEqualTo(2L + i)
            assertThat(event.aggregateIdentifier.type).isEqualTo("FEATURE_TOGGLE")
            assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
            assertThat(event.auditingInformation.date)
                .isEqualTo(feature?.lastModifiedDate?.get()?.toEpochMilli())
          }
        }
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `document and verify adding a subjects to a feature whitelist`() {
    eventStreamGenerator.createFeature("projectImport") { it.featureName = "projectImport" }

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(
                        "/features/projectImport/subjects/5db09bc5-68f2-48e7-b04a-68dc49e7ef56")),
                content = CreateWhitelistedSubjectResource(COMPANY)))
        .andExpectAll(status().isOk)
        .andDo(
            document(
                "features/add-subject-to-whitelist",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(WHITELISTED_SUBJECT_RESPONSE_FIELDS_DESCRIPTORS)))

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(
                        "/features/projectImport/subjects/0ce2f8ca-6491-4390-87a1-11ca9ffe9f90")),
                content = CreateWhitelistedSubjectResource(COMPANY)))
        .andExpectAll(status().isOk)

    val feature =
        featureRepository.findByNameWithDetails("projectImport").also {
          requireNotNull(it)
          assertThat(it.name).isEqualTo("projectImport")
          assertThat(it.state).isEqualTo(WHITELIST_ACTIVATED)
          assertThat(it.version).isEqualTo(2L)
          assertThat(it.whitelistedSubjects.size).isEqualTo(2)
          assertThat(
                  it.whitelistedSubjects.any { item ->
                    item.subjectRef == "0ce2f8ca-6491-4390-87a1-11ca9ffe9f90".toUUID() &&
                        item.featureName == "projectImport" &&
                        item.type == COMPANY
                  })
              .isTrue
          assertThat(
                  it.whitelistedSubjects.any { item ->
                    item.subjectRef == "5db09bc5-68f2-48e7-b04a-68dc49e7ef56".toUUID() &&
                        item.featureName == "projectImport" &&
                        item.type == COMPANY
                  })
              .isTrue
        }

    featureToggleEventStoreUtils
        .verifyContainsAndGet(SubjectAddedToWhitelistEventAvro::class.java, null, 2)
        .also { events ->
          for (i in events.indices) {
            val event = events[i]
            assertThat(event.aggregateIdentifier.version).isEqualTo(1L + i)
            assertThat(event.aggregateIdentifier.type).isEqualTo(FEATURE_TOGGLE.name)
            assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
            if (i == 1) {
              assertThat(event.auditingInformation.date)
                  .isEqualTo(feature?.lastModifiedDate?.get()?.toEpochMilli())
            } else {
              assertThat(event.auditingInformation.date)
                  .isLessThan(feature?.lastModifiedDate?.get()?.toEpochMilli())
            }
          }
        }
    featureToggleEventStoreUtils.reset()
  }

  @Test
  fun `verify a subject cannot be added again in whitelist if already present in the list`() {
    val subjectId = "5db09bc5-68f2-48e7-b04a-68dc49e7ef56"

    eventStreamGenerator
        .createFeature("projectImport") { it.featureName = "projectImport" }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = subjectId
          it.type = COMPANY.toString()
        }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = randomUUID().toString()
          it.type = COMPANY.toString()
        }

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(
                        "/features/projectImport/subjects/5db09bc5-68f2-48e7-b04a-68dc49e7ef56")),
                content = CreateWhitelistedSubjectResource(PROJECT)))
        .andExpectAll(status().isBadRequest)
  }

  @Test
  fun `verify and document deleting a subjects from a feature whitelist successfully`() {
    val subjectId1 = "5db09bc5-68f2-48e7-b04a-68dc49e7ef56"
    val subjectId2 = "0ce2f8ca-6491-4390-87a1-11ca9ffe9f90"

    eventStreamGenerator
        .createFeature("projectImport") { it.featureName = "projectImport" }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = subjectId1
          it.type = COMPANY.toString()
        }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = subjectId2
          it.type = COMPANY.toString()
        }

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(
                        "/features/projectImport/subjects/5db09bc5-68f2-48e7-b04a-68dc49e7ef56"))))
        .andExpectAll(status().isOk)
        .andDo(
            document(
                "features/delete-subject-from-whitelist",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(WHITELISTED_SUBJECT_ID_RESPONSE_FIELDS_DESCRIPTORS)))

    featureRepository.findByNameWithDetails("projectImport").also {
      assertNotNull(it)
      it?.let {
        assertThat(it.name).isEqualTo("projectImport")
        assertThat(it.state).isEqualTo(WHITELIST_ACTIVATED)
        assertThat(it.version).isEqualTo(3L)
        assertThat(it.whitelistedSubjects.size).isEqualTo(1)
        assertThat(
                it.whitelistedSubjects.any { whitelistedFeature ->
                  whitelistedFeature.subjectRef == subjectId2.toUUID() &&
                      whitelistedFeature.featureName == "projectImport"
                })
            .isTrue
        assertThat(
                it.whitelistedSubjects.any { whitelistedFeature ->
                  whitelistedFeature.subjectRef == subjectId1.toUUID() &&
                      whitelistedFeature.featureName == "projectImport"
                })
            .isFalse
      }
    }
  }

  @Test
  fun `verify one cannot delete a subject from a whitelist that does not contain that subject`() {
    eventStreamGenerator.createFeature("projectImport") { it.featureName = "projectImport" }

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(
                        "/features/projectImport/subjects/5db09bc5-68f2-48e7-b04a-68dc49e7ef56"))))
        .andExpectAll(status().isBadRequest)
  }

  @Test
  fun `verify one cannot delete a subject from a whitelist of a non-existing feature`() {
    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(
                        "/features/projectImport/subjects/5db09bc5-68f2-48e7-b04a-68dc49e7ef56"))))
        .andExpectAll(status().isNotFound)
  }

  @Test
  fun `verify and document deleting a feature successfully`() {
    eventStreamGenerator
        .createFeature("projectImport") { it.featureName = "projectImport" }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = randomUUID().toString()
          it.type = COMPANY.toString()
        }
        .addSubjectToWhitelistOfFeature("projectImport") {
          it.featureName = "projectImport"
          it.subjectRef = randomUUID().toString()
          it.type = COMPANY.toString()
        }

    mockMvc
        .perform(requestBuilder(delete(latestVersionOf("/features/projectImport"))))
        .andExpectAll(status().isOk)
        .andDo(
            document(
                "features/document-delete-feature",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(FEATURE_ID_RESPONSE_FIELD_DESCRIPTORS)))

    featureRepository.findByName("projectImport").also { assertNull(it) }
  }

  @Test
  fun `verify one cannot delete a feature that does not exist`() {
    mockMvc
        .perform(requestBuilder(delete(latestVersionOf("/features/projectImport"))))
        .andExpectAll(status().isNotFound)
  }

  companion object {
    private val ABSTRACT_RESOURCE_FIELD_DESCRIPTORS =
        arrayOf(
            fieldWithPath("createdBy.displayName")
                .description("Name of the user that created the resource"),
            fieldWithPath("createdBy.id").description("ID of the user that created the resource"),
            fieldWithPath("createdDate").description("Date of resource creation"),
            fieldWithPath("lastModifiedBy.displayName")
                .description("Name of the user that modified the resource last"),
            fieldWithPath("lastModifiedBy.id")
                .description("ID of the user that modified the resource last"),
            fieldWithPath("lastModifiedDate").description("Date of the last modification"))

    val CREATE_FEATURE_REQUEST_FIELDS =
        listOf(fieldWithPath("name").description("Name of the features"))

    private val FEATURE_RESPONSE_FIELDS_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("id").description("UUID of the feature"),
            fieldWithPath("version").description("Version of the feature resource"),
            fieldWithPath("name").description("Name of the feature"),
            fieldWithPath("displayName").description("User-friendly display name of the feature"),
            fieldWithPath("state")
                .description(
                    "Current toggle state of the feature. One of 'whitelistActivated', 'enabled', 'disabled'"),
            subsectionWithPath("_links").ignored())

    private val FEATURE_TOGGLE_SUBJECT_RESPONSE_FIELDS_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("id").description("UUID of the feature"),
            fieldWithPath("featureId").description("UUID of the feature"),
            fieldWithPath("subjectId").description("UUID of the subject"),
            fieldWithPath("version").description("Version of the feature resource"),
            fieldWithPath("name").description("Name of the feature"),
            fieldWithPath("displayName").description("User-friendly display name of the feature"),
            fieldWithPath("type")
                .optional()
                .description(
                    "Type of the subject in whitelist. Currently supported types are 'Project' and 'Company. " +
                        "Only supplied if the subject is whitelisted"),
            fieldWithPath("whitelisted")
                .type(Boolean)
                .description("Indicates whether a whitelist entry is present for the subject"),
            fieldWithPath("featureState").description("Feature toggle state"),
            fieldWithPath("enabledForSubject")
                .type(Boolean)
                .description(
                    "Indicates whether the feature is enabled for the subject (by whitelist or feature state)"),
            subsectionWithPath("_links").ignored())

    private val WHITELISTED_SUBJECT_RESPONSE_FIELDS_DESCRIPTORS =
        listOf(
            // *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("featureId").description("UUID of the feature"),
            fieldWithPath("featureName").description("Name of the feature"),
            fieldWithPath("subjectId")
                .description("UUID referencing the subject that is whitelisted"),
            fieldWithPath("type").description("Type of the subject"),
            subsectionWithPath("_links").ignored().optional())

    private val WHITELISTED_SUBJECT_ID_RESPONSE_FIELDS_DESCRIPTORS =
        listOf(
            // *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("featureId").description("UUID of the feature"),
            fieldWithPath("subjectId")
                .description("UUID referencing the subject that is whitelisted"),
            subsectionWithPath("_links").ignored().optional())

    private val FEATURE_ID_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            // *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("id").description("UUID of the feature"),
            subsectionWithPath("_links").ignored())

    val LOCATION_HEADER_DESCRIPTOR: HeaderDescriptor =
        HeaderDocumentation.headerWithName(HttpHeaders.LOCATION)
            .description("Location of the created resource")
  }
}
