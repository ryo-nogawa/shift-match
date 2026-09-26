package com.example.shiftmatch.domain;

import java.time.LocalDate;

/**
 * 祝日を表すレコード。
 *
 * @param date 祝日の日付
 * @param name 祝日の名称（例：「元日」「スポーツの日」）
 */
public record Holiday(LocalDate date, String name) {}
