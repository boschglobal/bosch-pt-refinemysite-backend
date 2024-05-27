/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.company.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.request.CreateCompanyResource
import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.request.CreatePostBoxAddressResource
import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.request.CreateStreetAddressResource
import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.response.CompanyListResource
import com.bosch.pt.iot.smartsite.dataimport.company.api.resource.response.CompanyResource
import com.bosch.pt.iot.smartsite.dataimport.company.model.Company
import com.bosch.pt.iot.smartsite.dataimport.company.rest.CompanyRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CompanyImportService(
    private val existingCompanies: MutableList<CompanyResource> = ArrayList(),
    private val companyRestClient: CompanyRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<Company> {

  val existingCompanyIds: Set<UUID>
    get() = existingCompanies.map(CompanyResource::id).toSet()

  override fun importData(data: Company) {
    authenticationService.selectAdmin()
    filterExistingCompany(data)
    if (idRepository.containsId(TypedId.typedId(ResourceTypeEnum.company, data.id))) {
      LOGGER.warn("Skipped existing company (id: ${data.id})")
      return
    }
    val createdCompany = call { companyRestClient.create(map(data)) }!!
    idRepository.store(TypedId.typedId(ResourceTypeEnum.company, data.id), createdCompany.id)
  }

  fun loadExistingCompanies() {
    authenticationService.selectAdmin()

    val page = AtomicInteger()
    var companyListResource: CompanyListResource

    do {
      companyListResource = call { companyRestClient.existingCompanies(page.getAndIncrement()) }!!
      existingCompanies.addAll(companyListResource.items)
    } while (page.get() < companyListResource.totalPages)
  }

  fun resetCompanyData() = existingCompanies.clear()

  private fun map(company: Company): CreateCompanyResource {
    val postBoxAddress =
        company.postBoxAddress?.let {
          CreatePostBoxAddressResource(it.city, it.zipCode, it.area, it.country, it.postBox)
        }

    val streetAddress =
        company.streetAddress?.let {
          CreateStreetAddressResource(
              it.city, it.zipCode, it.area, it.country, it.street, it.houseNumber)
        }

    return CreateCompanyResource(company.name, streetAddress, postBoxAddress)
  }

  private fun filterExistingCompany(company: Company) {
    if (idRepository.containsId(TypedId.typedId(ResourceTypeEnum.company, company.id))) {
      return
    }

    val existingCompany =
        existingCompanies.firstOrNull { companyResource: CompanyResource ->
          companyResource.name == company.name
        }
    existingCompany?.let {
      idRepository.store(TypedId.typedId(ResourceTypeEnum.company, company.id), it.id)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CompanyImportService::class.java)
  }
}
