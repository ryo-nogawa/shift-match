/**
 * 画面 1 の従業員一覧と基本シフトパネル（F-2・F-6・F-8、8.1 節・8.5 節）。
 *
 * 行（#employee-rows の .employee-row）と基本シフトパネル（#base-panels の .base-panel）は
 * 別々の要素なので、常に data-row-id で対応づける。
 */
(function () {
  "use strict";

  const MAX_ROWS = 12;
  const DAY_LABELS = ["月", "火", "水", "木", "金"];
  const DEFAULT_START = "07:30";
  const DEFAULT_END = "18:30";

  /**
   * name 属性の従業員インデックスを振り替える。Spring の checkbox 用マーカー（先頭の "_"）も対象。
   */
  function rewriteEmployeeIndex(name, index) {
    return name.replace(/^(_?)employees\[\d+\]/, "$1employees[" + index + "]");
  }

  /**
   * 行ごとの入力（氏名・区分）とパネルの入力（曜日ごとの start/end）の name を、
   * 並び順どおり 0 から欠番なく振り直す（8.5 節）。要素は name プロパティを持つものなら何でもよい。
   */
  function renumber(rows) {
    rows.forEach(function (row, index) {
      row.rowFields.concat(row.panelFields).forEach(function (field) {
        field.name = rewriteEmployeeIndex(field.name, index);
      });
    });
  }

  /** 基本シフトの要約（例：07:30〜18:30）を作る（8.1 節）。基本シフトに休みはない。 */
  function summarize(days) {
    const starts = [];
    const ends = [];
    days.forEach(function (day) {
      if (day.start) {
        starts.push(day.start);
      }
      if (day.end) {
        ends.push(day.end);
      }
    });
    // HH:mm 形式はゼロ埋めされているので、文字列の大小比較で時刻の前後を判定できる
    starts.sort();
    ends.sort();
    return starts.length > 0 && ends.length > 0
      ? starts[0] + "〜" + ends[ends.length - 1]
      : "未選択";
  }

  /** 行数に応じたボタンの有効・無効を返す（F-2・F-6・F-8）。 */
  function buttonStates(rowCount) {
    const states = [];
    for (let i = 0; i < rowCount; i++) {
      states.push({
        upDisabled: i === 0,
        downDisabled: i === rowCount - 1,
        deleteDisabled: rowCount <= 1,
      });
    }
    return { rows: states, addDisabled: rowCount >= MAX_ROWS };
  }

  if (typeof module !== "undefined" && module.exports) {
    module.exports = { rewriteEmployeeIndex, renumber, summarize, buttonStates, MAX_ROWS };
  }

  if (typeof document === "undefined") {
    return;
  }

  document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form");
    const rowsContainer = document.getElementById("employee-rows");
    const panelsContainer = document.getElementById("base-panels");
    const addButton = document.getElementById("add-employee-btn");
    const timeOptions = (form.getAttribute("data-time-options") || "")
      .split("|")
      .filter(function (value) {
        return value !== "";
      });
    let nextRowId = 0;
    let selectedRowId = null;

    function rows() {
      return Array.from(rowsContainer.querySelectorAll(".employee-row"));
    }

    function panelOf(rowId) {
      return (
        Array.from(panelsContainer.querySelectorAll(".base-panel")).find(function (panel) {
          return panel.getAttribute("data-row-id") === rowId;
        }) || null
      );
    }

    function readDays(panel) {
      return Array.from(panel.querySelectorAll(".day-row")).map(function (dayRow) {
        return {
          start: dayRow.querySelector(".start-select").value,
          end: dayRow.querySelector(".end-select").value,
        };
      });
    }

    function notifyChanged() {
      document.dispatchEvent(new CustomEvent("employees-changed"));
    }

    function updateSummary(row) {
      const panel = panelOf(row.getAttribute("data-row-id"));
      if (panel) {
        row.querySelector(".summary").textContent = summarize(readDays(panel));
      }
    }

    function refreshRows() {
      const currentRows = rows();
      renumber(
        currentRows.map(function (row) {
          const panel = panelOf(row.getAttribute("data-row-id"));
          return {
            rowFields: Array.from(row.querySelectorAll("[name]")),
            panelFields: panel ? Array.from(panel.querySelectorAll("[name]")) : [],
          };
        })
      );
      const states = buttonStates(currentRows.length);
      currentRows.forEach(function (row, i) {
        row.querySelector(".move-up-btn").disabled = states.rows[i].upDisabled;
        row.querySelector(".move-down-btn").disabled = states.rows[i].downDisabled;
        row.querySelector(".delete-btn").disabled = states.rows[i].deleteDisabled;
      });
      addButton.disabled = states.addDisabled;
    }

    function selectRow(rowId) {
      selectedRowId = rowId;
      rows().forEach(function (row) {
        row.classList.toggle("selected", row.getAttribute("data-row-id") === rowId);
      });
      panelsContainer.querySelectorAll(".base-panel").forEach(function (panel) {
        panel.hidden = panel.getAttribute("data-row-id") !== rowId;
      });
    }

    function createTimeSelect(className, selected) {
      const select = document.createElement("select");
      select.className = className;
      const empty = document.createElement("option");
      empty.value = "";
      empty.textContent = "-- 未選択 --";
      select.appendChild(empty);
      timeOptions.forEach(function (time) {
        const option = document.createElement("option");
        option.value = time;
        option.textContent = time;
        select.appendChild(option);
      });
      select.value = selected;
      return select;
    }

    function createButton(className, label) {
      const button = document.createElement("button");
      button.type = "button";
      button.className = className;
      button.textContent = label;
      return button;
    }

    function cell(child) {
      const td = document.createElement("td");
      if (child) {
        td.appendChild(child);
      }
      return td;
    }

    // name の番号は refreshRows で振り直すので、ここでは仮に 0 を付ける
    function createRow(rowId) {
      const row = document.createElement("tr");
      row.className = "employee-row";
      row.setAttribute("data-row-id", rowId);

      const moveCell = document.createElement("td");
      moveCell.className = "button-cell";
      moveCell.appendChild(createButton("move-up-btn", "▲"));
      moveCell.appendChild(createButton("move-down-btn", "▼"));
      row.appendChild(moveCell);

      const nameInput = document.createElement("input");
      nameInput.type = "text";
      nameInput.className = "name-input";
      nameInput.name = "employees[0].name";
      row.appendChild(cell(nameInput));

      const typeSelect = document.createElement("select");
      typeSelect.className = "type-select";
      typeSelect.name = "employees[0].employmentType";
      const template = rowsContainer.querySelector(".type-select");
      Array.from(template.options).forEach(function (source) {
        const option = document.createElement("option");
        option.value = source.value;
        option.textContent = source.textContent;
        typeSelect.appendChild(option);
      });
      typeSelect.value = "FULL_TIME";
      row.appendChild(cell(typeSelect));

      const summary = cell(null);
      summary.className = "summary";
      row.appendChild(summary);

      row.appendChild(cell(createButton("delete-btn", "削除")));
      return row;
    }

    function createPanel(rowId) {
      const panel = document.createElement("div");
      panel.className = "base-panel";
      panel.setAttribute("data-row-id", rowId);
      panel.hidden = true;

      const table = document.createElement("table");
      const head = document.createElement("thead");
      const headRow = document.createElement("tr");
      ["曜日", "開始", "終了", ""].forEach(function (label) {
        const th = document.createElement("th");
        th.textContent = label;
        headRow.appendChild(th);
      });
      head.appendChild(headRow);
      table.appendChild(head);

      const body = document.createElement("tbody");
      DAY_LABELS.forEach(function (label, d) {
        const dayRow = document.createElement("tr");
        dayRow.className = "day-row";
        const labelCell = cell(null);
        labelCell.textContent = label;
        dayRow.appendChild(labelCell);

        const start = createTimeSelect("start-select", DEFAULT_START);
        start.name = "employees[0].days[" + d + "].start";
        dayRow.appendChild(cell(start));
        const end = createTimeSelect("end-select", DEFAULT_END);
        end.name = "employees[0].days[" + d + "].end";
        dayRow.appendChild(cell(end));

        dayRow.appendChild(cell(createButton("copy-to-all-btn", "全曜日へ")));
        body.appendChild(dayRow);
      });
      table.appendChild(body);
      panel.appendChild(table);
      return panel;
    }

    function addRow() {
      if (rows().length >= MAX_ROWS) {
        return;
      }
      // サーバーが付けた data-row-id（0 始まりの番号）と重ならないよう、追加行は "r" 付きの ID にする
      const rowId = "r" + nextRowId++;
      const row = createRow(rowId);
      rowsContainer.appendChild(row);
      panelsContainer.appendChild(createPanel(rowId));
      updateSummary(row);
      refreshRows();
      selectRow(rowId);
      notifyChanged();
    }

    function deleteRow(row) {
      const currentRows = rows();
      if (currentRows.length <= 1) {
        return;
      }
      const rowId = row.getAttribute("data-row-id");
      const index = currentRows.indexOf(row);
      const panel = panelOf(rowId);
      if (panel) {
        panel.remove();
      }
      row.remove();
      refreshRows();
      if (selectedRowId === rowId) {
        const remaining = rows();
        selectRow(remaining[Math.min(index, remaining.length - 1)].getAttribute("data-row-id"));
      }
      notifyChanged();
    }

    function moveRow(row, delta) {
      const sibling = delta < 0 ? row.previousElementSibling : row.nextElementSibling;
      if (!sibling) {
        return;
      }
      if (delta < 0) {
        sibling.before(row);
      } else {
        sibling.after(row);
      }
      refreshRows();
      notifyChanged();
    }

    function copyToAllDays(panel, sourceRow) {
      const start = sourceRow.querySelector(".start-select").value;
      const end = sourceRow.querySelector(".end-select").value;
      panel.querySelectorAll(".day-row").forEach(function (dayRow) {
        dayRow.querySelector(".start-select").value = start;
        dayRow.querySelector(".end-select").value = end;
      });
    }

    function rowOfPanel(panel) {
      const rowId = panel.getAttribute("data-row-id");
      return (
        rows().find(function (row) {
          return row.getAttribute("data-row-id") === rowId;
        }) || null
      );
    }

    rowsContainer.addEventListener("click", function (event) {
      const row = event.target.closest(".employee-row");
      if (!row) {
        return;
      }
      if (event.target.closest(".move-up-btn")) {
        moveRow(row, -1);
      } else if (event.target.closest(".move-down-btn")) {
        moveRow(row, 1);
      } else if (event.target.closest(".delete-btn")) {
        deleteRow(row);
        return;
      }
      selectRow(row.getAttribute("data-row-id"));
    });

    rowsContainer.addEventListener("focusin", function (event) {
      const row = event.target.closest(".employee-row");
      if (row) {
        selectRow(row.getAttribute("data-row-id"));
      }
    });

    rowsContainer.addEventListener("input", function (event) {
      if (event.target.classList.contains("name-input")) {
        notifyChanged();
      }
    });

    panelsContainer.addEventListener("change", function (event) {
      const panel = event.target.closest(".base-panel");
      if (!panel) {
        return;
      }
      const row = rowOfPanel(panel);
      if (row) {
        updateSummary(row);
      }
      notifyChanged();
    });

    panelsContainer.addEventListener("click", function (event) {
      const button = event.target.closest(".copy-to-all-btn");
      if (!button) {
        return;
      }
      const panel = button.closest(".base-panel");
      copyToAllDays(panel, button.closest(".day-row"));
      const row = rowOfPanel(panel);
      if (row) {
        updateSummary(row);
      }
      notifyChanged();
    });

    addButton.addEventListener("click", addRow);

    rows().forEach(function (row) {
      const panel = panelOf(row.getAttribute("data-row-id"));
      updateSummary(row);
    });
    refreshRows();
    const first = rows()[0];
    if (first) {
      selectRow(first.getAttribute("data-row-id"));
    }
  });
})();
