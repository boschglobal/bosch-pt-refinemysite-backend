/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.StatisticsController
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.StatisticsListResource
import java.time.LocalDate
import java.util.UUID

fun StatisticsController.calculatePpc(
    project: AggregateIdentifierAvro,
    startDate: LocalDate,
    duration: Long,
    grouped: Boolean = false
): StatisticsListResource =
    getMetricsForProjects(project.getUuid(), grouped, startDate, duration, listOf("ppc"))

fun StatisticsController.calculateRfv(
    project: AggregateIdentifierAvro,
    startDate: LocalDate,
    duration: Long,
    grouped: Boolean = false
): StatisticsListResource =
    getMetricsForProjects(project.getUuid(), grouped, startDate, duration, listOf("rfv"))

private fun AggregateIdentifierAvro.getUuid() = UUID.fromString(getIdentifier())
