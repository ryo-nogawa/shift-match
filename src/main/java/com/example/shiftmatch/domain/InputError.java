package com.example.shiftmatch.domain;

/**
 * 入力チェックのエラーを表すレコード。
 *
 * <p>エラーコード（仕様 ID）とメッセージを保持します。
 */
public record InputError(String code, String message) {}
