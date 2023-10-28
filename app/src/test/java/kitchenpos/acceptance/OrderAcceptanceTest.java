package kitchenpos.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import kitchenpos.acceptance.step.OrderStep;
import kitchenpos.acceptance.step.TableGroupStep;
import kitchenpos.acceptance.step.TableStep;
import kitchenpos.ui.request.MenuProductCreateRequest;
import kitchenpos.ui.request.OrderCreateRequest;
import kitchenpos.ui.request.OrderLineItemCreateRequest;
import kitchenpos.ui.request.OrderUpdateOrderStatusRequest;
import kitchenpos.ui.request.TableCreateRequest;
import kitchenpos.ui.request.TableGroupCreateRequest;
import kitchenpos.ui.response.MenuGroupResponse;
import kitchenpos.ui.response.OrderResponse;
import kitchenpos.ui.response.ProductResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static kitchenpos.acceptance.step.MenuGroupStep.MENU_GROUP_REQUEST_일식;
import static kitchenpos.acceptance.step.MenuGroupStep.메뉴_그룹_생성_요청하고_메뉴_그룹_반환;
import static kitchenpos.acceptance.step.MenuStep.MENU_CREATE_REQUEST_스키야키;
import static kitchenpos.acceptance.step.MenuStep.메뉴_생성_요청하고_아이디_반환;
import static kitchenpos.acceptance.step.OrderStep.주문_생성_요청;
import static kitchenpos.acceptance.step.OrderStep.주문_생성_요청하고_주문_반환;
import static kitchenpos.acceptance.step.OrderStep.주문_조회_요청;
import static kitchenpos.acceptance.step.ProductStep.PRODUCT_CREATE_REQUEST_스키야키;
import static kitchenpos.acceptance.step.ProductStep.상품_생성_요청하고_상품_반환;
import static kitchenpos.acceptance.step.TableGroupStep.테이블_그룹_삭제_요청;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

public class OrderAcceptanceTest extends AcceptanceTest {

    @Nested
    class OrderCreateTest {

        @Test
        void 주문을_생성한다() {
            final Long orderTableId = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, false));

            final MenuGroupResponse menuGroup = 메뉴_그룹_생성_요청하고_메뉴_그룹_반환(MENU_GROUP_REQUEST_일식);
            final ProductResponse product = 상품_생성_요청하고_상품_반환(PRODUCT_CREATE_REQUEST_스키야키);

            final Long menuId = 메뉴_생성_요청하고_아이디_반환(
                    MENU_CREATE_REQUEST_스키야키(
                            BigDecimal.valueOf(11_900),
                            menuGroup.getId(),
                            List.of(new MenuProductCreateRequest(product.getId(), 1L))
                    )
            );

            final ExtractableResponse<Response> response = 주문_생성_요청(
                    new OrderCreateRequest(
                            orderTableId,
                            List.of(new OrderLineItemCreateRequest(menuId, 2L))
                    ));

