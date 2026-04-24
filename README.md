# 🏆 Quiz Leaderboard System

## 📌 Overview
This project is part of the SRM Internship Assignment.

It fetches quiz data from an API, removes duplicate responses,
calculates participant scores, and generates a leaderboard.

---

## 🎯 Objective
- Aggregate scores per participant  
- Remove duplicates using (roundId + participant)  
- Generate sorted leaderboard  
- Compute total score  

---

## 🔄 Workflow
1. Poll API 10 times (0–9)
2. Maintain 5-second delay
3. Collect responses
4. Remove duplicates
5. Aggregate scores
6. Sort leaderboard
7. Submit result

---

## 🧠 Deduplication Logic

```java
String key = roundId + "|" + participant;

if (seen.add(key)) {
    scores.merge(participant, score, Integer::sum);
}
```

---

## 🏗️ Tech Stack

* Java 11
* HttpClient API

---

## ▶️ How to Run

```bash
javac src/QuizLeaderboardApp.java
java -cp src QuizLeaderboardApp
```

---

## 📤 API

GET:

```
/quiz/messages?regNo=YOUR_REG_NO&poll=0-9
```

POST:

```
/quiz/submit
```

---

## 👨💻 Author

Gitesh Kukreja
