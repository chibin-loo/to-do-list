import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        List<Event> events = new ArrayList<>();
        List<String> calendarLinks = Files.readAllLines(Paths.get("links.txt"));
        for (String link : calendarLinks) {
            if (link.isBlank()) {
                continue;
            }
            addEvents(link, events);
        }

        loadTasks(events);

        // add tasks
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nAdd a task? Type its name, or type 'done' to finish: ");
            String name = scanner.nextLine().trim();

            if (name.equalsIgnoreCase("done")) {
                break; // leave the loop
            }

            System.out.print("What date? (like 2026-01-18, or just press Enter to skip): ");
            String date = scanner.nextLine().trim();

            System.out.print("What time? (like 08:00, or Enter to skip): ");
            String time = scanner.nextLine().trim();
            Event e = new Event();
            e.name = name;
            e.date = date.isBlank() ? "no date" : date;
            e.time = time.isBlank() ? "no time" : time;
            e.userAdded = true;
            events.add(e);
        }

        // --- NEW: split events into upcoming and past ---
        List<Event> upcoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();
        for (Event e : events) {
            if (!isPast(e.date, e.time)) {
                upcoming.add(e);
            } else {
                past.add(e);
            }
        }

        while (true) {
            showList(upcoming);
            System.out.print(
                    "\nType a number to remove it, 'd' + number to mark done (like 'd3'), 'past' to view past events, or 'quit': ");
            String command = scanner.nextLine().trim();

            if (command.equalsIgnoreCase("quit")) {
                saveTasks(upcoming, past);
                break;
            }

            if (command.equalsIgnoreCase("past")) {
                showList(past);
                continue;
            }

            if (command.toLowerCase().startsWith("d")) {
                // mark done: everything after the 'd' is the number
                int index = readNumber(command.substring(1));
                if (isValid(index, upcoming)) {
                    upcoming.get(index - 1).done = true;
                }
            } else {
                // plain number: remove it
                int index = readNumber(command);
                if (isValid(index, upcoming)) {
                    upcoming.remove(index - 1);
                }
            }
        }
        scanner.close();
    }

    // Prints the list with numbers and a [done] marker
    static void showList(List<Event> list) {
        System.out.println("\nYour list (" + list.size() + "):");
        for (int i = 0; i < list.size(); i++) {
            Event e = list.get(i);
            String when = e.time.isBlank() ? e.date : (e.date + " " + e.time);
            String mark = e.done ? " [done]" : "";
            System.out.println((i + 1) + ") " + when + "   " + e.name + mark);
        }
    }

    // Turns typed text into a number; returns -1 if it wasn't a number
    static int readNumber(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    // Checks the number actually points at an item in the list
    static boolean isValid(int index, List<Event> list) {
        if (index < 1 || index > list.size()) {
            System.out.println("That number isn't on the list.");
            return false;
        }
        return true;
    }

    static void printEvents(String link) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(link)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String[] lines = response.body().split("\n");

        String name = "";
        String date = "";

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("SUMMARY:")) {
                name = line.substring(8);
            }
            if (line.startsWith("DTSTART")) {
                int colon = line.indexOf(":");
                date = line.substring(colon + 1);
            }
            if (line.startsWith("END:VEVENT")) {
                System.out.println(prettyDate(date) + "   " + name);
                name = "";
                date = "";
            }
        }
    }

    static void addEvents(String link, List<Event> events) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(link)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String[] lines = response.body().split("\n");

        String name = "";
        String date = "";

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("SUMMARY:")) {
                name = line.substring(8);
            }
            if (line.startsWith("DTSTART")) {
                int colon = line.indexOf(":");
                date = line.substring(colon + 1);
            }
            if (line.startsWith("END:VEVENT")) {
                // Instead of printing, make an Event and add it to the list
                Event e = new Event();
                e.name = name;
                e.date = prettyDate(date);
                e.time = prettyTime(date);
                events.add(e);
                name = "";
                date = "";
            }
        }
    }

    static String prettyDate(String raw) {
        if (raw.length() >= 8) {
            return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
        }
        return raw;
    }

    // Pulls the time out of something like 20260118T235900Z -> "23:59"
    static String prettyTime(String raw) {
        int t = raw.indexOf("T");
        if (t >= 0 && raw.length() >= t + 5) {
            String hh = raw.substring(t + 1, t + 3);
            String mm = raw.substring(t + 3, t + 5);
            return hh + ":" + mm;
        }
        return ""; // some events (all-day ones) have no time
    }

    // True if this event's date+time is already in the past.
    static boolean isPast(String date, String time) {
        try {
            LocalDate day = LocalDate.parse(date);

            // If there's no time, treat it as end-of-day so it stays up all day
            LocalTime clock = time.isBlank() ? LocalTime.of(23, 59) : LocalTime.parse(time);

            LocalDateTime when = LocalDateTime.of(day, clock);
            return when.isBefore(LocalDateTime.now());
        } catch (Exception ex) {
            return false; // if the date/time is missing or weird, keep showing it
        }
    }

    // Writes your own tasks (not calendar events) to tasks.txt, one per line
    static void saveTasks(List<Event> upcoming, List<Event> past) throws Exception {
        List<String> lines = new ArrayList<>();

        // Check both lists, since a task could be upcoming or past
        for (Event e : upcoming) {
            if (e.userAdded)
                lines.add(lineFor(e));
        }
        for (Event e : past) {
            if (e.userAdded)
                lines.add(lineFor(e));
        }

        Files.write(Paths.get("tasks.txt"), lines);
        System.out.println("Saved " + lines.size() + " of your tasks.");
    }

    // Turns one task into a single line of text like: Study|2026-01-20|18:00|false
    static String lineFor(Event e) {
        return e.name + "|" + e.date + "|" + e.time + "|" + e.done;
    }

    // Reads your saved tasks back from tasks.txt (if it exists) into the list
    static void loadTasks(List<Event> events) throws Exception {
        // If there's no file yet (first ever run), just do nothing
        if (!Files.exists(Paths.get("tasks.txt"))) {
            return;
        }

        List<String> lines = Files.readAllLines(Paths.get("tasks.txt"));
        for (String line : lines) {
            if (line.isBlank())
                continue;

            // Split the line back into its 4 pieces at each |
            String[] parts = line.split("\\|");
            if (parts.length < 4)
                continue; // skip anything malformed

            Event e = new Event();
            e.name = parts[0];
            e.date = parts[1];
            e.time = parts[2];
            e.done = parts[3].equals("true");
            e.userAdded = true; // it came from your file, so it's yours
            events.add(e);
        }
    }
}

class Event {
    String name;
    String date;
    String time = "";
    boolean done = false;
    boolean userAdded = false;
}