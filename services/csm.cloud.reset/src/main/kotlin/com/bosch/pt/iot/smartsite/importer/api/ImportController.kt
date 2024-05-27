package com.bosch.pt.iot.smartsite.importer.api

import com.bosch.pt.iot.smartsite.importer.boundary.DataImportService
import com.bosch.pt.iot.smartsite.importer.boundary.DynamicDataImportService
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/import"])
class ImportController(
    private val dataImportService: DataImportService,
    private val dynamicDataImportService: DynamicDataImportService
) {

  @RequestMapping(method = [RequestMethod.POST])
  fun importData(
      @RequestParam dataset: String,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      rootDate: LocalDate?,
      @RequestParam(required = false) numberOfAdditionalWorkAreas: Int?
  ) {
    val date = rootDate ?: LocalDate.now()
    if (numberOfAdditionalWorkAreas != null && numberOfAdditionalWorkAreas > 0) {
      dynamicDataImportService.importData(dataset, date, numberOfAdditionalWorkAreas)
    } else {
      dataImportService.importData(dataset, date)
    }
  }
}
