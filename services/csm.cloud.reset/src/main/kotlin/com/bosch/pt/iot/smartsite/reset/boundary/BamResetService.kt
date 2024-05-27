/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.reset.boundary

import com.bosch.pt.iot.smartsite.reset.RestoreDbResettable
import com.bosch.pt.iot.smartsite.reset.jdbc.JdbcResetStrategy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.stereotype.Service

@Service
@Profile("reset-bam")
class BamResetService(
    @Qualifier("postgresResetStrategy") jdbcResetStrategy: JdbcResetStrategy,
    @Qualifier("bamJdbcTemplate") jdbcOperations: JdbcOperations
) :
    AbstractRelationalDatabaseResetService(jdbcResetStrategy, jdbcOperations, "bam"),
    RestoreDbResettable
