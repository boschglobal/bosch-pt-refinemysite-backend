package com.bosch.pt.iot.smartsite.mpxj

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.PRIMAVERA_P6_XML
import org.junit.jupiter.api.Test

/**
 * This class checks that IDs are written to / read from a P6 xml file using mpxj as we expect it.
 */
class P6IdTest : MpxjIdBaseTest() {

  companion object {
    val parentGuid = "3e330b47-d7d8-45ba-9a1c-ab5628c3a344".toUUID()
    val child1Guid = "9a0bc821-e9d9-4c7c-a5d8-99cbecb43bb7".toUUID()
    val child2Guid = "0e9b1c93-b82c-4b54-8d23-c2af095a19ae".toUUID()
    val child3Guid = "f69d8db2-725c-4132-9176-814d522d70bf".toUUID()

    const val printXml = false
  }

  @Test
  fun `id in p6 with sync`() {
    testIds(
        printXml,
        PRIMAVERA_P6_XML,
        true,
        DirectInsertNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                DirectInsertNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 10000, 10001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 1000, 1001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            1,
            6,
            parentGuid,
            listOf(
                ValidationNode("Child2", 2, 1001, child2Guid, emptyList()),
                ValidationNode(
                    "Child1",
                    3,
                    101,
                    child1Guid,
                    listOf(ValidationNode("Child3", 4, 10001, child3Guid, emptyList()))))))
  }

  @Test
  fun `id in p6 with sync ids reversed`() {
    testIds(
        printXml,
        PRIMAVERA_P6_XML,
        true,
        DirectInsertNode(
            "Parent",
            100,
            101,
            parentGuid,
            listOf(
                AfterDirectInsertNode(
                    "Child1",
                    5,
                    6,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 10000, 10001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            1,
            101,
            parentGuid,
            listOf(
                ValidationNode("Child2", 2, 10001, child2Guid, emptyList()),
                ValidationNode(
                    "Child1",
                    3,
                    6,
                    child1Guid,
                    listOf(ValidationNode("Child3", 4, 1001, child3Guid, emptyList()))))))
  }

  @Test
  fun `id in p6 without sync`() {
    testIds(
        printXml,
        PRIMAVERA_P6_XML,
        false,
        DirectInsertNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                DirectInsertNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 10000, 10001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            1,
            6,
            parentGuid,
            listOf(
                ValidationNode("Child2", 2, 10001, child2Guid, emptyList()),
                ValidationNode(
                    "Child1",
                    3,
                    101,
                    child1Guid,
                    listOf(ValidationNode("Child3", 4, 1001, child3Guid, emptyList()))))))
  }

  @Test
  fun `id in p6 without sync reversed`() {
    testIds(
        printXml,
        PRIMAVERA_P6_XML,
        false,
        DirectInsertNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                AfterDirectInsertNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 10000, 10001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            1,
            6,
            parentGuid,
            listOf(
                ValidationNode("Child2", 2, 10001, child2Guid, emptyList()),
                ValidationNode(
                    "Child1",
                    3,
                    101,
                    child1Guid,
                    listOf(ValidationNode("Child3", 4, 1001, child3Guid, emptyList()))))))
  }

  @Test
  fun `id in ms project without sync reversed - invalid order`() {
    testIds(
        printXml,
        PRIMAVERA_P6_XML,
        false,
        DirectInsertNode(
            "Parent",
            100,
            101,
            parentGuid,
            listOf(
                DirectInsertNode(
                    "Child1",
                    5,
                    6,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 10000, 10001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 1000, 1001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            1,
            101,
            parentGuid,
            listOf(
                ValidationNode("Child2", 2, 1001, child2Guid, emptyList()),
                ValidationNode(
                    "Child1",
                    3,
                    6,
                    child1Guid,
                    listOf(ValidationNode("Child3", 4, 10001, child3Guid, emptyList()))))))
  }
}
