package com.example.shiftmatch.domain;

/**
 * 従業員情報を表すレコード。
 *
 * <p>従業員の名前と早番・遅番の希望を保持します。
 */
public record Employee(String name, Wish earlyWish, Wish lateWish) {}
