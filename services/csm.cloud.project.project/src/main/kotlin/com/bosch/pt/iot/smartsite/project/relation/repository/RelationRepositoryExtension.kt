/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.repository

import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationFilterDto
import java.util.UUID
import org.springframework.data.domain.Pageable

interface RelationRepositoryExtension {

  fun findForFilters(filters: RelationFilterDto, pageable: Pageable): List<UUID>

  fun countForFilters(filters: RelationFilterDto): Long
}
