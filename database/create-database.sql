SELECT 'CREATE DATABASE library_db'
WHERE NOT EXISTS (
    SELECT FROM pg_database
    WHERE datname = 'library_db'
)
\gexec