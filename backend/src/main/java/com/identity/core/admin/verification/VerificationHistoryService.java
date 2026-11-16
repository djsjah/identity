package com.identity.core.admin.verification;

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
import com.identity.core.admin.verification.dto.VerificationHistoryDTO;
import com.identity.core.admin.verification.dto.VerificationHistoryDetailsDTO;
import com.identity.core.admin.verification.dto.VerificationHistorySearchDTO;
import com.identity.persistence.verification.history.VerificationHistory;
import com.identity.persistence.verification.history.VerificationHistoryRepository;
import com.identity.persistence.user.version.UserVersion;
import com.identity.persistence.user.version.UserVersionRepository;

@Service
@LogExecutionTime
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationHistoryService {
    private final VerificationHistoryRepository historyRepository;
    private final UserVersionRepository userVersionRepository;
    private final VerificationHistoryMapper historyMapper;

    public SliceResponseDTO<VerificationHistoryDTO> getAll(int page, int size) {
        return historyMapper.toSliceResponse(historyRepository.findSlice(pageable(page, size)));
    }

    public VerificationHistoryDetailsDTO getById(String id) {
        VerificationHistory history = historyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verification history not found"));

        UserVersion userVersion = userVersionRepository.findAt(
                history.getUser().getId(),
                history.getVerificationDate()
        ).orElseThrow(() -> new IllegalStateException("User version not found for verification " + id));

        return historyMapper.toDetailDTO(history, userVersion);
    }

    public SliceResponseDTO<VerificationHistoryDTO> search(int page, int size, VerificationHistorySearchDTO request) {
        Pageable pageable = pageable(page, size);

        if (request.result() != null) {
            return historyMapper.toSliceResponse(historyRepository.findByResult(request.result(), pageable));
        }
        if (request.userId() != null) {
            return historyMapper.toSliceResponse(historyRepository.findByUserId(request.userId().longValue(), pageable));
        }
        if (request.imageId() != null) {
            return historyMapper.toSliceResponse(historyRepository.findByImageId(request.imageId(), pageable));
        }
        if (request.verificationDate() != null) {
            return historyMapper.toSliceResponse(
                    historyRepository.findByVerificationDateBetween(
                            LocalDateTime.parse(request.verificationDate()),
                            LocalDateTime.now(ZoneOffset.UTC),
                            pageable
                    )
            );
        }

        return historyMapper.toSliceResponse(historyRepository.findSlice(pageable));
    }

    @Transactional
    public void deleteById(String id) { historyRepository.deleteById(id); }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "verificationDate", "id"));
    }
}
