package com.example.demoAI.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import io.grpc.Context;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

@RestController
public class VideoToTextController {

    @PostMapping("/api/convert")
    public String convertVideoToText(@RequestParam("file") MultipartFile file) {
        // Kiểm tra xem tệp đã được tải lên hay không
        if (file.isEmpty()) {
            return "Vui lòng chọn một tệp video.";
        }

        try {
            // Tạo đường dẫn tạm thời cho tệp video
            Path tempFilePath = Files.createTempFile("temp", ".mp3");
            file.transferTo(tempFilePath.toFile());

            // Tạo URI của file trên Google Cloud Storage (GCS)
            String gcsUri = uploadToGCS(tempFilePath);

            // Tạo Credentials Provider từ key file
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream("./demoAI/src/main/resources/keyfile1.json"));
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

            // Tạo cài đặt cho SpeechClient
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();

            // Khởi tạo SpeechClient
            try (SpeechClient speechClient = SpeechClient.create(settings)) {
                // Đọc dữ liệu từ tệp và chuyển đổi thành ByteString
                byte[] data = Files.readAllBytes(tempFilePath);
                ByteString audioBytes = ByteString.copyFrom(data);

                // Tạo RecognitionConfig
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                        .setLanguageCode("vi-VN")
                        .build();

                // Tạo RecognitionAudio từ URI của file trên GCS
                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setUri(gcsUri)
                        .build();


                // Gửi yêu cầu bất đồng bộ đến Google Cloud Speech-to-Text API và nhận kết quả
                OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speechClient.longRunningRecognizeAsync(config, audio);

                // Chờ cho đến khi quá trình nhận diện hoàn tất và nhận kết quả
                LongRunningRecognizeResponse longRunningResponse = response.get();

                // Trích xuất văn bản từ kết quả
                StringBuilder transcriptBuilder = new StringBuilder();
                for (SpeechRecognitionResult result : longRunningResponse.getResultsList()) {
                    // Lấy văn bản từ mỗi kết quả
                    String transcript = result.getAlternatives(0).getTranscript();
                    transcriptBuilder.append(transcript).append("\n");
                }

                // Xóa tệp tạm thời
                Files.deleteIfExists(tempFilePath);

                // Trả về văn bản trích xuất từ video
                return transcriptBuilder.toString();
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "Lỗi xảy ra khi chuyển đổi video thành văn bản.";
        }
    }

    private String uploadToGCS(Path filePath) {
        try {
            // Lấy tên file từ đường dẫn
            String fileName = filePath.getFileName().toString();

            // Tải file keyfile.json để xác thực với GCS
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream("./demoAI/src/main/resources/keyfile1.json"));

            // Khởi tạo client của GCS
            Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

            // Tạo tên bucket và đường dẫn đến file trên GCS
            String bucketName = "aispeech2text";
            String objectName = fileName; // Sử dụng tên file từ đường dẫn

            // Tạo bucket nếu chưa tồn tại
            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                bucket = storage.create(BucketInfo.of(bucketName));
            }

            // Tạo file object từ đường dẫn tới file trên local file system
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            // Tải file lên GCS
            Blob blob = storage.create(blobInfo, Files.readAllBytes(filePath));

            // Trả về URI của file trên GCS
            return "gs://" + bucketName + "/" + objectName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
