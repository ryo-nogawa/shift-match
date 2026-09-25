package com.example.shiftmatch.domain;

import java.time.LocalTime;
import java.util.List;

/**
 * 従業員情報を表すレコード。
 *
 * <p>従業員の名前、勤務可能時間帯、休みの有無を保持します。
 */
public record Employee(
    String name, List<Wish> wishes, boolean off, LocalTime start, LocalTime end) {

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

  /**
   * 旧形式のコンストラクタ。移行期間中は残されます。
   *
   * <p>T14 で削除予定の移行用コンストラクタです。新規コードでは {@link #working(String, LocalTime,
   * LocalTime)} または {@link #onLeave(String)} を使用してください。
   *
   * @param name 従業員名
   * @param wishes 希望のリスト
   * @deprecated T14 で削除予定
   */
  @Deprecated
  public Employee(String name, List<Wish> wishes) {
    this(name, wishes, false, null, null);
  }

  /**
   * 勤務可能時間帯を指定して従業員を作成します。
   *
   * @param name 従業員名
   * @param start 勤務開始時刻
   * @param end 勤務終了時刻
   * @return 新しい従業員インスタンス
   */
  public static Employee working(String name, LocalTime start, LocalTime end) {
    List<Wish> wishes =
        List.of(
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE);
    return new Employee(name, wishes, false, start, end);
  }

  /**
   * 休みの従業員を作成します。
   *
   * @param name 従業員名
   * @return 新しい従業員インスタンス（休み）
   */
  public static Employee onLeave(String name) {
    List<Wish> wishes =
        List.of(
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE,
            Wish.UNAVAILABLE);
    return new Employee(name, wishes, true, null, null);
  }
}
