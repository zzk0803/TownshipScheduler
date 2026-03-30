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
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.crawling.HtmlUploadService;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j

@Route(value = "crawling")
@Menu(title = "Wiki Crawling", order = 2.00d)
@AnonymousAllowed
public class CrawlingWikiView extends VerticalLayout {

    private enum CrawlingMode {
        AUTO_CRAWL,
        MANUAL_UPLOAD
    }

    private final Button actionButton;

    private final CrawlingWikiViewPresenter presenter;

    private final HtmlUploadService htmlUploadService;

    private final VerticalLayout uploadPanel;

    private final RadioButtonGroup<CrawlingMode> modeSelector;

    public CrawlingWikiView(
            CrawlingWikiViewPresenter crawlingWikiViewPresenter,
            HtmlUploadService htmlUploadService
    ) {
        this.presenter = crawlingWikiViewPresenter;
        this.presenter.setProductsView(this);
        this.htmlUploadService = htmlUploadService;
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
                                getCurrentUI().access(()-> add(prepareCoordCellGrid()));
                            }
                    );
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
                new Paragraph("3. 保存类型选择 \"网页，全部 (*.htm;*.html)\""),
                new Paragraph("4. 将生成的 .html 文件和同名文件夹一起压缩为 ZIP 文件"),
                new Paragraph("5. 上传此 ZIP 文件到此处")
        );

        // Download example button
        Button downloadExampleBtn = new Button("下载示例说明 ZIP", VaadinIcon.DOWNLOAD.create());
        downloadExampleBtn.addClickListener(e -> {
            var zipBaos = createInstructionsZip();
            downloadExampleBtn.getElement().setAttribute(
                    "href",
                    "data:application/zip;base64," + java.util.Base64.getEncoder().encodeToString(zipBaos.toByteArray())
            );
            downloadExampleBtn.getElement().setAttribute("download", "upload_instructions.zip");
        });

        InMemoryUploadHandler inMemoryHandler = UploadHandler
                .inMemory((metadata, data) -> {
                    // Get other information about the file.
                    String fileName = metadata.fileName();
                    String mimeType = metadata.contentType();
                    long contentLength = metadata.contentLength();

                    // Do something with the file data...
                     handleUploadSuccess(data, fileName);
                });

        Upload upload = new Upload(inMemoryHandler);
        upload.setAcceptedFileTypes("application/zip", ".zip");
        upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
        upload.addFileRejectedListener(event -> {
            Notification.show("上传失败：" + event.getFileName(), 5000, Notification.Position.BOTTOM_CENTER);
        });
        upload.addFileRejectedListener(event -> {
            Notification.show("文件被拒绝：" + event.getErrorMessage(), 5000, Notification.Position.BOTTOM_CENTER);
        });

        // Status message
        Span statusMessage = new Span("支持的文件格式：ZIP（包含 HTML 和资源文件夹）");
        statusMessage.getStyle().set("font-size", "0.875rem").set("color", "var(--lumo-secondary-text-color)");

        panel.add(instructionsTitle, instructionsLayout, downloadExampleBtn, upload, statusMessage);
        return panel;
    }

    private void handleUploadSuccess(byte[] data, String fileName) {
        try (var inputStream = new ByteArrayInputStream(data)) {

            // Validate ZIP header
            htmlUploadService.validateZipHeader(data);

            // Process the uploaded file
            var document = htmlUploadService.processUploadedZip(inputStream);

            Notification.show("文件上传成功！正在处理...", 3000, Notification.Position.TOP_CENTER);

            // Process the document
            presenter.asyncProcessFromUploadedHtml(document)
                    .whenComplete((unused, throwable) -> {
                        getCurrentUI().access(() -> {
                            if (throwable == null) {
                                add(prepareCoordCellGrid());
                                Notification.show("上传并处理成功！", 5000, Notification.Position.BOTTOM_CENTER);
                            } else {
                                Notification.show("处理失败：" + throwable.getMessage(), 8000, Notification.Position.BOTTOM_CENTER);
                            }
                        });
                    });

        }
        catch (Exception e) {
            log.error("处理上传文件时出错", e);
            Notification.show("文件处理错误：" + e.getMessage(), 8000, Notification.Position.BOTTOM_CENTER);
        }
    }


    private Anchor createLink(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.setTarget("_blank");
        anchor.getElement().setAttribute("rel", "noopener noreferrer");
        return anchor;
    }

    private java.io.ByteArrayOutputStream createInstructionsZip() {
        // Create a simple ZIP with instructions
        var baos = new java.io.ByteArrayOutputStream();
        try (var zos = new java.util.zip.ZipOutputStream(baos)) {
            var entry = new java.util.zip.ZipEntry("README.txt");
            zos.putNextEntry(entry);
            
            String instructions = """
                    Township Wiki 网页保存指南
                    ==========================
                    
                    1. 打开浏览器访问：https://township.fandom.com/wiki/Goods
                    
                    2. 保存网页：
                       - Windows/Linux: 按 Ctrl+S
                       - macOS: 按 Cmd+S
                    
                    3. 选择保存类型：
                       选择“网页，全部 (*.htm;*.html)”
                       这将保存 HTML 文件和所有相关资源（图片、CSS 等）
                    
                    4. 打包为 ZIP：
                       - 您会得到一个 HTML 文件和一个同名的文件夹
                       - 将它们一起选中，右键 → 发送到 → 压缩文件夹
                       - 或使用 7-Zip、WinRAR 等工具压缩
                    
                    5. 上传：
                       将 ZIP 文件上传到本页面即可
                    
                    注意事项:
                    - 确保保存的是完整的网页（包含 resources 文件夹）
                    - ZIP 文件大小不要超过 50MB
                    - 如果遇到问题，请检查文件格式是否正确
                    """;
            
            zos.write(instructions.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        } catch (Exception e) {
            log.error("创建说明 ZIP 失败", e);
        }
        
        return baos;
    }


    public void onActionDone() {
        actionButton.setEnabled(true);
    }

    private UI getCurrentUI() {
        return UI.getCurrent();
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
        if (presenter.boolTownshipCrawled()) {
            actionButton.setEnabled(false);
            add(prepareCoordCellGrid());
        }
    }

}
