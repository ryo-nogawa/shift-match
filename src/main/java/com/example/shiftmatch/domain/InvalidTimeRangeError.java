package com.example.shiftmatch.domain;

/**
 * 勤務可能時間帯（開始・終了）の入力エラーを表すレコード。
 *
 * <p>V-3 で検出された、行番号とエラーメッセージを保持します。
 */
public record InvalidTimeRangeError(int rowIndex, String message) {}
