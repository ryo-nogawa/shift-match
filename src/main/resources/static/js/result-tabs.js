/**
 * 画面 3（結果）のタブ切り替え（7.1 節・8.3 節）。
 *
 * 切り替えのロジックは switchTab に分け、DOM 要素に見立てたオブジェクトでも動くようにしている。
 */
(function () {
  "use strict";

  const TABS = ["calendar", "employees", "detail"];

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

  if (typeof module !== "undefined" && module.exports) {
    module.exports = { switchTab };
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
  });
})();
