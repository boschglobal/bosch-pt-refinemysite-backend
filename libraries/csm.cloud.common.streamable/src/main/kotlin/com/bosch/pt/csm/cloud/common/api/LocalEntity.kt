/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.api

import java.io.Serializable

open class LocalEntity<T : Serializable> : AbstractPersistable<T>()
