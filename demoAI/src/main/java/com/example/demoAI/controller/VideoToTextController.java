package com.example.demoAI.controller;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
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
            // Khởi tạo SpeechClient
            try (SpeechClient speechClient = SpeechClient.create()) {
                // Tạo đường dẫn tạm thời cho tệp video
                Path tempFilePath = Files.createTempFile("temp", ".flac");
                file.transferTo(tempFilePath.toFile());

                // Đọc dữ liệu từ tệp và chuyển đổi thành ByteString
                byte[] data = Files.readAllBytes(tempFilePath);
                ByteString audioBytes = ByteString.copyFrom(data);

                // Tạo RecognitionConfig
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                        .setLanguageCode("en-US")
                        .build();

                // Tạo RecognitionAudio từ ByteString
                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(audioBytes)
                        .build();

                // Gửi yêu cầu đến Google Cloud Speech-to-Text API và nhận kết quả
                RecognizeResponse response = speechClient.recognize(config, audio);

                // Trích xuất văn bản từ kết quả
                StringBuilder transcriptBuilder = new StringBuilder();
                for (SpeechRecognitionResult result : response.getResultsList()) {
                    // Lấy văn bản từ mỗi kết quả
                    String transcript = result.getAlternatives(0).getTranscript();
                    transcriptBuilder.append(transcript).append("\n");
                }

                // Xóa tệp tạm thời
                Files.deleteIfExists(tempFilePath);

                // Trả về văn bản trích xuất từ video
                return transcriptBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Lỗi xảy ra khi chuyển đổi video thành văn bản.";
        }
    }
}
