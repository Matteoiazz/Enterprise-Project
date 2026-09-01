package com.tripify.catalog_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogImageService {

    private final Cloudinary cloudinary;

    public List<String> upload(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        if (files == null) {
            return urls;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Sono ammesse solo immagini");
            }
            try {
                Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap("folder", "tripify_catalog"));
                Object url = result.get("secure_url") != null ? result.get("secure_url") : result.get("url");
                if (url != null) {
                    urls.add(url.toString());
                }
            } catch (IOException e) {
                log.error("Upload immagine annuncio fallito: {}", e.getMessage());
                throw new RuntimeException("Errore durante l'upload di un'immagine", e);
            }
        }
        return urls;
    }
}
