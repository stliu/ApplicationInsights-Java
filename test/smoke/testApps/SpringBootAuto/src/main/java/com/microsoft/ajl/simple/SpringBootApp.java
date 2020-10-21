package com.microsoft.ajl.simple;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.microsoft.applicationinsights.core.dependencies.google.common.io.ByteStreams;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class SpringBootApp extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
        return applicationBuilder.sources(SpringBootApp.class);
    }

    @Scheduled(fixedRate = 1000)
    public int fixedRateScheduler() throws IOException {
        URL obj = new URL("https://www.bing.com/search?q=spaces%20test");
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.getContentType();
        InputStream content = connection.getInputStream();
        ByteStreams.exhaust(content);
        content.close();
        return connection.getResponseCode();
    }
}
