package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.service.PlayerService;
import zzk.townshipscheduler.ui.components.GoodsCategoriesPanel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Route("/player")
@Menu
@AnonymousAllowed
public class PlayerView extends VerticalLayout implements BeforeEnterObserver {

    private final TownshipAuthenticationContext townshipAuthenticationContext;

    private final GoodsCategoriesPanel goodsCategoriesPanel;

    private final PlayerService playerService;


    public PlayerView(
            TownshipAuthenticationContext townshipAuthenticationContext,
            GoodsCategoriesPanel goodsCategoriesPanel,
            PlayerService playerService
    ) {
        this.townshipAuthenticationContext = townshipAuthenticationContext;
        this.goodsCategoriesPanel = goodsCategoriesPanel;
        this.playerService = playerService;
        setWidthFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (Objects.isNull(townshipAuthenticationContext)) {
            new Dialog(new Text("townshipAuthenticationContext=null")).open();
        }

        AccountEntity currentUser = townshipAuthenticationContext.getUserDetails();
        if (currentUser != null) {
            Optional<PlayerEntity> playerOptional =
                    playerService.findPlayerEntitiesByAppUser(currentUser);
            PlayerEntity playerEntity = playerOptional.orElseThrow(() -> {
                String name = currentUser.getName();
                String username = currentUser.getUsername();
                return new RuntimeException(String.format(
                        "name=%s,username=%s,no find any player information",
                        name,
                        username
                ));
            });

            VerticalLayout tabContent = new VerticalLayout();
            tabContent.setMargin(false);
            tabContent.setPadding(false);
            tabContent.setHeightFull();

            Tab basicTab = new Tab("Basic");
            Tab fieldFactoryTab = new Tab("Field&Factory");
            Tab warehouseTab = new Tab("Warehouse Stock");
            Map<Tab, Composite<VerticalLayout>> tabArticleMap
                    = Map.of(
                    basicTab, new PlayerBasicArticle(
                            playerEntity,
                            playerService
                    )
                    ,
                    fieldFactoryTab, new PlayerFieldFactoryArticle(
                            playerEntity,
                            playerService
                    )
                    ,
                    warehouseTab, new PlayerWarehouseArticle(
                            playerEntity,
                            playerService,
                            goodsCategoriesPanel
                    )
            );
            Tabs articlesTabs = new Tabs();
            articlesTabs.addTabAtIndex(0, basicTab);
            articlesTabs.addTabAtIndex(1, fieldFactoryTab);
            articlesTabs.addTabAtIndex(2, warehouseTab);
            articlesTabs.setSelectedTab(basicTab);
            articlesTabs.setOrientation(Tabs.Orientation.VERTICAL);
            articlesTabs.addSelectedChangeListener(selectedChangeEvent -> {
                Tab selectedTab = selectedChangeEvent.getSelectedTab();
                tabContent.removeAll();
                tabContent.addComponentAsFirst(tabArticleMap.get(selectedTab));
            });
            articlesTabs.setWidth(15, Unit.PERCENTAGE);

            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setSizeFull();
            horizontalLayout.addAndExpand(tabContent);
            horizontalLayout.add(articlesTabs);
            tabContent.removeAll();
            tabContent.addComponentAsFirst(tabArticleMap.get(basicTab));
            addAndExpand(horizontalLayout);

//            TabSheet tabSheet = new TabSheet();
//            tabSheet.setWidth("100%");
//            tabSheet.add(
//                    "Basic",
//                    new PlayerBasicArticle(playerEntity, playerService)
//            );
//            tabSheet.add(
//                    "Field&Factory",
//                    new PlayerFieldFactoryArticle(
//                            playerEntity,
//                            playerService
//                    )
//            );
//            tabSheet.add(
//                    "Warehouse",
//                    new PlayerWarehouseArticle(
//                            playerEntity,
//                            playerService,
//                            goodsCategoriesPanel
//                    )
//            );
//
//            addAndExpand(tabSheet);
        } else {
            List<PlayerEntity> players = playerService.findAllPlayer();
            Grid<PlayerEntity> grid = new Grid<>();
            grid.addColumn(player -> player.getAccount().getName()).setHeader("Name");
            grid.addColumn(PlayerEntity::getLevel).setHeader("Level");
            grid.addColumn(player -> player.getFieldFactoryEntities().size()).setHeader("Factory Capability");
            grid.addColumn(player -> player.getWarehouseEntity()
                    .getProductAmountMap()
                    .values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum()).setHeader("Warehouse Occupy");
            grid.setItems(players);
            addAndExpand(grid);
        }

    }

}
