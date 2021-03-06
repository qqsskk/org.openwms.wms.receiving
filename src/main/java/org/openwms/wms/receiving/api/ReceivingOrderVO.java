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

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A ReceivingOrderVO.
 *
 * @author Heiko Scherrer
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReceivingOrderVO implements Serializable {

    /** The persistent identifier. */
    @JsonProperty("pKey")
    private String pKey;
    /** The unique identifier of an {@code ReceivingOrder}. */
    @NotEmpty
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("state")
    private String state;
    /** A set of {@code ReceivingOrderPosition}s belonging to this order. */
    @JsonProperty("positions")
    private Set<@Valid ReceivingOrderPositionVO> positions = new HashSet<>(0);

    @JsonCreator
    ReceivingOrderVO() {
    }

    public ReceivingOrderVO(@NotEmpty String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

     public Set<ReceivingOrderPositionVO> getPositions() {
        return positions;
    }

    public void setPositions(Set<ReceivingOrderPositionVO> positions) {
        this.positions = positions;
    }
}
