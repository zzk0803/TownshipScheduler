package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.crawling.TownshipFandomCrawlingProcessFacade;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.dao.WikiCrawledEntityRepository;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;

import java.util.*;

@Data
@SpringComponent
@UIScope
@RequiredArgsConstructor
public class ProductCategoriesPanelPresenter {

    private final ProductEntityRepository productEntityRepository;

    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    private final TransactionTemplate transactionTemplate;

    private final TownshipFandomCrawlingProcessFacade townshipFandomCrawlingProcessFacade;

    private ProductCategoriesPanel productCategoriesPanel;

    private FieldFactoryInfoEntity currentSelectFactory;

    private Optional<ProductEntity> currentSelect;

    private List<FieldFactoryInfoEntity> factoryList;

    private Set<ProductEntity> productEntities;

    private GridListDataView<ProductEntity> gridListDataView;


    public void setGroupByCategoryGrid(ProductCategoriesPanel productCategoriesPanel) {
        this.productCategoriesPanel = productCategoriesPanel;
    }


    public void queryAndCache() {

        productEntities = productEntityRepository.findBy(
                ProductEntity.class,
                Sort.by(Sort.Order.asc("level"))
        );

        factoryList = productEntities.stream()
                .map(ProductEntity::getFieldFactoryInfo)
                .distinct()
                .sorted(Comparator.comparing(FieldFactoryInfoEntity::getLevel, Integer::compareTo))
                .toList();
    }


    public void setupGridItems(Grid<ProductEntity> grid) {
        gridListDataView = grid.setItems(productEntities);
    }


    public void setupCategoriesItems(RadioButtonGroup<FieldFactoryInfoEntity> categorySelectComponent) {
        categorySelectComponent.setItems(this.factoryList);
    }


    public void refreshImgBtnClick(ProductEntity productEntity) {
        String name = productEntity.getName();
        WikiCrawledEntity wikiCrawledEntity = wikiCrawledEntityRepository.findByText(name);
        String html = wikiCrawledEntity.getHtml();
        townshipFandomCrawlingProcessFacade
                .downloadImage(html)
                .thenAccept(
                        bytes -> {
                            transactionTemplate.execute(ts -> {
                                wikiCrawledEntity.setImageBytes(bytes);
                                productEntity.setCrawledAsImage(wikiCrawledEntityRepository.save(wikiCrawledEntity));
                                return productEntityRepository.save(productEntity);
                            });
                        }
                )
                .join();
        productCategoriesPanel.refreshImgBtnClickDone(productEntity);
    }

    public void filterGoods(String filterCriteria) {
        if (filterCriteria.isBlank()) {
            reset();
            getGridListDataView().refreshAll();
        }

        this.gridListDataView.addFilter(product -> {
                    String productName = product.getName();
                    return productName.contains(filterCriteria) || filterCriteria.contains(productName);
                }
        );
    }


    public void reset() {
        this.gridListDataView.removeFilters();
        this.currentSelectFactory = null;
    }

    public void filterCategoryProduct(FieldFactoryInfoEntity category) {
        if (Objects.nonNull(category)) {
            this.currentSelectFactory = category;
            this.gridListDataView.addFilter(product -> this.currentSelectFactory.equals(product.getFieldFactoryInfo()));
        }
    }

}
