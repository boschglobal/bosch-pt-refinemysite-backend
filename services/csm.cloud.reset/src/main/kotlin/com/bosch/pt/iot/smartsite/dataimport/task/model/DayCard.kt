/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.common.model.UserBasedImport
import java.math.BigDecimal

data class DayCard(
    override val id: String,
    val version: Long? = null,
    val taskId: String,
    val title: String,
    var manpower: BigDecimal,
    val date: Int,
    val notes: String? = null,
    val status: DayCardStatusEnum? = null,
    val etag: String? = null,
    val task: Task? = null,
    override val createWithUserId: String
) : UserBasedImport(), ImportObject {

  init {
    this.manpower = manpower.setScale(2)
  }
}
