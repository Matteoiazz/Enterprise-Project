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
                byte[] bytes = file.getBytes();
                if (!looksLikeImage(bytes)) {
                    throw new IllegalArgumentException("Sono ammesse solo immagini");
                }
                Map<?, ?> result = cloudinary.uploader().upload(bytes,
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

    /**
     * Il Content-Type multipart è dichiarato dal client e falsificabile a piacere:
     * controlla invece i byte iniziali del file (magic number) contro le firme dei
     * formati immagine più comuni, prima di caricarlo sotto un URL pubblico che
     * sembra un'immagine legittima dell'app.
     */
    private boolean looksLikeImage(byte[] bytes) {
        if (bytes.length < 12) return false;
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return true; // JPEG
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return true; // PNG
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') return true; // GIF
        if (bytes[0] == 'B' && bytes[1] == 'M') return true; // BMP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return true; // WEBP
        return false;
    }
}
