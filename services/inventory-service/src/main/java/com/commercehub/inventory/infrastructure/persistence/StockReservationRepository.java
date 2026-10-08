package com.commercehub.inventory.infrastructure.persistence;

import com.commercehub.inventory.domain.entity.StockReservationEntity;
import com.commercehub.inventory.domain.enumtype.ReservationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class StockReservationRepository implements PanacheRepositoryBase<StockReservationEntity, String> {

    public List<StockReservationEntity> findReservedByOrderId(String orderId) {
        return list("orderId = ?1 and status = ?2", orderId, ReservationStatus.RESERVED);
    }
}
