package com.example.shiftmatch.domain;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 営業日ごとのシフト算出結果を表すレコード。
 *
 * <p>割り当て結果がない場合（不成立の日）は {@code assignment} が空になります。
 */
public record DailyShiftResult(
    LocalDate date, int availableCount, Optional<AssignmentResult> assignment) {}
