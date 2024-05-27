/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.monitoring.bam.importer.tables

import com.bosch.pt.csm.cloud.bim.common.BimAggregateTypeEnum
import com.bosch.pt.csm.cloud.bim.model.project.messages.BimModelDeletedEventAvro
import com.bosch.pt.csm.cloud.bim.model.project.messages.BimModelUploadCompletedEventAvro
import com.bosch.pt.csm.cloud.bim.model.project.messages.BimModelUploadRequestedEventAvro
import com.bosch.pt.csm.cloud.bim.model.project.messages.BimModelUploadTimedOutEventAvro
import com.bosch.pt.csm.cloud.bim.model.project.messages.ProjectDeletedEventAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class BimModelsTable(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

  fun ingestBimModel(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>) {
    when (val key = record.key()) {
      is MessageKeyAvro ->
          record.value()?.let { event ->
            if (key.aggregateIdentifier.type == BimAggregateTypeEnum.PROJECT.name) {
              when (event) {
                is BimModelUploadRequestedEventAvro -> upsertBimModel(event)
                is BimModelUploadCompletedEventAvro -> upsertBimModel(event)
                is BimModelUploadTimedOutEventAvro -> upsertBimModel(event)
                is BimModelDeletedEventAvro -> deleteBimModels(event)
                is ProjectDeletedEventAvro -> deleteBimModels(event)
              }
            }
          }
    }
  }

  private fun upsertBimModel(event: BimModelUploadRequestedEventAvro) {
    namedParameterJdbcTemplate.update(
        """INSERT INTO bim_models (project_id, model_id, version_id, model_name, uploaded_by, status)
                VALUES (:projectId, :modelId, :versionId, :modelName, :uploadedBy, :status)
                ON CONFLICT (project_id, model_id, version_id) DO UPDATE
                    SET model_name = :modelName,
                      uploaded_by = :uploadedBy,
                      status = :status"""
            .trimIndent(),
        MapSqlParameterSource()
            .addValue("projectId", event.aggregateIdentifier.identifier)
            .addValue("modelId", event.modelIdentifier)
            .addValue("versionId", event.versionId)
            .addValue("modelName", event.name)
            .addValue("uploadedBy", event.auditingInformation.user)
            .addValue("status", "UPLOAD_PENDING"))
  }

  private fun upsertBimModel(event: BimModelUploadCompletedEventAvro) {
    namedParameterJdbcTemplate.update(
        """INSERT INTO bim_models (project_id, model_id, version_id, model_name, uploaded_by, status)
                VALUES (:projectId, :modelId, :versionId, :modelName, :uploadedBy, :status)
                ON CONFLICT (project_id, model_id, version_id) DO UPDATE
                    SET status = :status"""
            .trimIndent(),
        MapSqlParameterSource()
            .addValue("projectId", event.aggregateIdentifier.identifier)
            .addValue("modelId", event.modelIdentifier)
            .addValue("versionId", event.versionId)
            .addValue("modelName", "<unknown>")
            .addValue("uploadedBy", "<unknown>")
            .addValue("status", "UPLOAD_COMPLETED"))
  }

  private fun upsertBimModel(event: BimModelUploadTimedOutEventAvro) {
    namedParameterJdbcTemplate.update(
        """INSERT INTO bim_models (project_id, model_id, version_id, model_name, uploaded_by, status)
                VALUES (:projectId, :modelId, :versionId, :modelName, :uploadedBy, :status)
                ON CONFLICT (project_id, model_id, version_id) DO UPDATE
                    SET status = :status"""
            .trimIndent(),
        MapSqlParameterSource()
            .addValue("projectId", event.aggregateIdentifier.identifier)
            .addValue("modelId", event.modelIdentifier)
            .addValue("versionId", event.versionId)
            .addValue("modelName", "<unknown>")
            .addValue("uploadedBy", "<unknown>")
            .addValue("status", "UPLOAD_FAILED"))
  }

  private fun deleteBimModels(event: BimModelDeletedEventAvro) {
    namedParameterJdbcTemplate.update(
        """UPDATE bim_models
          |    SET status = :status
          |    WHERE project_id = :projectId AND model_id = :modelId"""
            .trimMargin(),
        MapSqlParameterSource()
            .addValue("projectId", event.aggregateIdentifier.identifier)
            .addValue("modelId", event.modelIdentifier)
            .addValue("status", "DELETED"))
  }

  private fun deleteBimModels(event: ProjectDeletedEventAvro) {
    namedParameterJdbcTemplate.update(
        """UPDATE bim_models
          |    SET status = :status
          |    WHERE project_id = :projectId"""
            .trimMargin(),
        MapSqlParameterSource()
            .addValue("projectId", event.aggregateIdentifier.identifier)
            .addValue("status", "DELETED"))
  }
}
