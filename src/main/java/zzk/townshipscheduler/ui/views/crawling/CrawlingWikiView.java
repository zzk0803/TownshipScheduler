package zzk.townshipscheduler.ui.views.crawling;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;

import java.time.Duration;

@Getter
@Slf4j
@Route(value = "crawling")
@Menu(
        title = "Wiki Crawling",
        order = 2.00d
)
@AnonymousAllowed
public class CrawlingWikiView
        extends VerticalLayout {

    private final Button actionButton;

    private final CrawlingWikiViewPresenter presenter;

    private final VerticalLayout uploadPanel;

    private final ValueSignal<CrawlingMode> crawlingModeValueSignal
            = new ValueSignal<>(CrawlingMode.AUTO_CRAWL);

    @Setter
    private UI currentUi;

    public CrawlingWikiView(
            CrawlingWikiViewPresenter crawlingWikiViewPresenter
    ) {
        this.presenter = crawlingWikiViewPresenter;
        this.presenter.setProductsView(this);
        addClassName("township-fandom-view");
        setSizeFull();
        setMargin(false);
        setSpacing(true);

        // Create mode selector
        add(createModeSelector());

        // Create crawl button
        actionButton = createCrawlButton();

        // Create upload panel
        uploadPanel = createUploadPanel();

        add(actionButton, uploadPanel);
    }

    private RadioButtonGroup<CrawlingMode> createModeSelector() {
        RadioButtonGroup<CrawlingMode> radioGroup = new RadioButtonGroup<>();
        radioGroup.setLabel("Select Approach To Wiki Data:");
        radioGroup.setItems(CrawlingMode.values());
        radioGroup.setItemLabelGenerator(mode -> switch (mode) {
            case AUTO_CRAWL -> "Crawling Online Wiki";
            case MANUAL_UPLOAD -> "Upload Offline Wiki";
        });
        radioGroup.setValue(CrawlingMode.AUTO_CRAWL);

        radioGroup.addValueChangeListener(event -> {
            CrawlingMode selectedModel = event.getValue();
            this.crawlingModeValueSignal.set(selectedModel);
        });

        return radioGroup;
    }

    private Button createCrawlButton() {
        final Button actionButton;
        actionButton = new Button("Start Crawling And Process", VaadinIcon.PLAY.create());
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.setDisableOnClick(true);
        actionButton.addClickListener(click -> {
            presenter.asyncProcess()
                    .whenComplete((_, throwable) -> {
                        if (throwable != null) {
                            currentUi.access(() -> {
                                Notification notification = new Notification(throwable.toString());
                                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                notification.setPosition(Notification.Position.MIDDLE);
                                notification.setDuration(Duration.ofSeconds(3)
                                        .toSecondsPart());
                                notification.open();
                                actionButton.setDisableOnClick(false);
                            });
                            return;
                        }
                        getCurrentUi().access(() -> add(prepareCoordCellGrid()));
                    });
        });
        actionButton.bindVisible(crawlingModeValueSignal.map(value -> value == CrawlingMode.AUTO_CRAWL));
        return actionButton;
    }

    public Grid<WikiCrawledParsedCoordCellEntity> prepareCoordCellGrid() {
        Grid<WikiCrawledParsedCoordCellEntity> grid = new Grid<>(WikiCrawledParsedCoordCellEntity.class);
        grid.setItemDetailsRenderer(new TextRenderer<>(WikiCrawledParsedCoordCellEntity::getHtml));
        grid.setWidthFull();
        presenter.setupTownshipCoordCellGrid(grid);
        return grid;
    }

    private VerticalLayout createUploadPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setSpacing(true);

        // Instructions
        H3 instructionsTitle = new H3("Guide:");

        VerticalLayout instructionsLayout = new VerticalLayout();
        instructionsLayout.setPadding(false);
        instructionsLayout.add(
                new HorizontalLayout(
                        new Button("Use builtin offline file") {{
                            addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                            addClickListener(event -> CrawlingWikiView.this.presenter.asyncProcessFromOfflineHtml()
                                    .whenComplete((unused, throwable) -> {
                                        getCurrentUi().access(() -> add(prepareCoordCellGrid()));
                                    })
                                    .exceptionally(throwable -> {
                                        currentUi.access(() -> {
                                            Notification notification = new Notification("Error occur when get data from fandom wiki");
                                            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                            notification.setPosition(Notification.Position.MIDDLE);
                                            notification.setDuration(Duration.ofSeconds(3)
                                                    .toSecondsPart());
                                            notification.open();
                                            actionButton.setDisableOnClick(false);
                                        });
                                        return null;
                                    }));
                        }}
                ),
                new HorizontalLayout(
                        new Paragraph("1. Open Page :: Township Wiki Goods :"),
                        createLink("https://township.fandom.com/wiki/Goods", "Township Wiki Goods")
                ) {{
                    setAlignItems(Alignment.BASELINE);
                }},
                new Paragraph("2. Press Ctrl+S Save it"),
                new Paragraph("3. select \"MHTML  (*.mhtml;*.mht)\""),
                new Paragraph("4. file name should be \"Goods _ Township Wiki _ Fandom.mhtml\""),
                new Paragraph("5. Change To .txt"),
                new Paragraph("5. Upload The File")
        );

        Upload upload = new Upload(this.presenter.createUploadHandler());
        upload.setAcceptedFileExtensions(".txt");
        upload.addFileRejectedListener(event -> {
            Notification.show("failed：" + event.getFileName(), 5000, Notification.Position.BOTTOM_CENTER);
        });
        upload.addFileRejectedListener(event -> {
            Notification.show("rejected：" + event.getErrorMessage(), 5000, Notification.Position.BOTTOM_CENTER);
        });

        // Status message
        Span statusMessage = new Span("file format：MHTML");
        statusMessage.getStyle()
                .set("font-size", "0.875rem")
                .set("color", "var(--lumo-secondary-text-color)");

        panel.add(instructionsTitle, instructionsLayout, upload, statusMessage);
        panel.bindVisible(crawlingModeValueSignal.map(value -> value == CrawlingMode.MANUAL_UPLOAD));
        return panel;
    }

    private Anchor createLink(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.setTarget("_blank");
        anchor.getElement()
                .setAttribute("rel", "noopener noreferrer");
        return anchor;
    }

    public void onActionDone() {
        actionButton.setEnabled(true);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setCurrentUi(UI.getCurrent());
        if (presenter.boolTownshipCrawled()) {
            actionButton.setEnabled(false);
            add(prepareCoordCellGrid());
        }
    }

    private enum CrawlingMode {
        AUTO_CRAWL, MANUAL_UPLOAD
    }

}
