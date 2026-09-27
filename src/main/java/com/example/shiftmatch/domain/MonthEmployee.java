package com.example.shiftmatch.domain;

/**
 * 月間シフトを作成した時点の従業員（氏名と区分）を表すレコード。
 *
 * @param name 従業員名
 * @param employmentType 区分
 */
public record MonthEmployee(String name, EmploymentType employmentType) {}
