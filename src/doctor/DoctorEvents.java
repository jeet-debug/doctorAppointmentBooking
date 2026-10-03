package doctor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

/**
 * Tiny event bus so every panel can stay in sync with the doctors table.
 *
 * Any panel that shows doctors (All Doctors, Book Appointment dropdown,
 * Dashboard counters ...) can do:
 *
 *     DoctorEvents.addListener(this::refresh);
 *
 * and anyone who changes doctors (add / delete / status change) calls:
 *
 *     DoctorEvents.fireChanged();
 */
public final class DoctorEvents {

        private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

        private DoctorEvents() {
        }

        public static void addListener(Runnable r) {
                if (r != null && !LISTENERS.contains(r)) {
                        LISTENERS.add(r);
                }
        }

        public static void removeListener(Runnable r) {
                LISTENERS.remove(r);
        }

        public static void fireChanged() {
                Runnable run = () -> {
                        for (Runnable r : LISTENERS) {
                                try {
                                        r.run();
                                } catch (Exception ex) {
                                        ex.printStackTrace();
                                }
                        }
                };
                if (SwingUtilities.isEventDispatchThread()) {
                        run.run();
                } else {
                        SwingUtilities.invokeLater(run);
                }
        }
}