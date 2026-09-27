/**
 * 画面 1 の従業員一覧（F-1・F-2・F-6・F-8、8.1 節・8.5 節）。
 *
 * 行（#employee-rows の .employee-row）には、氏名・区分・曜日休み（パートだけ）の入力がある。
 */
(function () {
  "use strict";

  const MAX_ROWS = 12;
  const DAY_LABELS = ["月", "火", "水", "木", "金"];
  const PART_TIME = "PART_TIME";

  /**
   * name 属性の従業員インデックスを振り替える。Spring の checkbox 用マーカー（先頭の "_"）も対象。
   */
  function rewriteEmployeeIndex(name, index) {
    return name.replace(/^(_?)employees\[\d+\]/, "$1employees[" + index + "]");
  }

  /**
   * 行ごとの入力（氏名・区分・曜日休み）の name を、並び順どおり 0 から欠番なく振り直す（8.5 節）。
   * 要素は name プロパティを持つものなら何でもよい。曜日休みの値（0〜4）は変えない。
   */
  function renumber(rows) {
    rows.forEach(function (row, index) {
      row.rowFields.forEach(function (field) {
        field.name = rewriteEmployeeIndex(field.name, index);
      });
    });
  }

  /**
   * 曜日休みのチェックボックスを区分に合わせる（F-1）。パート以外は、チェックを外して無効にする。
   */
  function syncOffDays(employmentType, checkboxes) {
    const isPartTime = employmentType === PART_TIME;
    checkboxes.forEach(function (checkbox) {
      checkbox.disabled = !isPartTime;
      if (!isPartTime) {
        checkbox.checked = false;
      }
    });
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
    module.exports = { rewriteEmployeeIndex, renumber, syncOffDays, buttonStates, MAX_ROWS };
  }

  if (typeof document === "undefined") {
    return;
  }

  document.addEventListener("DOMContentLoaded", function () {
    const rowsContainer = document.getElementById("employee-rows");
    const addButton = document.getElementById("add-employee-btn");
    let nextRowId = 0;
    let selectedRowId = null;

    function rows() {
      return Array.from(rowsContainer.querySelectorAll(".employee-row"));
    }

    function offDayBoxesOf(row) {
      return Array.from(row.querySelectorAll(".off-day-checkbox"));
    }

    function notifyChanged() {
      document.dispatchEvent(new CustomEvent("employees-changed"));
    }

    function refreshRows() {
      const currentRows = rows();
      renumber(
        currentRows.map(function (row) {
          return { rowFields: Array.from(row.querySelectorAll("[name]")) };
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

    // 月〜金のチェックボックス（値 0〜4）。既定は区分が常勤なので無効
    function createOffDaysCell() {
      const td = document.createElement("td");
      td.className = "off-days-cell";
      DAY_LABELS.forEach(function (label, d) {
        const wrapper = document.createElement("label");
        wrapper.className = "off-day";
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.className = "off-day-checkbox";
        checkbox.name = "employees[0].offDays";
        checkbox.value = String(d);
        wrapper.appendChild(checkbox);
        wrapper.appendChild(document.createTextNode(label));
        td.appendChild(wrapper);
      });
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

      row.appendChild(createOffDaysCell());
      syncOffDays(typeSelect.value, offDayBoxesOf(row));

      row.appendChild(cell(createButton("delete-btn", "削除")));
      return row;
    }

    function addRow() {
      if (rows().length >= MAX_ROWS) {
        return;
      }
      // サーバーが付けた data-row-id（0 始まりの番号）と重ならないよう、追加行は "r" 付きの ID にする
      const rowId = "r" + nextRowId++;
      rowsContainer.appendChild(createRow(rowId));
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
      row.remove();
      refreshRows();
      if (selectedRowId === rowId) {
        const remaining = rows();
        selectRow(remaining[Math.min(index, remaining.length - 1)].getAttribute("data-row-id"));
      }
      notifyChanged();
    }

    // 行の DOM ごと動かすので、曜日休みのチェック状態も行と一緒に移動する
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

    rowsContainer.addEventListener("change", function (event) {
      const row = event.target.closest(".employee-row");
      if (!row) {
        return;
      }
      if (event.target.classList.contains("type-select")) {
        syncOffDays(event.target.value, offDayBoxesOf(row));
        notifyChanged();
      } else if (event.target.classList.contains("off-day-checkbox")) {
        notifyChanged();
      }
    });

    addButton.addEventListener("click", addRow);

    rows().forEach(function (row) {
      syncOffDays(row.querySelector(".type-select").value, offDayBoxesOf(row));
    });
    refreshRows();
    const first = rows()[0];
    if (first) {
      selectRow(first.getAttribute("data-row-id"));
    }
  });
})();
