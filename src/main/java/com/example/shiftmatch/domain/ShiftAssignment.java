package com.example.shiftmatch.domain;

import java.time.LocalTime;

/**
 * 1 人分のシフト割り当てを表すレコード。
 *
 * <p>従業員、割り当てられた枠、休憩の開始・終了時刻を保持します。
 */
public record ShiftAssignment(
    Employee employee, ShiftSlot slot, LocalTime breakStart, LocalTime breakEnd) {}
