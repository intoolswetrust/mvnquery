package com.github.kwart.mvnquery;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.maven.index.updater.ResourceFetcher;

public class Java11HttpClient implements ResourceFetcher {
        private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

        private URI uri;

        @Override
        public void connect(String id, String url) throws IOException {
            this.uri = URI.create(url + "/");
        }

        @Override
        public void disconnect() throws IOException {
        }

        @Override
        public InputStream retrieve(String name) throws IOException, FileNotFoundException {
            HttpRequest request = HttpRequest.newBuilder().uri(uri.resolve(name)).GET().build();
            try {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    return response.body();
                } else {
                    throw new IOException("Unexpected response: " + response);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }
    }