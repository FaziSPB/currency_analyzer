# NBP Currency Analyzer

A desktop app built with JavaFX that fetches historical currency exchange rates from the NBP API, calculates basic statistics, and displays trends on an interactive chart.

## Features

* **Interactive Chart:** Shows exchange rate changes over the last 30 days.
* **Fast Calculations:** Calculates min, max, and average rates using Java `parallelStream()`.
* **Clean Code:** Keeps data fetching logic separated from the user interface.
* **Modern Java:** Uses Java 21 Records and Jackson for JSON parsing.

## Tech Stack

* **Java 21**
* **JavaFX** (UI & Charts)
* **Jackson** (JSON parsing)
* **Maven** (Build tool)
* **NBP REST API** (Data source)

## How to Run

### Requirements
* JDK 21 or newer
* Git

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/FaziSPB/currency_analyzer.git
   cd currency_analyzer
2. **Run the app:**
   # Windows
   .\mvnw javafx:run
   # macOS/Linux
   ./mvnw javafx:run
   # Run from IDE
   Open the project and launch com.szymon.Main.java.
<img width="967" height="667" alt="image" src="https://github.com/user-attachments/assets/bf52a602-1100-42a2-a862-cf64cea2f25c" />

   
