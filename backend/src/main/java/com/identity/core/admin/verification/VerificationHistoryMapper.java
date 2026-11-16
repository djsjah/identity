package com.identity.core.admin.verification;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import com.identity.app.api.dto.PaginationDTO;
import com.identity.app.api.dto.SliceResponseDTO;
import com.identity.core.admin.verification.dto.VerificationHistoryDTO;
import com.identity.core.admin.verification.dto.VerificationHistoryDetailsDTO;
import com.identity.core.admin.verification.dto.VerificationUserDTO;
import com.identity.core.admin.image.ImageMapper;
import com.identity.persistence.verification.history.VerificationHistory;
import com.identity.persistence.user.version.UserVersion;

@Component
@RequiredArgsConstructor
public class VerificationHistoryMapper {
    private final ImageMapper imageMapper;

    public VerificationHistoryDTO toDTO(VerificationHistory history) {
        return new VerificationHistoryDTO(
                history.getId(),
                imageMapper.toDTO(history.getImage()),
                new VerificationUserDTO(
                        history.getUser().getId(),
                        history.getUser().getRole(),
                        history.getUser().getUsername(),
                        history.getUser().getEmail()
                ),
                history.getVerificationDate(),
                history.getResult()
        );
    }

    public List<VerificationHistoryDTO> toDTO(List<VerificationHistory> histories) {
        return histories.stream().map(this::toDTO).toList();
    }

    public VerificationHistoryDetailsDTO toDetailDTO(VerificationHistory history, UserVersion userVersion) {
        VerificationHistoryDTO historyDTO = toDTO(history);

        return new VerificationHistoryDetailsDTO(
                historyDTO.id(),
                historyDTO.image(),
                historyDTO.user(),
                new VerificationHistoryDetailsDTO.UserSnapshot(
                        userVersion.getRole(),
                        userVersion.getEmail()
                ),
                historyDTO.verificationDate(),
                historyDTO.result()
        );
    }

    public SliceResponseDTO<VerificationHistoryDTO> toSliceResponse(Slice<VerificationHistory> slice) {
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
