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
package org.openwms.wms.receiving.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openwms.core.units.api.Measurable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * A ReceivingOrderPositionVO.
 *
 * @author Heiko Scherrer
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReceivingOrderPositionVO implements Serializable {

    /** The unique position ID within an ReceivingOrder - must not be empty. */
    @NotEmpty
    @JsonProperty("positionId")
    private String positionId;
    /** The expected quantity of the expected product - must not be {@literal null}. */
    @NotNull
    @JsonProperty("quantityExpected")
    private Measurable<?, ?, ?> quantityExpected;
    /** The unique SKU of the expected {@code Product} - must not be empty. */
    @NotNull
    @JsonProperty("product")
    private ProductVO product;
    /** Optional: How the position should be processed, manually oder automatically. */
    @JsonProperty("startMode")
    private String startMode;
    /** Optional: Expected receipts may also carry the unique identifier of the suppliers {@code TransportUnit}. */
    @JsonProperty("transportUnitId")
    private String transportUnitId;
    /** Optional: The suppliers type of {@code TransportUnit}. */
    @JsonProperty("transportUnitType")
    private String transportUnitType;
    @JsonProperty("supplierPackingUnit")
    private String supplierPackingUnit;

    @JsonCreator
    ReceivingOrderPositionVO() {
    }

    public ReceivingOrderPositionVO(@NotEmpty String positionId, @NotEmpty Measurable<?, ?, ?> quantityExpected, @NotNull ProductVO product) {
        this.positionId = positionId;
        this.quantityExpected = quantityExpected;
        this.product = product;
    }

    public String getPositionId() {
        return positionId;
    }

    public Measurable<?, ?, ?> getQuantityExpected() {
        return quantityExpected;
    }

    public void setQuantityExpected(Measurable<?, ?, ?> quantityExpected) {
        this.quantityExpected = quantityExpected;
    }

    public ProductVO getProduct() {
        return product;
    }

    public String getStartMode() {
        return startMode;
    }

    public String getTransportUnitId() {
        return transportUnitId;
    }

    public String getTransportUnitType() {
        return transportUnitType;
    }

    public String getSupplierPackingUnit() {
        return supplierPackingUnit;
    }
}
