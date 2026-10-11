# NBP Currency Analyzer

A desktop app built with JavaFX that fetches historical currency exchange rates from the NBP API, calculates statistics, and displays trends on an interactive chart.

## Features

* **Interactive Chart:** Shows exchange rate changes over the last 30 days, with tooltips showing the exact rate and date for each point.
* **Moving Average:** Optional 5-day moving average line on the chart.
* **Statistics:** Average, min and max (with dates), standard deviation and percentage change over the period, calculated with Java `parallelStream()`.
* **Currency Converter:** Converts any amount between PLN and the NBP table A currencies (including cross rates, e.g. EUR → USD) using the latest average rates.
* **Export:** Saves the selected currency's rates and statistics to CSV, and the chart to PNG.
* **Responsive UI:** Data is fetched in the background with a loading indicator, a Refresh button and a clear error message when the NBP API is unreachable.
* **Clean Code:** Keeps data fetching logic separated from the user interface.
* **Modern Java:** Uses Java records and Jackson for JSON parsing.

## Tech Stack

* **Java 21**
* **JavaFX** (UI & Charts)
* **Jackson** (JSON parsing)
* **Maven** (Build tool, via Maven Wrapper)
* **NBP REST API** (Data source)

## How to Run

### Requirements
* JDK 21 or newer (with `JAVA_HOME` pointing to it)
* Git

Maven does not need to be installed: the Maven Wrapper downloads it on the first run.

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/FaziSPB/currency_analyzer.git
   cd currency_analyzer/currency_analyzer
   ```

2. **Run the app:**
   ```bash
   # Windows
   .\mvnw.cmd javafx:run

   # macOS/Linux
   ./mvnw javafx:run
   ```
   You can also open the project in your IDE and launch `com.szymon.Main`.

3. **Build a runnable JAR (optional):**
   ```bash
   # Windows
   .\mvnw.cmd package

   # macOS/Linux
   ./mvnw package
   ```
   The JAR is created at `target/currency_analyzer-1.0-SNAPSHOT.jar` and can be started with `java -jar target/currency_analyzer-1.0-SNAPSHOT.jar`.

### Run from .exe (Windows)

1. Download [Launch4j](https://sourceforge.net/projects/launch4j/).
2. Build the JAR (step 3 above).
3. In Launch4j, select `currency_analyzer/target/currency_analyzer-1.0-SNAPSHOT.jar` as the input JAR, generate the `.exe` file and launch it.

<img width="967" height="667" alt="image" src="https://github.com/user-attachments/assets/bf52a602-1100-42a2-a862-cf64cea2f25c" />
