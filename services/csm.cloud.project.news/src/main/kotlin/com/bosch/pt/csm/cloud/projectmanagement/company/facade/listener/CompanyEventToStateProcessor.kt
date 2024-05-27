/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener

import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.ObjectRelationService
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

/**
 * This class is responsible for updating the local state based on a project event. The local state
 * contains the view of the event log required to determine news.
 */
@Component
class CompanyEventToStateProcessor(val objectRelationService: ObjectRelationService) {

  fun updateStateFromCompanyEvent(message: SpecificRecordBase?) {
    if (message is EmployeeEventAvro) {
      updateFromEmployeeEvent(message)
    }
  }

  private fun updateFromEmployeeEvent(event: EmployeeEventAvro) {
    objectRelationService.saveEmployeeToCompanyRelation(event)
    objectRelationService.saveEmployeeToUserRelation(event)
  }
}
