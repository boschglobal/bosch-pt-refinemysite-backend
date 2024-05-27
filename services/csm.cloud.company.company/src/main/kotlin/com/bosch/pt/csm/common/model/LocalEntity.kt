/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.model

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import java.io.Serializable
import com.bosch.pt.csm.cloud.common.api.AbstractPersistable

@LibraryCandidate(library = "csm.cloud.common.streamable")
open class LocalEntity<T : Serializable> : AbstractPersistable<T>()
