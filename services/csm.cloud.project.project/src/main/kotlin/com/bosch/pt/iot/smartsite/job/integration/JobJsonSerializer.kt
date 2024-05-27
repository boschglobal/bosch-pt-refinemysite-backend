/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.job.integration

import com.bosch.pt.csm.cloud.job.messages.JsonSerializedObjectAvro
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsCsvCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsJsonCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsPdfCommand
import com.bosch.pt.iot.smartsite.project.calendar.command.dto.DownloadableResult
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.dto.ExportCalendarJobContext
import com.bosch.pt.iot.smartsite.project.copy.api.ProjectCopyCommand
import com.bosch.pt.iot.smartsite.project.copy.command.dto.CopiedProjectResult
import com.bosch.pt.iot.smartsite.project.copy.facade.job.dto.ProjectCopyJobContext
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportCommand
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportZipCommand
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.dto.ProjectExportJobContext
import com.bosch.pt.iot.smartsite.project.importer.api.ProjectImportCommand
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobContext
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobContext
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.jackson.JsonComponentModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class JobJsonSerializer(
    private val jsonComponentModule: JsonComponentModule,
    private val typeAliasesToClasses: Map<String, Class<*>>
) {

  private val classesToTypeAliases = typeAliasesToClasses.entries.associate { (k, v) -> v to k }

  private val objectMapper =
      jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(jsonComponentModule)
        configure(WRITE_DATES_AS_TIMESTAMPS, false)
      }

  fun serialize(objectToSerialize: Any): JsonSerializedObject {
    val alias =
        classesToTypeAliases[objectToSerialize::class.java]
            ?: error("Unknown class: ${objectToSerialize::class.java}")
    return JsonSerializedObject(alias, objectMapper.writeValueAsString(objectToSerialize))
  }

  fun deserialize(serializedObject: JsonSerializedObject): Any {
    val clazz =
        typeAliasesToClasses[serializedObject.type]
            ?: error("Unknown type alias: ${serializedObject.type}")
    return objectMapper.readValue(serializedObject.json, clazz)
  }
}

data class JsonSerializedObject(val type: String, val json: String) {
  fun toAvro(): JsonSerializedObjectAvro =
      JsonSerializedObjectAvro.newBuilder().setType(type).setJson(json).build()
}

@Configuration
open class JobJsonSerializerConfiguration {

  @Bean
  open fun jobJsonSerializer(jsonComponentModule: JsonComponentModule): JobJsonSerializer =
      JobJsonSerializer(
          jsonComponentModule,
          mapOf(
              "ExportCalendarAsCsvCommand" to ExportCalendarAsCsvCommand::class.java,
              "ExportCalendarAsJsonCommand" to ExportCalendarAsJsonCommand::class.java,
              "ExportCalendarAsPdfCommand" to ExportCalendarAsPdfCommand::class.java,
              "ExportCalendarJobContext" to ExportCalendarJobContext::class.java,
              "DownloadableResult" to DownloadableResult::class.java,
              "CopiedProjectResult" to CopiedProjectResult::class.java,
              "ProjectCopyCommand" to ProjectCopyCommand::class.java,
              "ProjectCopyJobContext" to ProjectCopyJobContext::class.java,
              "ProjectExportCommand" to ProjectExportCommand::class.java,
              "ProjectExportZipCommand" to ProjectExportZipCommand::class.java,
              "ProjectExportJobContext" to ProjectExportJobContext::class.java,
              "ProjectImportCommand" to ProjectImportCommand::class.java,
              "ProjectImportJobContext" to ProjectImportJobContext::class.java,
              "RescheduleCommand" to RescheduleCommand::class.java,
              "RescheduleJobContext" to RescheduleJobContext::class.java,
              "RescheduleResultDto" to RescheduleResultDto::class.java,
              "Unit" to Unit::class.java),
      )
}
