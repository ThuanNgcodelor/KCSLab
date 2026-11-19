package com.example.app.intake.service;

import com.example.app.intake.dto.SampleCodeEventResponse;
import com.example.app.intake.dto.SampleCodingResponse;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.entity.SampleCodeEvent;
import com.example.app.intake.entity.SampleCodingStatus;
import com.example.app.intake.repository.SampleCodeEventRepository;
import com.example.app.intake.repository.SampleRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CFC-TN-14 coding workflow: {@code PENDING_CODING -> PENDING_VERIFICATION -> VERIFIED}, with
 * {@code CORRECTION_REQUIRED} when the independent check fails. The verifier must differ from the coder.
 * Which roles may verify is an assumption (ADMIN/RECEPTIONIST) that LAB still has to confirm.
 */
@Service
public class SampleCodingService {
    private final SampleRepository samples;
    private final SampleCodeEventRepository events;
    private final SampleCodeSequence sequence;

    public SampleCodingService(SampleRepository samples, SampleCodeEventRepository events,
                               SampleCodeSequence sequence) {
        this.samples = samples; this.events = events; this.sequence = sequence;
    }

    @Transactional(readOnly = true)
    public SampleCodingResponse find(Long sampleId) {
        return SampleCodingResponse.from(samples.findById(sampleId).orElseThrow(this::notFound));
    }

    @Transactional(readOnly = true)
    public List<SampleCodeEventResponse> history(Long sampleId) {
        if (!samples.existsById(sampleId)) throw notFound();
        return events.findBySample_IdOrderByIdAsc(sampleId).stream().map(SampleCodeEventResponse::from).toList();
    }

    /** The coder confirms the code is assigned and the label is printed/applied. */
    @Transactional
    public SampleCodingResponse submit(Long sampleId, User actor) {
        requireCodingRole(actor);
        Sample sample = lock(sampleId);
        requireStatus(sample, SampleCodingStatus.PENDING_CODING);
        sample.submitCoding(actor);
        events.save(new SampleCodeEvent(sample, SampleCodeEvent.Type.SUBMITTED, sample.getSampleCode(),
                sample.getSampleCode(), null, actor));
        return SampleCodingResponse.from(sample);
    }

    @Transactional
    public SampleCodingResponse verify(Long sampleId, User actor) {
        requireCodingRole(actor);
        return verifyIndependent(sampleId, actor);
    }

    /** Called only after the receiving service checks the accepted transfer and its sample. */
    @Transactional
    public SampleCodingResponse verifyAtKcs(Long sampleId, User actor) {
        if (actor.getRole() != Role.TECHNICIAN && actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền đối chiếu tại KCS");
        }
        return verifyIndependent(sampleId, actor);
    }

    private SampleCodingResponse verifyIndependent(Long sampleId, User actor) {
        Sample sample = lock(sampleId);
        requireStatus(sample, SampleCodingStatus.PENDING_VERIFICATION);
        requireIndependent(sample, actor);
        sample.verifyCoding(actor);
        events.save(new SampleCodeEvent(sample, SampleCodeEvent.Type.VERIFIED, sample.getSampleCode(),
                sample.getSampleCode(), null, actor));
        return SampleCodingResponse.from(sample);
    }

    @Transactional
    public SampleCodingResponse reject(Long sampleId, String reason, User actor) {
        requireCodingRole(actor);
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do khi đối chiếu mã không đạt");
        }
        Sample sample = lock(sampleId);
        requireAtIntake(sample);
        requireStatus(sample, SampleCodingStatus.PENDING_VERIFICATION);
        requireIndependent(sample, actor);
        sample.rejectCoding();
        events.save(new SampleCodeEvent(sample, SampleCodeEvent.Type.REJECTED, sample.getSampleCode(), null,
                reason.trim(), actor));
        return SampleCodingResponse.from(sample);
    }

    /**
     * Issues a new code after a rejected check. The old code stays in the event history and is never
     * reissued because the daily counter only moves forward.
     */
    @Transactional
    public SampleCodingResponse correct(Long sampleId, String sourceCode, String productType,
                                        String reason, User actor) {
        requireCodingRole(actor);
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do hiệu chỉnh mã mẫu");
        }
        Sample sample = lock(sampleId);
        requireAtIntake(sample);
        requireStatus(sample, SampleCodingStatus.CORRECTION_REQUIRED);
        String newSource = sourceCode == null || sourceCode.isBlank()
                ? sample.getSourceCode() : SampleCodes.normalizeSource(sourceCode);
        String newProduct = productType == null || productType.isBlank()
                ? sample.getProductType() : productType.trim();
        if (newSource.equals(sample.getSourceCode()) && newProduct.equals(sample.getProductType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hiệu chỉnh phải thay đổi mã nguồn hoặc loại sản phẩm");
        }
        String oldCode = sample.getSampleCode();
        int next = sequence.next(sample.getReceivedDate());
        String newCode = SampleCodes.format(newSource, newProduct, sample.getReceivedDate(), next);
        sample.applyCorrection(newCode, newSource, newProduct, next);
        samples.flush();
        events.save(new SampleCodeEvent(sample, SampleCodeEvent.Type.CORRECTED, oldCode, newCode,
                reason.trim(), actor));
        return SampleCodingResponse.from(sample);
    }

    @Transactional
    public SampleCodingResponse replaceManualCode(Long sampleId, String code, String reason, User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin được sửa mã mẫu");
        }
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã mẫu không được để trống");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do sửa mã mẫu");
        }
        Sample sample = lock(sampleId);
        requireAtIntake(sample);
        String normalized = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.equalsIgnoreCase(sample.getSampleCode())) return SampleCodingResponse.from(sample);
        if (samples.existsBySampleCodeIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã mẫu đã tồn tại");
        }
        String old = sample.getSampleCode();
        sample.replaceManualCode(normalized, actor);
        samples.flush();
        events.save(new SampleCodeEvent(sample, SampleCodeEvent.Type.CORRECTED, old, normalized,
                reason.trim(), actor));
        return SampleCodingResponse.from(sample);
    }

    private Sample lock(Long id) {
        return samples.findByIdForUpdate(id).orElseThrow(this::notFound);
    }

    private void requireAtIntake(Sample sample) {
        if (sample.getStatus() != com.example.app.intake.entity.ReceiptStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể hiệu chỉnh mã khi mẫu đang hoặc đã được bàn giao; cần xử lý bàn giao trước");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mẫu");
    }

    private void requireStatus(Sample sample, SampleCodingStatus expected) {
        if (sample.getCodingStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Trạng thái mã hóa hiện tại là " + sample.getCodingStatus() + ", không phải " + expected);
        }
    }

    private void requireIndependent(Sample sample, User actor) {
        if (sample.getCodedBy() != null && sample.getCodedBy().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Người đối chiếu phải khác người mã hóa");
        }
    }

    private void requireCodingRole(User actor) {
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.RECEPTIONIST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thao tác mã hóa mẫu");
        }
    }
}
