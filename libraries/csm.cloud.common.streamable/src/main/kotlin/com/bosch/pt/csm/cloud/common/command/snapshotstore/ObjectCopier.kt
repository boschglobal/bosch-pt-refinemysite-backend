/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.google.gson.Gson

object ObjectCopier {

  fun <T> deepCopy(value: T, clazz: Class<T>): T {
    val json = Gson().toJson(value, clazz)
    return Gson().fromJson(json, clazz)
  }
}
