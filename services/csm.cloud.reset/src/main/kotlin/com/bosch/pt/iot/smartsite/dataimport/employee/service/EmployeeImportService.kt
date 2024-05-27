/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.employee.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.company.service.CompanyImportService
import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.request.CreateEmployeeResource
import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.response.EmployeeListResource
import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.response.EmployeeResource
import com.bosch.pt.iot.smartsite.dataimport.employee.model.Employee
import com.bosch.pt.iot.smartsite.dataimport.employee.rest.EmployeeRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmployeeImportService(
    private val existingEmployees: MutableList<EmployeeResource> = ArrayList(),
    private val employeeRestClient: EmployeeRestClient,
    private val authenticationService: AuthenticationService,
    private val companyImportService: CompanyImportService,
    private val idRepository: IdRepository
) : ImportService<Employee> {

  override fun importData(data: Employee) {
    authenticationService.selectAdmin()
    filterExistingEmployee(data)

    if (idRepository.containsId(TypedId.typedId(ResourceTypeEnum.employee, data.id))) {
      LOGGER.warn("Skipped existing employee (id: " + data.id + ")")
      return
    }

    val companyId = idRepository[TypedId.typedId(ResourceTypeEnum.company, data.companyId)]!!
    val createdEmployee = call { employeeRestClient.create(companyId, map(data)) }!!

    idRepository.store(TypedId.typedId(ResourceTypeEnum.employee, data.id), createdEmployee.id)
  }

  fun loadExistingEmployees() {
    authenticationService.selectAdmin()
    companyImportService.existingCompanyIds.forEach(
        Consumer { companyId: UUID ->
          val page = AtomicInteger()
          var employeeListResource: EmployeeListResource
          do {
            employeeListResource =
                call { employeeRestClient.existingEmployees(companyId, page.getAndIncrement()) }!!
            existingEmployees.addAll(employeeListResource.items)
          } while (page.get() < employeeListResource.totalPages)
        })
  }

  fun resetEmployeeData() = existingEmployees.clear()

  private fun map(employee: Employee): CreateEmployeeResource {
    val userId = idRepository[TypedId.typedId(ResourceTypeEnum.user, employee.userId)]
    return CreateEmployeeResource(userId, employee.roles)
  }

  private fun filterExistingEmployee(employee: Employee) {
    if (idRepository.containsId(TypedId.typedId(ResourceTypeEnum.employee, employee.id))) {
      return
    }

    val existingEmployee =
        existingEmployees.firstOrNull { employeeResource: EmployeeResource ->
          employeeResource.user.id ==
              idRepository[TypedId.typedId(ResourceTypeEnum.user, employee.userId)]
        }

    existingEmployee?.let {
      idRepository.store(TypedId.typedId(ResourceTypeEnum.employee, employee.id), it.id)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(EmployeeImportService::class.java)
  }
}
