/**
 * 画面 3（結果）のタブ切り替え（7.1 節・8.3 節）。
 *
 * 切り替えのロジックは switchTab に分け、DOM 要素に見立てたオブジェクトでも動くようにしている。
 */
(function () {
  "use strict";

  const TABS = ["calendar", "employees", "detail"];

  // 画面 2 の入力カレンダーも calendar-day を使うため、結果カレンダー配下に限定する
  const RESULT_CALENDAR_DAY_SELECTOR = "#tab-calendar .calendar-day";

  /**
   * 指定したタブのパネルだけを表示し、そのタブのボタンを選択状態にする。
   * 未知のタブなら何も変えずに false を返す。
   */
  function switchTab(tab, buttons, panels) {
    if (TABS.indexOf(tab) < 0) {
      return false;
    }
    buttons.forEach(function (button) {
      const active = button.getAttribute("data-tab") === tab;
      button.classList.toggle("active", active);
      button.setAttribute("aria-selected", String(active));
    });
    panels.forEach(function (panel) {
      panel.hidden = panel.id !== "tab-" + tab;
    });
    return true;
  }

  /** HH:mm を 0 時からの分に直す。 */
  function toMinutes(time) {
    const parts = time.split(":");
    return Number(parts[0]) * 60 + Number(parts[1]);
  }

  /** 開始と終了（HH:mm）の差を、前 0 埋めの hh:mm で返す（7.2 節の合計時間）。 */
  function formatDuration(start, end) {
    const minutes = toMinutes(end) - toMinutes(start);
    const hours = Math.floor(minutes / 60);
    return String(hours).padStart(2, "0") + ":" + String(minutes % 60).padStart(2, "0");
  }

  /** 選んだ日付の日別詳細だけを表示する。 */
  function showDay(date, sections) {
    sections.forEach(function (section) {
      section.hidden = section.getAttribute("data-date") !== date;
    });
  }

  if (typeof module !== "undefined" && module.exports) {
    module.exports = { switchTab, formatDuration, showDay, RESULT_CALENDAR_DAY_SELECTOR };
  }

  if (typeof document === "undefined") {
    return;
  }

  /** 日別詳細のタブを開く。日付の選択は日別詳細側（detail-date）と連動する。 */
  function openDetail(date) {
    const buttons = Array.from(document.querySelectorAll("[data-tab]"));
    const panels = Array.from(document.querySelectorAll(".tab-panel"));
    switchTab("detail", buttons, panels);
    const select = document.getElementById("detail-date");
    if (select && date) {
      select.value = date;
      select.dispatchEvent(new Event("change"));
    }
  }

  window.openDetail = openDetail;

  document.addEventListener("DOMContentLoaded", function () {
    const buttons = Array.from(document.querySelectorAll("[data-tab]"));
    const panels = Array.from(document.querySelectorAll(".tab-panel"));
    buttons.forEach(function (button) {
      button.addEventListener("click", function () {
        switchTab(button.getAttribute("data-tab"), buttons, panels);
      });
    });
    // 日別詳細：合計時間を埋め、日付の選択とセクションの表示を連動させる
    document.querySelectorAll(".duration").forEach(function (span) {
      span.textContent =
        "(" + formatDuration(span.getAttribute("data-start"), span.getAttribute("data-end")) + ")";
    });
    const detailDate = document.getElementById("detail-date");
    const daySections = Array.from(document.querySelectorAll(".day-detail"));
    if (detailDate) {
      detailDate.addEventListener("change", function () {
        showDay(detailDate.value, daySections);
      });
      showDay(detailDate.value, daySections);
    }
    // カレンダーの日付ボタンを押すと、その日の日別詳細を開く（8.3 節）
    document.addEventListener("click", function (event) {
      const dayButton = event.target.closest(RESULT_CALENDAR_DAY_SELECTOR);
      if (dayButton) {
        openDetail(dayButton.getAttribute("data-date"));
      }
    });
  });
})();
