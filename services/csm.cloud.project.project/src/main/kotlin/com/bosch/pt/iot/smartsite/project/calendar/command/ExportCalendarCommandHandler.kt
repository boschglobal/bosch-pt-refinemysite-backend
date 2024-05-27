/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.command

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.iot.smartsite.pdf.integration.PdfIntegrationService
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsCsvCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsJsonCommand
import com.bosch.pt.iot.smartsite.project.calendar.api.ExportCalendarAsPdfCommand
import com.bosch.pt.iot.smartsite.project.calendar.boundary.CalendarExportDataService
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.dto.CalendarExportRowDto
import com.bosch.pt.iot.smartsite.project.calendar.command.dto.CalendarExport
import com.bosch.pt.iot.smartsite.project.calendar.facade.rest.CalendarHtmlController.Companion.EXPORT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshotStore
import com.fasterxml.jackson.databind.ObjectMapper
import com.opencsv.ICSVWriter
import com.opencsv.bean.HeaderColumnNameMappingStrategy
import com.opencsv.bean.StatefulBeanToCsvBuilder
import datadog.trace.api.Trace
import java.io.StringWriter
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder.fromHttpUrl

@Component
open class ExportCalendarCommandHandler(
    private val logger: Logger,
    private val projectSnapshotStore: ProjectSnapshotStore,
    private val calendarExportDataService: CalendarExportDataService,
    private val objectMapper: ObjectMapper,
    private val pdfIntegrationService: PdfIntegrationService,
    private val apiVersionProperties: ApiVersionProperties,
    @Value("\${csm.project.url}") private val projectServiceUrl: String,
) {

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: ExportCalendarAsCsvCommand): CalendarExport {
    logger.debug("Exporting calendar as csv ...")
    val projectSnapshot = projectSnapshotStore.findOrFail(command.projectIdentifier)

    val calendarExportRows =
        calendarExportDataService.generateExportRows(
            command.projectIdentifier, command.calendarExportParameters)

    val writer = StringWriter()

    StatefulBeanToCsvBuilder<CalendarExportRowDto>(writer)
        .withQuotechar(ICSVWriter.DEFAULT_QUOTE_CHARACTER)
        .withSeparator(ICSVWriter.DEFAULT_SEPARATOR)
        .withMappingStrategy(headerColumnNameMappingStrategy)
        .withOrderedResults(true)
        .build()
        .write(calendarExportRows)

    return writer.toString().toByteArray(Charset.defaultCharset()).run {
      CalendarExport(this, getFileName(projectSnapshot.title, "csv"))
    }
  }

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: ExportCalendarAsJsonCommand): CalendarExport {
    logger.debug("Exporting calendar as json ...")
    val projectSnapshot = projectSnapshotStore.findOrFail(command.projectIdentifier)

    val calendarExportRows =
        calendarExportDataService.generateExportRows(
            command.projectIdentifier, command.calendarExportParameters)

    return objectMapper.writeValueAsString(calendarExportRows).toByteArray().run {
      CalendarExport(this, getFileName(projectSnapshot.title, "json"))
    }
  }

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: ExportCalendarAsPdfCommand): CalendarExport {
    logger.debug("Exporting calendar as pdf ...")

    val projectSnapshot = projectSnapshotStore.findOrFail(command.projectIdentifier)
    val uri =
        fromHttpUrl(projectServiceUrl)
            .path(apiVersionProperties.version.prefix + apiVersionProperties.version.max)
            .path(EXPORT_BY_PROJECT_ID_ENDPOINT)
            .build(command.projectIdentifier)
    val pdf =
        pdfIntegrationService.convertToPdf(
            uri, command.calendarExportParameters, command.token, command.locale)

    return CalendarExport(
        (pdf as ByteArrayResource).byteArray, getFileName(projectSnapshot.title, "pdf"))
  }

  private fun getFileName(title: String, extension: String) =
      "${DateTimeFormatter.ISO_DATE.format(LocalDate.now())} " +
          "${title.replace(ILLEGAL_CHARACTERS_REGEX.toRegex(), "")}.$extension"

  private class CustomComparator : Comparator<String> {
    private val fields = CalendarExportRowDto::class.java.declaredFields.map { it.name.uppercase() }
    override fun compare(s1: String, s2: String) = fields.indexOf(s1) - fields.indexOf(s2)
  }

  companion object {
    private val headerColumnNameMappingStrategy:
        HeaderColumnNameMappingStrategy<CalendarExportRowDto> =
        HeaderColumnNameMappingStrategy<CalendarExportRowDto>().apply {
          type = CalendarExportRowDto::class.java
          setColumnOrderOnWrite(CustomComparator())
        }

    private val ILLEGAL_CHARACTERS_REGEX =
        listOf(
                "/",
                "\n",
                "\r",
                "\t",
                "\u0000",
                "\u000C",
                "`",
                "?",
                "*",
                "\\\\",
                "<",
                ">",
                "|",
                "\"",
                ":")
            .joinToString(separator = "", prefix = "[", postfix = "]")
  }
}
