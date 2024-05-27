/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DayCardRepository : JpaRepository<DayCard, Long>, DayCardRepositoryExtension {

  fun findByContextObject(contextObject: ObjectIdentifier): DayCard?

  fun findAllByTaskIdentifier(taskIdentifier: UUID): List<DayCard>

  fun deleteByContextObjectIdentifier(identifier: UUID): Long

  fun deleteByProjectIdentifier(projectIdentifier: UUID)

  @Query("select d.id FROM DayCard d where d.taskIdentifier = :id")
  fun findIdsByTaskIdentifier(@Param("id") taskIdentifier: UUID?): List<Long>
}
