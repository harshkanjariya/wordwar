// utils/gameValidation.ts
import fetch from "node-fetch";
import {ApiError} from "../bootstrap/errors";

/**
 * Check if a cell is empty before placing character
 */
export function validateEmptyCell(liveGame: any, row: number, col: number) {
  if (liveGame.cellData[row][col] != null && liveGame.cellData[row][col] !== "") {
    throw new ApiError("Cell is already filled", 400);
  }
}

/**
 * Check if selected cells form a straight line (horizontal, vertical, or diagonal)
 */
export function validateLinearSelection(cells: { row: number; col: number }[]) {
  if (cells.length < 2) return; // Single cell is always valid

  const allRows = cells.map(c => c.row);
  const allCols = cells.map(c => c.col);

  const isHorizontal = allRows.every(r => r === allRows[0]) &&
    isConsecutive(allCols);

  const isVertical = allCols.every(c => c === allCols[0]) &&
    isConsecutive(allRows);

  const isDiagonal = isDiagonalLine(cells);

  if (!(isHorizontal || isVertical || isDiagonal)) {
    throw new ApiError("Cells must be in a straight line (horizontal, vertical, or diagonal)", 400);
  }
}

/**
 * Ensure values are consecutive (either strictly increasing or decreasing by 1)
 */
function isConsecutive(values: number[]) {
  const sorted = [...values].sort((a, b) => a - b);
  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i] - sorted[i - 1] !== 1) return false;
  }
  return true;
}

/**
 * Check diagonal alignment
 */
function isDiagonalLine(cells: { row: number; col: number }[]) {
  // sort cells by row for consistency
  const sorted = [...cells].sort((a, b) => a.row - b.row);

  for (let i = 1; i < sorted.length; i++) {
    const prev = sorted[i - 1];
    const curr = sorted[i];
    if (Math.abs(curr.row - prev.row) !== 1 || Math.abs(curr.col - prev.col) !== 1) {
      return false;
    }
  }
  return true;
}

/**
 * Check all cells are filled with characters
 */
export function validateCellsFilled(liveGame: any, cells: { row: number; col: number }[]) {
  for (const { row, col } of cells) {
    if (!liveGame.cellData[row][col] || liveGame.cellData[row][col] === "") {
      throw new ApiError("Selected cells must all be filled", 400);
    }
  }
}

/**
 * Build word from selected cells
 */
export function buildWordFromCells(liveGame: any, cells: { row: number; col: number }[]) {
  return cells.map(({ row, col }) => liveGame.cellData[row][col]).join("");
}

/**
 * Check dictionary API for validity
 */
export async function validateWord(word: string) {
  const url = `https://api.dictionaryapi.dev/api/v2/entries/en/${word}`;
  const res = await fetch(url);

  if (!res.ok) {
    throw new ApiError(`Invalid word: ${word}`, 400);
  }

  const data = await res.json();
  if (!Array.isArray(data) || data.length === 0) {
    throw new ApiError(`Invalid word: ${word}`, 400);
  }
  return true;
}

/**
 * Check if word already claimed
 */
export function validateWordNotClaimed(word: string, dbGame: any) {
  const claimed = dbGame.claimedWords || {};
  const allWords = Object.values(claimed).flat();
  if (allWords.includes(word)) {
    throw new ApiError("Word already claimed by another player", 400);
  }
}
