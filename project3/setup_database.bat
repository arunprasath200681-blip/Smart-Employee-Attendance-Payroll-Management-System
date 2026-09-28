@echo off
title Setup Database - Smart Payroll System
color 0B

echo ==============================================================
echo  Initializing MySQL Database: smart_payroll_db
echo ==============================================================
echo.

set MYSQL_EXE="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

if not exist %MYSQL_EXE% (
    set MYSQL_EXE=mysql
)

echo Executing database.sql ...
%MYSQL_EXE% -u root -pArun@2006 < database.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Database and sample tables created successfully!
) else (
    echo.
    echo [NOTICE] If prompt asked for password, please enter your MySQL root password.
)

echo.
pause
