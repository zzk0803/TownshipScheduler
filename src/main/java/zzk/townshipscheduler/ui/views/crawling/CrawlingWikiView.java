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
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;

@Slf4j
@Route(value = "crawling")
@Menu(title = "Wiki Crawling", order = 2.00d)
@AnonymousAllowed
public class CrawlingWikiView extends VerticalLayout {

    private enum CrawlingMode {
        AUTO_CRAWL,
        MANUAL_UPLOAD
    }

    @Getter
    @Setter
    private UI currentUi;

    private final Button actionButton;

    private final CrawlingWikiViewPresenter presenter;

    private final VerticalLayout uploadPanel;

    private final RadioButtonGroup<CrawlingMode> modeSelector;

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
        actionButton = new Button("Start Crawling And Process", VaadinIcon.PLAY.create());
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.setDisableOnClick(true);
        actionButton.addClickListener(click -> {
            presenter.asyncProcess()
                    .whenComplete((unused, throwable) -> {
                        getCurrentUi().access(() -> add(prepareCoordCellGrid()));
                            }
                    )
                    .exceptionally(throwable -> {
                        currentUi.access(() -> {
                            Notification notification = new Notification("Error occur when get data from fandom wiki");
                            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                            notification.setPosition(Notification.Position.MIDDLE);
                            notification.open();
                            actionButton.setDisableOnClick(false);
                        });
                        return null;
                    })
            ;
        });

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
        radioGroup.setLabel("选择数据获取方式:");
        radioGroup.setItems(CrawlingMode.values());
        radioGroup.setItemLabelGenerator(mode -> switch (mode) {
            case AUTO_CRAWL -> "自动爬取（从 Fandom Wiki）";
            case MANUAL_UPLOAD -> "手动上传（保存的网页文件）";
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

    private VerticalLayout createUploadPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setSpacing(true);

        // Instructions
        H3 instructionsTitle = new H3("如何保存和上传网页:");

        VerticalLayout instructionsLayout = new VerticalLayout();
        instructionsLayout.setPadding(false);
        instructionsLayout.add(
                new HorizontalLayout(
                        Alignment.BASELINE,
                        new Paragraph("1. 在浏览器中打开 Township Wiki Goods 页面:"),
                        createLink("https://township.fandom.com/wiki/Goods", "Township Wiki Goods 页面")
                ),
                new Paragraph("2. 按 Ctrl+S (或 Cmd+S) 另存为"),
                new Paragraph("3. 保存类型选择 \"MHTML 单个文件 (*.mhtml;*.mht)\""),
                new Paragraph("4. 文件名会自动设置为 \"Goods _ Township Wiki _ Fandom.mhtml\""),
                new Paragraph("5. 上传此 MHTML 文件到此处")
        );

        // Download example button
        Button downloadExampleBtn = new Button("下载使用说明", VaadinIcon.DOWNLOAD.create());
        downloadExampleBtn.addClickListener(e -> {
            var instructionsText = createInstructionsText();
            downloadExampleBtn.getElement().setAttribute(
                    "href",
                    "data:text/plain;charset=utf-8," + java.net.URLEncoder.encode(instructionsText, java.nio.charset.StandardCharsets.UTF_8)
            );
            downloadExampleBtn.getElement().setAttribute("download", "mhtml_upload_instructions.txt");
        });

        InMemoryUploadHandler inMemoryHandler = UploadHandler
                .inMemory((metadata, data) -> {
                    // Get other information about the file.
                    String fileName = metadata.fileName();
                    String mimeType = metadata.contentType();
                    long contentLength = metadata.contentLength();

                    getCurrentUi().access(() -> {
                        Notification.show("文件上传成功！正在处理...", 3000, Notification.Position.TOP_CENTER);
                    });

                    // Do something with the file data...
                    this.presenter.handleUploadSuccess(data, fileName)
                            .whenComplete(
                                    (unused, throwable) -> {
                                        getCurrentUi().access(
                                                () -> {
                                                    if (throwable == null) {
                                                        add(prepareCoordCellGrid());
                                                        Notification.show("上传并处理成功！", 5000, Notification.Position.BOTTOM_CENTER);
                                                    } else {
                                                        Notification.show("处理失败：" + throwable.getMessage(), 8000, Notification.Position.BOTTOM_CENTER);
                                                    }
                                                });
                                    }
                            )
                            .exceptionally(throwable -> {
                                currentUi.access(() -> {
                                    Notification notification = new Notification("Error occur when get data from fandom wiki");
                                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                    notification.setPosition(Notification.Position.MIDDLE);
                                    notification.open();
                                    actionButton.setDisableOnClick(false);
                                });
                                return null;
                            })
                    ;
                });

        Upload upload = new Upload(inMemoryHandler);
        upload.setAcceptedFileTypes("message/rfc822", ".mhtml", ".mht");
        upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
        upload.addFileRejectedListener(event -> {
            Notification.show("上传失败：" + event.getFileName(), 5000, Notification.Position.BOTTOM_CENTER);
        });
        upload.addFileRejectedListener(event -> {
            Notification.show("文件被拒绝：" + event.getErrorMessage(), 5000, Notification.Position.BOTTOM_CENTER);
        });

        // Status message
        Span statusMessage = new Span("支持的文件格式：MHTML（单个文件，包含所有资源）");
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
                MHTML 文件上传说明
                
                步骤：
                1. 打开 Township Wiki Goods 页面：
                   https://township.fandom.com/wiki/Goods
                
                2. 按 Ctrl+S (或 Cmd+S) 另存为
                
                3. 保存类型选择：
                   - Windows: "MHTML 单个文件 (*.mhtml;*.mht)"
                   - Mac: 可能显示为"Web Archive"或"MIME HTML"
                
                4. 文件名：
                   - 会自动设置为 "Goods _ Township Wiki _ Fandom.mhtml"
                   - 这是单个文件，包含所有资源（图片、CSS 等）
                
                5. 上传：
                   将 MHTML 文件直接上传到本页面即可
                
                注意事项:
                - MHTML 是单个文件，不需要压缩
                - 文件大小不要超过 50MB
                - 如果浏览器没有 MHTML 选项，请尝试使用 Edge 或 IE
                - 此格式会保存完整的网页内容和所有图片
                
                优势:
                - 比 ZIP 更可靠（无解压兼容性问题）
                - 包含所有资源（图片、样式）
                - 单个文件，易于管理
                """;
    }


    public void onActionDone() {
        actionButton.setEnabled(true);
    }


    private Grid<WikiCrawledParsedCoordCellEntity> prepareCoordCellGrid() {
        Grid<WikiCrawledParsedCoordCellEntity> grid = new Grid<>(WikiCrawledParsedCoordCellEntity.class);
        grid.setItemDetailsRenderer(new TextRenderer<>(WikiCrawledParsedCoordCellEntity::getHtml));
        grid.setWidthFull();
        presenter.setupTownshipCoordCellGrid(grid);
        return grid;
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

}
