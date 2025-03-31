package com.edem.birdie;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BirdService {
    private final BirdRepository repository;
    private final   S3Client s3Client;
    private final S3Presigner s3Presigner;

    private String BUCKET_NAME = "imagery-app";



    public Map<String, Object> getImages(int page, int size) {
        Map<String, Object> result = new HashMap<>();

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        Page<Bird> carPage = repository.findAll(pageable);

        List<Map<String, String>> imageData = carPage.getContent().stream()
                .map(car -> {
                    Map<String, String> imageMap = new HashMap<>();
                    imageMap.put("key", car.getS3url());
                    imageMap.put("value", car.getDescription() != null ? car.getDescription() : "No description available");
                    return imageMap;
                })
                .collect(Collectors.toList());

        result.put("images", imageData);
        result.put("totalPages", carPage.getTotalPages());
        result.put("currentPage", carPage.getNumber());
        result.put("hasNextPage", carPage.hasNext());

        return result;
    }



    public String uploadImage(MultipartFile file, String description) throws IOException {

            String key = "image_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            String s3Url = "https://" + BUCKET_NAME + ".s3.amazonaws.com/" + key;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // Save to the database
            Bird bird = Bird.builder()
                    .name(file.getOriginalFilename())
                    .description(description)
                    .s3url(s3Url)
                    .build();

            repository.save(bird);
        System.out.println("service" + file.getSize());

        return "success";
    }



    public boolean deleteImage(String imageKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(imageKey)
                    .build();
            Bird birdOptional = repository.findByS3urlEndingWith(imageKey).orElseThrow( ()-> new RuntimeException("Image not found"));
            repository.delete(birdOptional);
            s3Client.deleteObject(deleteObjectRequest);
            return true;
        } catch (S3Exception e) {
            e.printStackTrace();
            System.out.println("false");
            return false;
        }
    }


}
