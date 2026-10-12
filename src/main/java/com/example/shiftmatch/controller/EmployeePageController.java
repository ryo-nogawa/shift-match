package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.HolidayDataUnavailableError;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import com.example.shiftmatch.service.HolidayService;
import com.example.shiftmatch.service.ShiftStorageService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * 従業員の個別ページ（7.3 節）のコントローラーです。
 */
@Controller
public class EmployeePageController {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmployeePageController.class);

  private static final String NOT_CREATED_MESSAGE = "この月のシフトはまだ作成されていません";

  private static final String EMPLOYEE_NOT_FOUND_MESSAGE = "この月のシフトに該当する従業員はいません";

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
   * @param response HTTP レスポンス（該当する従業員がいないとき 404 を設定する）
   * @return ビュー名
   * @throws ResponseStatusException month が不正、または祝日データの収録範囲外の場合（400）
   */
  @GetMapping("/employee")
  public String employee(
      @RequestParam("month") String month,
      @RequestParam(name = "name", required = false) String name,
      Model model,
      HttpServletResponse response) {
    YearMonth yearMonth =
        InputParsers.parseYearMonth(month)
            .filter(value -> CalendarController.isWithinHolidayDataRange(value))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    model.addAttribute("backMonth", yearMonth.toString());
    Optional<SavedMonthlyShift> saved = shiftStorageService.load(yearMonth);
    if (saved.isEmpty()) {
      model.addAttribute("notice", NOT_CREATED_MESSAGE);
      return "employee";
    }
    Optional<MonthEmployee> employee =
        saved.get().employees().stream()
            .filter(candidate -> candidate.name().equals(name))
            .findFirst();
    if (employee.isEmpty()) {
      response.setStatus(HttpStatus.NOT_FOUND.value());
      model.addAttribute("notice", EMPLOYEE_NOT_FOUND_MESSAGE);
      return "employee";
    }
    model.addAttribute(
        "employeePage",
        employeePageViewFactory.create(
            saved.get().result(), employee.get(), holidaysOrEmpty(yearMonth)));
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
