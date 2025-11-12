package coffeeshop.simulation;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CoffeeShopSim extends Application {
    private double arrivalRate = 0.5; // customers arriving per minute
    private double serviceRate = 1.0; // customers served per minute
    private int numBaristas = 1; // number of baristas
    private final int simulationDuration = 480; // fixed at 8 hours to reflect a typical work day
    private double currentTime = 0;

    private PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    private Queue<Customer> customerQueue = new LinkedList<>();
    private List<Barista> baristas = new ArrayList<>();
    private List<Customer> servedCustomers = new ArrayList<>();
    private Random random = new Random();

    private int maxQueueLength = 0;
    private double totalWaitTime = 0;
    private int customerCount = 0;

    private XYChart.Series<Number, Number> queueLengthSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> waitTimeSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> utilizationSeries = new XYChart.Series<>();
    private LineChart<Number, Number> queueLengthChart;
    private LineChart<Number, Number> waitTimeChart;
    private LineChart<Number, Number> utilizationChart;

    private Label statusLabel;
    private ScheduledExecutorService simulationExecutor;
    private boolean isSimulationRunning = false;
    private double reportingInterval = 1.0;
    private double nextReportTime = 0;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        VBox mainContent = new VBox(10);

        VBox controlPanel = createControlPanel();
        mainContent.getChildren().add(controlPanel);

        VBox chartsPanel = createChartsPanel();
        mainContent.getChildren().add(chartsPanel);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setContent(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);
        root.setCenter(scrollPane);

        statusLabel = new Label("Press Start to begin the simulation.");
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Coffee Shop Queue Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setResizable(true);

        for (int i = 0; i < numBaristas; i++) {
            baristas.add(new Barista(i));
        }

        primaryStage.setOnCloseRequest(e -> {
            if (simulationExecutor != null) {
                simulationExecutor.shutdown();
            }
        });
    }

    private class Event implements Comparable<Event> {
        private final double time;
        private final EventType type;
        private final Customer customer;
        private final Barista barista;

        public Event(double time, EventType type, Customer customer, Barista barista) {
            this.time = time;
            this.type = type;
            this.customer = customer;
            this.barista = barista;
        }

        public double getTime() {
            return time;
        }

        public EventType getType() {
            return type;
        }

        public Customer getCustomer() {
            return customer;
        }

        public Barista getBarista() {
            return barista;
        }

        @Override
        public int compareTo(Event other) {
            return Double.compare(this.time, other.time);
        }
    }

    private enum EventType {
        CUSTOMER_ARRIVAL,
        SERVICE_COMPLETION,
        STATISTICS_REPORT
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-padding: 10;");

        // Arrival rate slider
        Label arrivalRateLabel = new Label("Arrival Rate (customers/min): " + arrivalRate);
        Slider arrivalRateSlider = new Slider(0.1, 2.0, arrivalRate);
        arrivalRateSlider.setShowTickMarks(true);
        arrivalRateSlider.setShowTickLabels(true);
        arrivalRateSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            arrivalRate = newVal.doubleValue();
            arrivalRateLabel.setText("Arrival Rate (customers/min): " + String.format("%.2f", arrivalRate));
            updateStatus();
        });

        // Service rate slider
        Label serviceRateLabel = new Label("Service Rate (customers/min): " + serviceRate);
        Slider serviceRateSlider = new Slider(0.5, 3.0, serviceRate);
        serviceRateSlider.setShowTickMarks(true);
        serviceRateSlider.setShowTickLabels(true);
        serviceRateSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            serviceRate = newVal.doubleValue();
            serviceRateLabel.setText("Service Rate (customers/min): " + String.format("%.2f", serviceRate));
            updateStatus();
        });

        // Number of baristas slider
        Label baristaCountLabel = new Label("Number of Baristas: " + numBaristas);
        Slider baristaCountSlider = new Slider(1, 5, numBaristas);
        baristaCountSlider.setShowTickMarks(true);
        baristaCountSlider.setShowTickLabels(true);
        baristaCountSlider.setMajorTickUnit(1);
        baristaCountSlider.setMinorTickCount(0);
        baristaCountSlider.setBlockIncrement(1);
        baristaCountSlider.setSnapToTicks(true);
        baristaCountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            numBaristas = newVal.intValue();
            baristaCountLabel.setText("Number of Baristas: " + numBaristas);
            updateBaristaCount();
        });

        // Simulation speed slider
        Label speedLabel = new Label("Simulation Speed: " + simulationSpeed);
        Slider speedSlider = new Slider(1, 10, simulationSpeed);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            simulationSpeed = newVal.intValue();
            speedLabel.setText("Simulation Speed: " + simulationSpeed);

            if (isSimulationRunning) {
                pauseSimulation();
                startSimulation();
            }
        });

        // Control buttons
        Button startButton = new Button("Start Simulation");
        Button resetButton = new Button("Reset Simulation");

        startButton.setOnAction(e -> {
            if (!isSimulationRunning) {
                startSimulation();
                startButton.setText("Pause Simulation");
            } else {
                pauseSimulation();
                startButton.setText("Resume Simulation");
            }
            isSimulationRunning = !isSimulationRunning;
        });

        resetButton.setOnAction(e -> {
            resetSimulation();
            startButton.setText("Start Simulation");
            isSimulationRunning = false;
        });

        HBox buttonBox = new HBox(10, startButton, resetButton);

        panel.getChildren().addAll(
                arrivalRateLabel, arrivalRateSlider,
                serviceRateLabel, serviceRateSlider,
                baristaCountLabel, baristaCountSlider,
                speedLabel, speedSlider,
                buttonBox
        );
        return panel;
    }

    private VBox createChartsPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-padding: 10;");

        NumberAxis xAxis1 = new NumberAxis("Time (minutes)", 0, 480, 60);
        NumberAxis yAxis1 = new NumberAxis("Queue Length", 0, 10, 1);
        queueLengthChart = new LineChart<>(xAxis1, yAxis1);
        queueLengthChart.setTitle("Queue Length Over Time");
        queueLengthSeries.setName("Queue Length");
        queueLengthChart.getData().add(queueLengthSeries);
        queueLengthChart.setPrefSize(600, 400);

        NumberAxis xAxis2 = new NumberAxis("Time (minutes)", 0, 480, 60);
        NumberAxis yAxis2 = new NumberAxis("Wait Time (minutes)", 0, 10, 1);
        waitTimeChart = new LineChart<>(xAxis2, yAxis2);
        waitTimeChart.setTitle("Customer Wait Time");
        waitTimeSeries.setName("Wait Time");
        waitTimeChart.getData().add(waitTimeSeries);
        waitTimeChart.setPrefSize(600, 400);

        NumberAxis xAxis3 = new NumberAxis("Time (minutes)", 0, 480, 60);
        NumberAxis yAxis3 = new NumberAxis("Utilization (%)", 0, 100, 10);
        utilizationChart = new LineChart<>(xAxis3, yAxis3);
        utilizationChart.setTitle("Barista Utilization");
        utilizationSeries.setName("Utilization");
        utilizationChart.getData().add(utilizationSeries);
        utilizationChart.setPrefSize(600, 400);

        panel.getChildren().addAll(queueLengthChart, waitTimeChart, utilizationChart);

        return panel;
    }

    private int simulationSpeed = 5;

    private void startSimulation() {
        if (currentTime == 0) {
            initializeSimulation();
        }

        simulationExecutor = Executors.newSingleThreadScheduledExecutor();
        simulationExecutor.scheduleAtFixedRate(() -> {
            if (currentTime < simulationDuration && !eventQueue.isEmpty()) {
                int eventsToProcess = simulationSpeed * 5;
                executeSteps(eventsToProcess);
            } else if (eventQueue.isEmpty() || currentTime >= simulationDuration) {
                simulationExecutor.shutdown();
                Platform.runLater(() -> {
                    statusLabel.setText("Simulation completed. Final statistics: " + getStatistics());
                });
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    private void pauseSimulation() {
        if (simulationExecutor != null) {
            simulationExecutor.shutdown();
        }
    }

    private void resetSimulation() {
        pauseSimulation();
        currentTime = 0;
        customerQueue.clear();
        baristas.clear();
        servedCustomers.clear();
        eventQueue.clear();
        maxQueueLength = 0;
        totalWaitTime = 0;
        customerCount = 0;
        nextReportTime = 0;

        for (int i = 0; i < numBaristas; i++) {
            baristas.add(new Barista(i));
        }

        Platform.runLater(() -> {
            queueLengthSeries.getData().clear();
            waitTimeSeries.getData().clear();
            utilizationSeries.getData().clear();
            statusLabel.setText("Simulation reset. Press Start to begin.");
        });
    }

    private void initializeSimulation() {
        eventQueue.clear();

        double firstArrivalTime = generateInterArrivalTime();
        eventQueue.add(new Event(firstArrivalTime, EventType.CUSTOMER_ARRIVAL, new Customer(firstArrivalTime), null));

        eventQueue.add(new Event(nextReportTime, EventType.STATISTICS_REPORT, null, null));
    }

    private void executeSteps(int maxEvents) {
        int eventsProcessed = 0;

        while (!eventQueue.isEmpty() && currentTime < simulationDuration && eventsProcessed < maxEvents) {
            Event event = eventQueue.poll();
            currentTime = event.getTime();

            if (currentTime > simulationDuration) {
                break;
            }

            processEvent(event);
            eventsProcessed++;

            if (eventsProcessed % 10 == 0) {
                Platform.runLater(this::updateStatus);
            }
        }
        Platform.runLater(this::updateStatus);
    }

    private void processEvent(Event event) {
        switch (event.getType()) {
            case CUSTOMER_ARRIVAL:
                handleCustomerArrival(event.getCustomer());
                break;
            case SERVICE_COMPLETION:
                handleServiceCompletion(event.getBarista(), event.getCustomer());
                break;
            case STATISTICS_REPORT:
                handleStatisticsReport();
                break;
        }
    }

    private void handleCustomerArrival(Customer customer) {
        customerQueue.add(customer);
        customerCount++;

        maxQueueLength = Math.max(maxQueueLength, customerQueue.size());

        double nextArrivalTime = currentTime + generateInterArrivalTime();
        eventQueue.add(new Event(nextArrivalTime, EventType.CUSTOMER_ARRIVAL,
                new Customer(nextArrivalTime), null));

        for (Barista barista : baristas) {
            if (!barista.isBusy() && !customerQueue.isEmpty()) {
                Customer nextCustomer = customerQueue.poll();
                startService(barista, nextCustomer);
                break;
            }
        }
    }

    private void handleServiceCompletion(Barista barista, Customer customer) {
        barista.setIdle();

        double totalTime = currentTime - customer.getArrivalTime();
        customer.setServiceEndTime(currentTime);

        servedCustomers.add(customer);

        if (!customerQueue.isEmpty()) {
            Customer nextCustomer = customerQueue.poll();
            startService(barista, nextCustomer);
        }
    }

    private void handleStatisticsReport() {
        updateCharts();

        nextReportTime = currentTime + reportingInterval;
        eventQueue.add(new Event(nextReportTime, EventType.STATISTICS_REPORT, null, null));
    }

    private void startService(Barista barista, Customer customer) {
        double waitTime = currentTime - customer.getArrivalTime();
        customer.setWaitTime(waitTime);
        totalWaitTime += waitTime;

        double serviceTime = generateServiceTime();

        barista.setBusy(customer);

        double serviceEndTime = currentTime + serviceTime;
        eventQueue.add(new Event(serviceEndTime, EventType.SERVICE_COMPLETION, customer, barista));
    }

    private double generateInterArrivalTime() {
        return -Math.log(1 - random.nextDouble()) / arrivalRate;
    }

    private double generateServiceTime() {
        return -Math.log(1 - random.nextDouble()) / serviceRate;
    }

    private void updateBaristaCount() {
        if (isSimulationRunning) {
            int currentBaristaCount = baristas.size();

            if (numBaristas > currentBaristaCount) {
                for (int i = currentBaristaCount; i < numBaristas; i++) {
                    Barista newBarista = new Barista(i);
                    baristas.add(newBarista);

                    if (!customerQueue.isEmpty()) {
                        Customer nextCustomer = customerQueue.poll();
                        startService(newBarista, nextCustomer);
                    }
                }
            } else if (numBaristas < currentBaristaCount) {
                List<Barista> idleBaristas = new ArrayList<>();
                List<Barista> busyBaristas = new ArrayList<>();

                for (Barista barista : baristas) {
                    if (barista.isBusy()) {
                        busyBaristas.add(barista);
                    } else {
                        idleBaristas.add(barista);
                    }
                }

                List<Barista> newBaristas = new ArrayList<>();
                int remaining = numBaristas;

                for (Barista barista : busyBaristas) {
                    if (remaining > 0) {
                        newBaristas.add(barista);
                        remaining--;
                    } else {
                        Customer customer = barista.getCurrentCustomer();
                        if (customer != null) {
                            customerQueue.add(customer);

                            eventQueue.removeIf(event ->
                                    event.getType() == EventType.SERVICE_COMPLETION &&
                                            event.getBarista() == barista);
                        }
                    }
                }
                for (Barista barista : idleBaristas) {
                    if (remaining > 0) {
                        newBaristas.add(barista);
                        remaining--;
                    }
                }
                baristas = newBaristas;
            }
        } else {
            baristas.clear();
            for (int i = 0; i < numBaristas; i++) {
                baristas.add(new Barista(i));
            }
        }
    }

    private void updateCharts() {
        Platform.runLater(() -> {
            queueLengthSeries.getData().add(new XYChart.Data<>(currentTime, customerQueue.size()));

            if (!servedCustomers.isEmpty()) {
                double recentAvgWait = 0;
                int count = 0;
                for (int i = Math.max(0, servedCustomers.size() - 5); i < servedCustomers.size(); i++) {
                    recentAvgWait += servedCustomers.get(i).getWaitTime();
                    count++;
                }
                if (count > 0) {
                    recentAvgWait /= count;
                    waitTimeSeries.getData().add(new XYChart.Data<>(currentTime, recentAvgWait));
                }
            }

            int busyBaristas = 0;
            for (Barista barista : baristas) {
                if (barista.isBusy()) {
                    busyBaristas++;
                }
            }
            double utilization = (double) busyBaristas / numBaristas * 100;
            utilizationSeries.getData().add(new XYChart.Data<>(currentTime, utilization));
        });
    }

    private void updateStatus() {
        Platform.runLater(() -> {
            int currentHour = (int) (currentTime / 60);
            int currentMinute = (int) (currentTime % 60);
            statusLabel.setText(String.format(
                    "Simulation time: %02d:%02d | Customers served: %d | Current queue: %d | %s",
                    currentHour, currentMinute, servedCustomers.size(), customerQueue.size(), getStatistics()));
        });
    }

    private String getStatistics() {
        if (servedCustomers.isEmpty()) {
            return "No customers served yet";
        }

        double avgWaitTime = totalWaitTime / servedCustomers.size();

        int busyBaristas = 0;
        for (Barista barista : baristas) {
            if (barista.isBusy()) {
                busyBaristas++;
            }
        }
        double utilization = baristas.isEmpty() ? 0 : (double) busyBaristas / baristas.size() * 100;

        return String.format("Avg wait: %.2f min | Max queue: %d | Current utilization: %.1f%%",
                avgWaitTime, maxQueueLength, utilization);
    }

    private static class Customer {
        private final double arrivalTime;
        private double waitTime;
        private double serviceEndTime;

        public Customer(double arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public double getArrivalTime() {
            return arrivalTime;
        }

        public void setWaitTime(double waitTime) {
            this.waitTime = waitTime;
        }

        public double getWaitTime() {
            return waitTime;
        }

        public void setServiceEndTime(double serviceEndTime) {
            this.serviceEndTime = serviceEndTime;
        }

        public double getServiceEndTime() {
            return serviceEndTime;
        }
    }

    private static class Barista {
        private final int id;
        private boolean busy;
        private Customer currentCustomer;

        public Barista(int id) {
            this.id = id;
            this.busy = false;
        }

        public boolean isBusy() {
            return busy;
        }

        public void setBusy(Customer customer) {
            busy = true;
            currentCustomer = customer;
        }

        public void setIdle() {
            busy = false;
        }

        public Customer getCurrentCustomer() {
            return currentCustomer;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}