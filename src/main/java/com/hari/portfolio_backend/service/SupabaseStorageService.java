
package com.hari.portfolio_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.bucket}")
    private String bucket;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String bucketUrl() {
        return supabaseUrl.replaceAll("/+$", "")
                + "/storage/v1/object/"
                + encodePathSegment(bucket);
    }

    private String objectUrl(String fileName) {
        return bucketUrl() + "/"
                + encodePathSegment(fileName);
    }

    // Authenticate using the legacy service_role JWT.
    private HttpRequest.Builder authorizedRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey);
    }

    public void upload(
            String fileName,
            byte[] fileBytes,
            String contentType) throws IOException {

        HttpRequest request = authorizedRequest(objectUrl(fileName))
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        ensureSuccess(send(request), "upload");
    }

    public byte[] download(String fileName) throws IOException {

        HttpRequest request = authorizedRequest(objectUrl(fileName))
                .GET()
                .build();

        HttpResponse<byte[]> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Supabase download was interrupted.", exception);
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            String errorBody = new String(
                    response.body(), StandardCharsets.UTF_8);

            throw new IOException(
                    "Supabase download failed. HTTP status: "
                            + response.statusCode()
                            + ". Response: "
                            + limitErrorMessage(errorBody));
        }

        return response.body();
    }

    public void delete(String fileName) throws IOException {

        String json = "{\"prefixes\":[\""
                + escapeJson(fileName)
                + "\"]}";

        HttpRequest request = authorizedRequest(bucketUrl())
                .header("Content-Type", "application/json")
                .method(
                        "DELETE",
                        HttpRequest.BodyPublishers.ofString(json))
                .build();

        ensureSuccess(send(request), "delete");
    }

    private HttpResponse<String> send(
            HttpRequest request) throws IOException {

        try {
            return httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Supabase request was interrupted.", exception);
        }
    }

    private void ensureSuccess(
            HttpResponse<String> response,
            String operation) throws IOException {

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IOException(
                    "Supabase " + operation
                            + " failed. HTTP status: "
                            + response.statusCode()
                            + ". Response: "
                            + limitErrorMessage(response.body()));
        }
    }

    private String limitErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "(empty response)";
        }

        return message.length() > 1000
                ? message.substring(0, 1000)
                : message;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
