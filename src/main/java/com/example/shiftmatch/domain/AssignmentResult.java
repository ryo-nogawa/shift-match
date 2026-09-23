package com.example.shiftmatch.domain;

import java.util.List;

/**
 * シフト割り当て結果を表すレコード。
 *
 * <p>早番と遅番に割り当てられた従業員、スコア、未出勤者を保持します。
 */
public record AssignmentResult(
    List<Employee> earlyEmployees,
    List<Employee> lateEmployees,
    int score,
    List<Employee> unassignedEmployees) {}
