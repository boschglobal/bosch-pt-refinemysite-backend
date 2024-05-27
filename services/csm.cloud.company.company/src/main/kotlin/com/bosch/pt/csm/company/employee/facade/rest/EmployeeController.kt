/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.common.facade.rest.ETag
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.company.query.CompanyQueryService
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.company.employee.asEmployeeId
import com.bosch.pt.csm.company.employee.command.api.CreateEmployeeCommand
import com.bosch.pt.csm.company.employee.command.api.DeleteEmployeeCommand
import com.bosch.pt.csm.company.employee.command.api.UpdateEmployeeCommand
import com.bosch.pt.csm.company.employee.command.handler.CreateEmployeeCommandHandler
import com.bosch.pt.csm.company.employee.command.handler.DeleteEmployeeCommandHandler
import com.bosch.pt.csm.company.employee.command.handler.UpdateEmployeeCommandHandler
import com.bosch.pt.csm.company.employee.facade.rest.resource.request.SaveEmployeeResource
import com.bosch.pt.csm.company.employee.facade.rest.resource.request.SearchEmployeesFilterResource
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeResource
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.factory.EmployeeListResourceFactory
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.factory.EmployeeResourceFactory
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.factory.EmployeeSearchResultResourceFactory
import com.bosch.pt.csm.company.employee.query.EmployeeQueryService
import com.bosch.pt.csm.company.employee.query.employableuser.EmployableUserQueryService
import com.bosch.pt.csm.company.employee.shared.model.Employee
import com.bosch.pt.csm.user.user.boundary.dto.UserFilterCriteriaDto
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import java.util.UUID
import jakarta.validation.Valid
import org.springframework.data.domain.Page
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
class EmployeeController(
    private val companyQueryService: CompanyQueryService,
    private val createEmployeeCommandHandler: CreateEmployeeCommandHandler,
    private val updateEmployeeCommandHandler: UpdateEmployeeCommandHandler,
    private val deleteEmployeeCommandHandler: DeleteEmployeeCommandHandler,
    private val employeeQueryService: EmployeeQueryService,
    private val employableUserQueryService: EmployableUserQueryService,
    private val employeeListResourceFactory: EmployeeListResourceFactory,
    private val employeeResourceFactory: EmployeeResourceFactory,
    private val employeeSearchResultResourceFactory: EmployeeSearchResultResourceFactory
) {

  @PostMapping(
      EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH, EMPLOYEES_BY_COMPANY_ID_AND_EMPLOYEE_ID_ENDPOINT_PATH)
  fun createEmployee(
      @PathVariable(PATH_VARIABLE_COMPANY_ID) companyId: CompanyId,
      @PathVariable(value = PATH_VARIABLE_EMPLOYEE_ID, required = false) employeeId: EmployeeId?,
      @RequestBody saveEmployeeResource: @Valid SaveEmployeeResource?
  ): ResponseEntity<EmployeeResource> {

    val employeeIdentifier =
        createEmployeeCommandHandler.handle(
            CreateEmployeeCommand(
                employeeId, saveEmployeeResource!!.userId, companyId, saveEmployeeResource.roles))
    val employee = employeeQueryService.findEmployeeWithDetailsByIdentifier(employeeIdentifier)

    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(
                getCurrentApiVersionPrefix() +
                    EMPLOYEES_BY_COMPANY_ID_AND_EMPLOYEE_ID_ENDPOINT_PATH)
            .buildAndExpand(companyId, employeeIdentifier)
            .toUri()

    return ResponseEntity.created(location).body(employeeResourceFactory.build(employee))
  }

  @GetMapping(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
  fun findEmployeesByCompany(
      @PathVariable(PATH_VARIABLE_COMPANY_ID) companyId: UUID?,
      @PageableDefault(sort = ["user.firstName, user.lastName"], size = 100) pageable: Pageable?
  ): ResponseEntity<ListResponseResource<EmployeeResource>> {
    val company = companyQueryService.findCompanyByIdentifier(companyId!!.asCompanyId())
    val employees: Page<Employee> = employeeQueryService.findAllByCompany(company, pageable!!)

    return ResponseEntity(
        employeeListResourceFactory.buildListItems(employees, company), HttpStatus.OK)
  }

  @PostMapping(EMPLOYEE_SEARCH_ENDPOINT_PATH)
  fun search(
      @RequestBody filter: SearchEmployeesFilterResource?,
      @PageableDefault(sort = ["company.displayName, user.createdAt"], size = 100)
      pageable: Pageable
  ): ResponseEntity<ListResponseResource<EmployeeSearchResultItemResource>> =
      UserFilterCriteriaDto(filter!!.name, filter.email, filter.companyName)
          .let { employableUserQueryService.search(it, pageable) }
          .let { employeeSearchResultResourceFactory.build(it) }
          .let { ResponseEntity(it, HttpStatus.OK) }

  @GetMapping(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH)
  fun findEmployeeById(
      @PathVariable(PATH_VARIABLE_EMPLOYEE_ID) employeeId: UUID?
  ): ResponseEntity<EmployeeResource> {
    val employee =
        employeeQueryService.findEmployeeWithDetailsByIdentifier(employeeId!!.asEmployeeId())
    return ResponseEntity(employeeResourceFactory.build(employee), HttpStatus.OK)
  }

  @PutMapping(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH)
  fun updateEmployee(
      @PathVariable(PATH_VARIABLE_EMPLOYEE_ID) employeeIdentifier: UUID,
      @RequestBody update: @Valid SaveEmployeeResource?,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") eTag: ETag?
  ): ResponseEntity<EmployeeResource> {
    updateEmployeeCommandHandler.handle(
        UpdateEmployeeCommand(
            employeeIdentifier.asEmployeeId(), eTag!!.toVersion(), update!!.roles))

    val employee =
        employeeQueryService.findEmployeeWithDetailsByIdentifier(employeeIdentifier.asEmployeeId())
    return ResponseEntity.ok()
        .eTag(employee.version.toString())
        .body(employeeResourceFactory.build(employee))
  }

  @DeleteMapping(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH)
  fun deleteEmployee(
      @PathVariable(PATH_VARIABLE_EMPLOYEE_ID) employeeIdentifier: UUID
  ): ResponseEntity<Void> {
    deleteEmployeeCommandHandler.handle(DeleteEmployeeCommand(employeeIdentifier.asEmployeeId()))
    return ResponseEntity.noContent().build()
  }

  companion object {
    const val EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH = "/companies/{companyId}/employees"
    const val EMPLOYEES_BY_COMPANY_ID_AND_EMPLOYEE_ID_ENDPOINT_PATH =
        "/companies/{companyId}/employees/{employeeId}"
    const val EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH = "/companies/employees/{employeeId}"
    const val EMPLOYEE_SEARCH_ENDPOINT_PATH = "/companies/employees/search"
    const val PATH_VARIABLE_COMPANY_ID = "companyId"
    const val PATH_VARIABLE_EMPLOYEE_ID = "employeeId"
  }
}
