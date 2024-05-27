/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject

@Suppress("UtilityClassWithPublicConstructor")
open class DioAssembler {

  companion object {

    fun addToContext(importContext: ImportContext, dataImportObject: DataImportObject<*>) {
      importContext.map[dataImportObject.id] = dataImportObject.identifier
      importContext.dioMap[dataImportObject.id] = dataImportObject
    }

    fun addToContext(importContext: ImportContext, dataImportObjects: List<DataImportObject<*>>) {
      dataImportObjects.forEach { importObject ->
        importContext.map[importObject.id] = importObject.identifier
        importContext.dioMap[importObject.id] = importObject
      }
    }
  }
}
