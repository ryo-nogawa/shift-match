CREATE TABLE IF NOT EXISTS saved_employee (
  row_index INT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  off BOOLEAN NOT NULL,
  start_time TIME NULL,
  end_time TIME NULL
);

CREATE TABLE IF NOT EXISTS saved_assignment (
  assignment_index INT PRIMARY KEY,
  employee_name VARCHAR(255) NOT NULL,
  slot VARCHAR(16) NOT NULL,
  break_start TIME NOT NULL,
  break_end TIME NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_score (
  id INT PRIMARY KEY CHECK (id = 1),
  score INT NOT NULL
);
