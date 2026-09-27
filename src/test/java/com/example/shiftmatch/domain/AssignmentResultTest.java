package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AssignmentResult")
class AssignmentResultTest {

  @Nested
  @DisplayName("[H-1] 割り当て結果")
  class AssignmentConstruction {

    @Test
    @DisplayName("[H-1] Given: 8件の割り当てが与えられたとき, When: AssignmentResultを生成すると, Then: 割り当てがそれぞれ取得できる")
    void canCreateAssignmentResultWithEightAssignments() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      int score = 5;
      List<Employee> unassigned = new ArrayList<>();

      AssignmentResult result = new AssignmentResult(assignments, score, unassigned);

      assertEquals(8, result.assignments().size());
      assertEquals(score, result.score());
      assertEquals(0, result.unassignedEmployees().size());
    }

    @Test
    @DisplayName(
        "[H-1] Given: 割り当ての件数が8でないとき, When: AssignmentResultを生成すると, Then:"
            + " IllegalArgumentExceptionがスローされる")
    void throwsExceptionWhenAssignmentsCountIsNotEight() {
      List<ShiftAssignment> assignments = new ArrayList<>();
      assignments.add(createAssignment(0, ShiftSlot.SLOT_1));
      assignments.add(createAssignment(1, ShiftSlot.SLOT_1));

      int score = 0;
      List<Employee> unassigned = new ArrayList<>();

      assertThrows(
          IllegalArgumentException.class,
          () -> new AssignmentResult(assignments, score, unassigned));
    }

    @Test
    @DisplayName("[H-1] Given: 割り当てが与えられたとき, When: assignmentsにアクセスすると, Then: 不変なリストが返される")
    void returnsImmutableAssignments() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      int score = 0;
      List<Employee> unassigned = new ArrayList<>();

      AssignmentResult result = new AssignmentResult(assignments, score, unassigned);
      List<ShiftAssignment> returnedAssignments = result.assignments();

