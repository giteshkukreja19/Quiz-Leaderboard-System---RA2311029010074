# Quiz Leaderboard

This is my submission for the SRM Internship Assignment. 

The application polls the quiz API 10 times to sequentially fetch events, processes them to filter out duplicate responses, and then submits a finalized leaderboard based on participant scores.

## How it works

1. It makes HTTP GET requests to the `/quiz/messages` API 10 times (poll indexes 0-9).
2. Between each request, there's a 5-second delay.
3. As the API returns events, I extract the `roundId` and `participant` to form a unique key so we don't double-count duplicate scores if the payloads overlap.
4. It aggregates the final scores per participant.
5. In the end, the leaderboard gets sorted in descending order and POSTed back to `/quiz/submit`.

### Dealing with Duplicates (Highlight)

A key part of the assignment was handling duplicate API responses. Since the API can send repeated events across different polls, I'm using a `HashSet` to deduplicate based on a `roundId|participant` composite key. This ensures idempotent behavior:

```java
String key = roundId + "|" + participant;

if (seen.add(key)) {
    scores.merge(participant, score, Integer::sum);
}
```

## Running the Code

It's built with standard Java 11+ and the built-in `HttpClient` (no external libraries needed). You can compile and run it like this:

```bash
javac src/QuizLeaderboardApp.java
java -cp src QuizLeaderboardApp
```

### Sample Output

```text
Starting Quiz Leaderboard Processing...

Poll 0 completed successfully.
...
Poll 9 completed successfully.

Leaderboard:
1. George - 795
2. Hannah - 750
3. Ivan - 745
Total Score: 2290
```

---
**Author**: Gitesh Kukreja (RA2311029010074)
