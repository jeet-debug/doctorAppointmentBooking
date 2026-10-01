CREATE DATABASE IF NOT EXISTS medibook;

USE medibook;

-- Doctors table
CREATE TABLE IF NOT EXISTS doctors (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    image_path VARCHAR(500) NULL,                                                   
    gender VARCHAR(20),
    experience INT DEFAULT 0,
    consultation_fee DECIMAL(10,2) DEFAULT 0,
    available_days VARCHAR(100),
    available_time VARCHAR(100),
    status VARCHAR(20) DEFAULT 'Active'
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

INSERT IGNORE INTO users (username, password)
VALUES ('admin', 'admin123');

-- Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id INT PRIMARY KEY AUTO_INCREMENT,

    patient_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    gender VARCHAR(20),

    doctor_id INT NOT NULL,

    appointment_date DATE NOT NULL,
    appointment_time VARCHAR(20) NOT NULL,

    fee DECIMAL(10,2) DEFAULT 0,

    status VARCHAR(20) DEFAULT 'Booked',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointments_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctors(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    UNIQUE KEY uq_doctor_slot
        (doctor_id, appointment_date, appointment_time, status)
) ENGINE=InnoDB;