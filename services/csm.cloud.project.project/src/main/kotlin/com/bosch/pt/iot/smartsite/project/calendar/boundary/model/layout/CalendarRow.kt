/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCell.Companion.blockerFor
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCell.Companion.merge
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.CalendarCellType.EMPTY
import java.util.function.Supplier
import java.util.stream.IntStream
import org.springframework.util.Assert

data class CalendarRow(
    var cells: MutableList<MutableList<CalendarCell>> = mutableListOf(),
    var type: GridType = GridType.WORK_AREA_ROW
) {

  fun appendCell(col: Int, cell: CalendarCell) = appendCell(0, col, cell)

  /*
   * Append a cell to the grid the given row and column.
   * Note - If there is already one cell for the given coordinates
   * the method iterate through the multiple rows to find one empty.
   */
  fun appendCell(minRow: Int, col: Int, cell: CalendarCell) {

    Assert.isTrue(cell.width >= 1, "Width must be 1 or greater")
    val row: Int = findFirstEmptyCellInColumn(minRow, col, cell.width, cell.height)

    // create the actual cell to be rendered
    setCell(row, col, cell, true)

    // create blocker cells that fill up space in the grid but won't be rendered
    for (colOffset in 0 until cell.width) {
      for (rowOffset in 0 until cell.height) {
        // create a blocker cell for all cells of the rectangle spanned by width and height,
        // except for the top left cell, which is the actual cell to be rendered
        if (!(colOffset == 0 && rowOffset == 0)) {
          setCell(row + rowOffset, col + colOffset, blockerFor(cell), true)
        }
      }
    }
  }

  /*
   * Merges adjacent empty cells into a single cell. Cells are merged vertically but never
   * horizontally. Merged cells will no longer be considered empty cells. Space occupied by the
   * merged cells will be occupied by blocker cells.
   *
   * maxRowExclusive a row index that specifies the upper boundary for the merge operation.
   * The specified row and larger rows will not be merged.
   */
  fun mergeEmptyCellsInColumns(maxRowExclusive: Int) {

    Assert.isTrue(maxRowExclusive >= 0, "maxRows must not be smaller than 0.")
    Assert.isTrue(maxRowExclusive <= cells.size, "maxRows must be smaller than the row count.")

    for (col in 1 until getMaxNumberOfColumns()) {
      // the cell which is being merged with other cells
      var mergeCell: CalendarCell? = null
      for (row in 0 until maxRowExclusive) {
        if (isEmpty(row, col)) {
          if (mergeCell == null) {
            mergeCell = merge()
            setCell(row, col, mergeCell, false)
          } else {
            mergeCell.height = mergeCell.height + 1
            setCell(row, col, blockerFor(mergeCell), false)
          }
        } else {
          mergeCell = null
        }
      }
    }
  }

  /*
   * Creates missing cells to make the grid rectangular.
   * i.e. that each row has the same number of columns.
   */
  fun makeRectangular(minCols: Int) {

    val maxCols = getMaxNumberOfColumns().coerceAtLeast(minCols)
    val rows = cells.size

    for (row in 0 until rows) {
      for (col in 0 until maxCols) {
        ensureCellAt(row, col)
      }
    }
  }

  /*
   * This function mark the cell in the top of the grid ( first row )
   * and the ones that correspond to the end of the week
   * for a different style in fragments.
   */
  fun markCellsForStyling() {
    if (cells.isEmpty()) {
      return
    }

    // Set all the first row cell as true ( limit the upper table )
    for (cell in cells[0]) {
      cell.isOnFirstRow = true
    }

    for (row in cells) {
      for (col in 1 until row.size) {
        if (col % 7 == 0) {
          row[col].isEndOfWeek = true
        }
      }
    }
  }

  /*
   * Find the first empty row for a given column.
   */
  private fun findFirstEmptyCellInColumn(
      startRow: Int,
      col: Int,
      requiredWidth: Int,
      requiredHeight: Int
  ): Int {

    var row = startRow

    while (true) {

      var allEmpty = true

      for (colOffset in 0 until requiredWidth) {
        for (rowOffset in 0 until requiredHeight) {
          if (cellAt(row + rowOffset, col + colOffset).type != EMPTY) {
            allEmpty = false
          }
        }
      }

      if (allEmpty) {
        return row
      }

      row++
    }
  }

  /*
   * Sets the cell at the specified position.
   * exceptionOnOverride set to throw an exception when trying to override a cell that
   * has been set already.
   */
  private fun setCell(row: Int, col: Int, cell: CalendarCell, exceptionOnOverride: Boolean) {

    check(!(!isEmpty(row, col) && exceptionOnOverride)) {
      "Cell is occupied already at row $row, col $col: ${cellAt(row, col)}"
    }

    cells[row][col] = cell
  }

  fun cellAt(row: Int, col: Int): CalendarCell {
    ensureCellAt(row, col)
    return cells[row][col]
  }

  private fun ensureCellAt(row: Int, col: Int) {
    expandList(cells, row) { ArrayList() }
    expandList(cells[row], col) { CalendarCell.empty() }
  }

  private fun getMaxNumberOfColumns() = cells.asSequence().map { it.size }.maxOrNull() ?: 0

  private fun isEmpty(row: Int, col: Int) = cellAt(row, col).type == EMPTY

  companion object {

    private fun <T> expandList(list: MutableList<T>, desiredIndex: Int, supplier: Supplier<T>) {

      val actualSize = list.size
      val desiredSize = desiredIndex + 1

      if (actualSize < desiredSize) {
        IntStream.range(actualSize, desiredSize).forEach { list.add(supplier.get()) }
      }
    }
  }
}

enum class GridType {
  WEEK_ROW,
  WORK_AREA_ROW;

  val isWeekRow: Boolean
    get() = this == WEEK_ROW

  val isWorkAreaRow: Boolean
    get() = this == WORK_AREA_ROW
}
