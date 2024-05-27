/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.listener

import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.NamedObjectService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.ObjectRelationService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import jakarta.transaction.Transactional
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class CompanyEventToStateProcessor(
    private val objectRelationService: ObjectRelationService,
    private val namedObjectService: NamedObjectService
) {

  @Transactional
  fun updateStateFromCompanyEvent(message: SpecificRecordBase) {
    when (message) {
      is CompanyEventAvro -> updateFromCompanyEvent(message)
      is EmployeeEventAvro -> updateFromEmployeeEvent(message)
    }
  }

  private fun updateFromCompanyEvent(event: CompanyEventAvro) {
    if (event.getName() == CompanyEventEnumAvro.DELETED) {
      namedObjectService.deleteAll(
          listOf(ObjectIdentifier(event.getAggregate().getAggregateIdentifier())))
    } else {
      namedObjectService.saveCompanyName(event)
    }
  }

  private fun updateFromEmployeeEvent(event: EmployeeEventAvro) {
    // the relations are not removed when an employee deleted events is received
    // because the employee might still be referenced by a (deactivated) participant
    if (event.getName() != EmployeeEventEnumAvro.DELETED) {
      objectRelationService.saveEmployeeToCompanyRelation(event)
      objectRelationService.saveEmployeeToUserRelation(event)
    }
  }
}