      assertThrows(
          UnsupportedOperationException.class,
          () -> returnedAssignments.add(createAssignment(8, ShiftSlot.SLOT_6)));
    }
  }

  @Nested
  @DisplayName("[F-4] ずれの合計をリストで返す")
  class GapMinutesList {

    @Test
    @DisplayName("[F-4] Given: 8件の割り当てのとき, When: gapMinutesListを呼ぶと, Then: 8件のずれを返す")
    void gapMinutesListReturnsSameCount() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      int score = 0;
      List<Employee> unassigned = new ArrayList<>();
      AssignmentResult result = new AssignmentResult(assignments, score, unassigned);

      var gapList = result.gapMinutesList();

      assertEquals(8, gapList.size());
    }

    @Test
    @DisplayName(
        "[F-4] Given: 各割り当てのずれが計算されるとき, When: gapMinutesListを呼ぶと, Then:" + " 期待される各人のずれが返される")
    void gapMinutesListReturnsCorrectGaps() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      // すべての従業員が 7:30-18:30（660分）に設定されている
      // 各枠の勤務時間から手計算したずれ：
      // i=0：枠1(420分) → gap = 660-420 = 240
      // i=1：枠2(450分) → gap = 660-450 = 210
      // i=2：枠3(480分) → gap = 660-480 = 180
      // i=3：枠4(450分) → gap = 660-450 = 210
      // i=4：枠5(540分) → gap = 660-540 = 120
      // i=5：枠6(570分) → gap = 660-570 = 90
      // i=6：枠6(570分) → gap = 660-570 = 90  （Math.min(6, 5) = 5 → 枠6）
      // i=7：枠6(570分) → gap = 660-570 = 90  （Math.min(7, 5) = 5 → 枠6）
      List<Integer> expectedGaps = List.of(240, 210, 180, 210, 120, 90, 90, 90);
      int expectedScore = 1230;
      AssignmentResult result = new AssignmentResult(assignments, expectedScore, new ArrayList<>());

      List<Integer> actualGaps = result.gapMinutesList();

      assertEquals(expectedGaps, actualGaps, "ずれのリストが期待値と一致する");
      int sumOfGaps = actualGaps.stream().mapToInt(i -> i.intValue()).sum();
      assertEquals(expectedScore, sumOfGaps, "ずれの合計がスコアと一致する");
    }
  }

  @Nested
  @DisplayName("[F-4][F-3] 未出勤者の理由")
  class UnassignedReasonLabel {

    private AssignmentResult resultWithUnassigned(Employee unassigned) {
      return new AssignmentResult(createStandardAssignments(), 0, List.of(unassigned));
    }

    @Test
    @DisplayName(
        "[F-3][F-4] Given: 未出勤者が割り当て済みの人と入れ替えてもずれの合計が変わらないとき,"
            + " When: 理由を取得すると, Then: 同じずれの案があり、優先度が高い割り当て済みの人が選ばれたと氏名つきで示す")
    void namesTheAssignedEmployeeWhenSwapKeepsTotalGap() {
      // Employee0 は枠 1 で 660 分の時間帯。同じ 660 分の Ito も枠 1 に入れ、ずれは 240 分で同じになる
      Employee ito = Employee.working("Ito", LocalTime.of(7, 30), LocalTime.of(18, 30));

      String label = resultWithUnassigned(ito).unassignedReasonLabel(ito);

      assertEquals("入れる枠はあったが、同じずれの案があり、入力順で優先度が高い Employee0 が選ばれた", label);
    }

    @Test
    @DisplayName(
        "[F-4] Given: どの割り当て済みの人と入れ替えてもずれの合計が変わるとき," + " When: 理由を取得すると, Then: より小さいずれの案が選ばれたと示す")
    void saysLowerGapWhenNoSwapKeepsTotalGap() {
      // 7:30〜18:00（630 分）は枠 1〜5 に入れるが、どの枠でも 660 分の人とはずれが異なる
      Employee shorter = Employee.working("Short", LocalTime.of(7, 30), LocalTime.of(18, 0));

      String label = resultWithUnassigned(shorter).unassignedReasonLabel(shorter);

      assertEquals("入れる枠はあったが、より小さいずれの案が選ばれた", label);
    }

    @Test
    @DisplayName("[F-4] Given: 未出勤者が休みのとき, When: 理由を取得すると, Then: 「休み」を返す")
    void returnsOffLabelForEmployeeOnLeave() {
      Employee off = Employee.onLeave("Off");

      assertEquals("休み", resultWithUnassigned(off).unassignedReasonLabel(off));
    }

    @Test
    @DisplayName("[F-4] Given: 未出勤者がどの枠にも入れないとき, When: 理由を取得すると, Then: 「どの枠にも入れない」を返す")
    void returnsNoAvailableSlotLabel() {
      Employee narrow = Employee.working("Narrow", LocalTime.of(9, 0), LocalTime.of(10, 0));

      assertEquals("どの枠にも入れない", resultWithUnassigned(narrow).unassignedReasonLabel(narrow));
    }
  }

  @Nested
  @DisplayName("[7.2] 未出勤の理由（5.3節のパート優先の入れ替え）")
  class UnassignedReasonLabelForPartTime {

    private AssignmentResult resultWithUnassigned(Employee unassigned) {
      return new AssignmentResult(createStandardAssignments(), 0, List.of(unassigned));
    }

    @Test
    @DisplayName(
        "[7.2] Given: 未出勤者がパートで、同じずれの割り当て済みの人が常勤のとき, When: 理由を取得すると, Then:"
            + " パートの実労働時間が少ない案が選ばれたと示す")
    void namesPartTimePreferenceWhenSwapKeepsTotalGapAndAssignedIsNotPartTime() {
      // Employee0 は枠1（375分の実労働時間）で660分の時間帯、ずれ240分。
      // PartA も同じ660分の時間帯（残り時間は上限なし）なので、枠1でずれ240分は同じになる
      Employee partA =
          Employee.working(
              "PartA", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30));

      String label = resultWithUnassigned(partA).unassignedReasonLabel(partA);

      assertEquals("入れる枠はあったが、同じずれの案があり、パートの実労働時間が少ない案が選ばれた", label);
    }

    @Test
    @DisplayName(
        "[7.2] Given: 未出勤者と割り当て済みの人がどちらも常勤で、ずれの合計が変わらないとき, When: 理由を取得すると, Then:"
            + " 入力順で優先度が高い氏名を示す（パート優先の文言にならない）")
    void namesTheAssignedEmployeeWhenBothAreNotPartTime() {
      Employee fullTimeSwap = Employee.working("Ueda", LocalTime.of(7, 30), LocalTime.of(18, 30));

      String label = resultWithUnassigned(fullTimeSwap).unassignedReasonLabel(fullTimeSwap);

      assertEquals("入れる枠はあったが、同じずれの案があり、入力順で優先度が高い Employee0 が選ばれた", label);
    }

    @Test
    @DisplayName(
        "[7.2][H-4] Given: 同じずれの枠があるが、パートの週の残り時間がその枠の実労働時間に足りないとき, When: 理由を取得すると,"
            + " Then: 氏名入りの文言にならず、より小さいずれの案が選ばれたと示す")
    void doesNotNameAssignedEmployeeWhenGapMatchesButCanAssignFailsForThatSlot() {
      List<ShiftAssignment> assignments = new ArrayList<>();
      assignments.add(exactMatchAssignment("E0", ShiftSlot.SLOT_1));
      assignments.add(exactMatchAssignment("E1", ShiftSlot.SLOT_1));
      assignments.add(exactMatchAssignment("E2", ShiftSlot.SLOT_2));
      // E3 は枠3（実労働時間435分）に8:00〜17:00（540分）で入り、ずれ60分
      assignments.add(
          new ShiftAssignment(
              Employee.working("E3", LocalTime.of(8, 0), LocalTime.of(17, 0)),
              ShiftSlot.SLOT_3,
              LocalTime.of(12, 0),
              LocalTime.of(12, 45)));
      assignments.add(exactMatchAssignment("E4", ShiftSlot.SLOT_4));
      assignments.add(exactMatchAssignment("E5", ShiftSlot.SLOT_5));
      assignments.add(exactMatchAssignment("E6", ShiftSlot.SLOT_6));
      assignments.add(exactMatchAssignment("E7", ShiftSlot.SLOT_6));

      // PartC は7:30〜16:30（540分）で枠1・2・3・4に入れる。ずれは枠3で60分（E3と同じ）。
      // 残り時間410分は枠1(375)・2(405)・4(405)には足りるが、枠3(435)には足りない
      Employee partC =
          Employee.working(
                  "PartC", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(16, 30))
              .withWeeklyRemainingMinutes(410);

      AssignmentResult result = new AssignmentResult(assignments, 0, List.of(partC));
      String label = result.unassignedReasonLabel(partC);

      assertEquals("入れる枠はあったが、より小さいずれの案が選ばれた", label);
    }

    private ShiftAssignment exactMatchAssignment(String name, ShiftSlot slot) {
      Employee employee = Employee.working(name, slot.startTime(), slot.endTime());
      return new ShiftAssignment(employee, slot, LocalTime.of(12, 0), LocalTime.of(12, 45));
    }
  }

  @Nested
  @DisplayName("[7.2] 未出勤の理由（これまでの出勤日数の優先）")
  class UnassignedReasonLabelForPriorWorkDays {

    private AssignmentResult resultWithUnassigned(Employee unassigned) {
      return new AssignmentResult(createStandardAssignments(), 0, List.of(unassigned));
    }

    @Test
    @DisplayName(
        "[7.2] Given: 未出勤者の出勤日数が割り当て済みの人より多いとき, When: 理由を取得すると, Then:" + " 出勤日数が少ない割り当て済みの人の氏名を示す")
    void namesAssignedEmployeeWithFewerPriorWorkDays() {
      // Employee0（割り当て済み）は出勤日数2。Itoは同じ660分の時間帯で枠1のずれが同じになるが出勤日数5（多い）
      Employee ito =
          Employee.working("Ito", LocalTime.of(7, 30), LocalTime.of(18, 30)).withPriorWorkDays(5);
      List<ShiftAssignment> assignments = createStandardAssignmentsWithPriorWorkDays(2);
      AssignmentResult result = new AssignmentResult(assignments, 0, List.of(ito));

      String label = result.unassignedReasonLabel(ito);

      assertEquals("入れる枠はあったが、同じずれの案があり、これまでの出勤日数が少ない Employee0 が選ばれた", label);
    }

    @Test
    @DisplayName(
        "[7.2] Given: 未出勤者がパートで出勤日数が割り当て済みの常勤より多いとき, When: 理由を取得すると, Then:"
            + " パートの文言ではなく出勤日数の文言が返る")
    void prefersPriorWorkDaysLabelOverPartTimeLabel() {
      Employee partA =
          Employee.working(
                  "PartA", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30))
              .withPriorWorkDays(5);
      List<ShiftAssignment> assignments = createStandardAssignmentsWithPriorWorkDays(2);
      AssignmentResult result = new AssignmentResult(assignments, 0, List.of(partA));

      String label = result.unassignedReasonLabel(partA);

      assertEquals("入れる枠はあったが、同じずれの案があり、これまでの出勤日数が少ない Employee0 が選ばれた", label);
    }

    @Test
    @DisplayName("[7.2] Given: 未出勤者と割り当て済みの人の出勤日数が同じとき, When: 理由を取得すると, Then: 既存の入力順の文言が返る")
    void fallsBackToInputOrderLabelWhenPriorWorkDaysAreEqual() {
      Employee ito =
          Employee.working("Ito", LocalTime.of(7, 30), LocalTime.of(18, 30)).withPriorWorkDays(3);
      List<ShiftAssignment> assignments = createStandardAssignmentsWithPriorWorkDays(3);
      AssignmentResult result = new AssignmentResult(assignments, 0, List.of(ito));

      String label = result.unassignedReasonLabel(ito);

      assertEquals("入れる枠はあったが、同じずれの案があり、入力順で優先度が高い Employee0 が選ばれた", label);
    }

    @Test
    @DisplayName(
        "[7.2] Given: 未出勤者・割り当て済みの人の出勤日数がnull（保存済みの古いデータ）のとき, When: 理由を取得すると,"
            + " Then: 出勤日数が同じものとして既存の入力順の文言が返る")
    void treatsNullPriorWorkDaysAsEqual() {
      Employee ito = Employee.working("Ito", LocalTime.of(7, 30), LocalTime.of(18, 30));

      String label = resultWithUnassigned(ito).unassignedReasonLabel(ito);

      assertEquals("入れる枠はあったが、同じずれの案があり、入力順で優先度が高い Employee0 が選ばれた", label);
    }

    private List<ShiftAssignment> createStandardAssignmentsWithPriorWorkDays(int days) {
      List<ShiftAssignment> assignments = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        Employee employee =
            Employee.working("Employee" + i, LocalTime.of(7, 30), LocalTime.of(18, 30))
                .withPriorWorkDays(days);
        assignments.add(
            new ShiftAssignment(
                employee,
                ShiftSlot.values()[Math.min(i, 5)],
                LocalTime.of(12, 0),
                LocalTime.of(12, 45)));
      }
      return assignments;
    }
  }

  private List<ShiftAssignment> createStandardAssignments() {
    List<ShiftAssignment> assignments = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      assignments.add(createAssignment(i, ShiftSlot.values()[Math.min(i, 5)]));
    }
    return assignments;
  }

  private ShiftAssignment createAssignment(int id, ShiftSlot slot) {
    Employee employee =
        Employee.working("Employee" + id, LocalTime.of(7, 30), LocalTime.of(18, 30));
    return new ShiftAssignment(employee, slot, LocalTime.of(12, 0), LocalTime.of(12, 45));
  }
}
