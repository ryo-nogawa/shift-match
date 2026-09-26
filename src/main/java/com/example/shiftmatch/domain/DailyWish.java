package com.example.shiftmatch.domain;

import java.time.LocalTime;

/**
 * 1 日の希望を表すレコード。
 *
 * <p>休みまたは勤務可能な時間帯を保持します。
 */
public record DailyWish(boolean off, LocalTime start, LocalTime end) {}
