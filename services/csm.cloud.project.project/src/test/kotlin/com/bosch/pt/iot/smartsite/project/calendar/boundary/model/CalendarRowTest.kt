/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarRow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CalendarRowTest {

  private val calendarRow = CalendarRow()

  /*  __
   * |__|
   */
  @Test
  fun `verify append a cell into a empty column is successful`() {
    calendarRow.appendCell(0, 0, CELL_MODEL)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0)).isEqualTo(CELL_MODEL)
    assertThat(calendarRow.getRowCount()).isEqualTo(1)
    assertThat(calendarRow.getColumnCount()).isEqualTo(1)
  }

  /*  __
   * |__|
   * |__|
   */
  @Test
  fun `verify append a cell into a occupied space is successful`() {
    // place cell at (0, 0)
    calendarRow.appendCell(0, buildCell("c00"))

    // should be placed at (1, 0) because  (0, 0) is already occupied
    val cell = buildCell("c10")
    calendarRow.appendCell(0, cell)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(1, 0)).isEqualTo(cell)
    assertThat(calendarRow.getRowCount()).isEqualTo(2)
    assertThat(calendarRow.getColumnCount()).isEqualTo(1)
  }

  /*  __
   * |__|
   *
   *  __
   * |__|
   */
  @Test
  fun `verify append a cell at a specific row is successful`() {
    calendarRow.appendCell(0, 0, buildCell("c00"))

    val cell = buildCell("c20")
    calendarRow.appendCell(2, 0, cell)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(2, 0)).isEqualTo(cell)
    assertThat(calendarRow.getRowCount()).isEqualTo(3)
    assertThat(calendarRow.getColumnCount()).isEqualTo(1)
  }

  /*  _____
   * |_____|
   */
  @Test
  fun `verify append a cell with a specific width is successful`() {
    val cell = buildCell("c00-c01", 2, 1)
    calendarRow.appendCell(0, cell)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0)).isEqualTo(cell)
    assertThat(calendarRow.cellAt(0, 1).type.isBlocker).isTrue
    assertThat(calendarRow.getRowCount()).isEqualTo(1)
    assertThat(calendarRow.getColumnCount()).isEqualTo(2)
  }

  /*  __
   * |  |
   * |__|
   */
  @Test
  fun `verify append a cell with a specific height is successful`() {
    val cell = buildCell("c00-c10", 1, 2)
    calendarRow.appendCell(0, cell)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0)).isEqualTo(cell)
    assertThat(calendarRow.cellAt(1, 0).type.isBlocker).isTrue
    assertThat(calendarRow.getRowCount()).isEqualTo(2)
    assertThat(calendarRow.getColumnCount()).isEqualTo(1)
  }

  /*  _____
   * |     |
   * |_____|
   */
  @Test
  fun `verify append a cell with a specific width and height is successful`() {
    val cell = buildCell("c00-c11", 2, 2)
    calendarRow.appendCell(0, cell)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0)).isEqualTo(cell)
    assertThat(calendarRow.cellAt(0, 1).type.isBlocker).isTrue
    assertThat(calendarRow.cellAt(1, 0).type.isBlocker).isTrue
    assertThat(calendarRow.cellAt(1, 1).type.isBlocker).isTrue
    assertThat(calendarRow.getRowCount()).isEqualTo(2)
    assertThat(calendarRow.getColumnCount()).isEqualTo(2)
  }

  /*     __
   *  __|__|
   * |_____|
   */
  @Test
  fun `verify append a cell with a specific width into a occupied space is successful`() {
    val c01 = buildCell("c01")
    calendarRow.appendCell(1, c01)

    val c10 = buildCell("c20", 2, 1)
    calendarRow.appendCell(0, c10)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0).type.isEmpty).isTrue
    assertThat(calendarRow.cellAt(0, 1)).isEqualTo(c01)
    assertThat(calendarRow.cellAt(1, 0)).isEqualTo(c10)
    assertThat(calendarRow.cellAt(1, 1).type.isBlocker).isTrue
    assertThat(calendarRow.getRowCount()).isEqualTo(2)
    assertThat(calendarRow.getColumnCount()).isEqualTo(2)
  }

  /*  __
   * |__|
   *  __
   * |__|
   * |  |
   * |__|
   */
  @Test
  fun `verify append a cell with a specific height into a occupied space is successful`() {
    val c00 = buildCell("c00")
    calendarRow.appendCell(0, c00)

    val c20 = buildCell("c20")
    calendarRow.appendCell(2, 0, c20)

    val c30 = buildCell("c30", 1, 2)
    calendarRow.appendCell(0, c30)

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0)).isEqualTo(c00)
    assertThat(calendarRow.cellAt(1, 0).type.isEmpty).isTrue
    assertThat(calendarRow.cellAt(2, 0)).isEqualTo(c20)
    assertThat(calendarRow.cellAt(3, 0)).isEqualTo(c30)
    assertThat(calendarRow.cellAt(4, 0).type.isBlocker).isTrue
    assertThat(calendarRow.getRowCount()).isEqualTo(5)
    assertThat(calendarRow.getColumnCount()).isEqualTo(1)
  }

  /*  ..         ..
   *  ..    >
   *  __    >    __
   * |__|       |__|
   */
  @Test
  fun `verify merge cells is successful`() {
    val c20 = buildCell("c20")
    calendarRow.appendCell(2, 0, c20)

    calendarRow.mergeEmptyCellsInColumns(calendarRow.getRowCount())

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0).type.isMerge).isFalse
    assertThat(calendarRow.cellAt(1, 0).type.isBlocker).isFalse
    assertThat(calendarRow.cellAt(2, 0)).isEqualTo(c20)
    assertThat(calendarRow.getRowCount()).isEqualTo(3)
    assertThat(calendarRow.getColumnCount()).isEqualTo(1)
  }

  @Test
  fun `verify merge empty cells is successful`() {
    // place cell in column 1, leaving column all empty, and spanning a four cell grid
    val c11 = buildCell("c11", 1, 1)
    calendarRow.appendCell(1, 1, c11)

    calendarRow.mergeEmptyCellsInColumns(calendarRow.getRowCount())

    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cellAt(0, 0).type.isMerge).isFalse
    assertThat(calendarRow.cellAt(1, 0).type.isBlocker).isFalse
    assertThat(calendarRow.getRowCount()).isEqualTo(2)
    assertThat(calendarRow.getColumnCount()).isEqualTo(2)
  }

  /*     __
   *  __|__|
   * |__|
   */
  @Test
  fun `verify fill the row with empty cells to make a rectangle is successful`() {
    val c01 = buildCell("c01", 1, 1)
    calendarRow.appendCell(1, c01)

    val c10 = buildCell("c10", 1, 1)
    calendarRow.appendCell(1, 0, c10)

    calendarRow.makeRectangular(0)

    // do not use grid#cellAt below because that would generate missing cells, thereby undermining
    // what actually should be tested
    assertThat(calendarRow).isNotNull
    assertThat(calendarRow.cells).isNotNull
    assertThat(calendarRow.cells[0][0].type.isEmpty).isTrue
    assertThat(calendarRow.cells[1][1].type.isEmpty).isTrue
    assertThat(calendarRow.getRowCount()).isEqualTo(2)
    assertThat(calendarRow.getColumnCount()).isEqualTo(2)
  }

  @Test
  fun `verify exception is trow for an out of bound index is successful`() {
    val c00 = buildCell("c00")
    calendarRow.appendCell(0, 0, c00)

    assertThatThrownBy { calendarRow.mergeEmptyCellsInColumns(calendarRow.getRowCount() + 1) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("maxRows must be smaller than the row count.")
    assertThatThrownBy { calendarRow.mergeEmptyCellsInColumns(-1) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("maxRows must not be smaller than 0.")
  }

  companion object {
    val CELL_MODEL = buildCell("cell")

    private fun buildCell(content: String, width: Int = 1, height: Int = 1) =
        CalendarCell(
            content,
            width,
            height,
            "",
            isRender = false,
            isOnFirstRow = false,
            isEndOfWeek = false,
            type = CalendarCellType.TASK)

    private fun CalendarRow.getColumnCount() = cells.asSequence().map { it.size }.maxOrNull() ?: 0

    private fun CalendarRow.getRowCount() = cells.size
  }
}
