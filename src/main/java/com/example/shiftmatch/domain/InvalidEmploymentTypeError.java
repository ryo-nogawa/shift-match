package com.example.shiftmatch.domain;

/**
 * 雇用区分の入力エラーを表すレコード。
 *
 * <p>V-7 で検出された、行番号とエラーメッセージを保持します。
 */
public record InvalidEmploymentTypeError(int rowIndex, String message) {}
