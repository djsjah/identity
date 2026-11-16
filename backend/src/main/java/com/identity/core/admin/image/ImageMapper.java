package com.identity.core.admin.image;

import java.util.List;

import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import com.identity.app.api.dto.PaginationDTO;
import com.identity.app.api.dto.SliceResponseDTO;
import com.identity.core.admin.image.dto.ImageDTO;
import com.identity.persistence.image.Image;

@Component
public class ImageMapper {
    public ImageDTO toDTO(Image image) {
        return new ImageDTO(image.getId(), image.getFilepath(), image.getUploadDate());
    }

    public List<ImageDTO> toDTO(List<Image> images) { return images.stream().map(this::toDTO).toList(); }

    public SliceResponseDTO<ImageDTO> toSliceResponse(Slice<Image> slice) {
        return new SliceResponseDTO<>(
                new PaginationDTO(
                        slice.getNumber(),
                        slice.getSize(),
                        slice.getNumberOfElements(),
                        slice.isFirst(),
                        slice.isLast(),
                        slice.hasNext(),
                        slice.hasPrevious()
                ),
                toDTO(slice.getContent())
        );
    }
}
