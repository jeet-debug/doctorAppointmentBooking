package appointment;

public class AppointmentData {

    public static class Doctor {

        public final int id;
        public final String name;
        public final String spec;
        public final int fee;

        public Doctor(int id, String name, String spec, int fee) {
            this.id = id;
            this.name = name;
            this.spec = spec;
            this.fee = fee;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private AppointmentData() {
    }
}