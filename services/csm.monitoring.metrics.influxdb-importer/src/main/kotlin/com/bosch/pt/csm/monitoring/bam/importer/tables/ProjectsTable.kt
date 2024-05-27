/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.monitoring.bam.importer.tables

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import org.apache.avro.generic.GenericRecord
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class ProjectsTable(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

  fun ingestProject(event: ProjectEventAvro) {
    namedParameterJdbcTemplate.update(
        """INSERT INTO projects (identifier, name, created_by, last_active)
                VALUES (:identifier, :title, :createdBy, to_timestamp(:lastActive))
                ON CONFLICT (identifier) DO UPDATE
                    SET name = :title,
                        last_active = to_timestamp(:lastActive)"""
            .trimIndent(),
        MapSqlParameterSource()
            .addValue("identifier", event.aggregate.aggregateIdentifier.identifier)
            .addValue("title", event.aggregate.title)
            .addValue("createdBy", event.aggregate.auditingInformation.createdBy.identifier)
            .addValue("lastActive", extractEventTimeAsEpochSeconds(event)))
  }

  fun ingestProjectActivity(projectIdentifier: String, event: GenericRecord) {
    namedParameterJdbcTemplate.update(
        """UPDATE projects
                SET last_active = to_timestamp(:lastActive)
                WHERE identifier = :identifier"""
            .trimIndent(),
        MapSqlParameterSource()
            .addValue("identifier", projectIdentifier)
            .addValue("lastActive", extractEventTimeAsEpochSeconds(event)))
  }

  private fun extractEventTimeAsEpochSeconds(event: GenericRecord): Long =
      if (event.hasField("auditingInformation"))
          event
              .extract("auditingInformation")["date"]
              .toString()
              .toLong()
              .toInstantByMillis()
              .epochSecond
      else if (event.hasField("aggregate"))
          event
              .extract("aggregate")
              .extract("auditingInformation")["lastModifiedDate"]
              .toString()
              .toLong()
              .toInstantByMillis()
              .epochSecond
      else 0
}
