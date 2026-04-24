import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.*;

public class QuizLeaderboardApp {

    static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    static final String REG_NO = "RA2311029010074";
    static final int TOTAL_POLLS = 10;
    static final long DELAY_MS = 5000;

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        System.out.println("Starting Quiz Leaderboard Processing...\n");

        for (int poll = 0; poll < TOTAL_POLLS; poll++) {

            String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + poll;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.out.println("Poll " + poll + " failed. Skipping...");
                    continue;
                }

                if (response.body() == null || response.body().isEmpty()) {
                    System.out.println("Poll " + poll + " returned empty body. Skipping...");
                    continue;
                }

                List<String[]> events = parseEvents(response.body());

                for (String[] event : events) {
                    String roundId = event[0];
                    String participant = event[1];
                    int score = Integer.parseInt(event[2]);

                    String key = roundId + "|" + participant;

                    if (seen.add(key)) {
                        scores.merge(participant, score, Integer::sum);
                    }
                }

                System.out.println("Poll " + poll + " completed successfully.");

            } catch (Exception e) {
                System.out.println("Error in poll " + poll + ": " + e.getMessage());
            }

            if (poll < TOTAL_POLLS - 1) {
                Thread.sleep(DELAY_MS);
            }
        }

        // Sort leaderboard
        List<Map.Entry<String, Integer>> leaderboard = scores.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        int totalScore = scores.values().stream().mapToInt(Integer::intValue).sum();

        System.out.println("\nLeaderboard:");
        int rank = 1;
        for (Map.Entry<String, Integer> entry : leaderboard) {
            System.out.println(rank++ + ". " + entry.getKey() + " - " + entry.getValue());
        }

        System.out.println("Total Score: " + totalScore);

        // Build JSON
        StringBuilder lb = new StringBuilder("[");
        for (int i = 0; i < leaderboard.size(); i++) {
            var e = leaderboard.get(i);
            lb.append("{\"participant\":\"").append(e.getKey())
                    .append("\",\"totalScore\":").append(e.getValue()).append("}");
            if (i != leaderboard.size() - 1)
                lb.append(",");
        }
        lb.append("]");

        String payload = "{\"regNo\":\"" + REG_NO + "\",\"leaderboard\":" + lb + "}";

        // Submit
        HttpRequest submitReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> submitRes = client.send(submitReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("\nSubmission Response:");
        System.out.println(submitRes.body());

        if (submitRes.body().contains("\"isCorrect\":true")) {
            System.out.println("✅ SUCCESS: Correct Submission!");
        } else {
            System.out.println("❌ ERROR: Something is wrong. Check logic.");
        }
    }

    // Improved parser
    static List<String[]> parseEvents(String json) {
        List<String[]> result = new ArrayList<>();

        try {
            String eventsPart = json.split("\"events\"\\s*:\\s*\\[")[1].split("]")[0];

            String[] objects = eventsPart.split("\\},\\s*\\{");

            for (String obj : objects) {
                obj = obj.replace("{", "").replace("}", "");

                String roundId = extract(obj, "roundId");
                String participant = extract(obj, "participant");
                String score = extractNumber(obj, "score");

                if (roundId != null && participant != null && score != null) {
                    result.add(new String[] { roundId, participant, score });
                }
            }

        } catch (Exception e) {
            System.out.println("Parsing error: " + e.getMessage());
        }

        return result;
    }

    static String extract(String text, String key) {
        try {
            String pattern = "\"" + key + "\":\"";
            int start = text.indexOf(pattern) + pattern.length();
            int end = text.indexOf("\"", start);
            return text.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    static String extractNumber(String text, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = text.indexOf(pattern) + pattern.length();

            StringBuilder num = new StringBuilder();
            while (start < text.length() && Character.isDigit(text.charAt(start))) {
                num.append(text.charAt(start++));
            }
            return num.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
