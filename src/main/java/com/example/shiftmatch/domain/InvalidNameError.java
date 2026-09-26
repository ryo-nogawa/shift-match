package com.example.shiftmatch.domain;

/**
 * 氏名の入力エラーを表すレコード。
 *
 * <p>V-6 で検出された、行番号とエラーメッセージを保持します。
 */
public record InvalidNameError(int rowIndex, String message) {}
