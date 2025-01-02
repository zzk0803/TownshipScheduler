package zzk.townshipscheduler.backend.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * DTO for {@link PlayerEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PlayerEntityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 7196839563493521579L;

    private final Long id;

    private final Integer level;

    private final AccountEntityDtoJustId account;

    private final Set<FieldFactoryEntityDto> fieldFactoryEntities;

    private final Set<OrderEntityDto> orderEntities;

    private final WarehouseEntityDto warehouseEntity;

    public PlayerEntityDto(
            Long id,
            Integer level,
            AccountEntityDtoJustId account,
            Set<FieldFactoryEntityDto> fieldFactoryEntities,
            Set<OrderEntityDto> orderEntities,
            WarehouseEntityDto warehouseEntity
    ) {
        this.id = id;
        this.level = level;
        this.account = account;
        this.fieldFactoryEntities = fieldFactoryEntities;
        this.orderEntities = orderEntities;
        this.warehouseEntity = warehouseEntity;
    }

    public Long id() {
        return id;
    }

    public Integer level() {
        return level;
    }

    public AccountEntityDtoJustId account() {
        return account;
    }

    public Set<FieldFactoryEntityDto> fieldFactoryEntities() {
        return fieldFactoryEntities;
    }

    public Set<OrderEntityDto> orderEntities() {
        return orderEntities;
    }

    public WarehouseEntityDto warehouseEntity() {
        return warehouseEntity;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlayerEntityDto) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.level, that.level) &&
               Objects.equals(this.account, that.account) &&
               Objects.equals(this.fieldFactoryEntities, that.fieldFactoryEntities) &&
               Objects.equals(this.orderEntities, that.orderEntities) &&
               Objects.equals(this.warehouseEntity, that.warehouseEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, level, account, fieldFactoryEntities, orderEntities, warehouseEntity);
    }

    @Override
    public String toString() {
        return "PlayerEntityDto[" +
               "id=" + id + ", " +
               "level=" + level + ", " +
               "account=" + account + ", " +
               "fieldFactoryEntities=" + fieldFactoryEntities + ", " +
               "orderEntities=" + orderEntities + ", " +
               "warehouseEntity=" + warehouseEntity + ']';
    }


}
