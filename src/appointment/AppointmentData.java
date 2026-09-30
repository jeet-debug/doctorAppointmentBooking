package appointment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared data for both panels (doctors list + booked appointments).
 * Abhi sab memory me hai. Baad me database lagana ho to sirf yahi file badlegi.
 */
public class AppointmentData {

  public static class Doctor {
    public final String name, spec;
    public final int fee;

    public Doctor(String name, String spec, int fee) {
      this.name = name;
      this.spec = spec;
      this.fee = fee;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static class Booking {
    public int id;
    public String patient, phone, gender, time, status = "Booked";
    public int age;
    public Doctor doctor;
    public LocalDate date;
  }

  public static final String[] SPECS = {
      "Heart (Cardiologist)", "Skin (Dermatologist)", "Bones (Orthopedic)",
      "Child (Pediatrician)", "Brain (Neurologist)", "Eye (Ophthalmologist)",
      "Dental (Dentist)", "Women's Health (Gynecologist)", "General Physician"
  };

  public static final List<Doctor> DOCTORS = new ArrayList<>();

  static {
    DOCTORS.add(new Doctor("Dr. Rajesh Sharma", SPECS[0], 800));
    DOCTORS.add(new Doctor("Dr. Anita Verma", SPECS[0], 900));
    DOCTORS.add(new Doctor("Dr. Sunil Kumar", SPECS[1], 500));
    DOCTORS.add(new Doctor("Dr. Priya Singh", SPECS[1], 600));
    DOCTORS.add(new Doctor("Dr. Amit Gupta", SPECS[2], 700));
    DOCTORS.add(new Doctor("Dr. Neha Kapoor", SPECS[3], 500));
    DOCTORS.add(new Doctor("Dr. Vikram Rao", SPECS[4], 1000));
    DOCTORS.add(new Doctor("Dr. Meera Iyer", SPECS[5], 600));
    DOCTORS.add(new Doctor("Dr. Sneha Joshi", SPECS[6], 400));
    DOCTORS.add(new Doctor("Dr. Kavita Nair", SPECS[7], 700));
    DOCTORS.add(new Doctor("Dr. Rahul Mehta", SPECS[8], 300));
  }

  public static final List<Booking> BOOKINGS = new ArrayList<>();
  private static int nextId = 1001;

  public static int nextId() {
    return nextId++;
  }

  /** True if this doctor already has an active booking at that date + time. */
  public static boolean isTaken(Doctor d, LocalDate date, String time) {
    for (Booking b : BOOKINGS) {
      if ("Booked".equals(b.status) && b.doctor == d
          && b.date.equals(date) && b.time.equals(time)) {
        return true;
      }
    }
    return false;
  }

  private AppointmentData() {
  }
}
