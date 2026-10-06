CREATE DATABASE IF NOT EXISTS medibook;

USE medibook;


-- =========================================================
-- DOCTORS TABLE
-- =========================================================

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


-- =========================================================
-- USERS TABLE
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,

    username VARCHAR(100) NOT NULL UNIQUE,

    password VARCHAR(255) NOT NULL
);

INSERT IGNORE INTO users (username, password)
VALUES ('admin', 'admin123');


-- =========================================================
-- OLD PAYMENT TABLE REMOVE
-- =========================================================

DROP TABLE IF EXISTS payments;


-- =========================================================
-- OLD APPOINTMENTS TABLE REMOVE
-- =========================================================

DROP TABLE IF EXISTS appointments;


-- =========================================================
-- APPOINTMENTS TABLE
-- =========================================================

CREATE TABLE appointments (

    id INT PRIMARY KEY AUTO_INCREMENT,


    -- Follow-up relationship
    parent_appointment_id INT NULL,

    appointment_type VARCHAR(20)
        NOT NULL DEFAULT 'NEW',

    followup_days INT NULL,

    followup_reason VARCHAR(255) NULL,


    -- Patient information
    patient_name VARCHAR(100) NOT NULL,

    age INT NOT NULL,

    phone VARCHAR(20) NOT NULL,

    gender VARCHAR(20),


    -- Doctor
    doctor_id INT NOT NULL,


    -- Appointment
    appointment_date DATE NOT NULL,

    appointment_time VARCHAR(20) NOT NULL,


    -- Total consultation fee
    fee DECIMAL(10,2) DEFAULT 0,


    -- Appointment status
    status VARCHAR(20) DEFAULT 'Booked',


    -- Created time
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,


    -- =====================================================
    -- DOCTOR RELATION
    -- =====================================================

    CONSTRAINT fk_appointments_doctor

        FOREIGN KEY (doctor_id)

        REFERENCES doctors(id)

        ON UPDATE CASCADE

        ON DELETE RESTRICT,


    -- =====================================================
    -- FOLLOW-UP RELATION
    -- =====================================================

    CONSTRAINT fk_parent_appointment

        FOREIGN KEY (parent_appointment_id)

        REFERENCES appointments(id)

        ON UPDATE CASCADE

        ON DELETE SET NULL,


    -- =====================================================
    -- SLOT INDEX
    -- =====================================================

    INDEX idx_doctor_slot (

        doctor_id,

        appointment_date,

        appointment_time,

        status

    )

) ENGINE=InnoDB;


-- =========================================================
-- PAYMENTS TABLE
-- =========================================================

CREATE TABLE payments (

    id INT PRIMARY KEY AUTO_INCREMENT,


    -- Which appointment this payment belongs to
    appointment_id INT NOT NULL,


    -- Payment amount
    amount DECIMAL(10,2) NOT NULL,


    -- Cash / UPI / Card / Online
    payment_method VARCHAR(30)
        DEFAULT 'Cash',


    -- Paid / Failed / Pending
    payment_status VARCHAR(20)
        DEFAULT 'Paid',


    -- Optional note
    payment_note VARCHAR(255) NULL,


    -- Payment date & time
    paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,


    -- Appointment relation
    CONSTRAINT fk_payments_appointment

        FOREIGN KEY (appointment_id)

        REFERENCES appointments(id)

        ON UPDATE CASCADE

        ON DELETE CASCADE,


    -- Fast lookup by appointment
    INDEX idx_payments_appointment (

        appointment_id

    )

) ENGINE=InnoDB;