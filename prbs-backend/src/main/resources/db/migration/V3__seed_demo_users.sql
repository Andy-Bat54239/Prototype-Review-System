-- Demo users mirror MOCK_USERS in prbs-app/src/data.js. Lets the React app log
-- in immediately without an admin needing to onboard anyone first.
INSERT INTO users (name, email, role, status, created_at, updated_at) VALUES
    ('Alice Uwase',      'alice@university.ac.rw',      'STUDENT',    'ACTIVE',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('James Nkosi',      'james@university.ac.rw',      'STUDENT',    'ACTIVE',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Fatima Diallo',    'fatima@university.ac.rw',     'STUDENT',    'ACTIVE',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Kwame Asante',     'kwame@university.ac.rw',      'STUDENT',    'ACTIVE',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Priya Sharma',     'priya@university.ac.rw',      'STUDENT',    'ACTIVE',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Chidi Okafor',     'chidi@university.ac.rw',      'STUDENT',    'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dr. Sarah Mensah', 'supervisor@university.ac.rw', 'SUPERVISOR', 'ACTIVE',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Admin User',       'admin@university.ac.rw',      'ADMIN',      'ACTIVE',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
