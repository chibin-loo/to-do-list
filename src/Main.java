import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        List<Event> events = new ArrayList<>();
        List<String> calendarLinks = Files.readAllLines(Paths.get("links.txt"));
        for (String link : calendarLinks) {
            if (link.isBlank())
                continue;
            addEvents(link, events);
        }

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

            Event e = new Event();
            e.name = name;
            e.date = date.isBlank() ? "no date" : date;
            events.add(e);
        }

        // --- NEW: split events into upcoming and past ---
        List<Event> upcoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();
        for (Event e : events) {
            if (isPast(e.date)) {
                past.add(e);
            } else {
                upcoming.add(e);
            }
        }

        // Always show upcoming
        System.out.println("\nUpcoming (" + upcoming.size() + "):");
        for (Event e : upcoming) {
            System.out.println(e.date + "   " + e.name);
        }

        // Only show past if the user asks
        System.out.print("\nShow " + past.size() + " past items too? (yes/no): ");
        String answer = scanner.nextLine().trim();
        if (answer.equalsIgnoreCase("yes")) {
            System.out.println("\nPast:");
            for (Event e : past) {
                System.out.println(e.date + "   " + e.name);
            }
        }
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

    // Is this date before today? If the date is missing or weird, we say "not past"
    // so it still shows.
    static boolean isPast(String date) {
        try {
            return LocalDate.parse(date).isBefore(LocalDate.now());
        } catch (Exception ex) {
            return false;
        }
    }
}

class Event {
    String name;
    String date;
}