            assertAll(
                    () -> assertThat(response.statusCode()).isEqualTo(CREATED.value()),
                    () -> assertThat(response.jsonPath().getLong("orderTableId")).isEqualTo(orderTableId)
            );
        }

        @Test
        void 주문_항목은_반드시_1개_이상이어야_한다() {
            final Long orderTableId = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, false));

            final ExtractableResponse<Response> response = 주문_생성_요청(
                    new OrderCreateRequest(
                            orderTableId,
                            List.of()
                    )
            );

            assertThat(response.statusCode()).isEqualTo(INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void 동일한_메뉴는_1개의_주문항목으로_표시되어야_한다() {
            final Long orderTableId = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, false));

            final MenuGroupResponse menuGroup = 메뉴_그룹_생성_요청하고_메뉴_그룹_반환(MENU_GROUP_REQUEST_일식);
            final ProductResponse product = 상품_생성_요청하고_상품_반환(PRODUCT_CREATE_REQUEST_스키야키);

            final Long menuId = 메뉴_생성_요청하고_아이디_반환(
                    MENU_CREATE_REQUEST_스키야키(
                            BigDecimal.valueOf(11_900),
                            menuGroup.getId(),
                            List.of(new MenuProductCreateRequest(product.getId(), 1L))
                    )
            );

            final ExtractableResponse<Response> response = 주문_생성_요청(new OrderCreateRequest(
                    orderTableId,
                    List.of(
                            new OrderLineItemCreateRequest(menuId, 1L),
                            new OrderLineItemCreateRequest(menuId, 1L)
                    )
            ));

            assertThat(response.statusCode()).isEqualTo(INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void 주문을_생성하려면_주문하는_테이블이_존재해야_한다() {
            final MenuGroupResponse menuGroup = 메뉴_그룹_생성_요청하고_메뉴_그룹_반환(MENU_GROUP_REQUEST_일식);
            final ProductResponse product = 상품_생성_요청하고_상품_반환(PRODUCT_CREATE_REQUEST_스키야키);

            final Long menuId = 메뉴_생성_요청하고_아이디_반환(
                    MENU_CREATE_REQUEST_스키야키(
                            BigDecimal.valueOf(11_900),
                            menuGroup.getId(),
                            List.of(new MenuProductCreateRequest(product.getId(), 1L))
                    )
            );

            final ExtractableResponse<Response> response = 주문_생성_요청(new OrderCreateRequest(
                    1L,
                    List.of(new OrderLineItemCreateRequest(menuId, 1L))
            ));

            assertThat(response.statusCode()).isEqualTo(INTERNAL_SERVER_ERROR.value());
        }
    }

    @Nested
    class OrderQueryTest {

        @Test
        void 주문을_조회한다() {
            final Long orderTableId = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, false));
            final MenuGroupResponse menuGroup = 메뉴_그룹_생성_요청하고_메뉴_그룹_반환(MENU_GROUP_REQUEST_일식);
            final ProductResponse product = 상품_생성_요청하고_상품_반환(PRODUCT_CREATE_REQUEST_스키야키);

            final Long menuId = 메뉴_생성_요청하고_아이디_반환(
                    MENU_CREATE_REQUEST_스키야키(
                            BigDecimal.valueOf(11_900),
                            menuGroup.getId(),
                            List.of(new MenuProductCreateRequest(product.getId(), 1L))
                    )
            );

            final OrderResponse savedOrder = 주문_생성_요청하고_주문_반환(
                    new OrderCreateRequest(
                            orderTableId,
                            List.of(new OrderLineItemCreateRequest(menuId, 2L))
                    )
            );

            final ExtractableResponse<Response> response = 주문_조회_요청();
            final List<OrderResponse> result = response.jsonPath().getList("", OrderResponse.class);

            assertAll(
                    () -> assertThat(response.statusCode()).isEqualTo(OK.value()),
                    () -> assertThat(result).hasSize(1),
                    () -> assertThat(result.get(0))
                            .usingRecursiveComparison()
                            .isEqualTo(savedOrder)
            );
        }
    }

    @Nested
    class QueryUpdateTest {

        @Test
        void 주문_상태를_변경한다() {
            final Long orderTableId = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, false));

            final MenuGroupResponse menuGroup = 메뉴_그룹_생성_요청하고_메뉴_그룹_반환(MENU_GROUP_REQUEST_일식);
            final ProductResponse product = 상품_생성_요청하고_상품_반환(PRODUCT_CREATE_REQUEST_스키야키);

            final Long menuId = 메뉴_생성_요청하고_아이디_반환(
                    MENU_CREATE_REQUEST_스키야키(
                            BigDecimal.valueOf(11_900),
                            menuGroup.getId(),
                            List.of(new MenuProductCreateRequest(product.getId(), 1L))
                    )
            );

            final OrderResponse savedOrder = 주문_생성_요청하고_주문_반환(
                    new OrderCreateRequest(
                            orderTableId,
                            List.of(new OrderLineItemCreateRequest(menuId, 2L))
                    )
            );

            final ExtractableResponse<Response> response = OrderStep.주문_상태_변경_요청(savedOrder.getId(), new OrderUpdateOrderStatusRequest("COMPLETION"));
            final OrderResponse result = response.jsonPath().getObject("", OrderResponse.class);

            assertAll(
                    () -> assertThat(response.statusCode()).isEqualTo(OK.value()),
                    () -> assertThat(result.getOrderStatus()).isEqualTo("COMPLETION")
            );
        }

        @Test
        void 주문_상태가_COMPLETION인_테이블_상태는_변경할_수_없다() {
            final Long orderTableId = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, false));

            final MenuGroupResponse menuGroup = 메뉴_그룹_생성_요청하고_메뉴_그룹_반환(MENU_GROUP_REQUEST_일식);
            final ProductResponse product = 상품_생성_요청하고_상품_반환(PRODUCT_CREATE_REQUEST_스키야키);

            final Long menuId = 메뉴_생성_요청하고_아이디_반환(
                    MENU_CREATE_REQUEST_스키야키(
                            BigDecimal.valueOf(11_900),
                            menuGroup.getId(),
                            List.of(new MenuProductCreateRequest(product.getId(), 1L))
                    )
            );

            final OrderResponse savedOrder = 주문_생성_요청하고_주문_반환(
                    new OrderCreateRequest(
                            orderTableId,
                            List.of(new OrderLineItemCreateRequest(menuId, 2L))
                    )
            );

            OrderStep.주문_상태_변경_요청(savedOrder.getId(), new OrderUpdateOrderStatusRequest("COMPLETION"));

            final ExtractableResponse<Response> response = OrderStep.주문_상태_변경_요청(savedOrder.getId(), new OrderUpdateOrderStatusRequest("COMPLETION"));

            assertThat(response.statusCode()).isEqualTo(INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void 주문_상태가_식사중이거나_조리중인_테이블은_그룹을_해제할_수_없다() {
            final Long table1Id = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, true));
            final Long table2Id = TableStep.테이블_생성_요청하고_아이디_반환(new TableCreateRequest(5, true));

            final Long tableGroupId = TableGroupStep.테이블_그룹_생성_요청하고_아이디_반환(new TableGroupCreateRequest(List.of(table1Id, table2Id)));

            final MenuGroupResponse menuGroup = 메뉴_그룹_생성_요청하고_메뉴_그룹_반환(MENU_GROUP_REQUEST_일식);
            final ProductResponse product = 상품_생성_요청하고_상품_반환(PRODUCT_CREATE_REQUEST_스키야키);

            final Long menuId = 메뉴_생성_요청하고_아이디_반환(
                    MENU_CREATE_REQUEST_스키야키(
                            BigDecimal.valueOf(11_900),
                            menuGroup.getId(),
                            List.of(new MenuProductCreateRequest(product.getId(), 1L))
                    )
            );

            주문_생성_요청(
                    new OrderCreateRequest(
                            table1Id,
                            List.of(new OrderLineItemCreateRequest(menuId, 2L))
                    )
            );

            final ExtractableResponse<Response> response = 테이블_그룹_삭제_요청(tableGroupId);

            assertThat(response.statusCode()).isEqualTo(INTERNAL_SERVER_ERROR.value());
        }
    }
}
