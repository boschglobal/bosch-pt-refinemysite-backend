/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.util

import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.company
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IdRepositoryTest {

  private var idRepository = IdRepository()

  @BeforeEach
  fun setup() {
    idRepository.reset()
  }

  @Test
  fun verifyGetNullValue() {
    assertThat(idRepository[TypedId(null, null)]).isNull()
  }

  @Test
  fun verifyContainsId() {
    val typedId = TypedId(company, "Test")
    idRepository.store(typedId, randomUUID())
    assertThat(idRepository.containsId(typedId)).isTrue
  }

  @Test
  fun verifyGetId() {
    val id = randomUUID()
    val typedId = TypedId(company, "Test")
    idRepository.store(typedId, id)
    assertThat(idRepository[typedId]).isEqualTo(id)
  }
}
