package com.example.shiftmatch.domain;

import java.time.LocalTime;
import java.util.List;

/**
 * 従業員情報を表すレコード。
 *
 * <p>従業員の名前、勤務可能時間帯、休みの有無を保持します。
 */
public record Employee(String name, boolean off, LocalTime start, LocalTime end) {

  /**
   * 勤務可能時間帯を指定して従業員を作成します。
   *
   * @param name 従業員名
   * @param start 勤務開始時刻
   * @param end 勤務終了時刻
   * @return 新しい従業員インスタンス
   */
  public static Employee working(String name, LocalTime start, LocalTime end) {
    return new Employee(name, false, start, end);
  }

  /**
   * 休みの従業員を作成します。
   *
   * @param name 従業員名
   * @return 新しい従業員インスタンス（休み）
   */
  public static Employee onLeave(String name) {
    return new Employee(name, true, null, null);
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
   * この従業員が入れる枠を、枠 1 → 6 の順で返します。
   *
   * <p>枠の勤務時間が入力時間帯に完全に含まれる枠のみを返します。休みの従業員、または開始・終了時刻が設定されていない場合は空のリストを返します。
   *
   * @return 入れる枠のリスト
   */
  public List<ShiftSlot> workableSlots() {
    return java.util.Arrays.stream(ShiftSlot.values()).filter(slot -> this.canWork(slot)).toList();
  }

  /**
   * この従業員が出勤しない理由を返します。
   *
   * <p>出勤する場合（割り当てられる可能性がある場合）や、出勤しない場合でも理由が異なります：
   * <ul>
   *   <li>{@link UnassignedReason#ON_LEAVE} : 従業員が休み
   *   <li>{@link UnassignedReason#NO_AVAILABLE_SLOT} : 入れる枠がない
   *   <li>{@link UnassignedReason#LOWER_GAP_CHOSEN} : 入れる枠はあったが、より小さいずれの案が選ばれた
   * </ul>
   *
   * @return 未出勤の理由
   */
  public UnassignedReason unassignedReason() {
    if (off) {
      return UnassignedReason.ON_LEAVE;
    }
    if (workableSlots().isEmpty()) {
      return UnassignedReason.NO_AVAILABLE_SLOT;
    }
    return UnassignedReason.LOWER_GAP_CHOSEN;
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
