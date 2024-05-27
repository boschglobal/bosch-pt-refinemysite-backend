/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.RfvCustomization
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface RfvCustomizationRepository : JpaRepository<RfvCustomization, Long> {

  fun findByIdentifier(identifier: UUID): RfvCustomization?

  fun findAllByProjectIdentifier(projectIdentifier: UUID): List<RfvCustomization>
}
