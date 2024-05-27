/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.facade.rest

import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.csm.common.facade.rest.ApiDocumentationSnippets.ID_AND_VERSION_FIELD_DESCRIPTORS
import com.bosch.pt.csm.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.csm.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.common.facade.rest.resource.request.SuggestionResource
import com.bosch.pt.csm.common.i18n.Key
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.company.query.CompanyQueryService
import com.bosch.pt.csm.company.company.query.CompanyQueryService.Companion.SEARCH_COMPANIES_ALLOWED_SORTING_PROPERTIES
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.COMPANIES_ENDPOINT_PATH
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.COMPANIES_SEARCH_ENDPOINT_PATH
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.COMPANY_BY_COMPANY_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.COMPANY_SUGGESTIONS_ENDPOINT_PATH
import com.bosch.pt.csm.company.company.facade.rest.resource.CreateCompanyResourceBuilder
import com.bosch.pt.csm.company.company.facade.rest.resource.PostBoxAddressDtoBuilder
import com.bosch.pt.csm.company.company.facade.rest.resource.StreetAddressDtoBuilder
import com.bosch.pt.csm.company.company.facade.rest.resource.request.FilterCompanyListResource
import com.bosch.pt.csm.company.company.facade.rest.resource.request.SaveCompanyResource
import com.google.common.net.HttpHeaders.ETAG
import com.google.common.net.HttpHeaders.LOCATION
import java.util.Locale
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.NEXT
import org.springframework.hateoas.IanaLinkRelations.PREV
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.IF_MATCH
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
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Attributes
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Test and Document Company API")
class CompanyApiDocumentationTest : AbstractApiDocumentationTest() {

