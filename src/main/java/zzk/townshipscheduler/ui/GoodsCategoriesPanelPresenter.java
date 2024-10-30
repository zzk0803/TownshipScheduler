package zzk.townshipscheduler.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.crawling.CrawlingFacade;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.persistence.GoodsRepository;
import zzk.townshipscheduler.backend.persistence.TownshipCrawled;
import zzk.townshipscheduler.backend.persistence.TownshipCrawledRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@SpringComponent
@RequiredArgsConstructor
class GoodsCategoriesPanelPresenter {

    private final GoodsRepository goodsRepository;

    private final TownshipCrawledRepository townshipCrawledRepository;

    private final TransactionTemplate transactionTemplate;

    private final CrawlingFacade crawlingFacade;

    private GoodsCategoriesPanel component;

    private String currentCategory;

    private String currentFilterCriteria;

    @Getter
    @Setter
    private transient Optional<Goods> optionalSelectedGoods;

    private Set<String> categoriesData;

    private List<Goods> goodsData;

    private ListDataProvider<Goods> listDataProvider;


    public void setGroupByCategoryGrid(GoodsCategoriesPanel goodsCategoriesPanel) {
        this.component = goodsCategoriesPanel;
    }


    public void queryAndCacheGoods() {
        categoriesData = goodsRepository.queryCategories();
//        goodsData = goodsRepository.findBy(Goods.class);
        goodsData = goodsRepository.findBy(Goods.class, Sort.by(Sort.Order.asc("level")));
    }

    public void setupCategoriesItems(ListBox<String> categoryListBox) {
        categoryListBox.setItems(Objects.requireNonNull(this.categoriesData));
    }

    public void setupGridItems(Grid<Goods> grid) {
        listDataProvider = new ListDataProvider<>(goodsData);
        grid.setItems(listDataProvider);
    }


    public void refreshImgBtnClick(Goods goods) {
        String name = goods.getName();
        TownshipCrawled townshipCrawled = townshipCrawledRepository.findByText(name);
        String html = townshipCrawled.getHtml();
        CompletableFuture<byte[]> completableFuture = crawlingFacade.downloadImage(html);
        completableFuture.thenAccept(bytes -> {
            transactionTemplate.execute(ts -> {
                townshipCrawled.setImageBytes(bytes);
                townshipCrawledRepository.save(townshipCrawled);
                goods.setImageBytes(bytes);
                return goodsRepository.save(goods);
            });
        }).join();
        component.refreshImgBtnClickDone(goods);
    }

    public void filterGoods(String filterCriteria) {
        if (filterCriteria.isBlank() | filterCriteria.isEmpty()) {
            this.listDataProvider.clearFilters();
        }
        this.listDataProvider.addFilter(goods -> goods.toString().contains(this.currentFilterCriteria));
    }


    public void categoryGoods(String category) {
        this.currentCategory = category;
        this.listDataProvider.addFilter(goods -> goods.getCategory().equals(this.currentCategory));
    }

    public void filterCategories(String filterCriteria, ListBox<String> categoryListBox) {
        if (filterCriteria.isBlank() || filterCriteria.isEmpty()) {
            categoryListBox.setItems(categoriesData);
        }
        categoryListBox.setItems(this.listDataProvider.getItems().stream()
                .collect(Collectors.groupingBy(Goods::getCategory))
                .keySet()
                .toArray(String[]::new)
        );
    }

    public void reset() {
        this.currentCategory = null;
        this.listDataProvider.clearFilters();
    }

}
