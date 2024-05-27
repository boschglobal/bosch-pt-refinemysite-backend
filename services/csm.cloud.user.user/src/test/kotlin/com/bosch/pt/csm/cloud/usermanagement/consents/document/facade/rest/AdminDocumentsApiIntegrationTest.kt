/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.DE
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.consents.common.ConsentsAggregateTypeEnum.DOCUMENT
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentCreatedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.TERMS_AND_CONDITIONS
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.Document
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.repository.DocumentRepository
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextKafkaEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentChangedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentVersionIncrementedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import io.mockk.unmockkStatic
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminDocumentsApiIntegrationTest : AbstractApiDocumentationTest() {

  @Autowired
  private lateinit var consentsEventStoreUtils: EventStoreUtils<ConsentsContextKafkaEvent>

  @Autowired private lateinit var documentRepository: DocumentRepository

  @Autowired private lateinit var consentsEventBus: ConsentsContextLocalEventBus

  private val admin by lazy {
    repositories.userRepository.findOneByIdentifier(UserId(getIdentifier("admin")))!!
  }

  private lateinit var document: Document
  private lateinit var documentVersion: DocumentVersion

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitUser("user1").submitUser("admin") { it.admin = true }

    val documentId = DocumentId(UUID.fromString("6cfeca0e-4bab-4bfa-9787-5cc6454bffc7"))
    transactionTemplate.executeWithoutResult {
      consentsEventBus.emit(
          DocumentCreatedEvent(
              documentId,
              TERMS_AND_CONDITIONS,
              DE,
              Locale.ENGLISH,
              ClientSet.ALL,
              "Terms & Conditions",
              URL("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/"),
            com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion(
              DocumentVersionId("47104561-4bab-4bfa-9787-5cc6454bffc7"),
              LocalDateTime.of(2021, 12, 1, 12, 0)
            ),
              LocalDateTime.now(),
              admin.identifier),
          0)
    }

    document = documentRepository.findByIdentifier(documentId)!!
    documentVersion = document.versions.first()

    consentsEventStoreUtils.reset()
    setAuthentication("admin")
  }

  @AfterEach fun unmockkUUID() = unmockkStatic(UUID::class)

  @Test
  fun `GET returns 403 for a non-admin user`() {
    setAuthentication("user1")

    mockMvc
        .perform(requestBuilder(get(version1of("/documents"))))
        .andExpectAll(status().isForbidden)

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `GET by id returns 403 for a non-admin user`() {
    setAuthentication("user1")

    mockMvc
        .perform(
            requestBuilder(
                get(version1of("/documents/{documentId}"), "47104561-4bab-4bfa-9787-5cc6454bffc7")))
        .andExpectAll(status().isForbidden)

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `GET returns list of document versions`() {
    mockMvc
        .perform(requestBuilder(get(version1of("/documents"))))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "items": [
                  {
                    "identifier": "6cfeca0e-4bab-4bfa-9787-5cc6454bffc7",
                    "displayName": "Terms & Conditions",
                    "client": "ALL",
                    "type": "TERMS_AND_CONDITIONS",
                    "country": "DE",
                    "locale": "en",
                    "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                    "versions": [
                      {
                        "identifier": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                        "lastChanged": "2021-12-01T12:00:00Z"
                      }
                    ]
                  }
                ]
              }
          """.trimIndent()))
        .andDo(
            document(
                "documents/document-get-all-documents",
                preprocessResponse(prettyPrint()),
                responseFields(
                        fieldWithPath("items[]")
                            .description("List of document versions")
                            .type(JsonFieldType.ARRAY))
                    .andWithPrefix("items[].", DOCUMENT_RESPONSE_FIELDS),
            ))

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `GET by id returns a document by identifier`() {
    mockMvc
        .perform(requestBuilder(get(version1of("/documents/{documentId}"), document.identifier)))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "identifier": "6cfeca0e-4bab-4bfa-9787-5cc6454bffc7",
                "displayName": "Terms & Conditions",
                "client": "ALL",
                "type": "TERMS_AND_CONDITIONS",
                "country": "DE",
                "locale": "en",
                "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                "versions": [
                  {
                    "identifier": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                    "lastChanged": "2021-12-01T12:00:00Z"
                  }
                ]
              }
          """.trimIndent()))
        .andDo(
            document(
                "documents/document-get-document-by-id",
                preprocessResponse(prettyPrint()),
                pathParameters(DOCUMENT_PATH_PARAMETER_DESCRIPTOR),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `GET by id returns 404 for non existent version`() {
    mockMvc
        .perform(requestBuilder(get(version1of("/documents/12304561-4bab-4bfa-9787-5cc6454bffc7"))))
        .andExpectAll(status().isNotFound)

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST returns 403 for a non-admin user`() {
    setAuthentication("user1")

    val resource =
        DocumentAdminCreateResource(
            type = "TERMS_AND_CONDITIONS",
            country = "US",
            locale = "en",
            client = "ALL",
            displayName = "Terms & Conditions",
            url =
                "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2F",
            lastChanged = LocalDateTime.of(2022, 6, 1, 6, 15).toInstant(ZoneOffset.UTC))

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(requestBuilder(post(version1of("/documents")), content = resource))
        .andExpectAll(status().isForbidden)

    assertThat(documentRepository.findAll()).hasSize(1)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST creates a document and version`() {
    val resource =
        DocumentAdminCreateResource(
            type = "TERMS_AND_CONDITIONS",
            country = "US",
            locale = "en",
            client = "ALL",
            displayName = "Terms & Conditions",
            url =
                "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2F",
            lastChanged = LocalDateTime.of(2022, 6, 1, 6, 15).toInstant(ZoneOffset.UTC))

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(requestBuilder(post(version1of("/documents")), content = resource))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "displayName": "Terms & Conditions",
                "client": "ALL",
                "type": "TERMS_AND_CONDITIONS",
                "country": "US",
                "locale": "en",
                "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                "versions": [
                  {
                    "lastChanged": "2022-06-01T06:15:00Z"
                  }
                ]
              }
          """.trimIndent()),
            jsonPath("identifier").exists(),
            jsonPath("versions[0].identifier").exists(),
        )
        .andDo(
            document(
                "documents/document-create-document",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(DOCUMENT_CREATE_REQUEST_FIELDS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    val createdDocument =
        documentRepository.findByCountryInOrLocaleIn(listOf(US), listOf(Locale.US)).firstOrNull()
    assertThat(createdDocument).isNotNull
    assertThat(createdDocument!!.versions).hasSize(1)

    consentsEventStoreUtils.verifyContainsAndGet(DocumentCreatedEventAvro::class.java, null).also {
      assertThat(it.aggregateIdentifier.identifier).isNotNull
      assertThat(it.aggregateIdentifier.version).isEqualTo(0)
      assertThat(it.aggregateIdentifier.type).isEqualTo(DOCUMENT.value)
      assertThat(it.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
      assertThat(it.auditingInformation.date)
          .isEqualTo(createdDocument.createdDate.get().toEpochMilli())
      assertThat(it.url)
          .isEqualTo("https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/")
      assertThat(it.title).isEqualTo(resource.displayName)
      assertThat(it.country.toString()).isEqualTo(resource.country)
      assertThat(it.locale).isEqualTo(resource.locale)
      assertThat(it.type).isEqualTo("TERMS_AND_CONDITIONS")
      assertThat(it.client).isEqualTo("ALL")
      assertThat(it.initialVersion.identifier).isNotNull
      assertThat(it.initialVersion.lastChanged).isEqualTo(resource.lastChanged.toEpochMilli())
    }
  }

  @Test
  fun `POST returns 400 if country can not be parsed`() {
    val resource =
        DocumentAdminCreateResource(
            type = "TERMS_AND_CONDITIONS",
            country = "asd",
            locale = "en",
            client = "ALL",
            displayName = "Terms & Conditions",
            url =
                "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2F",
            lastChanged = LocalDateTime.of(2022, 1, 1, 12, 0).toInstant(ZoneOffset.UTC))

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(requestBuilder(post(version1of("/documents")), content = resource))
        .andExpectAll(status().isBadRequest)

    assertThat(documentRepository.findAll()).hasSize(1)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST returns 400 if a document for country and locale already exists`() {
    val resource =
        DocumentAdminCreateResource(
            type = "TERMS_AND_CONDITIONS",
            country = "DE",
            locale = "en",
            client = "ALL",
            displayName = "Terms & Conditions",
            url =
                "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2F",
            lastChanged = LocalDateTime.of(2022, 1, 1, 12, 0).toInstant(ZoneOffset.UTC))

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(requestBuilder(post(version1of("/documents")), content = resource))
        .andExpectAll(status().isBadRequest)

    assertThat(documentRepository.findAll()).hasSize(1)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST returns 400 if more than language is set in locale of document`() {
    val resource =
        DocumentAdminCreateResource(
            type = "TERMS_AND_CONDITIONS",
            country = "DE",
            locale = "de_DE",
            client = "ALL",
            displayName = "Terms & Conditions",
            url =
                "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2F",
            lastChanged = LocalDateTime.of(2022, 1, 1, 12, 0).toInstant(ZoneOffset.UTC))

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(
            requestBuilder(post(version1of("/documents"), document.identifier), content = resource))
        .andExpectAll(status().isBadRequest)

    assertThat(documentRepository.findAll()).hasSize(1)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST new version returns 404 if document does not exist`() {
    val resource = DocumentVersionIncrementAdminResource(Instant.now())

    mockMvc
        .perform(
            requestBuilder(
                post(version1of("/documents/{documentId}/versions"), randomUUID()),
                content = resource))
        .andExpectAll(status().isNotFound)

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST new version returns 400 if new versions lastChanged is older than current latest version`() {
    val resource = DocumentVersionIncrementAdminResource(Instant.ofEpochMilli(0))

    val numberOfVersions = documentRepository.findByIdentifier(document.identifier)!!.versions.size

    mockMvc
        .perform(
            requestBuilder(
                post(version1of("/documents/{documentId}/versions"), document.identifier),
                content = resource))
        .andExpectAll(status().isBadRequest)

    assertThat(documentRepository.findByIdentifier(document.identifier)!!.versions.size)
        .isEqualTo(numberOfVersions)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `POST new version successfully adds new version to document`() {
    val resource =
        DocumentVersionIncrementAdminResource(
            LocalDateTime.of(2022, 12, 1, 6, 15).toInstant(ZoneOffset.UTC))

    val numberOfVersions = documentRepository.findByIdentifier(document.identifier)!!.versions.size

    mockMvc
        .perform(
            requestBuilder(
                post(version1of("/documents/{documentId}/versions"), document.identifier),
                content = resource))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "identifier": "6cfeca0e-4bab-4bfa-9787-5cc6454bffc7",
                "displayName": "Terms & Conditions",
                "client": "ALL",
                "type": "TERMS_AND_CONDITIONS",
                "country": "DE",
                "locale": "en",
                "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                "versions": [
                  {
                    "identifier": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                    "lastChanged": "2021-12-01T12:00:00Z"
                  },
                  {
                    "lastChanged": "2022-12-01T06:15:00Z"
                  }
                ]
              }
          """.trimIndent()))
        .andDo(
            document(
                "documents/document-increment-document-version",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(DOCUMENT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(DOCUMENT_INCREMENT_VERSION_REQUEST_FIELDS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    val documentWithNewVersion = documentRepository.findByIdentifier(document.identifier)!!
    assertThat(documentWithNewVersion.versions).hasSize(2)

    assertThat(documentRepository.findByIdentifier(document.identifier)!!.versions.size)
        .isEqualTo(numberOfVersions + 1)
    consentsEventStoreUtils
        .verifyContainsAndGet(DocumentVersionIncrementedEventAvro::class.java, null)
        .also { event ->
          assertThat(event.aggregateIdentifier.identifier).isNotNull
          assertThat(event.aggregateIdentifier.version).isEqualTo(1)
          assertThat(event.aggregateIdentifier.type).isEqualTo(DOCUMENT.value)
          assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
          assertThat(event.auditingInformation.date)
              .isEqualTo(documentWithNewVersion.lastModifiedDate.get().toEpochMilli())
          val newVersion =
              documentWithNewVersion.versions.first {
                event.version.identifier == it.identifier.toString()
              }
          assertThat(event.version.lastChanged).isEqualTo(newVersion.lastChanged.toEpochMilli())
        }
  }

  @Test
  fun `PUT returns 403 for a non-admin user`() {
    setAuthentication("user1")

    val resource =
        DocumentAdminUpdateResource(
            "Terms & Conditions 2",
            "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2Fnew")

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(
            requestBuilder(
                put(version1of("/documents/{documentId}"), document.identifier),
                content = resource))
        .andExpectAll(status().isForbidden)

    assertThat(documentRepository.findAll()).hasSize(1)
    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `PUT updates url and name of a document`() {
    val resource =
        DocumentAdminUpdateResource(
            "Terms & Conditions 2",
            "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2Fnew")

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(
            requestBuilder(
                put(version1of("/documents/{documentId}"), document.identifier),
                content = resource))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "identifier": "6cfeca0e-4bab-4bfa-9787-5cc6454bffc7",
                "displayName": "Terms & Conditions 2",
                "client": "ALL",
                "type": "TERMS_AND_CONDITIONS",
                "country": "DE",
                "locale": "en",
                "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/new",
                "versions": [
                  {
                    "identifier": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                    "lastChanged": "2021-12-01T12:00:00Z"
                  }
                ]
              }
          """.trimIndent()))
        .andDo(
            document(
                "documents/document-update-document",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(DOCUMENT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(DOCUMENT_UPDATE_REQUEST_FIELDS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    assertThat(documentRepository.findAll()).hasSize(1)

    val updatedDocument = documentRepository.findByIdentifier(document.identifier)!!
    consentsEventStoreUtils.verifyContainsAndGet(DocumentChangedEventAvro::class.java, null).also {
        event ->
      assertThat(event.aggregateIdentifier.identifier).isNotNull
      assertThat(event.aggregateIdentifier.version).isEqualTo(1)
      assertThat(event.aggregateIdentifier.type).isEqualTo(DOCUMENT.value)
      assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
      assertThat(event.auditingInformation.date)
          .isEqualTo(updatedDocument.lastModifiedDate.get().toEpochMilli())
      assertThat(event.title).isEqualTo("Terms & Conditions 2")
      assertThat(event.url)
          .isEqualTo(
              "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/new")
    }
  }

  @Test
  fun `PUT updates displayName of a document`() {
    val resource = DocumentAdminUpdateResource("Terms & Conditions 2", null)

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(
            requestBuilder(
                put(version1of("/documents/{documentId}"), document.identifier),
                content = resource))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "identifier": "6cfeca0e-4bab-4bfa-9787-5cc6454bffc7",
                "displayName": "Terms & Conditions 2",
                "client": "ALL",
                "type": "TERMS_AND_CONDITIONS",
                "country": "DE",
                "locale": "en",
                "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                "versions": [
                  {
                    "identifier": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                    "lastChanged": "2021-12-01T12:00:00Z"
                  }
                ]
              }
          """.trimIndent()))
        .andDo(
            document(
                "documents/document-update-document",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(DOCUMENT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(DOCUMENT_UPDATE_REQUEST_FIELDS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    assertThat(documentRepository.findAll()).hasSize(1)

    val updatedDocument = documentRepository.findByIdentifier(document.identifier)!!
    consentsEventStoreUtils.verifyContainsAndGet(DocumentChangedEventAvro::class.java, null).also {
        event ->
      assertThat(event.aggregateIdentifier.identifier).isNotNull
      assertThat(event.aggregateIdentifier.version).isEqualTo(1)
      assertThat(event.aggregateIdentifier.type).isEqualTo(DOCUMENT.value)
      assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
      assertThat(event.auditingInformation.date)
          .isEqualTo(updatedDocument.lastModifiedDate.get().toEpochMilli())
      assertThat(event.title).isEqualTo("Terms & Conditions 2")
    }
  }

  @Test
  fun `PUT updates url of a document`() {
    val resource =
        DocumentAdminUpdateResource(
            null,
            "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2Fnew")

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(
            requestBuilder(
                put(version1of("/documents/{documentId}"), document.identifier),
                content = resource))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "identifier": "6cfeca0e-4bab-4bfa-9787-5cc6454bffc7",
                "displayName": "Terms & Conditions",
                "client": "ALL",
                "type": "TERMS_AND_CONDITIONS",
                "country": "DE",
                "locale": "en",
                "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/new",
                "versions": [
                  {
                    "identifier": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                    "lastChanged": "2021-12-01T12:00:00Z"
                  }
                ]
              }
          """.trimIndent()))
        .andDo(
            document(
                "documents/document-update-document",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(DOCUMENT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(DOCUMENT_UPDATE_REQUEST_FIELDS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    assertThat(documentRepository.findAll()).hasSize(1)

    val updatedDocument = documentRepository.findByIdentifier(document.identifier)!!
    consentsEventStoreUtils.verifyContainsAndGet(DocumentChangedEventAvro::class.java, null).also {
        event ->
      assertThat(event.aggregateIdentifier.identifier).isNotNull
      assertThat(event.aggregateIdentifier.version).isEqualTo(1)
      assertThat(event.aggregateIdentifier.type).isEqualTo(DOCUMENT.value)
      assertThat(event.auditingInformation.user).isEqualTo(getIdentifier("admin").toString())
      assertThat(event.auditingInformation.date)
          .isEqualTo(updatedDocument.lastModifiedDate.get().toEpochMilli())
      assertThat(event.url)
          .isEqualTo(
              "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/new")
    }
  }

  @Test
  fun `PUT with empty resource changes nothing and emits no events`() {
    val resource = DocumentAdminUpdateResource(null, null)

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(
            requestBuilder(
                put(version1of("/documents/{documentId}"), document.identifier),
                content = resource))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "identifier": "6cfeca0e-4bab-4bfa-9787-5cc6454bffc7",
                "displayName": "Terms & Conditions",
                "client": "ALL",
                "type": "TERMS_AND_CONDITIONS",
                "country": "DE",
                "locale": "en",
                "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                "versions": [
                  {
                    "identifier": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                    "lastChanged": "2021-12-01T12:00:00Z"
                  }
                ]
              }
          """.trimIndent()))
        .andDo(
            document(
                "documents/document-update-document",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(DOCUMENT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(DOCUMENT_UPDATE_REQUEST_FIELDS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    assertThat(documentRepository.findAll()).hasSize(1)

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `PUT returns 404 if document is not found`() {
    val resource =
        DocumentAdminUpdateResource(
            "Terms & Conditions 2",
            "https%3A%2F%2Fwww.bosch-pt.com%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2Fnew")

    assertThat(documentRepository.findAll()).hasSize(1)

    mockMvc
        .perform(
            requestBuilder(
                put(version1of("/documents/12304561-4bab-4bfa-9787-5cc6454bffc7")),
                content = resource))
        .andExpectAll(status().isNotFound)

    assertThat(documentRepository.findAll()).hasSize(1)
    consentsEventStoreUtils.verifyEmpty()
  }

  companion object {
    private val DOCUMENT_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName("documentId").description("Identifier of the document")

    private val DOCUMENT_INCREMENT_VERSION_REQUEST_FIELDS =
        listOf(
            ConstrainedFields(DocumentAdminUpdateResource::class.java)
                .withPath("lastChanged")
                .description(
                    "Timestamp when the document version was last changed," +
                        " e.g. 2022-02-01T00:00:00Z (ISO8601 format)")
                .type(JsonFieldType.STRING),
        )

    private val DOCUMENT_UPDATE_REQUEST_FIELDS =
        listOf(
            ConstrainedFields(DocumentAdminUpdateResource::class.java)
                .withPath("displayName")
                .description("Localized display name of the document, e.g. Terms & Conditions")
                .type(JsonFieldType.STRING)
                .optional(),
            ConstrainedFields(DocumentAdminUpdateResource::class.java)
                .withPath("url")
                .description(
                    "URL-encoded URL to the document, e.g. https%3A%2F%2Fwww.bosch-pt.com" +
                        "%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2F")
                .type(JsonFieldType.STRING)
                .optional(),
        )

    private val DOCUMENT_CREATE_REQUEST_FIELDS =
        listOf(
            ConstrainedFields(DocumentAdminCreateResource::class.java)
                .withPath("displayName")
                .description("Localized display name of the document, e.g. Terms & Conditions")
                .type(JsonFieldType.STRING),
            ConstrainedFields(DocumentAdminCreateResource::class.java)
                .withPath("type")
                .description(
                    "Type of document, valid values are: ${DocumentType.values().map { it.toString() }}")
                .type(JsonFieldType.STRING),
            ConstrainedFields(DocumentAdminCreateResource::class.java)
                .withPath("client")
                .description(
                    "Client to which the document applies to, " +
                        "valid values are: ${ClientSet.values().map { it.toString() }}")
                .type(JsonFieldType.STRING),
            ConstrainedFields(DocumentAdminCreateResource::class.java)
                .withPath("country")
                .description("Country this document is for, e.g. DE")
                .type(JsonFieldType.STRING),
            ConstrainedFields(DocumentAdminCreateResource::class.java)
                .withPath("locale")
                .description("Locale of the document, e.g. en")
                .type(JsonFieldType.STRING),
            ConstrainedFields(DocumentAdminCreateResource::class.java)
                .withPath("url")
                .description(
                    "URL-encoded URL to the document, e.g. https%3A%2F%2Fwww.bosch-pt.com" +
                        "%2Fptlegalpages%2Fde%2Fptde%2Fen%2Frefinemysiteweb%2Ftermsofuse%2F")
                .type(JsonFieldType.STRING),
            ConstrainedFields(DocumentAdminCreateResource::class.java)
                .withPath("lastChanged")
                .description(
                    "Timestamp when this document was last changed, e.g. 2022-02-01T00:00:00Z (ISO8601 format)")
                .type(JsonFieldType.STRING),
        )

    private val DOCUMENT_RESPONSE_FIELDS =
        listOf(
            fieldWithPath("identifier")
                .description("Identifier of the document")
                .type(JsonFieldType.STRING),
            fieldWithPath("displayName")
                .description("Localized display name of the document, e.g. Terms & Conditions")
                .type(JsonFieldType.STRING),
            fieldWithPath("client")
                .description(
                    "Client to which the document applies to, " +
                        "valid values are: ${ClientSet.values().map { it.toString() }}")
                .type(JsonFieldType.STRING),
            fieldWithPath("type")
                .description(
                    "Type of document, valid values are: ${DocumentType.values().map { it.toString() }}")
                .type(JsonFieldType.STRING),
            fieldWithPath("country")
                .description("Country this document is for, e.g. DE")
                .type(JsonFieldType.STRING),
            fieldWithPath("locale")
                .description("Locale of the document, e.g. en")
                .type(JsonFieldType.STRING),
            fieldWithPath("url")
                .description(
                    "URL to the document, e.g. " +
                        "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/")
                .type(JsonFieldType.STRING),
            fieldWithPath("versions")
                .description("List of versions of this document")
                .type(JsonFieldType.ARRAY),
            fieldWithPath("versions[].identifier")
                .description("Identifier of the document version")
                .type(JsonFieldType.STRING),
            fieldWithPath("versions[].lastChanged")
                .description("Timestamp of the document version")
                .type(JsonFieldType.STRING),
        )
  }
}
