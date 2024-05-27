/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.common.facade.rest.ETag
import com.bosch.pt.csm.common.facade.rest.resource.request.SuggestionResource
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.company.command.api.DeleteCompanyCommand
import com.bosch.pt.csm.company.company.command.handler.CreateCompanyCommandHandler
import com.bosch.pt.csm.company.company.command.handler.DeleteCompanyCommandHandler
import com.bosch.pt.csm.company.company.command.handler.UpdateCompanyCommandHandler
import com.bosch.pt.csm.company.company.facade.rest.resource.request.FilterCompanyListResource
import com.bosch.pt.csm.company.company.facade.rest.resource.request.SaveCompanyResource
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource
import com.bosch.pt.csm.company.company.facade.rest.resource.response.factory.CompanyListResourceFactory
import com.bosch.pt.csm.company.company.facade.rest.resource.response.factory.CompanyResourceFactory
import com.bosch.pt.csm.company.company.facade.rest.resource.response.factory.CompanySuggestionsResourceFactory
import com.bosch.pt.csm.company.company.query.CompanyQueryService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import java.util.UUID
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@RestController
class CompanyController(
    private val companyQueryService: CompanyQueryService,
    private val createCompanyCommandHandler: CreateCompanyCommandHandler,
    private val updateCompanyCommandHandler: UpdateCompanyCommandHandler,
    private val deleteCompanyCommandHandler: DeleteCompanyCommandHandler,
    private val companyListResourceFactory: CompanyListResourceFactory,
    private val companyResourceFactory: CompanyResourceFactory,
    private val companySuggestionsResourceFactory: CompanySuggestionsResourceFactory
) {

  @PostMapping(COMPANIES_ENDPOINT_PATH, COMPANY_BY_COMPANY_ID_ENDPOINT_PATH)
  fun createCompany(
      @PathVariable(value = PATH_VARIABLE_COMPANY_ID, required = false) companyId: CompanyId?,
      @RequestBody body: @Valid SaveCompanyResource
  ): ResponseEntity<CompanyResource> {
    val companyIdentifier = createCompanyCommandHandler.handle(body.toCommand(companyId))
    val company = companyQueryService.findCompanyWithDetailsByIdentifier(companyIdentifier)!!

    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix() + COMPANY_BY_COMPANY_ID_ENDPOINT_PATH)
            .buildAndExpand(companyIdentifier)
            .toUri()

    return ResponseEntity.created(location).body(companyResourceFactory.build(company))
  }

  @PutMapping(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH)
  fun updateCompany(
      @PathVariable(PATH_VARIABLE_COMPANY_ID) identifier: CompanyId,
      @RequestBody body: @Valid SaveCompanyResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") etag: ETag
  ): ResponseEntity<CompanyResource> {
    updateCompanyCommandHandler.handle(body.toCommand(identifier, etag))
    val company = companyQueryService.findCompanyWithDetailsByIdentifier(identifier)
    return ResponseEntity.ok()
        .eTag(company!!.version.toString())
        .body(companyResourceFactory.build(company))
  }

  @DeleteMapping(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH)
  fun deleteCompany(
      @PathVariable(PATH_VARIABLE_COMPANY_ID) companyId: CompanyId
  ): ResponseEntity<Void> {
    deleteCompanyCommandHandler.handle(DeleteCompanyCommand(companyId))
    return ResponseEntity.noContent().build()
  }

  @PostMapping(COMPANY_SUGGESTIONS_ENDPOINT_PATH)
  fun suggestCompaniesByTerm(
      @RequestBody suggestion: SuggestionResource?,
      @PageableDefault(sort = ["displayName"], size = 100) pageable: Pageable?
  ): ResponseEntity<ListResponseResource<ResourceReference>> {
    val companies = companyQueryService.suggestCompaniesByTerm(suggestion?.term, pageable!!)
    return ResponseEntity.ok(companySuggestionsResourceFactory.build(companies))
  }

  @PostMapping(COMPANIES_SEARCH_ENDPOINT_PATH)
  fun searchCompanies(
      @RequestBody filter: FilterCompanyListResource,
      @PageableDefault(sort = ["name"], size = 100) pageable: Pageable?
  ): ResponseEntity<ListResponseResource<CompanyResource>> {
    val companies = companyQueryService.findCompaniesForFilters(filter.name, pageable!!)
    return ResponseEntity.ok(companyListResourceFactory.build(companies))
  }

  @GetMapping(COMPANIES_ENDPOINT_PATH)
  fun findAllCompanies(
      @PageableDefault(sort = ["name"], size = 100) pageable: Pageable?
  ): ResponseEntity<ListResponseResource<CompanyResource>> {
    val companyPage = companyQueryService.findAllCompanies(pageable!!)
    return ResponseEntity.ok(companyListResourceFactory.build(companyPage))
  }

  @GetMapping(COMPANY_BY_COMPANY_ID_ENDPOINT_PATH)
  fun findCompanyByIdentifier(
      @PathVariable(PATH_VARIABLE_COMPANY_ID) companyIdentifier: UUID?
  ): ResponseEntity<CompanyResource> {
    val company =
        companyQueryService.findCompanyWithDetailsByIdentifier(companyIdentifier!!.asCompanyId())
    return if (company != null) ResponseEntity(companyResourceFactory.build(company), HttpStatus.OK)
    else ResponseEntity.notFound().build()
  }

  companion object {
    const val COMPANIES_ENDPOINT_PATH = "/companies"
    const val COMPANIES_SEARCH_ENDPOINT_PATH = "/companies/search"
    const val COMPANY_SUGGESTIONS_ENDPOINT_PATH = "/companies/suggestions"
    const val COMPANY_BY_COMPANY_ID_ENDPOINT_PATH = "/companies/{companyId}"
    const val PATH_VARIABLE_COMPANY_ID = "companyId"
  }
}
