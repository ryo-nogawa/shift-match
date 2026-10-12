package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.HolidayDataUnavailableError;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import com.example.shiftmatch.service.HolidayService;
import com.example.shiftmatch.service.ShiftStorageService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 従業員の個別ページ（7.3 節）のコントローラーです。
 */
@Controller
public class EmployeePageController {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmployeePageController.class);

  private final ShiftStorageService shiftStorageService;

  private final HolidayService holidayService;

  private final EmployeePageViewFactory employeePageViewFactory;

  /**
   * コンストラクタです。
   *
   * @param shiftStorageService シフトの保存・復元サービス
   * @param holidayService 祝日サービス
   * @param employeePageViewFactory 個別ページの表示モデルの生成
   */
  @Autowired
  public EmployeePageController(
      ShiftStorageService shiftStorageService,
      HolidayService holidayService,
      EmployeePageViewFactory employeePageViewFactory) {
    this.shiftStorageService = shiftStorageService;
    this.holidayService = holidayService;
    this.employeePageViewFactory = employeePageViewFactory;
  }

  /**
   * 従業員の個別ページを表示します。
   *
   * @param month 対象月（YYYY-MM 形式）
   * @param name 従業員名
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @GetMapping("/employee")
  public String employee(
      @RequestParam("month") String month, @RequestParam("name") String name, Model model) {
    YearMonth yearMonth = InputParsers.parseYearMonth(month).orElseThrow();
    SavedMonthlyShift saved = shiftStorageService.load(yearMonth).orElseThrow();
    MonthEmployee employee =
        saved.employees().stream()
            .filter(candidate -> candidate.name().equals(name))
            .findFirst()
            .orElseThrow();
    model.addAttribute(
        "employeePage",
        employeePageViewFactory.create(saved.result(), employee, holidaysOrEmpty(yearMonth)));
    return "employee";
  }

  private Map<LocalDate, String> holidaysOrEmpty(YearMonth month) {
    try {
      return holidayService.holidaysOf(month);
    } catch (HolidayDataUnavailableError e) {
      LOGGER.warn("祝日データが取得できないため、祝日なしで表示します", e);
      return Map.of();
    }
  }
}
