/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.control.dto

import net.sf.mpxj.FieldType

open class ImportColumn(
    val name: String,
    val columnType: ImportColumnType,
    val fieldType: FieldType? = null,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ImportColumn) return false

    if (name != other.name) return false
    if (fieldType != other.fieldType) return false
    if (columnType != other.columnType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + fieldType.hashCode()
    result = 31 * result + columnType.hashCode()
    return result
  }
}
