package com.viettel.demo.service;

import com.viettel.demo.cache.CacheProperties;
import com.viettel.demo.cache.CachedMasterData;
import com.viettel.demo.cache.MasterDataCacheGateway;
import com.viettel.demo.context.TenantContext;
import com.viettel.demo.entity.MasterData;
import com.viettel.demo.messaging.MasterDataChangedEvent;
import com.viettel.demo.messaging.MasterDataEventPublisher;
import com.viettel.demo.repository.MasterDataRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/*
 * ==============================================================
 * MasterData Service — business logic layer
 * ==============================================================
 *
 * [Mục tiêu]
 * Service layer xử lý business logic cho MasterData.
 * Tầng này gọi Repository (đã tenant-aware) và cung cấp
 * API cho Controller.
 *
 * [Cách hoạt động hiện tại]
 * 1. Inject MasterDataRepository.
 * 2. Lấy tenant hiện tại từ TenantContext, không lấy tenant từ body.
 * 3. Gọi repository bằng các method có tenantId rõ ràng.
 * 4. Tạo mới dựa vào @PrePersist để set tenant_id.
 * 5. Update/delete đều tìm record trong phạm vi tenant trước.
 *
 * [Kiến thức cần tự research]
 * - @Service annotation
 * - Constructor injection vs @Autowired
 * - Optional<T> và cách xử lý khi entity không tìm thấy
 * - ResponseStatusException hoặc custom exception
 * - Soft delete pattern
 *
 * ==============================================================
 */
@Service
public class MasterDataService {

    private final MasterDataRepository repository;
    private final CacheProperties cacheProperties;
    private final MasterDataCacheGateway cacheGateway;
    private final MasterDataEventPublisher eventPublisher;

    public MasterDataService(
            MasterDataRepository repository,
            CacheProperties cacheProperties,
            MasterDataCacheGateway cacheGateway,
            MasterDataEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.cacheProperties = cacheProperties;
        this.cacheGateway = cacheGateway;
        this.eventPublisher = eventPublisher;
    }

    public List<MasterData> getAll() {
        return repository.findByTenantIdAndIsActiveTrue(currentTenantId());
    }

    public MasterData getById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID cannot be null");
        }
        return repository.findByTenantIdAndId(currentTenantId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MasterData not found"));
    }

    public MasterData getByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code cannot be blank");
        }
        Long tenantId = currentTenantId();
        String normalizedCode = code.trim();

        if (!cacheProperties.isEnabled()) {
            return findByTenantAndCode(tenantId, normalizedCode);
        }

        return cacheGateway.getByCode(tenantId, normalizedCode)
                .map(CachedMasterData::toDetachedEntity)
                .orElseGet(() -> {
                    MasterData data = findByTenantAndCode(tenantId, normalizedCode);
                    cacheGateway.putByCode(tenantId, normalizedCode, data);
                    return data;
                });
    }

    private MasterData findByTenantAndCode(Long tenantId, String code) {
        return repository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MasterData not found"));
    }

    public List<MasterData> getByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category cannot be blank");
        }
        return repository.findByTenantIdAndCategory(currentTenantId(), category);
    }

    public MasterData create(MasterData data) {
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterData cannot be null");
        }
        // tenant_id sẽ được set tự động trong @PrePersist của entity.
        MasterData saved = repository.save(data);
        /*
         * Kafka mini-lab:
         * Publish sau khi repository.save(...) đã thành công để event mô tả
         * dữ liệu đã được ghi vào PostgreSQL.
         *
         * Caveat: đây chưa phải outbox pattern, nên DB commit và Kafka publish
         * không atomic với nhau. Nếu publish fail, mini-lab cho fail rõ để học
         * thay vì giả vờ event đã gửi.
         */
        eventPublisher.publish(MasterDataChangedEvent.from(saved, "CREATED"));
        return saved;
    }

    public MasterData update(Long id, MasterData data) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID cannot be null");
        }
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterData cannot be null");
        }
        MasterData existing = getById(id);
        // Cập nhật các field cần thiết (code, name, category, isActive).
        existing.setCode(data.getCode());
        existing.setName(data.getName());
        existing.setCategory(data.getCategory());
        existing.setIsActive(data.getIsActive());
        MasterData saved = repository.save(existing);
        eventPublisher.publish(MasterDataChangedEvent.from(saved, "UPDATED"));
        return saved;
    }

    public void delete(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID cannot be null");
        }
        MasterData existing = getById(id);
        existing.setIsActive(false);
        repository.save(existing);
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context is missing");
        }
        return tenantId;
    }
}