  @BeforeEach
  fun setup() {
    setAuthentication("admin")
    eventStreamGenerator.submitCompany("company") {
      it.name = "ACME LTD"
      it.streetAddress = createCustomStreetAddress()
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    companyEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create company with both addresses`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("Company 1")
            .withPostBoxAddress(PostBoxAddressDtoBuilder.postBoxAddress().build())
            .withStreetAddress(StreetAddressDtoBuilder.streetAddress().build())
            .build()

    val expectedUrl = latestVersionOf(COMPANIES_ENDPOINT_PATH) + "/"

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(COMPANIES_ENDPOINT_PATH)), saveCompanyResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, containsString(expectedUrl)))
        .andExpect { result: MvcResult -> jsonPath("version").value(0).match(result) }
        .andExpectAll(
            jsonPath("id").isNotEmpty,
            jsonPath("name").value("Company 1"),
            jsonPath("streetAddress.zipCode").value(ADDRESS_ZIPCODE),
            jsonPath("streetAddress.city").value(ADDRESS_CITY),
            jsonPath("streetAddress.area").value(ADDRESS_AREA),
            jsonPath("streetAddress.country").value(ADDRESS_COUNTRY),
            jsonPath("streetAddress.street").value(ADDRESS_STREETNAME),
            jsonPath("streetAddress.houseNumber").value(ADDRESS_HOUSE_NUMBER),
            jsonPath("postBoxAddress.zipCode").value(ADDRESS_ZIPCODE),
            jsonPath("postBoxAddress.city").value(ADDRESS_CITY),
            jsonPath("postBoxAddress.area").value(ADDRESS_AREA),
            jsonPath("postBoxAddress.country").value(ADDRESS_COUNTRY),
            jsonPath("postBoxAddress.postBox").value(ADDRESS_POBOX))
        .andDo(
            document(
                "companies/document-create-company",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(COMPANY_REQUEST_FIELDS),
                responseHeaders(LOCATION_HEADER_DESCRIPTOR),
                responseFields(COMPANY_RESPONSE_FIELD_DESCRIPTORS),
                links(COMPANY_LINK_DESCRIPTORS)))

    companyEventStoreUtils
        .verifyContainsAndGet(CompanyEventAvro::class.java, CompanyEventEnumAvro.CREATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier).isNotEmpty
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(0)
            assertThat(aggregate.name).isEqualTo("Company 1")
            aggregate.streetAddress.also { address ->
              assertThat(address.zipCode).isEqualTo(ADDRESS_ZIPCODE)
              assertThat(address.city).isEqualTo(ADDRESS_CITY)
              assertThat(address.area).isEqualTo(ADDRESS_AREA)
              assertThat(address.country).isEqualTo(ADDRESS_COUNTRY)
              assertThat(address.street).isEqualTo(ADDRESS_STREETNAME)
              assertThat(address.houseNumber).isEqualTo(ADDRESS_HOUSE_NUMBER)
            }
            aggregate.postBoxAddress.also { address ->
              assertThat(address.zipCode).isEqualTo(ADDRESS_ZIPCODE)
              assertThat(address.city).isEqualTo(ADDRESS_CITY)
              assertThat(address.area).isEqualTo(ADDRESS_AREA)
              assertThat(address.country).isEqualTo(ADDRESS_COUNTRY)
              assertThat(address.postBox).isEqualTo(ADDRESS_POBOX)
            }
          }
        }
  }

  @Test
  fun `verify and document create company with both addresses and identifier`() {
    val companyIdentifier = UUID.randomUUID()
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("Company 1")
            .withPostBoxAddress(PostBoxAddressDtoBuilder.postBoxAddress().build())
            .withStreetAddress(StreetAddressDtoBuilder.streetAddress().build())
            .build()

    val expectedUrl = latestVersionOf(COMPANIES_ENDPOINT_PATH) + "/" + companyIdentifier

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH), companyIdentifier),
                saveCompanyResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, containsString(expectedUrl)))
        .andExpect { result: MvcResult -> jsonPath("version").value(0).match(result) }
        .andExpectAll(
            jsonPath("id").value(companyIdentifier.toString()),
            jsonPath("name").value("Company 1"),
            jsonPath("streetAddress.zipCode").value(ADDRESS_ZIPCODE),
            jsonPath("streetAddress.city").value(ADDRESS_CITY),
            jsonPath("streetAddress.area").value(ADDRESS_AREA),
            jsonPath("streetAddress.country").value(ADDRESS_COUNTRY),
            jsonPath("streetAddress.street").value(ADDRESS_STREETNAME),
            jsonPath("streetAddress.houseNumber").value(ADDRESS_HOUSE_NUMBER),
            jsonPath("postBoxAddress.zipCode").value(ADDRESS_ZIPCODE),
            jsonPath("postBoxAddress.city").value(ADDRESS_CITY),
            jsonPath("postBoxAddress.area").value(ADDRESS_AREA),
            jsonPath("postBoxAddress.country").value(ADDRESS_COUNTRY),
            jsonPath("postBoxAddress.postBox").value(ADDRESS_POBOX),
        )
        .andDo(
            document(
                "companies/document-create-company-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(COMPANY_REQUEST_FIELDS),
                pathParameters(parameterWithName("companyId").description("ID of the company")),
                responseHeaders(LOCATION_HEADER_DESCRIPTOR),
                responseFields(COMPANY_RESPONSE_FIELD_DESCRIPTORS),
                links(COMPANY_LINK_DESCRIPTORS)))

    companyEventStoreUtils
        .verifyContainsAndGet(CompanyEventAvro::class.java, CompanyEventEnumAvro.CREATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier)
                .isEqualTo(companyIdentifier.toString())
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(0)
            assertThat(aggregate.name).isEqualTo("Company 1")
            aggregate.streetAddress.also { address ->
              assertThat(address.zipCode).isEqualTo(ADDRESS_ZIPCODE)
              assertThat(address.city).isEqualTo(ADDRESS_CITY)
              assertThat(address.area).isEqualTo(ADDRESS_AREA)
              assertThat(address.country).isEqualTo(ADDRESS_COUNTRY)
              assertThat(address.street).isEqualTo(ADDRESS_STREETNAME)
              assertThat(address.houseNumber).isEqualTo(ADDRESS_HOUSE_NUMBER)
            }
            aggregate.postBoxAddress.also { address ->
              assertThat(address.zipCode).isEqualTo(ADDRESS_ZIPCODE)
              assertThat(address.city).isEqualTo(ADDRESS_CITY)
              assertThat(address.area).isEqualTo(ADDRESS_AREA)
              assertThat(address.country).isEqualTo(ADDRESS_COUNTRY)
              assertThat(address.postBox).isEqualTo(ADDRESS_POBOX)
            }
          }
        }
  }

  @Test
  fun `verify create company with postbox address`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("Company 1")
            .withPostBoxAddress(PostBoxAddressDtoBuilder.postBoxAddress().build())
            .build()

    val expectedUrl = latestVersionOf(COMPANIES_ENDPOINT_PATH) + "/"

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(COMPANIES_ENDPOINT_PATH)), saveCompanyResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, containsString(expectedUrl)))
        .andExpect { result: MvcResult -> jsonPath("version").value(0).match(result) }
        .andExpectAll(
            jsonPath("id").isNotEmpty,
            jsonPath("name").value("Company 1"),
            jsonPath("streetAddress").doesNotExist(),
            jsonPath("postBoxAddress.zipCode").value(ADDRESS_ZIPCODE),
            jsonPath("postBoxAddress.city").value(ADDRESS_CITY),
            jsonPath("postBoxAddress.area").value(ADDRESS_AREA),
            jsonPath("postBoxAddress.country").value(ADDRESS_COUNTRY),
            jsonPath("postBoxAddress.postBox").value(ADDRESS_POBOX))

    companyEventStoreUtils
        .verifyContainsAndGet(CompanyEventAvro::class.java, CompanyEventEnumAvro.CREATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier).isNotEmpty
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(0)
            assertThat(aggregate.name).isEqualTo("Company 1")
            assertThat(aggregate.streetAddress).isNull()
            aggregate.postBoxAddress.also { address ->
              assertThat(address.zipCode).isEqualTo(ADDRESS_ZIPCODE)
              assertThat(address.city).isEqualTo(ADDRESS_CITY)
              assertThat(address.area).isEqualTo(ADDRESS_AREA)
              assertThat(address.country).isEqualTo(ADDRESS_COUNTRY)
              assertThat(address.postBox).isEqualTo(ADDRESS_POBOX)
            }
          }
        }
  }

  @Test
  fun `verify create company with street address`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("Company 1")
            .withStreetAddress(StreetAddressDtoBuilder.streetAddress().build())
            .build()

    val expectedUrl = latestVersionOf(COMPANIES_ENDPOINT_PATH) + "/"

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(COMPANIES_ENDPOINT_PATH)), saveCompanyResource))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, containsString(expectedUrl)))
        .andExpect { result: MvcResult -> jsonPath("version").value(0).match(result) }
        .andExpectAll(
            jsonPath("id").isNotEmpty,
            jsonPath("name").value("Company 1"),
            jsonPath("streetAddress.zipCode").value(ADDRESS_ZIPCODE),
            jsonPath("streetAddress.city").value(ADDRESS_CITY),
            jsonPath("streetAddress.area").value(ADDRESS_AREA),
            jsonPath("streetAddress.country").value(ADDRESS_COUNTRY),
            jsonPath("streetAddress.street").value(ADDRESS_STREETNAME),
            jsonPath("streetAddress.houseNumber").value(ADDRESS_HOUSE_NUMBER),
            jsonPath("postBoxAddress").doesNotExist(),
        )

    companyEventStoreUtils
        .verifyContainsAndGet(CompanyEventAvro::class.java, CompanyEventEnumAvro.CREATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier).isNotEmpty
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(0)
            assertThat(aggregate.name).isEqualTo("Company 1")
            aggregate.streetAddress.also { address ->
              assertThat(address.zipCode).isEqualTo(ADDRESS_ZIPCODE)
              assertThat(address.city).isEqualTo(ADDRESS_CITY)
              assertThat(address.area).isEqualTo(ADDRESS_AREA)
              assertThat(address.country).isEqualTo(ADDRESS_COUNTRY)
              assertThat(address.street).isEqualTo(ADDRESS_STREETNAME)
              assertThat(address.houseNumber).isEqualTo(ADDRESS_HOUSE_NUMBER)
            }
            assertThat(aggregate.postBoxAddress).isNull()
          }
        }
  }

  @Test
  fun `verify create company without addresses`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource().withName("Company 1").build()

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(COMPANIES_ENDPOINT_PATH)), saveCompanyResource))
        .andExpect(status().isBadRequest)
        .andExpect(
            jsonPath("message")
                .value(
                    messageSource.getMessage(
                        Key.COMPANY_VALIDATION_ERROR_MISSING_ADDRESS, arrayOf(), Locale.ENGLISH)))

    companyEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document search companies`() {
    val filter = FilterCompanyListResource("Nu")

    eventStreamGenerator
        .submitCompany("company 2") { it.name = "company 2" }
        .submitCompany("company 3") { it.name = "company 3" }
        .submitCompany("company number") { it.name = "company number" }
        .submitCompany("company Numeric") { it.name = "company Numeric" }
        .submitCompany("company num") { it.name = "company num" }
        .submitCompany("company numb") { it.name = "company numb" }
        .submitCompany("company null") { it.name = "company null" }

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(COMPANIES_SEARCH_ENDPOINT_PATH)), filter)
                .param("size", "2")
                .param("page", "1"))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("items").isArray,
            jsonPath("items.length()").value(2),
            jsonPath("items[0].name").value("company numb"),
            jsonPath("items[1].name").value("company number"))
        .andDo(
            document(
                "companies/document-search-companies",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(SEARCH_COMPANIES_REQUEST_FIELD_DESCRIPTORS),
                sortingAndPagingRequestParameter(SEARCH_COMPANIES_ALLOWED_SORTING_PROPERTIES.keys),
                buildPagedItemsListResponseFields(COMPANIES_RESPONSE_FIELD_DESCRIPTORS),
                links(
                    linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
                    linkWithRel(PREV.value()).description(LINK_PREVIOUS_DESCRIPTION),
                    linkWithRel(NEXT.value()).description(LINK_NEXT_DESCRIPTION))))
  }

  @Test
  fun `verify and document get companies`() {
    eventStreamGenerator
        .submitCompany("company 2") { it.name = "company 2" }
        .submitCompany("company 3") { it.name = "company 3" }
        .submitCompany("company 4") { it.name = "company 4" }
        .submitCompany("company 5") { it.name = "company 5" }
        .submitCompany("company 6") { it.name = "company 6" }
        .submitCompany("company 7") { it.name = "company 7" }

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOf(COMPANIES_ENDPOINT_PATH)))
                .param("size", "3")
                .param("page", "1"))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpectAll(
            jsonPath("items").isArray,
            jsonPath("items.length()").value(3),
            jsonPath("items[0].name").value("company 4"),
            jsonPath("items[1].name").value("company 5"),
            jsonPath("items[2].name").value("company 6"))
        .andDo(
            document(
                "companies/document-get-companies",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                sortingAndPagingRequestParameter(SEARCH_COMPANIES_ALLOWED_SORTING_PROPERTIES.keys),
                buildPagedItemsListResponseFields(COMPANIES_RESPONSE_FIELD_DESCRIPTORS),
                links(
                    linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
                    linkWithRel(PREV.value()).description(LINK_PREVIOUS_DESCRIPTION),
                    linkWithRel(NEXT.value()).description(LINK_NEXT_DESCRIPTION))))
  }

  @Test
  fun `verify and document get company by id`() {
    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH), company.identifier)))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpect(jsonPath("name").value(company.name))
        .andDo(
            document(
                "companies/document-get-company",
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("companyId").description("ID of the company")),
                responseFields(COMPANY_RESPONSE_FIELD_DESCRIPTORS),
                links(COMPANY_LINK_DESCRIPTORS)))
  }

  @Test
  fun `verify and document get company by id not found`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH), UUID.randomUUID())))
        .andExpect(status().isNotFound)
        .andDo(document("companies/document-get-company-not-found"))
  }

  @Test
  fun `verify and document update company`() {
    val updateCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("Company 1")
            .withPostBoxAddress(PostBoxAddressDtoBuilder.postBoxAddress().build())
            .withStreetAddress(StreetAddressDtoBuilder.streetAddress().build())
            .build()

    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH), company.identifier),
                updateCompanyResource,
                0L))
        .andExpect(status().isOk)
        .andExpect(header().exists(ETAG))
        .andDo(
            document(
                "companies/document-update-company",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(COMPANY_REQUEST_FIELDS),
                pathParameters(parameterWithName("companyId").description("ID of the company")),
                requestHeaders(
                    headerWithName(IF_MATCH)
                        .description(
                            "Mandatory entity tag of the company to be updated (previously received value of the " +
                                "response header field `ETag`)")),
                responseHeaders(
                    headerWithName(org.springframework.http.HttpHeaders.ETAG)
                        .description(
                            "Entity tag of the updated company, needed for possible further updates of the task")),
                responseFields(COMPANY_RESPONSE_FIELD_DESCRIPTORS),
                links(COMPANY_LINK_DESCRIPTORS)))

    companyEventStoreUtils
        .verifyContainsAndGet(CompanyEventAvro::class.java, CompanyEventEnumAvro.UPDATED, 1)
        .also {
          it.first().aggregate.also { aggregate ->
            assertThat(aggregate.aggregateIdentifier.identifier).isNotEmpty
            assertThat(aggregate.aggregateIdentifier.version).isEqualTo(1)
            assertThat(aggregate.name).isEqualTo("Company 1")
          }
        }
  }

  @Test
  fun `verify and document delete company`() {
    val company =
        repositories.companyRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("company").asCompanyId())!!

    mockMvc
        .perform(
            requestBuilder(
                delete(latestVersionOf(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH), company.identifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "companies/document-delete-company",
                pathParameters(parameterWithName("companyId").description("ID of the company"))))

    companyEventStoreUtils.verifyContains(
        CompanyEventAvro::class.java, CompanyEventEnumAvro.DELETED, 1)
  }

  @Test
  fun `verify and document suggest companies`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(COMPANY_SUGGESTIONS_ENDPOINT_PATH)),
                SuggestionResource("ACME")))
        .andExpect(status().isOk)
        .andDo(
            document(
                "companies/document-suggest-companies",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(COMPANY_SUGGESTIONS_REQUEST_FIELD_DESCRIPTORS),
                sortingAndPagingRequestParameter(
                    CompanyQueryService.SUGGEST_COMPANIES_ALLOWED_SORTING_PROPERTIES.keys),
                buildPagedItemsListResponseFields(COMPANY_SUGGESTIONS_RESPONSE_FIELDS_DESCRIPTORS),
                links(COMPANY_SUGGESTIONS_LINK_DESCRIPTORS)))
  }

  companion object {

    private const val LINK_SELF_DESCRIPTION = "Link to the company resource itself"

    private const val LINK_NEXT_DESCRIPTION = "Link to the next company page"

    private const val LINK_PREVIOUS_DESCRIPTION = "Link to the previous company page"

    private val COMPANY_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION),
            linkWithRel("employees").description("Link to get all employees of the company"),
            linkWithRel("foremen")
                .description("Link to get all employees with role 'FM' (foreman)"),
            linkWithRel("constructionsitemanagers")
                .description(
                    "Link to get all employees with role 'CSM' (construction site manager)"),
            linkWithRel("administrator")
                .description("Link to get all employees with role 'CA' (Company administrator)"),
            linkWithRel("representative")
                .description("Link to get all employees with role 'CR' (representative)"),
            linkWithRel("delete").description("Link to delete the company"))

    private val field = ConstrainedFields(SaveCompanyResource::class.java)

    private val COMPANY_REQUEST_FIELDS =
        listOf(
            field.withPath("name").description("Name of the company").type(JsonFieldType.STRING),
            subsectionWithPath("streetAddress")
                .optional()
                .description("Street address of the company")
                .attributes(
                    Attributes.key("constraints")
                        .value(
                            "Either the street address or the post box address or both must be provided"))
                .type(JsonFieldType.OBJECT),
            fieldWithPath("streetAddress.street")
                .description("Street name")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Mandatory field. Size must be between 1 and 100 inclusive")),
            fieldWithPath("streetAddress.houseNumber")
                .description("House number")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Mandatory field. Size must be between 1 and 10 inclusive")),
            fieldWithPath("streetAddress.city")
                .description("City")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Size must be between 1 and 100 inclusive")),
            fieldWithPath("streetAddress.zipCode")
                .description("Zip-code")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints").value("Size must be between 1 and 10 inclusive")),
            fieldWithPath("streetAddress.area")
                .description("Area")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Size must be between 1 and 100 inclusive")),
            fieldWithPath("streetAddress.country")
                .description("Country")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Size must be between 1 and 100 inclusive")),
            subsectionWithPath("postBoxAddress")
                .optional()
                .description("Post office box of the company")
                .attributes(
                    Attributes.key("constraints")
                        .value(
                            "Either the street address or the post box address or both must be provided"))
                .type(JsonFieldType.OBJECT),
            fieldWithPath("postBoxAddress.postBox")
                .description("Postbox name")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Size must be between 1 and 100 inclusive")),
            fieldWithPath("postBoxAddress.city")
                .description("City")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Size must be between 1 and 100 inclusive")),
            fieldWithPath("postBoxAddress.zipCode")
                .description("Zip-code")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints").value("Size must be between 1 and 10 inclusive")),
            fieldWithPath("postBoxAddress.area")
                .description("Area")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Size must be between 1 and 100 inclusive")),
            fieldWithPath("postBoxAddress.country")
                .description("Country")
                .type(JsonFieldType.STRING)
                .attributes(
                    Attributes.key("constraints")
                        .value("Size must be between 1 and 100 inclusive")))

    private val COMPANY_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ID_AND_VERSION_FIELD_DESCRIPTORS,
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("name").description("Name of the company"),
            fieldWithPath("streetAddress").description("Street address of the company (optional)"),
            fieldWithPath("streetAddress.zipCode")
                .description("Zip code of the company's street address (max. 10 digits)"),
            fieldWithPath("streetAddress.city").description("City of the company's street address"),
            fieldWithPath("streetAddress.area")
                .description("Area of the company's street address (optional)"),
            fieldWithPath("streetAddress.country")
                .description("Country of the company's street address"),
            fieldWithPath("streetAddress.street")
                .description("Street of the company's street address"),
            fieldWithPath("streetAddress.houseNumber")
                .description("House number of the company's street address (max. 10 digits)"),
            fieldWithPath("postBoxAddress")
                .description("Post office box address of the company (optional)"),
            fieldWithPath("postBoxAddress.zipCode")
                .description("Zip code of the company's post office box (max. 10 digits)"),
            fieldWithPath("postBoxAddress.city")
                .description("City of the  company's post office box address"),
            fieldWithPath("postBoxAddress.area")
                .description("Area of the company's post office box address (optional)"),
            fieldWithPath("postBoxAddress.country")
                .description("Country of the company's post office box address"),
            fieldWithPath("postBoxAddress.postBox")
                .description("Post office box of the company's post office address"),
            subsectionWithPath("_links").ignored())

    private val searchFields = ConstrainedFields(FilterCompanyListResource::class.java)

    private val SEARCH_COMPANIES_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            searchFields
                .withPath("name")
                .optional()
                .description("Name of the company")
                .type(JsonFieldType.STRING))

    private val COMPANIES_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ID_AND_VERSION_FIELD_DESCRIPTORS,
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("name").description("Name of the company"),
            subsectionWithPath("_links").ignored())

    private val suggestFields = ConstrainedFields(SuggestionResource::class.java)

    private val COMPANY_SUGGESTIONS_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            suggestFields
                .withPath("term")
                .optional()
                .description(
                    "Term used to suggest companies. A \"starts-with\" logic is applied " +
                        "against the names of the companies")
                .type(JsonFieldType.STRING))

    private val COMPANY_SUGGESTIONS_RESPONSE_FIELDS_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("ID of the company"),
            fieldWithPath("displayName").description("Name of the company"))

    private val COMPANY_SUGGESTIONS_LINK_DESCRIPTORS =
        listOf(linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION))

    private const val ADDRESS_AREA = "Baden Württemberg"

    private const val ADDRESS_CITY = "Leinfelden-Echterdingen"

    private const val ADDRESS_COUNTRY = "Germany"

    private const val ADDRESS_HOUSE_NUMBER = "40-46"

    private const val ADDRESS_POBOX = "10 01 56"

    private const val ADDRESS_STREETNAME = "Max-Lang-Straße"

    private const val ADDRESS_ZIPCODE = "70745"

    private fun createCustomPostBoxAddress() =
        PostBoxAddressAvro.newBuilder()
            .apply {
              zipCode = ADDRESS_ZIPCODE
              city = ADDRESS_CITY
              area = ADDRESS_AREA
              country = ADDRESS_COUNTRY
              postBox = ADDRESS_POBOX
            }
            .build()

    private fun createCustomStreetAddress() =
        StreetAddressAvro.newBuilder()
            .apply {
              zipCode = ADDRESS_ZIPCODE
              city = ADDRESS_CITY
              area = ADDRESS_AREA
              country = ADDRESS_COUNTRY
              street = ADDRESS_STREETNAME
              houseNumber = ADDRESS_HOUSE_NUMBER
            }
            .build()
  }
}
