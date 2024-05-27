/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test

import org.apache.avro.generic.GenericRecord

@Suppress("UNCHECKED_CAST")
fun <T> GenericRecord.getFieldByPath(vararg fieldPath: String): T? =
    if (fieldPath.size == 1) {
      val field = fieldPath.first()
      if (this.hasField(field)) {
        val fieldValue: Any? = this[field]
        fieldValue as T?
      } else {
        null
      }
    } else {
      val rest = fieldPath.drop(1)
      val field = fieldPath.first()

      if (this.hasField(field)) {
        val fieldValue: Any? = this[field]
        if (fieldValue is GenericRecord) {
          fieldValue.getFieldByPath<T?>(*rest.toTypedArray())
        } else {
          null
        }
      } else {
        null
      }
    }
