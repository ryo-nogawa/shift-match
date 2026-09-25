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

  /**
   * 枠の勤務時間が、この従業員の入力した時間帯に完全に含まれるかを判定します。
   *
   * <p>休みの従業員、または開始・終了時刻が設定されていない場合は false を返します。
   *
   * @param slot 判定対象の枠
   * @return 枠の勤務時間が入力時間帯に完全に含まれる場合は true、そうでなければ false
   */
  public boolean canWork(ShiftSlot slot) {
    if (off || start == null || end == null) {
      return false;
    }
    return !slot.startTime().isBefore(start) && !slot.endTime().isAfter(end);
  }

  /**
   * 入力した時間帯と枠の勤務時間のずれ（分）を計算します。
   *
   * <p>ずれ = 入力時間帯の長さ（分） − 枠の勤務時間（分）
   *
   * @param slot 対象の枠
   * @return ずれ（分）
   * @throws IllegalStateException {@link #canWork(ShiftSlot)} が false の場合
   */
  public int gapMinutes(ShiftSlot slot) {
    if (!canWork(slot)) {
      throw new IllegalStateException("この従業員はこの枠に割り当てられません");
    }
    int inputMinutes = (int) java.time.temporal.ChronoUnit.MINUTES.between(start, end);
    return inputMinutes - slot.workMinutes();
  }
}
