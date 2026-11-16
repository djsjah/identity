package com.identity.core.admin.image;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.identity.app.api.dto.SliceResponseDTO;
import com.identity.app.logging.LogExecutionTime;
import com.identity.core.admin.image.dto.ImageDTO;
import com.identity.persistence.image.ImageRepository;

@Service
@LogExecutionTime
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    public SliceResponseDTO<ImageDTO> getAll(int page, int size) {
        return imageMapper.toSliceResponse(imageRepository.findSlice(pageable(page, size)));
    }

    public ImageDTO getById(String id) {
        return imageMapper.toDTO(imageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"))
        );
    }

    public SliceResponseDTO<ImageDTO> search(int page, int size, String filepath, LocalDateTime uploadDate) {
        Pageable pageable = pageable(page, size);

        if (filepath != null) {
            return imageMapper.toSliceResponse(imageRepository.findByFilepathContainingIgnoreCase(filepath, pageable));
        }
        if (uploadDate != null) {
            return imageMapper.toSliceResponse(
                    imageRepository.findByUploadDateBetween(uploadDate, LocalDateTime.now(ZoneOffset.UTC), pageable)
            );
        }

        return imageMapper.toSliceResponse(imageRepository.findSlice(pageable));
    }

    public boolean existsById(String id) { return imageRepository.existsById(id); }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDate", "id"));
    }
}
