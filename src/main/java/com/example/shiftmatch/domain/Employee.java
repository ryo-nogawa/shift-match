package com.example.shiftmatch.domain;

import java.util.List;

/**
 * 従業員情報を表すレコード。
 *
 * <p>従業員の名前と枠ごとの希望を保持します。
 */
public record Employee(String name, List<Wish> wishes) {

  /**
   * 希望のリストを不変なリストとして保持し、件数を検証する。
   *
   * @throws IllegalArgumentException 希望の件数が {@code ShiftSlot.values().length} でない場合
   */
  public Employee {
    if (wishes.size() != ShiftSlot.values().length) {
      throw new IllegalArgumentException("希望は必ずちょうど" + ShiftSlot.values().length + "件である必要があります");
    }
    wishes = List.copyOf(wishes);
  }
}
