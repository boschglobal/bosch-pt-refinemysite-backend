/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.DE
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.cloud.usermanagement.consents.common.ConsentsAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.UserConsentedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.repository.ConsentsUserRepository
import com.bosch.pt.csm.cloud.usermanagement.consents.document.Client
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.EULA
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.TERMS_AND_CONDITIONS
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentCreatedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentVersionIncrementedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.asValueObject
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.Document
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.repository.DocumentRepository
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextKafkaEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.ConsentDelayedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.UserConsentedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.net.URL
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class DocumentsApiIntegrationTest : AbstractApiDocumentationTest() {

  @Autowired
  private lateinit var consentsEventStoreUtils: EventStoreUtils<ConsentsContextKafkaEvent>

  @Autowired private lateinit var documentRepository: DocumentRepository
  @Autowired private lateinit var consentsUserRepository: ConsentsUserRepository

  @Autowired private lateinit var consentsEventBus: ConsentsContextLocalEventBus

  private val user by lazy {
    repositories.userRepository.findOneByIdentifier(UserId(getIdentifier("user1")))!!
  }
  private val admin by lazy {
    repositories.userRepository.findOneByIdentifier(UserId(getIdentifier("admin")))!!
  }

  private lateinit var document: Document
  private lateinit var documentVersion: DocumentVersion

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitUserAndActivate("user1") { it.locale = "de_DE" }
        .submitUser("admin") { it.admin = true }

    val documentId = DocumentId()
    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentCreatedEvent(
              documentId,
              TERMS_AND_CONDITIONS,
              DE,
              Locale.GERMAN,
              ClientSet.ALL,
              "Terms & Conditions",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(
                  DocumentVersionId("47104561-4bab-4bfa-9787-5cc6454bffc7"),
                  LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)
    }

    document = documentRepository.findByIdentifier(documentId)!!
    documentVersion = document.asValueObject().latestVersion()

    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          UserConsentedEvent(user.identifier, documentVersion.identifier, LocalDateTime.now()), 0)
    }

    consentsEventStoreUtils.reset()
    setAuthentication(user.identifier)
  }

  @AfterEach fun unmockkClock() = unmockkStatic(Clock::class)

  @Test
  fun `returns status 400 for invalid client`() {
    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "ALL"))
        .andExpectAll(status().isBadRequest)

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `returns fallback (DE - ENGLISH) t&c document if document for users country and locale does not exist`() {
    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentCreatedEvent(
              DocumentId(),
              TERMS_AND_CONDITIONS,
              DE,
              ENGLISH,
              ClientSet.ALL,
              "Terms & Conditions",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(
                  DocumentVersionId("37104561-4bab-4bfa-9787-5cc6454bffc7"),
                  LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)
    }

    eventStreamGenerator.submitUserAndActivate("user2") {
      it.country = IsoCountryCodeEnumAvro.US
      it.locale = Locale.US.toString()
    }
    consentsEventStoreUtils.reset()
    setAuthentication("user2")

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
                        {
                          "delayed": 0,
                          "items": [
                            {
                              "id": "37104561-4bab-4bfa-9787-5cc6454bffc7",
                              "displayName": "Terms & Conditions",
                              "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                              "consented": false,
                              "type": "TERMS_AND_CONDITIONS"
                            }
                          ]
                        }"""),
        )

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `returns fallback (DE - ENGLISH) t&c Document if user has no country or locale set`() {
    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentCreatedEvent(
              DocumentId(),
              TERMS_AND_CONDITIONS,
              DE,
              ENGLISH,
              ClientSet.ALL,
              "Terms & Conditions",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(
                  DocumentVersionId("37104561-4bab-4bfa-9787-5cc6454bffc7"),
                  LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)
    }

    eventStreamGenerator.submitUserAndActivate("user2") {
      it.country = null
      it.locale = null
    }
    consentsEventStoreUtils.reset()
    setAuthentication("user2")

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
                        {
                          "delayed": 0,
                          "items": [
                            {
                              "id": "37104561-4bab-4bfa-9787-5cc6454bffc7",
                              "displayName": "Terms & Conditions",
                              "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                              "consented": false,
                              "type": "TERMS_AND_CONDITIONS"
                            }
                          ]
                        }"""),
        )

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `returns T&C and EULA with consented true, if user consented to latest versions`() {
    eventStreamGenerator.submitUser("user2") {
      it.country = IsoCountryCodeEnumAvro.US
      it.locale = "en_US"
    }
    val userIdentifier = UserId(getIdentifier("user2"))
    val termsDocumentVersionId = DocumentVersionId("27104561-4bab-4bfa-9787-5cc6454bffc7")
    val eulaDocumentVersionId = DocumentVersionId("17104561-4bab-4bfa-9787-5cc6454bffc7")
    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentCreatedEvent(
              DocumentId(),
              EULA,
              US,
              ENGLISH,
              ClientSet.WEB,
              "EULA",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(eulaDocumentVersionId, LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)

      consentsEventBus.emit(
          DocumentCreatedEvent(
              DocumentId(),
              TERMS_AND_CONDITIONS,
              US,
              ENGLISH,
              ClientSet.ALL,
              "Terms & Conditions",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(termsDocumentVersionId, LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)

      consentsEventBus.emit(
          UserConsentedEvent(userIdentifier, termsDocumentVersionId, LocalDateTime.now()), 0)

      consentsEventBus.emit(
          UserConsentedEvent(userIdentifier, eulaDocumentVersionId, LocalDateTime.now()), 1)
    }
    consentsEventStoreUtils.reset()

    setAuthentication(userIdentifier)

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
                        {
                          "delayed": 0,
                          "items": [
                            {
                              "id": "17104561-4bab-4bfa-9787-5cc6454bffc7",
                              "displayName": "EULA",
                              "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                              "consented": true,
                              "type": "EULA"
                            },
                            {
                              "id": "27104561-4bab-4bfa-9787-5cc6454bffc7",
                              "displayName": "Terms & Conditions",
                              "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                              "consented": true,
                              "type": "TERMS_AND_CONDITIONS"
                            }
                          ]
                        }"""),
        )
        .andDo(
            document(
                "documents/document-get-documents",
                preprocessResponse(prettyPrint()),
                queryParameters(DOCUMENT_QUERY_PARAMETER_DESCRIPTORS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `returns T&C and EULA document with consented false, if user did not consent`() {
    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentCreatedEvent(
              DocumentId(),
              EULA,
              US,
              ENGLISH,
              ClientSet.WEB,
              "EULA",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(
                  DocumentVersionId("17104561-4bab-4bfa-9787-5cc6454bffc7"),
                  LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)

      consentsEventBus.emit(
          DocumentCreatedEvent(
              DocumentId(),
              TERMS_AND_CONDITIONS,
              US,
              ENGLISH,
              ClientSet.ALL,
              "Terms & Conditions",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(
                  DocumentVersionId("27104561-4bab-4bfa-9787-5cc6454bffc7"),
                  LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)
    }
    consentsEventStoreUtils.reset()

    eventStreamGenerator.submitUser("user2") {
      it.country = IsoCountryCodeEnumAvro.US
      it.locale = "en_US"
    }
    setAuthentication("user2")

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
                        {
                          "delayed": 0,
                          "items": [
                            {
                              "id": "27104561-4bab-4bfa-9787-5cc6454bffc7",
                              "displayName": "Terms & Conditions",
                              "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                              "consented": false,
                              "type": "TERMS_AND_CONDITIONS"
                            },
                            {
                              "id": "17104561-4bab-4bfa-9787-5cc6454bffc7",
                              "displayName": "EULA",
                              "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                              "consented": false,
                              "type": "EULA"
                            }
                          ]
                        }"""),
        )
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `returns terms and conditions document with consented false, if there is a new version`() {
    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentVersionIncrementedEvent(
              document.identifier,
              DocumentVersion(
                  DocumentVersionId("57104561-4bab-4bfa-9787-5cc6454bffc8"),
                  LocalDateTime.now().minusDays(1)),
              LocalDateTime.now(),
              admin.identifier),
          1)
    }
    consentsEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
                        {
                          "delayed": 0,
                          "items": [
                            {
                              "id": "57104561-4bab-4bfa-9787-5cc6454bffc8",
                              "displayName": "Terms & Conditions",
                              "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                              "consented": false,
                              "type": "TERMS_AND_CONDITIONS"
                            }
                          ]
                        }"""),
        )

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST returns 200 - ok on posting consent to already consented document (idempotency)`() {
    val documentVersionId = "47104561-4bab-4bfa-9787-5cc6454bffc7"
    val resource = BatchRequestResource(setOf(UUID.fromString(documentVersionId)))

    val numberOfConsentsBefore =
        consentsUserRepository.findByIdentifier(UserId(getIdentifier("user1")))!!.consents.size

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf("/users/current/consents")), content = resource))
        .andExpect(status().isOk)

    val numberOfConsentsAfter =
        consentsUserRepository.findByIdentifier(UserId(getIdentifier("user1")))!!.consents.size
    assertThat(numberOfConsentsAfter).isEqualTo(numberOfConsentsBefore)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST returns 200 - ok on posting consent to multiple documents`() {
    val newDocumentVersionId1 = "7d3acb63-5ff4-445f-9012-e33c24cfb449"
    val newDocumentVersionId2 = "8d3acb63-5ff4-445f-9012-e33c24cfb449"
    val resource =
        BatchRequestResource(
            setOf(UUID.fromString(newDocumentVersionId1), UUID.fromString(newDocumentVersionId2)))

    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentVersionIncrementedEvent(
              document.identifier,
              DocumentVersion(
                  DocumentVersionId(newDocumentVersionId1), LocalDateTime.now().minusDays(2)),
              LocalDateTime.now(),
              admin.identifier),
          1)

      consentsEventBus.emit(
          DocumentCreatedEvent(
              DocumentId(),
              EULA,
              DE,
              Locale.GERMAN,
              ClientSet.ALL,
              "EULA",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
              DocumentVersion(
                  DocumentVersionId(newDocumentVersionId2), LocalDateTime.now().minusMonths(12)),
              LocalDateTime.now(),
              admin.identifier),
          0)
    }
    consentsEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf("/users/current/consents")), content = resource))
        .andExpect(status().isOk)
        .andDo(
            document(
                "documents/document-consent",
                preprocessRequest(prettyPrint()),
                requestFields(DOCUMENT_REQUEST_FIELDS)))

    val consentsUser = consentsUserRepository.findByIdentifier(UserId(getIdentifier("user1")))!!
    assertThat(consentsUser.consents).hasSize(3)
    assertThat(consentsUser.consents)
        .extracting("documentVersionId")
        .contains(
            DocumentVersionId(newDocumentVersionId1),
            DocumentVersionId(newDocumentVersionId2),
        )

    consentsEventStoreUtils
        .verifyContainsAndGet(UserConsentedEventAvro::class.java, null, 2)
        .also { events ->
          for (i in events.indices) {
            val event = events[i]
            assertThat(event.aggregateIdentifier.identifier)
                .isEqualTo(getIdentifier("user1").toString())
            assertThat(event.aggregateIdentifier.version).isEqualTo(1L + i)
            assertThat(event.aggregateIdentifier.type).isEqualTo(USER.value)
            assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("user1").toString())
            assertThat(event.auditingInformation.date)
                .isEqualTo(consentsUser.lastModifiedDate.get().toEpochMilli())
          }

          assertThat(events.map { it.documentVersionIdentifier })
              .contains(newDocumentVersionId1, newDocumentVersionId2)
        }
  }

  @Test
  fun `POST returns 200 - ok on posting consent if user never consented or delayed so far`() {
    eventStreamGenerator.submitUser("user2") { it.locale = "de_DE" }

    val newDocumentVersionId = "7d3acb63-5ff4-445f-9012-e33c24cfb449"
    val resource = BatchRequestResource(setOf(UUID.fromString(newDocumentVersionId)))

    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentVersionIncrementedEvent(
              document.identifier,
              // Create a new document version with a date newer than the version created in the
              // init() method.
              DocumentVersion(
                  DocumentVersionId(newDocumentVersionId), LocalDateTime.now().minusMonths(11)),
              LocalDateTime.now(),
              admin.identifier),
          1)
    }
    document = documentRepository.findByIdentifier(document.identifier)!!
    val newDocumentVersion = document.asValueObject().latestVersion()

    consentsEventStoreUtils.reset()
    setAuthentication("user2")

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf("/users/current/consents")), content = resource))
        .andExpect(status().isOk)

    val consentsUser = consentsUserRepository.findByIdentifier(UserId(getIdentifier("user2")))!!
    assertThat(consentsUser.consents).hasSize(1)
    assertThat(consentsUser.consents)
        .extracting("documentVersionId")
        .contains(newDocumentVersion.identifier)

    consentsEventStoreUtils.verifyContainsAndGet(UserConsentedEventAvro::class.java, null).also {
      assertThat(it.aggregateIdentifier.identifier).isEqualTo(getIdentifier("user2").toString())
      assertThat(it.aggregateIdentifier.version).isEqualTo(0)
      assertThat(it.aggregateIdentifier.type).isEqualTo(USER.value)
      assertThat(it.auditingInformation.user).isEqualTo(getIdentifier("user2").toString())
      assertThat(it.auditingInformation.date)
          .isEqualTo(consentsUser.lastModifiedDate.get().toEpochMilli())
      assertThat(it.documentVersionIdentifier).isEqualTo(newDocumentVersionId)
    }
  }

  @Test
  fun `POST consent returns 400 if document version is not found`() {
    val documentVersionId = "123acb63-5ff4-445f-9012-e33c24cfb449"
    val resource = BatchRequestResource(setOf(UUID.fromString(documentVersionId)))

    val numberOfConsentsBefore =
        consentsUserRepository.findByIdentifier(UserId(getIdentifier("user1")))!!.consents.size

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf("/users/current/consents")), content = resource))
        .andExpect(status().isBadRequest)

    val numberOfConsentsAfter =
        consentsUserRepository.findByIdentifier(UserId(getIdentifier("user1")))!!.consents.size
    assertThat(numberOfConsentsAfter).isEqualTo(numberOfConsentsBefore)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST delay returns 200`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/documents/delay-consent"))))
        .andExpect(status().isOk)
        .andDo(document("documents/document-delay-consent"))

    val consentsUser = consentsUserRepository.findByIdentifier(UserId(getIdentifier("user2")))!!

    consentsEventStoreUtils.verifyContainsAndGet(ConsentDelayedEventAvro::class.java, null).also {
      assertThat(it.aggregateIdentifier.identifier).isEqualTo(getIdentifier("user2").toString())
      assertThat(it.aggregateIdentifier.version).isEqualTo(0)
      assertThat(it.aggregateIdentifier.type).isEqualTo(USER.value)
      assertThat(it.auditingInformation.user).isEqualTo(getIdentifier("user2").toString())
      assertThat(it.auditingInformation.date).isEqualTo(consentsUser.delayedAt.toEpochMilli())
    }
  }

  @Test
  fun `POST delay returns 200 if already delayed`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/documents/delay-consent"))))
        .andExpect(status().isOk)

    consentsEventStoreUtils.reset()

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/documents/delay-consent"))))
        .andExpect(status().isOk)

    val consentsUser = consentsUserRepository.findByIdentifier(UserId(getIdentifier("user2")))!!

    consentsEventStoreUtils.verifyContainsAndGet(ConsentDelayedEventAvro::class.java, null).also {
      assertThat(it.aggregateIdentifier.identifier).isEqualTo(getIdentifier("user2").toString())
      assertThat(it.aggregateIdentifier.version).isEqualTo(1)
      assertThat(it.aggregateIdentifier.type).isEqualTo(USER.value)
      assertThat(it.auditingInformation.user).isEqualTo(getIdentifier("user2").toString())
      assertThat(it.auditingInformation.date).isEqualTo(consentsUser.delayedAt.toEpochMilli())
    }
  }

  @Test
  fun `GET documents returns delayed value of around 16 hours after consent is delayed`() {
    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/documents/delay-consent"))))
        .andExpect(status().isOk)

    consentsEventStoreUtils.reset()

    val expectedDelayUpper = 16.hours.inWholeMilliseconds.toInt()
    val expectedDelayLower = expectedDelayUpper - 30.seconds.inWholeMilliseconds.toInt()
    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            jsonPath("delayed")
                .value(
                    allOf(
                        greaterThanOrEqualTo(expectedDelayLower),
                        lessThanOrEqualTo(expectedDelayUpper))),
        )

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `GET documents returns delayed value of 0 after 16 hours has passed after delay`() {
    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/documents/delay-consent"))))
        .andExpect(status().isOk)

    consentsEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            jsonPath("delayed").value(greaterThan(0)),
        )

    val delayDuration = Duration.ofHours(16)
    val originalClock = Clock.systemDefaultZone()
    mockkStatic(Clock::class)
    every { Clock.systemDefaultZone() } returns Clock.offset(originalClock, delayDuration)

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/current/documents")))
                .queryParam("client", "WEB"))
        .andExpectAll(
            status().isOk,
            jsonPath("delayed").value(0),
        )

    consentsEventStoreUtils.verifyEmpty()
  }

  companion object {
    private val DOCUMENT_QUERY_PARAMETER_DESCRIPTORS =
        listOf(
            parameterWithName("client")
                .description(
                    "Client the request is executed from, " +
                        "valid values are: ${Client.values().map { it.toString() }}"),
        )

    private val DOCUMENT_REQUEST_FIELDS =
        listOf(
            ConstrainedFields(BatchRequestResource::class.java)
                .withPath("ids[]")
                .description("List of document version identifiers to give consent to")
                .type(JsonFieldType.ARRAY))

    private val DOCUMENT_RESPONSE_FIELDS =
        listOf(
            fieldWithPath("delayed")
                .description(
                    "Time in milliseconds on how long asking for consent should be delayed. " +
                        "A value of 0 means that the user can be asked for consent. " +
                        "A value greater than 0 represents the time to wait in milliseconds " +
                        "until the user can be asked again.")
                .type(JsonFieldType.NUMBER),
            fieldWithPath("items[]")
                .description(
                    "Latest version of legal documents, i.e. latest version of terms and conditions")
                .type(JsonFieldType.ARRAY),
            fieldWithPath("items[].id")
                .description("Identifier of the document version")
                .type(JsonFieldType.STRING),
            fieldWithPath("items[].displayName")
                .description("Localized display name of the document")
                .type(JsonFieldType.STRING),
            fieldWithPath("items[].url")
                .description("URL to the document")
                .type(JsonFieldType.STRING),
            fieldWithPath("items[].consented")
                .description("Whether the user consented to this document or not")
                .type(JsonFieldType.BOOLEAN),
            fieldWithPath("items[].type")
                .description(
                    "Type of document, valid values are: ${DocumentType.values().map { it.toString() }}")
                .type(JsonFieldType.STRING),
        )
  }
}
