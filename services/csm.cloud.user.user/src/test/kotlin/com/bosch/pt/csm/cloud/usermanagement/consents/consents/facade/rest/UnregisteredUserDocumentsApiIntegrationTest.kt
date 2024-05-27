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
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.DE
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.consents.document.Client
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.TERMS_AND_CONDITIONS
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.DocumentCreatedEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextKafkaEvent
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextLocalEventBus
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import java.net.URL
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UnregisteredUserDocumentsApiIntegrationTest : AbstractApiDocumentationTest() {

  @Autowired
  private lateinit var consentsEventStoreUtils: EventStoreUtils<ConsentsContextKafkaEvent>

  @Autowired private lateinit var consentsEventBus: ConsentsContextLocalEventBus

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitUser("user1") {
          it.registered = false
          it.userId = "daniel"
          it.email = "smartsiteapp+daniel@gmail.com"
          it.registered = false
          it.firstName = null
          it.lastName = null
          it.country = null
          it.locale = null
        }
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
              UserId(getIdentifier("admin"))),
          0)
    }

    consentsEventStoreUtils.reset()
  }

  @Test
  fun `GET is accessible without a token (not authenticated)`() {
    SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext())

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/unregistered/documents")))
                .queryParam("client", "WEB")
                .queryParam("country", "DE")
                .queryParam("locale", "de_DE"))
        .andExpectAll(status().isOk)

    consentsEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `GET returns latest version of terms and conditions for country and locale`() {
    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf("/users/unregistered/documents")))
                .queryParam("client", "WEB")
                .queryParam("country", "DE")
                .queryParam("locale", "de_DE"))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
              {
                "items": [
                  {
                    "id": "47104561-4bab-4bfa-9787-5cc6454bffc7",
                    "displayName": "Terms & Conditions",
                    "url": "https://www.bosch-pt.com/ptlegalpages/de/ptde/en/refinemysiteweb/termsofuse/",
                    "type": "TERMS_AND_CONDITIONS"
                  }
                ]
              }
          """
                        .trimIndent()))
        .andDo(
            document(
                "documents/document-get-signup-documents",
                preprocessResponse(
                    prettyPrint(), modifyHeaders().remove(HttpHeaders.ACCEPT_LANGUAGE)),
                queryParameters(DOCUMENT_QUERY_PARAMETER_DESCRIPTORS),
                responseFields(DOCUMENT_RESPONSE_FIELDS),
            ))

    consentsEventStoreUtils.verifyEmpty()
  }

  companion object {
    private val DOCUMENT_QUERY_PARAMETER_DESCRIPTORS =
        listOf(
            parameterWithName("country").description("Country to request documents for"),
            parameterWithName("locale").description("Language of documents"),
            parameterWithName("client")
                .description(
                    "Client the request is executed from, " +
                        "valid values are: ${Client.values().map { it.toString() }}"),
        )

    private val DOCUMENT_RESPONSE_FIELDS =
        listOf(
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
            fieldWithPath("items[].type")
                .description(
                    "Type of document, valid values are: ${DocumentType.values().map { it.toString() }}")
                .type(JsonFieldType.STRING),
        )
  }
}
