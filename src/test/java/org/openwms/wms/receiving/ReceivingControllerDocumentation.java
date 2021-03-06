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
package org.openwms.wms.receiving;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openwms.core.units.api.Piece;
import org.openwms.wms.inventory.api.PackagingUnitApi;
import org.openwms.wms.receiving.api.CaptureRequestVO;
import org.openwms.wms.receiving.api.ProductVO;
import org.openwms.wms.receiving.api.ReceivingOrderPositionVO;
import org.openwms.wms.receiving.api.ReceivingOrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A ReceivingControllerDocumentation.
 *
 * @author Heiko Scherrer
 */
//@ActiveProfiles(SpringProfiles.ASYNCHRONOUS_PROFILE)
@ReceivingApplicationTest
class ReceivingControllerDocumentation {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ObjectMapper om;
    @MockBean
    private PackagingUnitApi packagingUnitApi;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        packagingUnitApi = mock(PackagingUnitApi.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(documentationConfiguration(restDocumentation)).build();
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(packagingUnitApi);
    }

    @Test void shall_return_index() throws Exception {
        mockMvc
                .perform(
                        get("/v1/receiving-orders/index")
                )
                .andExpect(status().isOk())
                .andDo(document("get-order-index", preprocessResponse(prettyPrint())))
        ;
    }

    @Transactional
    @Rollback
    @Test void shall_create_order() throws Exception {
        ReceivingOrderVO orderVO = new ReceivingOrderVO("4712");
        orderVO.getPositions().add(new ReceivingOrderPositionVO("1", null, null));
        mockMvc
                .perform(
                        post("/v1/receiving-orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(orderVO))
                )
                .andExpect(status().isBadRequest())
                .andDo(document("order-create-400", preprocessResponse(prettyPrint())))
        ;

        orderVO.getPositions().clear();
        orderVO.getPositions().add(new ReceivingOrderPositionVO("1", Piece.of(1), new ProductVO("SKU001")));
        mockMvc
                .perform(
                        post("/v1/receiving-orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(orderVO))
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(LOCATION, notNullValue()))
                .andDo(document("order-create",
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("orderId").description("An unique identifier of the ReceivingOrder to create"),
                                fieldWithPath("positions[]").description("An array of positions, must not be empty"),
                                fieldWithPath("positions[].positionId").description("Unique identifier of the ReceivingOrderPosition within the ReceivingOrder"),
                                fieldWithPath("positions[].quantityExpected").description("The expected quantity of the Product"),
                                fieldWithPath("positions[].quantityExpected.@class").description("Must be one of the static values to identify the type of UOM"),
                                fieldWithPath("positions[].quantityExpected.unitType").description("Must be one of the static values to identify the concrete UOM"),
                                fieldWithPath("positions[].quantityExpected.magnitude").description("The amount"),
                                fieldWithPath("positions[].product.sku").description("The SKU of the expected Product"))
                        )
                )
        ;
    }

    @Test void shall_find_all() throws Exception {
        mockMvc
                .perform(
                        get("/v1/receiving-orders")
                )
                .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andDo(document("order-find-all", preprocessResponse(prettyPrint())))
        ;
    }

    @Test void shall_find_order() throws Exception {
        mockMvc
                .perform(
                        get("/v1/receiving-orders/d8099b89-bdb6-40d3-9580-d56aeadd578f")
                )
                .andExpect(status().isOk())
                .andDo(document("order-find",
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("pKey").description("The synthetic unique identifier of the ReceivingOrder"),
                                fieldWithPath("orderId").description("The business key of the ReceivingOrder"),
                                fieldWithPath("state").description("The current state of the ReceivingOrder"),
                                fieldWithPath("positions[].positionId").description("The position of the ReceivingOrderPosition"),
                                fieldWithPath("positions[].quantityExpected").description("The expected quantity to be received"),
                                fieldWithPath("positions[].quantityExpected.unitType").description("The expected type"),
                                fieldWithPath("positions[].quantityExpected.*").ignored(),
                                fieldWithPath("positions[].product").description("The expected Product to be received"),
                                fieldWithPath("positions[].product.*").ignored()
                        )
                ))
        ;
    }

    @Test void shall_find_orderBy_BK() throws Exception {
        mockMvc
                .perform(
                        get("/v1/receiving-orders").param("orderId", "T4711")
                )
                .andExpect(status().isOk())
                .andDo(document("order-findby-orderid", preprocessResponse(prettyPrint())))
        ;
    }

    @Test void shall_NOT_find_order() throws Exception {
        mockMvc
                .perform(
                        get("/v1/receiving-orders/unknown")
                )
                .andExpect(status().isNotFound())
                .andDo(document("order-find-404", preprocessResponse(prettyPrint())))
        ;
    }

    @Transactional
    @Rollback
    @Test void shall_cancel_order() throws Exception {
        String toLocation = createOrder("4714");
        mockMvc
                .perform(
                        delete(toLocation)
                )
                .andExpect(status().isNoContent())
                .andDo(document("order-cancel", preprocessResponse(prettyPrint())))
        ;
    }

    @Transactional
    @Rollback
    @Test void shall_cancel_cancelled_order() throws Exception {
        String toLocation = createOrder("4715");
        mockMvc
                .perform(
                        delete(toLocation)
                )
                .andExpect(status().isNoContent())
        ;
        mockMvc
                .perform(
                        delete(toLocation)
                )
                .andExpect(status().isGone())
                .andExpect(jsonPath("messageKey", is(ReceivingMessages.ALREADY_CANCELLED)))
                .andDo(document("order-cancel-cancelled", preprocessResponse(prettyPrint())))
        ;
    }

    @Test void shall_NOT_cancel_order() throws Exception {
        String toLocation = createOrder("4716");
        mockMvc
                .perform(
                        delete(toLocation)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("messageKey", is(ReceivingMessages.CANCELLATION_DENIED)))
                .andDo(document("order-cancel-403", preprocessResponse(prettyPrint())))
        ;
    }

    @Transactional
    @Rollback
    @Test void shall_capture_order() throws Exception {
        CaptureRequestVO vo = new CaptureRequestVO();
        vo.setTransportUnitId("4711");
        vo.setLoadUnitLabel("1");
        vo.setQuantityReceived(Piece.of(1));
        vo.setProduct(new ProductVO("C1"));
        mockMvc
                .perform(
                        post("/v1/receiving-orders/{pKey}/capture", TestData.ORDER1_PKEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(vo))
                )
                .andDo(document("order-capture", preprocessResponse(prettyPrint())))
                .andExpect(status().isNoContent())
        ;
    }

    @Transactional
    @Rollback
    @Test void shall_capture_order_INSUFFISIENT() throws Exception {
        CaptureRequestVO vo = new CaptureRequestVO();
        vo.setTransportUnitId("4711");
        vo.setLoadUnitLabel("1");
        vo.setQuantityReceived(Piece.of(2));
        vo.setProduct(new ProductVO("C1"));
        mockMvc
                .perform(
                        post("/v1/receiving-orders/{pKey}/capture", TestData.ORDER1_PKEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(vo))
                )
                .andDo(document("order-capture-500", preprocessResponse(prettyPrint())))
                .andExpect(status().isInternalServerError())
        ;
    }

    public String createOrder(String orderId) throws Exception {
        MvcResult result = mockMvc
                .perform(
                        post("/v1/receiving-orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(new ReceivingOrderVO(orderId)))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return (String) result.getResponse().getHeaderValue(LOCATION);
    }
}
