/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompanyWithStreetAddress
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.common.facade.rest.ETag
import com.bosch.pt.csm.common.i18n.Key
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_EXIST_COMPANY_EMPLOYEE
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.company.facade.rest.resource.CreateCompanyResourceBuilder
import com.bosch.pt.csm.company.company.facade.rest.resource.PostBoxAddressDtoBuilder
import com.bosch.pt.csm.company.company.facade.rest.resource.StreetAddressDtoBuilder
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.AbstractAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.PostBoxAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.StreetAddressDto
import java.util.Optional
import java.util.UUID.randomUUID
import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK

class CompanyApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var cut: CompanyController

  @BeforeEach
  fun setup() {
    setAuthentication("admin")
  }

  @Test
  fun `verify create company fails with invalid street address country`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withStreetAddress(
                StreetAddressDtoBuilder.streetAddress().withCountry("invalid").build())
            .build()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createCompany(CompanyId(randomUUID()), saveCompanyResource) }
        .withMessage(COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME)
  }

  @Test
  fun `verify create company fails with invalid postbox address country`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withPostBoxAddress(
                PostBoxAddressDtoBuilder.postBoxAddress().withCountry("invalid").build())
            .build()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createCompany(CompanyId(randomUUID()), saveCompanyResource) }
        .withMessage(COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME)
  }

  @Test
  fun `verify create company fails with empty name`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("")
            .withPostBoxAddress(PostBoxAddressDtoBuilder.postBoxAddress().build())
            .withStreetAddress(StreetAddressDtoBuilder.streetAddress().build())
            .build()

    assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
      cut.createCompany(CompanyId(randomUUID()), saveCompanyResource)
    }
  }

  @Test
  fun `verify create company fails with missing addresses`() {
    val saveCompanyResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withPostBoxAddress(null)
            .withStreetAddress(null)
            .build()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createCompany(CompanyId(randomUUID()), saveCompanyResource) }
        .withMessage(Key.COMPANY_VALIDATION_ERROR_MISSING_ADDRESS)
  }

  @Test
  fun `verify update company with addresses and no change`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.streetAddress = createCustomStreetAddress()
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withPostBoxAddress(createCustomPostBoxAddressDto())
            .withStreetAddress(createCustomStreetAddressDto())
            .build()

    cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0")).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.also { body ->
        assertThat(body.id).isEqualTo(company.getIdentifier())
        assertThat(body.version).isEqualTo(0L)
      }
    }
  }

  @Test
  fun `verify update company removing postbox address`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.streetAddress = createCustomStreetAddress()
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withStreetAddress(createCustomStreetAddressDto())
            .build()

    cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0")).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.also { body ->
        assertThat(body.id).isEqualTo(company.getIdentifier())
        assertThat(body.version).isEqualTo(1L)
        assertThat(body.postBoxAddress).isNull()
        assertThatStreetAddressIsEquals(body.streetAddress!!, updateResource.streetAddress!!)
      }
    }
  }

  @Test
  fun `verify update company removing street address`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.streetAddress = createCustomStreetAddress()
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withPostBoxAddress(createCustomPostBoxAddressDto())
            .build()

    cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0")).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.also { body ->
        assertThat(body.id).isEqualTo(company.getIdentifier())
        assertThat(body.version).isEqualTo(1L)
        assertThat(body.streetAddress).isNull()
        assertThatPostBoxAddressIsEquals(body.postBoxAddress!!, updateResource.postBoxAddress!!)
      }
    }
  }

  @Test
  fun `verify update company without postbox address and no change`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.streetAddress = createCustomStreetAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withStreetAddress(createCustomStreetAddressDto())
            .build()

    cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0")).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.also { body ->
        assertThat(body.id).isEqualTo(company.getIdentifier())
        assertThat(body.version).isEqualTo(0L)
      }
    }
  }

  @Test
  fun `verify update company without street address and no change`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withPostBoxAddress(createCustomPostBoxAddressDto())
            .build()

    cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0")).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.also { body ->
        assertThat(body.id).isEqualTo(company.getIdentifier())
        assertThat(body.version).isEqualTo(0L)
      }
    }
  }

  @Test
  fun `verify update company fails with invalid street address`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.streetAddress = createCustomStreetAddress()
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withStreetAddress(
                StreetAddressDtoBuilder.streetAddress().withCountry("invalid").build())
            .withPostBoxAddress(createCustomPostBoxAddressDto())
            .build()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0"))
        }
        .withMessage(COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME)
  }

  @Test
  fun `verify update company fails with invalid post box address`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.streetAddress = createCustomStreetAddress()
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource()
            .withName("company")
            .withStreetAddress(createCustomStreetAddressDto())
            .withPostBoxAddress(
                PostBoxAddressDtoBuilder.postBoxAddress().withCountry("invalid").build())
            .build()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0"))
        }
        .withMessage(COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME)
  }

  @Test
  fun `verify update company fails without addresses`() {
    eventStreamGenerator.submitCompany("company") {
      it.name = "company"
      it.streetAddress = createCustomStreetAddress()
      it.postBoxAddress = createCustomPostBoxAddress()
    }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    val updateResource =
        CreateCompanyResourceBuilder.createCompanyResource().withName("company").build()

    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.updateCompany(company.getIdentifier().asCompanyId(), updateResource, ETag.from("0"))
    }
  }

  @Test
  fun `verify find company by id without delete link due to existing employees`() {
    eventStreamGenerator.submitUser("company_user").submitCompany("company").submitEmployee(
        "company_employee") { it.roles = listOf(CR) }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    cut.findCompanyByIdentifier(company.getIdentifier()).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.also { body ->
        assertThat(body.id).isEqualTo(company.getIdentifier())
        assertThat(body.name).isEqualTo(company.name)
        assertThat(body.getLink("delete")).isEqualTo(Optional.empty<Any>())
      }
    }
  }

  @Test
  fun `verify find company not found`() {
    cut.findCompanyByIdentifier(randomUUID()).also {
      assertThat(it.statusCode).isEqualTo(NOT_FOUND)
    }
  }

  @Test
  fun `verify delete company not found`() {
    assertThat(
            catchThrowableOfType(
                    { cut.deleteCompany(CompanyId(randomUUID())) },
                    AggregateNotFoundException::class.java)
                .messageKey)
        .isEqualTo(COMPANY_VALIDATION_ERROR_NOT_FOUND)
  }

  @Test
  fun `verify delete company fails due to existing employees`() {
    eventStreamGenerator
        .submitUser("company_user")
        .submitCompanyWithStreetAddress("company")
        .submitEmployee("company_employee") { it.roles = listOf(FM) }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    assertThat(
            catchThrowableOfType(
                    { cut.deleteCompany(company.getIdentifier().asCompanyId()) },
                    PreconditionViolationException::class.java)
                .messageKey)
        .isEqualTo(COMPANY_VALIDATION_ERROR_EXIST_COMPANY_EMPLOYEE)
  }

  private fun assertThatPostBoxAddressIsEquals(
      response: PostBoxAddressDto,
      update: PostBoxAddressDto
  ) {
    assertThatGeneralAddressIsEquals(response, update)
    assertThat(response.postBox).isEqualTo(update.postBox)
  }

  private fun assertThatStreetAddressIsEquals(
      response: StreetAddressDto,
      update: StreetAddressDto
  ) {
    assertThatGeneralAddressIsEquals(response, update)
    assertThat(response.street).isEqualTo(update.street)
    assertThat(response.houseNumber).isEqualTo(update.houseNumber)
  }

  private fun assertThatGeneralAddressIsEquals(
      response: AbstractAddressDto,
      update: AbstractAddressDto
  ) {
    assertThat(response.area).isEqualTo(update.area)
    assertThat(response.city).isEqualTo(update.city)
    assertThat(response.country).isEqualTo(update.country)
    assertThat(response.zipCode).isEqualTo(update.zipCode)
  }

  companion object {
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

    private fun createCustomPostBoxAddressDto() =
        PostBoxAddressDtoBuilder.postBoxAddress()
            .withZipCode(ADDRESS_ZIPCODE)
            .withCity(ADDRESS_CITY)
            .withArea(ADDRESS_AREA)
            .withCountry(ADDRESS_COUNTRY)
            .withPostBox(ADDRESS_POBOX)
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

    private fun createCustomStreetAddressDto() =
        StreetAddressDtoBuilder.streetAddress()
            .withZipCode(ADDRESS_ZIPCODE)
            .withCity(ADDRESS_CITY)
            .withArea(ADDRESS_AREA)
            .withCountry(ADDRESS_COUNTRY)
            .withStreet(ADDRESS_STREETNAME)
            .withHouseNumber(ADDRESS_HOUSE_NUMBER)
            .build()
  }
}
