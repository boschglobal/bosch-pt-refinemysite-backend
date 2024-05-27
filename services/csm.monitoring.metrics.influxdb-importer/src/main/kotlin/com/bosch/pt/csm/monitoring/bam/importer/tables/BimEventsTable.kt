/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.monitoring.bam.importer.tables

import com.bosch.pt.csm.cloud.bim.common.BimAggregateTypeEnum
import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class BimEventsTable(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

  fun ingestBimEvent(projectIdentifier: String, event: SpecificRecordBase) {
    namedParameterJdbcTemplate.update(
        """INSERT INTO bim_events
                |    (event_time, event_type, project_id, model_id, version_id, workarea_id, acting_user)
                |    VALUES (
                |        to_timestamp(:eventTime),
                |        :eventType,
                |        :projectId,
                |        :modelId,
                |        :versionId,
                |        :workAreaId,
                |        :actingUser)
                |    ON CONFLICT DO NOTHING"""
            .trimMargin(),
        MapSqlParameterSource()
            .addValue(
                "eventTime",
                event
                    .extract("auditingInformation")["date"]
                    .toString()
                    .toLong()
                    .toInstantByMillis()
                    .epochSecond)
            .addValue("eventType", event.javaClass.simpleName)
            .addValue("projectId", projectIdentifier)
            .addValue("modelId", extractModelId(event))
            .addValue("versionId", extractVersionId(event))
            .addValue("workAreaId", extractWorkAreaId(event))
            .addValue("actingUser", extractActingUser(event)))
  }

  private fun extractModelId(event: SpecificRecordBase): String? =
      if (event.hasField("modelIdentifier")) event["modelIdentifier"]?.toString() else null

  private fun extractVersionId(event: SpecificRecordBase): String? =
      if (event.hasField("versionId")) event["versionId"].toString() else null

  private fun extractWorkAreaId(event: SpecificRecordBase): String? {
    val aggregateIdentifier = event.extract("aggregateIdentifier")
    return if (aggregateIdentifier["type"] == BimAggregateTypeEnum.WORK_AREA.name)
        aggregateIdentifier["identifier"].toString()
    else null
  }

  private fun extractActingUser(event: SpecificRecordBase) =
      event.extract("auditingInformation")["user"].toString()
}
