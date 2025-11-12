# Coffee Shop Queue Simulation

## Requirements

To compile and run this simulation, you will need the following:

* **Operating System:** Compatible with Java (e.g., Windows, macOS, Linux).
* **Java Development Kit (JDK):** Version 11 or later is recommended, as JavaFX is more supported with these versions.
* **JavaFX SDK:** Modern JDK versions (11+) do not include JavaFX by default. You need to have the JavaFX SDK appropriate for your OS and JDK version.
* **Integrated Development Environment (IDE):** An IDE like IntelliJ IDEA, Eclipse, or VS Code is recommended for easier project management, compilation, and running.

## Installation and Setup

1.  **Get the Code:** Download the source code folder (`Coffee Shop Simulation`).
2.  **Open in IDE:**
    * Open your preferred Java IDE (e.g., IntelliJ IDEA, Eclipse).
    * Import the project: Use the IDE's import function (e.g., "Open" or "Import Project") and select the `Coffee Shop Simulation` folder.
3.  **Configure JDK:** Ensure the IDE is configured to use a compatible JDK (version 11 or later). 
4.  **Configure JavaFX:**
    * **If using an IDE like IntelliJ IDEA or Eclipse:** You typically need to configure the JavaFX library. 

## Running the Simulation

1.  **Compile:** Use your IDE's build/compile function to ensure there are no errors. This usually happens automatically when you try to run the application.
2.  **Run:**
    * Locate the `CoffeeShopApp.java` file within the `src/coffeeshop/simulation` package in your IDE's project structure.
    * Right-click on `CoffeeShopApp.java` and select "Run" or "Debug".
    * Ensure you are running it with the correct Run Configuration that includes the JavaFX VM options.
3.  **Interact:** The JavaFX GUI window should appear, allowing you to run the simulation.

## Using the Simulation

* **Adjust Parameters:** Use the sliders to change:
    * `Arrival Rate`: How often customers arrive (0.1 to 2.0 customers/min).
    * `Service Rate`: How fast baristas serve (0.5 to 3.0 customers/min).
    * `Number of Baristas`: How many baristas are working (1 to 5).
    * `Simulation Speed`: How fast the simulation runs visually (1 to 10).
* **Control Buttons:**
    * `Start/Pause/Resume Simulation`: Starts the simulation, pauses it if running, or resumes it if paused.
    * `Reset Simulation`: Stops the simulation, clears all data and charts, and resets the time to zero.
* **View Results:**
    * **Charts:** Real-time line charts display Queue Length, Customer Wait Time, and Barista Utilization over the simulated time.
    * **Status Bar:** The label at the bottom provides current simulation time, customers served, current queue size, and summary statistics (Avg Wait, Max Queue, Current Utilization) upon completion or periodically.
