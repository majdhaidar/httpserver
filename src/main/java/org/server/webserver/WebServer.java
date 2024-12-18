package org.server.webserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * The WebServer class implements an HTTP server capable of handling multiple client requests.
 * It provides endpoints for status checking and task processing.
 *
 * Features include:
 * - Ability to start the server on a specified port.
 * - Handling 'GET' requests to check server status.
 * - Handling 'POST' requests for processing tasks, including options for test and debug modes.
 * - Multithreaded request processing for enhanced performance.
 */
public class WebServer {
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";
    private final int port;
    private HttpServer server;

    public WebServer(int port) {
        this.port = port;
    }

    /**
     * Starts the HTTP server and initializes its endpoints.
     *
     * This method creates an instance of an HTTP server bound to the configured port and sets up
     * two specific contexts, one for status checks and another for handling tasks. It also configures
     * a fixed thread pool executor to allow the server to handle multiple requests concurrently.
     *
     * If an error occurs while attempting to initialize the server, it is logged, and the method exits.
     * The configured endpoints are:
     * - STATUS_ENDPOINT: Processes status check requests using the {@link #handleStatusCheckRequest(HttpExchange)} method.
     * - TASK_ENDPOINT: Processes task requests using the {@link #handleTaskRequest(HttpExchange)} method.
     */
    public void start() {
        try{
            // backlog is number of allowed requests kept in the queue if our app at the point is not able to process
            // 0 will simply use the system default
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);

        // set threads for our server to handle multiple threads
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    /**
     * Handles HTTP POST requests for task processing.
     *
     * This method processes incoming task requests by validating the request method
     * and headers, handling debug and test modes, and generating a response based
     * on the incoming request body. If the "X-Test" header is present and set to
     * "true", a predefined dummy response is returned. If the "X-Debug" header is
     * present and set to "true", the processing time for the request is included
     * in the response headers. The response itself is calculated by invoking the
     * {@code calculateResponse} method.
     *
     * @param exchange The HttpExchange object that encapsulates the HTTP request
     *                 and response communication.
     * @throws IOException If an I/O error occurs during request or response handling.
     */
    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if(!exchange.getRequestMethod().equals("POST")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();

        //check headers if we are in the test mode
        if(headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "Dummy response";
            sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }

        //check if we are in debug mode
        boolean isDebugMode = false;
        if(headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        long startTime = System.nanoTime();
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = calculateResponse(requestBytes);
        long finishTime = System.nanoTime();

        if(isDebugMode) {
            String debugMessage = String.format("Request took %d ms", (finishTime - startTime) / 1000000);
            exchange.getResponseHeaders().add("X-Debug-Message", debugMessage);
        }

        sendResponse(responseBytes, exchange);
    }

    /**
     * Processes the input array of bytes, interprets it as a comma-separated list of numbers,
     * computes the product of these numbers, and returns the result as a byte array in a formatted string.
     *
     * @param requestBytes The input byte array representing a comma-separated list of numbers.
     * @return A byte array containing the result of the multiplication in a formatted string.
     */
    private byte[] calculateResponse(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String[] stringNumbers = bodyString.split(",");

        BigInteger result = BigInteger.ONE;
        for(String number : stringNumbers) {
            result = result.multiply(new BigInteger(number));
        }

        return String.format("Result of the multiplication %s", result).getBytes();
    }

    /**
     * Handles HTTP GET requests for server status checks.
     *
     * This method processes incoming status check requests by validating the request
     * method to ensure it is a GET request. If the request method is invalid, the
     * connection is closed, and no further processing occurs. For valid requests, a
     * predefined response message indicating the server is alive is sent back to the client.
     *
     * @param exchange The HttpExchange object that encapsulates the HTTP request
     *                 and response communication.
     * @throws IOException If an I/O error occurs during request or response handling.
     */
    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if(!exchange.getRequestMethod().equals("GET")) {
            exchange.close();
            return;
        }

        String responseMessage = "Server is alive";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    /**
     * Sends a response to the client using the provided byte array as the response body.
     * This method sets the HTTP response headers and writes the response body to the client.
     *
     * @param responseBytes The byte array containing the response body to be sent to the client.
     * @param exchange      The HttpExchange object that encapsulates the HTTP request and response communication.
     * @throws IOException If an I/O error occurs while sending the response.
     */
    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }
}
