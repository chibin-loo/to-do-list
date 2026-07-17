import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> calendarLinks = Files.readAllLines(Paths.get("links.txt"));
        for (String link : calendarLinks) {
            if (link.isBlank())
                continue; // skip any empty lines
            printEvents(link);
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

    static String prettyDate(String raw) {
        if (raw.length() >= 8) {
            return raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
        }
        return raw;
    }
}