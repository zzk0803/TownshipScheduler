package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.crawling.TownshipFandomCrawlingProcessFacade;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SpringComponent
@UIScope
@RequiredArgsConstructor
public class GoodsCategoriesPanelPresenter {

    private final ProductEntityRepository productEntityRepository;

    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    private final TransactionTemplate transactionTemplate;

    private final TownshipFandomCrawlingProcessFacade townshipFandomCrawlingProcessFacade;

    private GoodsCategoriesPanel goodsCategoriesPanel;

    private FieldFactoryInfoEntity currentSelectFactory;

    private Optional<ProductEntity> currentSelect;

    private List<FieldFactoryInfoEntity> factoryList;

    private List<ProductEntity> productEntityList;

    private GridListDataView<ProductEntity> gridListDataView;


    public void setGroupByCategoryGrid(GoodsCategoriesPanel goodsCategoriesPanel) {
        this.goodsCategoriesPanel = goodsCategoriesPanel;
    }


    public void queryAndCache() {

        productEntityList = productEntityRepository.findBy(
                ProductEntity.class,
                Sort.by(Sort.Order.asc("level"))
        );

        factoryList = productEntityRepository.queryFieldFactory();
        factoryList.sort(Comparator.comparing(FieldFactoryInfoEntity::getLevel, Integer::compareTo));
    }


    public void setupGridItems(Grid<ProductEntity> grid) {
        gridListDataView = grid.setItems(productEntityList);
    }


    public void setupCategoriesItems(RadioButtonGroup<FieldFactoryInfoEntity> categorySelectComponent) {
        categorySelectComponent.setItems(this.factoryList);
    }


    public void refreshImgBtnClick(ProductEntity productEntity) {
        String name = productEntity.getName();
        WikiCrawledEntity wikiCrawledEntity = wikiCrawledEntityRepository.findByText(name);
        String html = wikiCrawledEntity.getHtml();
        CompletableFuture<byte[]> completableFuture = townshipFandomCrawlingProcessFacade.downloadImage(html);
        completableFuture.thenAccept(bytes -> {
            transactionTemplate.execute(ts -> {
                wikiCrawledEntity.setImageBytes(bytes);
                productEntity.setCrawledAsImage(wikiCrawledEntityRepository.save(wikiCrawledEntity));
                return productEntityRepository.save(productEntity);
            });
        }).join();
        goodsCategoriesPanel.refreshImgBtnClickDone(productEntity);
    }

    public void filterGoods(String filterCriteria) {
        if (filterCriteria.isBlank() | filterCriteria.isEmpty()) {
            this.gridListDataView.removeFilters();
            this.reset();
        }
        this.gridListDataView.addFilter(product ->
                product.getName().contains(filterCriteria)
                || product.getCategory().contains(filterCriteria)
                || product.getBomString().contains(filterCriteria)
                || product.getDurationString().contains(filterCriteria)
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

    public Optional<ProductEntity> getCurrentSelect() {
        return currentSelect;
    }

    public void setCurrentSelect(Optional<ProductEntity> currentSelect) {
        this.currentSelect = currentSelect;
    }

}
