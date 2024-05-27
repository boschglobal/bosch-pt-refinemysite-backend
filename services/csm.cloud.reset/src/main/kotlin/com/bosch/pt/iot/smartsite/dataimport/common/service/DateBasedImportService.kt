/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.service

import java.time.LocalDate

interface DateBasedImportService<T> : ImportService<T> {
  fun importData(data: T, rootDate: LocalDate)
}
