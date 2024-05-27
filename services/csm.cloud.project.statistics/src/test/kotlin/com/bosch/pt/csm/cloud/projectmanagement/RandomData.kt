/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import java.util.UUID.randomUUID
import org.apache.commons.lang3.RandomStringUtils

object RandomData {

  @JvmStatic fun user() = User(RandomStringUtils.randomAlphabetic(10), randomUUID())
}
