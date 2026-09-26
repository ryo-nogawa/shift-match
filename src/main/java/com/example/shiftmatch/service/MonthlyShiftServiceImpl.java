package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 月間シフト作成サービスの実装。
 *
 * <p>営業日ごとに最適なシフト割り当て案を算出します。
 */
@Service
public class MonthlyShiftServiceImpl implements MonthlyShiftService {

  private final HolidayService holidayService;
  private final ShiftAssignmentService assignmentService;
  private final WishResolver wishResolver;

  /**
   * 月間シフト作成サービスを初期化します。
   *
   * @param holidayService 祝日サービス
   * @param assignmentService 割り当てサービス
   */
  public MonthlyShiftServiceImpl(
      HolidayService holidayService, ShiftAssignmentService assignmentService) {
    this.holidayService = holidayService;
    this.assignmentService = assignmentService;
    this.wishResolver = new WishResolver();
  }

  @Override
  public MonthlyShiftResult create(MonthlyShiftInput input) {
    List<DailyShiftResult> results = new ArrayList<>();

    // 営業日ごとに独立して割り当てを算出
    for (LocalDate businessDay : holidayService.businessDays(input.month())) {
      DailyShiftResult dayResult = createForDay(businessDay, input);
      results.add(dayResult);
    }

    return new MonthlyShiftResult(input.month(), results);
  }

  private DailyShiftResult createForDay(LocalDate date, MonthlyShiftInput input) {
    // 従業員のうち、名前が空でないものだけを対象
    List<Employee> employees = new ArrayList<>();
    for (EmployeeProfile profile : input.employees()) {
      if (!profile.name().isBlank()) {
        DailyWish wish = wishResolver.resolve(profile, date, input.adjustments());
        employees.add(convertToEmployee(profile, wish));
      }
    }

    // 勤務できる人の数（有効な従業員のうち休みでない人）
    int availableCount = (int) employees.stream().filter(e -> !e.off()).count();

    // 割り当てを算出
    var assignment = assignmentService.assign(employees);

    return new DailyShiftResult(date, availableCount, assignment);
  }

  private Employee convertToEmployee(EmployeeProfile profile, DailyWish wish) {
    if (wish.off()) {
      return Employee.onLeave(profile.name(), profile.employmentType());
    }
    return Employee.working(profile.name(), profile.employmentType(), wish.start(), wish.end());
  }
}
