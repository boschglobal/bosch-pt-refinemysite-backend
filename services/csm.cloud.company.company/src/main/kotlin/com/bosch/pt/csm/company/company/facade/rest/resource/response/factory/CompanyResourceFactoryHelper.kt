/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.response.factory

import com.bosch.pt.csm.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.csm.common.model.ResourceReferenceAssembler.referTo
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.COMPANY_BY_COMPANY_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.PATH_VARIABLE_COMPANY_ID
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.PostBoxAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.StreetAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource.Companion.LINK_CAS
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource.Companion.LINK_CRS
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource.Companion.LINK_CSMS
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource.Companion.LINK_DELETE
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource.Companion.LINK_EMPLOYEES
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource.Companion.LINK_FMS
import com.bosch.pt.csm.company.company.query.CompanyQueryService
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH
import com.bosch.pt.csm.user.user.query.UserProjection
import com.bosch.pt.csm.user.user.query.UserQueryService
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Component
@Transactional(readOnly = true)
class CompanyResourceFactoryHelper(
    private val companyQueryService: CompanyQueryService,
    private val userQueryService: UserQueryService,
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  fun build(companies: List<Company>): List<CompanyResource> {
    if (companies.isEmpty()) {
      return emptyList()
    }

    val auditUser =
        companies
            .map { setOf(it.createdBy.get(), it.lastModifiedBy.get()) }
            .flatten()
            .toSet()
            .let { userQueryService.findAll(it) }
            .associateBy { it.id }

    return if (getCurrentUser().admin) {
      val companyIdentifiers = companies.map { it.identifier }
      val companiesWithEmployee =
          companyQueryService.findAllCompaniesWithEmployee(companyIdentifiers)
      companies.map {
        build(
            it,
            !companiesWithEmployee.contains(it.identifier),
            auditUser[it.createdBy.get()],
            auditUser[it.lastModifiedBy.get()])
      }
    } else {
      companies.map {
        build(it, false, auditUser[it.createdBy.get()], auditUser[it.lastModifiedBy.get()])
      }
    }
  }

  private fun build(
      company: Company,
      allowedToDelete: Boolean,
      createdBy: UserProjection?,
      lastModifiedBy: UserProjection?
  ): CompanyResource {
    Assert.notNull(company, "Company must be set")

    val streetAddressDto: StreetAddressDto? =
        if (company.streetAddress != null) {
          StreetAddressDto(company.streetAddress!!)
        } else null

    val postBoxAddressDto: PostBoxAddressDto? =
        if (company.postBoxAddress != null) {
          PostBoxAddressDto(company.postBoxAddress!!)
        } else null

    val resource =
        CompanyResource(
            id = company.identifier.toUuid(),
            version = company.version,
            createdDate = company.createdDate.get().toDate(),
            createdBy = referTo(createdBy, getDeletedUserReference()),
            lastModifiedDate = company.lastModifiedDate.get().toDate(),
            lastModifiedBy = referTo(lastModifiedBy, getDeletedUserReference()),
            name = company.name,
            streetAddress = streetAddressDto,
            postBoxAddress = postBoxAddressDto)

    // Add self reference
    resource.add(
        linkFactory
            .linkTo(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
            .withSelfRel())

    // Link to employees list
    resource.add(
        linkFactory
            .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
            .withRel(LINK_EMPLOYEES))

    // Link to employees list
    resource.add(
        linkFactory
            .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
            .withRel(LINK_CSMS))

    resource.add(
        linkFactory
            .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
            .withRel(LINK_FMS))

    resource.add(
        linkFactory
            .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
            .withRel(LINK_CRS))

    resource.add(
        linkFactory
            .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
            .withRel(LINK_CAS))

    // Add delete link if delete is possible
    if (allowedToDelete) {
      resource.add(
          linkFactory
              .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
              .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
              .withRel(LINK_DELETE))
    }

    return resource
  }
}
