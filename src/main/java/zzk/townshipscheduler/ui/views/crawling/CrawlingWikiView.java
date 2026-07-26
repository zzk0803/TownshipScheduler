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
import com.vaadin.flow.server.streams.UploadHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;

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

    private final RadioButtonGroup<CrawlingMode> modeSelector;

    @Setter
    @Getter
    private UI currentUi;

    public CrawlingWikiView(
            CrawlingWikiViewPresenter crawlingWikiViewPresenter
    ) {
        this.presenter = crawlingWikiViewPresenter;
        this.presenter.setProductsView(this);
        setupView();

        // Create mode selector
        modeSelector = createModeSelector();
        add(modeSelector);

        // Create crawl button
        actionButton = createCrawlButton();

        // Create upload panel
        uploadPanel = createUploadPanel();
        uploadPanel.setVisible(false); // Hidden by default

        add(actionButton, uploadPanel);
    }

    private void setupView() {
        addClassName("township-fandom-view");
        setSizeFull();
        setMargin(false);
        setSpacing(true);
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
            if (event.getValue() == CrawlingMode.AUTO_CRAWL) {
                actionButton.setVisible(true);
                uploadPanel.setVisible(false);
            } else {
                actionButton.setVisible(false);
                uploadPanel.setVisible(true);
            }
        });

        return radioGroup;
    }

    private Button createCrawlButton() {
        final Button actionButton;
        actionButton = new Button("Start Crawling And Process", VaadinIcon.PLAY.create());
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.setDisableOnClick(true);
        actionButton.addClickListener(click -> {
            presenter.asyncProcess().whenComplete((unused, throwable) -> {
                getCurrentUi().access(() -> add(prepareCoordCellGrid()));
            }).exceptionally(throwable -> {
                currentUi.access(() -> {
                    Notification notification = new Notification("Error occur when get data from fandom wiki");
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.setPosition(Notification.Position.MIDDLE);
                    notification.open();
                    actionButton.setDisableOnClick(false);
                });
                return null;
            });
        });
        return actionButton;
    }

    private Grid<WikiCrawledParsedCoordCellEntity> prepareCoordCellGrid() {
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
                new HorizontalLayout(new Paragraph("1. Open Page :: Township Wiki Goods :"), createLink("https://township.fandom.com/wiki/Goods", "Township Wiki Goods")) {{
                    setAlignItems(Alignment.BASELINE);
                }},
                new Paragraph("2. Press Ctrl+S Save it"),
                new Paragraph("3. select \"MHTML  (*.mhtml;*.mht)\""),
                new Paragraph("4. file name should be \"Goods _ Township Wiki _ Fandom.mhtml\""),
                new Paragraph("5. Change To .txt"),
                new Paragraph("5. Upload The File")
        );

        // Download example button
        Button downloadExampleBtn = new Button("Download Explanation", VaadinIcon.DOWNLOAD.create());
        downloadExampleBtn.addClickListener(e -> {
            var instructionsText = createInstructionsText();
            downloadExampleBtn.getElement().setAttribute("href", "data:text/plain;charset=utf-8," + java.net.URLEncoder.encode(instructionsText, java.nio.charset.StandardCharsets.UTF_8));
            downloadExampleBtn.getElement().setAttribute("download", "mhtml_upload_instructions.txt");
        });

        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            // Get other information about the file.
            String fileName = metadata.fileName();
            String mimeType = metadata.contentType();
            long contentLength = metadata.contentLength();

            getCurrentUi().access(() -> {
                Notification.show("File Received！Processing...", 3000, Notification.Position.TOP_CENTER);
            });

            // Do something with the file data...
            this.presenter.handleUploadSuccess(data, fileName)
                    .whenComplete((unused, throwable) -> {
                        getCurrentUi().access(() -> {
                            if (throwable == null) {
                                add(prepareCoordCellGrid());
                                Notification.show("Done!", 5000, Notification.Position.BOTTOM_CENTER);
                            } else {
                                Notification.show("Fail!" + throwable.getMessage(), 8000, Notification.Position.BOTTOM_CENTER);
                            }
                        });
                    }).exceptionally(throwable -> {
                        currentUi.access(() -> {
                            Notification notification = new Notification("Error occur when get data from fandom wiki");
                            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                            notification.setPosition(Notification.Position.MIDDLE);
                            notification.open();
                            actionButton.setDisableOnClick(false);
                        });
                        return null;
                    });
        }));
        upload.setAcceptedFileExtensions(".txt");
        upload.addFileRejectedListener(event -> {
            Notification.show("failed：" + event.getFileName(), 5000, Notification.Position.BOTTOM_CENTER);
        });
        upload.addFileRejectedListener(event -> {
            Notification.show("rejected：" + event.getErrorMessage(), 5000, Notification.Position.BOTTOM_CENTER);
        });

        // Status message
        Span statusMessage = new Span("file format：MHTML");
        statusMessage.getStyle().set("font-size", "0.875rem").set("color", "var(--lumo-secondary-text-color)");

        panel.add(instructionsTitle, instructionsLayout, downloadExampleBtn, upload, statusMessage);
        return panel;
    }

    private Anchor createLink(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.setTarget("_blank");
        anchor.getElement().setAttribute("rel", "noopener noreferrer");
        return anchor;
    }

    private String createInstructionsText() {
        return """
               Guild
               
               Step：
               1. Open Township Wiki Goods：
                  https://township.fandom.com/wiki/Goods
               
               2. Press Ctrl+S 
               
               3. If your system is：
                  - Windows: "MHTML single file (*.mhtml;*.mht)"
                  - Mac: May display as "Web Archive"or"MIME HTML"
               
               4. Filename：
                  -  "Goods _ Township Wiki _ Fandom.mhtml"
               
               5. Upload
               """;
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
