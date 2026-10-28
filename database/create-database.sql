-- Chạy trong SSMS; KHÔNG xóa hoặc thay đổi database cũ.
IF DB_ID(N'UTETra_Complete') IS NULL
    CREATE DATABASE UTETra_Complete;
GO
-- Chạy ứng dụng với profile sqlserver để JPA tạo bảng trong database mới.
-- Không chạy schema mới trực tiếp lên database cũ trước khi backup/migration.
