/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.repository

import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import java.io.Serializable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface ReplicatedEntityRepository<T : AbstractReplicatedEntity<*>, K : Serializable> :
    JpaRepository<T, K>, FindOneByIdentifierRepository
