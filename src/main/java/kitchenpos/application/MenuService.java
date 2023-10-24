package kitchenpos.application;

import kitchenpos.dao.MenuGroupRepository;
import kitchenpos.dao.MenuRepository;
import kitchenpos.dao.ProductRepository;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuProduct;
import kitchenpos.ui.request.MenuCreateRequest;
import kitchenpos.ui.response.MenuResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final ProductRepository productRepository;

    public MenuService(
            final MenuRepository menuRepository,
            final MenuGroupRepository menuGroupRepository,
            final ProductRepository productRepository) {
        this.menuRepository = menuRepository;
        this.menuGroupRepository = menuGroupRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public MenuResponse create(final MenuCreateRequest request) {
        final List<MenuProduct> menuProducts = request.getMenuProducts().stream()
                .map(it -> new MenuProduct(
                                productRepository.findById(it.getProductId()).get(),
                                it.getQuantity()
                        )
                ).collect(Collectors.toList());

        final Menu savedMenu = menuRepository.save(
                new Menu(
                        request.getName(),
                        request.getPrice(),
                        menuGroupRepository.findById(request.getMenuGroupId()).get(),
                        menuProducts
                )
        );

        savedMenu.updateMenuProducts(menuProducts);
        return MenuResponse.from(savedMenu);
    }

    public List<MenuResponse> list() {
        return MenuResponse.from(menuRepository.findAll());
    }
}
