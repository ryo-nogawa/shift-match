package com.example.shiftmatch.domain;

import java.time.LocalTime;
import java.util.List;

/**
 * 従業員情報を表すレコード。
 *
 * <p>従業員の名前、雇用区分、勤務可能時間帯、休みの有無、週の残り時間（H-4）、これまでの出勤日数（5.6 節）を保持します。
 */
public record Employee(
    String name,
    EmploymentType employmentType,
    boolean off,
    LocalTime start,
    LocalTime end,
    Integer weeklyRemainingMinutes,
    Integer priorWorkDays) {

  /**
   * 旧シグネチャのコンストラクタ（週の残り時間を上限なしで補う）。互換性のために残しています。
   *
   * <p>これまでの出勤日数（{@link #priorWorkDays()}）は不明として {@code null} を補います。
   *
   * @param name 従業員名
   * @param employmentType 雇用区分
   * @param off 休みの有無
   * @param start 勤務開始時刻
   * @param end 勤務終了時刻
   */
  public Employee(
      String name, EmploymentType employmentType, boolean off, LocalTime start, LocalTime end) {
    this(name, employmentType, off, start, end, null, null);
  }

  /**
   * 旧シグネチャのコンストラクタ（常勤・週の残り時間を上限なしで補う）。互換性のために残しています。
   *
   * <p>これまでの出勤日数（{@link #priorWorkDays()}）は不明として {@code null} を補います。
   *
   * @param name 従業員名
   * @param off 休みの有無
   * @param start 勤務開始時刻
   * @param end 勤務終了時刻
   */
  public Employee(String name, boolean off, LocalTime start, LocalTime end) {
    this(name, EmploymentType.FULL_TIME, off, start, end, null, null);
  }

  /**
   * 旧シグネチャのコンストラクタ（週の残り時間を指定し、これまでの出勤日数を不明で補う）。互換性のために残しています。
   *
   * @param name 従業員名
   * @param employmentType 雇用区分
   * @param off 休みの有無
   * @param start 勤務開始時刻
   * @param end 勤務終了時刻
   * @param weeklyRemainingMinutes 週の残り時間（分）
   */
  public Employee(
      String name,
      EmploymentType employmentType,
      boolean off,
      LocalTime start,
      LocalTime end,
      Integer weeklyRemainingMinutes) {
    this(name, employmentType, off, start, end, weeklyRemainingMinutes, null);
  }

  /**
   * 勤務可能時間帯を指定して従業員を作成します（常勤）。
   *
   * @param name 従業員名
   * @param start 勤務開始時刻
   * @param end 勤務終了時刻
   * @return 新しい従業員インスタンス（常勤）
   */
  public static Employee working(String name, LocalTime start, LocalTime end) {
    return new Employee(name, EmploymentType.FULL_TIME, false, start, end, null, null);
  }

  /**
   * 勤務可能時間帯と雇用区分を指定して従業員を作成します。
   *
   * @param name 従業員名
   * @param employmentType 雇用区分
   * @param start 勤務開始時刻
   * @param end 勤務終了時刻
   * @return 新しい従業員インスタンス
   */
  public static Employee working(
      String name, EmploymentType employmentType, LocalTime start, LocalTime end) {
    return new Employee(name, employmentType, false, start, end, null, null);
  }

  /**
   * 休みの従業員を作成します（常勤）。
   *
   * @param name 従業員名
   * @return 新しい従業員インスタンス（常勤、休み）
   */
  public static Employee onLeave(String name) {
    return new Employee(name, EmploymentType.FULL_TIME, true, null, null, null, null);
  }

  /**
   * 休みの従業員を作成します。
   *
   * @param name 従業員名
   * @param employmentType 雇用区分
   * @return 新しい従業員インスタンス（休み）
   */
  public static Employee onLeave(String name, EmploymentType employmentType) {
    return new Employee(name, employmentType, true, null, null, null, null);
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
   * 週の残り時間だけを変えた新しいインスタンスを返します。
   *
   * <p>これまでの出勤日数（{@link #priorWorkDays()}）は変更前の値を保ちます。
   *
   * @param minutes 週の残り時間（分）
   * @return 週の残り時間を変えた新しい従業員インスタンス
   */
  public Employee withWeeklyRemainingMinutes(int minutes) {
    return new Employee(name, employmentType, off, start, end, minutes, priorWorkDays);
  }

  /**
   * これまでの出勤日数（5.6 節）だけを変えた新しいインスタンスを返します。
   *
   * <p>週の残り時間（{@link #weeklyRemainingMinutes()}）は変更前の値を保ちます。
   *
   * @param days これまでの出勤日数
   * @return これまでの出勤日数を変えた新しい従業員インスタンス
   */
  public Employee withPriorWorkDays(int days) {
    return new Employee(name, employmentType, off, start, end, weeklyRemainingMinutes, days);
  }

  /**
   * 枠にこの従業員を割り当てられるかを判定します。
   *
   * <p>{@link #canWork(ShiftSlot)} が false の場合は false を返します。雇用区分がパート（{@link
   * EmploymentType#PART_TIME}）でない場合（常勤・管理職）は、週の残り時間にかかわらず true を返します（H-4 は常勤・管理職に適用しません）。パートの場合は、
   * 週の残り時間（{@link #weeklyRemainingMinutes()}）が {@code null}（上限なし）であるか、枠の実労働時間（{@link
   * ShiftSlot#actualWorkMinutes()}）以上であるときに true を返します（H-4）。
   *
   * @param slot 判定対象の枠
   * @return 割り当てられる場合は true、そうでなければ false
   */
  public boolean canAssign(ShiftSlot slot) {
    if (!canWork(slot)) {
      return false;
    }
    if (employmentType != EmploymentType.PART_TIME) {
      return true;
    }
    return weeklyRemainingMinutes == null || slot.actualWorkMinutes() <= weeklyRemainingMinutes;
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
   * <p>出勤する場合（割り当てられる可能性がある場合）や、出勤しない場合でも理由が異なります（7.2 節の判定順）：
   * <ul>
   *   <li>{@link UnassignedReason#ON_LEAVE} : 従業員が休み
   *   <li>{@link UnassignedReason#NO_AVAILABLE_SLOT} : {@link #canWork(ShiftSlot)} を満たす枠がない
   *   <li>{@link UnassignedReason#WEEKLY_LIMIT_EXCEEDED} :
   *       {@link #canWork(ShiftSlot)} を満たす枠はあるが、{@link #canAssign(ShiftSlot)} が true の枠がない（H-4）
   *   <li>{@link UnassignedReason#LOWER_GAP_CHOSEN} : それ以外
   * </ul>
   *
   * @return 未出勤の理由
   */
  public UnassignedReason unassignedReason() {
    if (off) {
      return UnassignedReason.ON_LEAVE;
    }
    List<ShiftSlot> workableSlots = workableSlots();
    if (workableSlots.isEmpty()) {
      return UnassignedReason.NO_AVAILABLE_SLOT;
    }
    if (workableSlots.stream().noneMatch(slot -> canAssign(slot))) {
      return UnassignedReason.WEEKLY_LIMIT_EXCEEDED;
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
