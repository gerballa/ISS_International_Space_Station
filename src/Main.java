import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        ConsoleUI consoleUI = new ConsoleUI();
        consoleUI.start();
    }
}

class ConsoleUI {
    private APIService apiService;
    private DataProcessor dataProcessor;
    private DatabaseManager databaseManager;

    public ConsoleUI() {
        apiService = new APIService();
        dataProcessor = new DataProcessor();
        databaseManager = new DatabaseManager();
    }

    public void start() {
        System.out.println("ISS Speed Clculation");

        try {
            LocationData currentLocation = apiService.getCurrenLocation();
            System.out.println("Current Location - Latitude : " + currentLocation.getLatitude() + ", Longitude: " + currentLocation.getLongitude());

            //Increase the delay to give time for the ISS to move
            Thread.sleep(10000);//Wait for 10 second

            LocationData newLocation = apiService.getCurrenLocation();
            System.out.println("New Location - Latitude : " + newLocation.getLatitude() + ", Longitude: " + newLocation.getLongitude());

            double speed = dataProcessor.calculateSpeed(currentLocation, newLocation);
            databaseManager.speedData(speed);

            System.out.println("ISS speed : " + speed + " km/h");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

class APIService {
    private static final String API_URL = "http://api.open-notify.org/iss-now.json";

    public LocationData getCurrenLocation() throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                System.out.println("Response" + response.toString());

                // Manually parse Json response
                int latIndex = response.indexOf("\"latitude\":");
                int lonIndex = response.indexOf("\"longitude\":");

                System.out.println("LatIndex: " + latIndex);
                System.out.println("LonIndex: " + lonIndex);

                if (latIndex != -1 && lonIndex != -1) {
                    int latStartIndex = response.indexOf("\"", latIndex + 11) + 1;
                    int latEndIndex = response.indexOf("\"", latStartIndex);
                    int lonStartIndex = response.indexOf("\"", lonIndex + 12) + 1;
                    int lonEndIndex = response.indexOf("\"", lonStartIndex);

                    String latitudeSubstring = response.substring(latStartIndex, latEndIndex);
                    String longitudeSubstring = response.substring(lonStartIndex, lonEndIndex);

                    System.out.println("Latitude Substring: " + latitudeSubstring);
                    System.out.println("Longitude Substring: " + longitudeSubstring);

                    double latitude = Double.parseDouble(latitudeSubstring);
                    double longitude = Double.parseDouble(longitudeSubstring);

                    long timestamp = System.currentTimeMillis(); // Use currrent timestamp

                    return new LocationData(latitude, longitude, timestamp);

                } else {
                    throw new Exception("Failed to parse the data from Json response.");
                }
            }
        } else {
            throw new Exception("Failed to retrieve location data. Response code :" + responseCode);
        }

    }
}

class DataProcessor {
    public double calculateSpeed(LocationData currentLocation, LocationData newLocation) {
        //Calculate the time difference in seconds

        long timeDifferencMillis = (long) (newLocation.getTimestamp() - currentLocation.getTimestamp());
        double timeDifferenceSeconds = timeDifferencMillis / 1000.0;

        //Calculate the distance between the 2 locations using Haversine formula
        double earthRadius = 6371000; // in meters (radius of earth)
        double dLat = Math.toRadians(newLocation.getLatitude() - currentLocation.getLatitude());
        double dLon = Math.toRadians(newLocation.getLongitude() - currentLocation.getLongitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(currentLocation.getLatitude())) * Math.cos(Math.toRadians(newLocation.getLatitude())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double disatance = earthRadius * c;

        //calculate the speed in meters per second

        double speed = disatance / timeDifferenceSeconds;

        return speed;
    }
}

class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/iss_data";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    public void speedData(double speed) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO speed_data (speed) VALUES (?) ";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setDouble(1, speed);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error saving data to the database: " + e.getMessage());
        }
    }
}

class LocationData {
    private double latitude;
    private double longitude;
    private long timestamp;// Timestamp in milliseconds

    public LocationData(double latitude, double longitude, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
