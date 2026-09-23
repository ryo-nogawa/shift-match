package com.example.shiftmatch.domain;

/**
 * 従業員の枠に対する希望を表す列挙型。
 */
public enum Wish {
  /**
   * ◎ 希望する（優先）。
   */
  DESIRED,

  /**
   * ○ 可能。
   */
  AVAILABLE,

  /**
   * × 不可（割り当てない）。
   */
  UNAVAILABLE,
}
