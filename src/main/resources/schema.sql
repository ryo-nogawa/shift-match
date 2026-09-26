DROP TABLE IF EXISTS saved_employee;
DROP TABLE IF EXISTS saved_assignment;
DROP TABLE IF EXISTS saved_score;

CREATE TABLE IF NOT EXISTS saved_input_employee (
  row_index INT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  employment_type VARCHAR(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_input_base_shift (
  row_index INT NOT NULL,
  day_index INT NOT NULL,
  off BOOLEAN NOT NULL,
  start_time TIME NULL,
  end_time TIME NULL,
  PRIMARY KEY (row_index, day_index)
);

CREATE TABLE IF NOT EXISTS saved_input_meta (
  id INT PRIMARY KEY CHECK (id = 1),
  last_target_month VARCHAR(7) NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_adjustment (
  adjust_date DATE NOT NULL,
  employee_name VARCHAR(255) NOT NULL,
  off BOOLEAN NOT NULL,
  start_time TIME NULL,
  end_time TIME NULL,
  PRIMARY KEY (adjust_date, employee_name)
);

CREATE TABLE IF NOT EXISTS saved_month_employee (
  target_month VARCHAR(7) NOT NULL,
  row_index INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (target_month, row_index)
);

CREATE TABLE IF NOT EXISTS saved_day (
  day_date DATE PRIMARY KEY,
  target_month VARCHAR(7) NOT NULL,
  available_count INT NOT NULL,
  score INT NULL
);

CREATE TABLE IF NOT EXISTS saved_day_assignment (
  day_date DATE NOT NULL,
  assignment_index INT NOT NULL,
  employee_name VARCHAR(255) NOT NULL,
  employment_type VARCHAR(16) NOT NULL,
  wish_start TIME NOT NULL,
  wish_end TIME NOT NULL,
  slot VARCHAR(16) NOT NULL,
  break_start TIME NOT NULL,
  break_end TIME NOT NULL,
  PRIMARY KEY (day_date, assignment_index)
);

CREATE TABLE IF NOT EXISTS saved_day_unassigned (
  day_date DATE NOT NULL,
  unassigned_index INT NOT NULL,
  employee_name VARCHAR(255) NOT NULL,
  employment_type VARCHAR(16) NOT NULL,
  off BOOLEAN NOT NULL,
  wish_start TIME NULL,
  wish_end TIME NULL,
  reason VARCHAR(32) NOT NULL,
  PRIMARY KEY (day_date, unassigned_index)
);

CREATE TABLE IF NOT EXISTS holiday (
  holiday_date DATE PRIMARY KEY,
  name VARCHAR(64) NOT NULL
);
