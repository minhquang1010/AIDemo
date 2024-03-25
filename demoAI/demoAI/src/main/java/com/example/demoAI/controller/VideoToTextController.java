package com.example.demoAI.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.texttospeech.v1.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.grpc.Context;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.google.cloud.texttospeech.v1.*;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import com.google.cloud.storage.*;

@RestController
public class VideoToTextController {

    @PostMapping("/api/convert")
    public String convertVideoToText(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "Vui lòng chọn một tệp video.";
        }

        try {
            Path tempFilePath = Files.createTempFile("temp", ".mp4");
            file.transferTo(tempFilePath.toFile());

            String gcsUri = uploadToGCS(tempFilePath);

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream("./demoAI/src/main/resources/keyfile1.json"));
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();

            try (SpeechClient speechClient = SpeechClient.create(settings)) {
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEnableAutomaticPunctuation(true)
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(48000)
                        .setLanguageCode("vi-VN")
                        .setModel(RecognitionConfig.getDefaultInstance().getModel())
                        .build();

                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setUri(gcsUri)
                        .build();

                OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speechClient.longRunningRecognizeAsync(config, audio);
                LongRunningRecognizeResponse longRunningResponse = response.get();

                StringBuilder transcriptBuilder = new StringBuilder();
                for (SpeechRecognitionResult result : longRunningResponse.getResultsList()) {
                    String transcript = result.getAlternatives(0).getTranscript();
                    transcriptBuilder.append(transcript).append("\n");
                }

                Files.deleteIfExists(tempFilePath);
                return transcriptBuilder.toString();
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Lỗi xảy ra khi chuyển đổi video thành văn bản.";
        }
    }

    private String uploadToGCS(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream("./demoAI/src/main/resources/keyfile1.json"));
            Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
            String bucketName = "aispeech2text";
            String objectName = fileName;

            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                bucket = storage.create(BucketInfo.of(bucketName));
            }

            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            Blob blob = storage.create(blobInfo, Files.readAllBytes(filePath));

            return "gs://" + bucketName + "/" + objectName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

//    private static final String API_KEY = "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";
//    private static final String API_KEY = "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";
    private static final String API_KEY = "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";

    @PostMapping("/api/chat")
    public ResponseEntity<String> chatCompletion(@RequestBody String userMessage) {
        try {
            String completion = getChatCompletion(userMessage);
            return ResponseEntity.ok(completion);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }

    private String getChatCompletion(String userMessage) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://api.openai.com/v1/chat/completions");

        // Add headers
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Authorization", "Bearer " + API_KEY);

        // Set request body using Gson
        Gson gson = new Gson();
        JsonObject requestBodyJson = new JsonObject();
        requestBodyJson.addProperty("model", "gpt-3.5-turbo");
        JsonArray messagesArray = new JsonArray();
        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("role", "user");
//        messageObject.addProperty("language", "vi");
//        messageObject.addProperty("content", userMessage + "| Notes: Please summarize as follows:\n" +
//                "- time\n" +
//                "- location\n" +
//                "- Content\n" +
//                "- things need to notice\n" +
//                "And other meanings");
        messageObject.addProperty("content", userMessage + "| Notes:Please summarize as follows:\n" +
                "- Title\n" +
                "- Time\n" +
                "- Location\n" +
                "- Content\n" +
                "- Things to pay attention to\n" +
                "and other content ideas. Please return the results to me as a html snippet with css that bolds the title, Time, Content, Location, Things to note and in front of each internal content with a - at the beginning.");
//        messageObject.addProperty("content", userMessage + "| Notes: Please give suggestions on what needs to be done or can be done, and how to solve the problem, if any, based on this text.");
        messagesArray.add(messageObject);
        requestBodyJson.add("messages", messagesArray);

        requestBodyJson.addProperty("temperature", 0.7);
        String requestBody = gson.toJson(requestBodyJson);
        httpPost.setEntity(new StringEntity(requestBody));

        // Execute the request
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();

        // Process the response
        String result = null;
        if (entity != null) {
            result = EntityUtils.toString(entity);
        }

        // Close the HttpClient
        httpClient.close();

        return result;
    }

}