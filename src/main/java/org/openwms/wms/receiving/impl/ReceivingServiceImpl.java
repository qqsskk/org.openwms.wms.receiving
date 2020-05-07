/*
 * Copyright 2005-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openwms.wms.receiving.impl;

import org.ameba.annotation.Measured;
import org.ameba.annotation.TxService;
import org.ameba.exception.NotFoundException;
import org.ameba.exception.ResourceExistsException;
import org.openwms.core.units.api.Measurable;
import org.openwms.wms.inventory.Product;
import org.openwms.wms.receiving.ReceivingOrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.openwms.wms.order.OrderState.CANCELED;
import static org.openwms.wms.order.OrderState.CREATED;
import static org.openwms.wms.order.OrderState.PROCESSING;
import static org.openwms.wms.order.OrderState.UNDEFINED;
import static org.openwms.wms.receiving.ReceivingMessages.ALREADY_CANCELLED;
import static org.openwms.wms.receiving.ReceivingMessages.CANCELLATION_DENIED;

/**
 * A ReceivingServiceImpl is a Spring managed transactional Services that deals with {@link ReceivingOrder}s.
 * 
 * @author Heiko Scherrer
 */
@Validated
@TxService
class ReceivingServiceImpl implements ReceivingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceivingServiceImpl.class);
    private final ReceivingOrderRepository repository;
    private final ApplicationEventPublisher publisher;

    ReceivingServiceImpl(ReceivingOrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * {@inheritDoc}
    public ReceivingOrderPosition createOrderPosition(OrderPositionKey orderPositionKey, String productId,
            UnitType quantity, String barcode) {

        // Get order data
        ReceivingOrder order = rcvOrderDao.findByOrderId(orderPositionKey.getOrderId());

        // Search Product
        Product product = productDao.findBySku(productId);

        List<LoadUnit> loadUnits = loadUnitDao.findByTransportUnit(new Barcode(barcode));
        String physicalPosition = "";
        if (!loadUnits.isEmpty()) {
            String currentMaxPosition = loadUnits.get(loadUnits.size() - 1).getPhysicalPosition();
            try {
                int val = Integer.valueOf(currentMaxPosition);
                val++;
                physicalPosition = String.valueOf(val);
            } catch (NumberFormatException e) {}
        }

        TransportUnit transportUnit = transportUnitSrv.findByBarcode(new Barcode(barcode));

        LoadUnit loadUnit = new LoadUnit(transportUnit, physicalPosition, product);

        PackagingUnit pu = new PackagingUnit(loadUnit, quantity);
        ReceivingOrderPosition rcvOrderPosition = new ReceivingOrderPosition(order, orderPositionKey.getPositionNo(),
                quantity, product);
        return wmsOrderDao.createOrderPosition(rcvOrderPosition);
    }
     */

    /**
     * {@inheritDoc}
     */
    @Override
    @Measured
    public ReceivingOrder createOrder(ReceivingOrder order) {
        Assert.notNull(order, "order to create must not be null");
        Optional<ReceivingOrder> opt = repository.findByOrderId(order.getOrderId());
        if (opt.isPresent()) {
            throw new ResourceExistsException(format("The ReceivingOrder with orderId [%s] already exists", order));
        }
        order = repository.save(order);
        publisher.publishEvent(new ReceivingOrderCreatedEvent(order));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("ReceivingOrder with orderId [{}] saved", order.getOrderId());
        }
        return order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Measured
    public ReceivingOrder capture(@NotEmpty String pKey, @NotEmpty String transportUnitId, @NotNull Measurable<?, ?, ?> quantityReceived, @NotNull Product product) {
        ReceivingOrder receivingOrder = repository.findBypKey(pKey).orElseThrow(() -> new NotFoundException("No ReceivingOrder found"));
        receivingOrder.getPositions().stream()
                .filter(p->p.getState() == CREATED || p.getState() == PROCESSING)
                .filter(p->p.getProduct().equals(product))
                .filter(p->p.getQuantityExpected().getUnitType().equals(quantityReceived.getUnitType()))
                .forEach( p -> {
                    // capture ...
                });
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Measured
    public ReceivingOrder findByPKey(String pKey) {
        Assert.hasText(pKey, "pKey must not be null");
        Optional<ReceivingOrder> order = repository.findBypKey(pKey);
        return order.orElseThrow(() -> new NotFoundException(format("ReceivingOrder with pKey [%s] does not exist", pKey)));
    }

    /**
     * {@inheritDoc}
     */
    @Measured
    @Override
    public Optional<ReceivingOrder> findByOrderId(@NotEmpty String orderId) {
        return repository.findByOrderId(orderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Measured
    public void cancelOrder(String pKey) {
        Assert.hasText(pKey, "pKey must not be null");
        ReceivingOrder order = repository.findBypKey(pKey).orElseThrow(() -> new NotFoundException(format("ReceivingOrder with pKey [%s] does not exist", pKey)));
        LOGGER.info("Cancelling ReceivingOrder [{}] in state [{}]", order.getOrderId(), order.getOrderState());
        if (order.getOrderState() == CANCELED) {
            throw new AlreadyCancelledException(
                    format("ReceivingOrder [%s] is already in state [%s]", order.getOrderId(), order.getOrderState()),
                    ALREADY_CANCELLED,
                    new String[]{order.getOrderId(), order.getOrderState().name(), order.getPersistentKey()}
            );
        }
        if (order.getOrderState() != UNDEFINED && order.getOrderState() != CREATED) {
            throw new CancellationDeniedException(
                    format("Cancellation of ReceivingOrder [%s] is not allowed because order is already in state [%s]", order.getOrderId(), order.getOrderState()),
                    CANCELLATION_DENIED,
                    new String[]{order.getOrderId(), order.getOrderState().name(), order.getPersistentKey()}
            );
        }
        order.setOrderState(CANCELED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Measured
    public List<ReceivingOrder> findAll() {
        List<ReceivingOrder> all = repository.findAll();
        all.forEach(o -> System.out.println(o.getOrderId()+o.getPositions()));
        return all;
    }
}