# HTML 文件上传功能实施计划

## 背景与目标

**问题**: Fandom 启用 Cloudflare 后，直接爬虫失效，MediaWiki API 方案可能不稳定  
**解决方案**: 提供用户手动上传选项，允许用户上传从浏览器保存的"网页，全部"格式文件

**用户体验流程**:
1. 用户访问 Crawling 页面
2. 选择爬取方式：
   - 自动爬取（原有功能）
   - 手动上传（新增功能）
3. 如选择上传，下载并打包网页文件为 ZIP
4. 上传 ZIP 文件到系统
5. 系统解析 HTML 并继续后续处理流程

---

## Task 1: 创建 HTML 上传服务类

**目标**: 处理上传的 ZIP 文件，提取并解析 HTML

**文件位置**: `src/main/java/zzk/townshipscheduler/backend/crawling/HtmlUploadService.java`

**核心职责**:
1. 接收 ZIP 文件输入流
2. 解压 ZIP 文件到临时目录
3. 查找主 HTML 文件（通常与文件夹同名的 .html 文件）
4. 读取 HTML 内容
5. 清理临时文件
6. 返回 Jsoup Document 对象

**关键方法**:
```java
public class HtmlUploadService {
    
    // 主方法：处理上传的 ZIP 文件
    public Document processUploadedZip(InputStream zipInputStream) throws IOException;
    
    // 解压 ZIP 到临时目录
    private Path unzipToTempDirectory(InputStream zipInputStream, Path tempDir) throws IOException;
    
    // 查找主 HTML 文件
    private Optional<Path> findMainHtmlFile(Path extractedDir);
    
    // 读取并解析 HTML
    private Document parseHtmlFile(Path htmlPath) throws IOException;
}
```

**技术要点**:
- 使用 `java.util.zip.ZipInputStream` 解压
- 临时目录使用 `Files.createTempDirectory()`
- 使用 Jsoup 解析 HTML: `Jsoup.parse(File, String)`
- 实现 `AutoCloseable` 接口确保资源清理
- 添加文件大小限制（防止 DoS 攻击）

**依赖**:
- Java Zip 支持（内置）
- Jsoup（已有）
- SLF4J Logger（已有）

---

## Task 2: 修改 TownshipDataCrawlingProcessor

**目标**: 支持从上传的 HTML 加载数据，而不仅限于网络爬取

**修改文件**: `src/main/java/zzk/townshipscheduler/backend/crawling/TownshipDataCrawlingProcessor.java`

**主要变更**:

### 2.1 添加新方法

```java
/**
 * Process uploaded HTML content instead of crawling
 */
public CompletableFuture<CrawledResult> processFromUploadedHtml(Document document) {
    // 复用现有的表格解析逻辑
    Elements articleTableElements = document.getElementsByClass("article-table");
    
    // ... 后续逻辑与 process() 方法相同
}
```

### 2.2 重构现有代码

**提取公共逻辑**:
```java
// 原 process() 方法中的核心逻辑提取为独立方法
private CompletableFuture<CrawledResult> doProcessTables(Elements articleTableElements) {
    // 表格遍历和解析逻辑
    for (int i = 0; i < articleTableElements.size(); i++) {
        // ... 现有逻辑
    }
    
    logger.info("do mending and fire image downloading");
    CompletableFuture.supplyAsync(this::fireImageDownloadAsync, townshipExecutorService);
    return CompletableFuture.supplyAsync(crawledDataMemory::completeAndMend, townshipExecutorService);
}
```

**修改现有方法**:
```java
public CompletableFuture<CrawledResult> process() {
    Document document = loadDocument(true);
    Elements articleTableElements = document.getElementsByClass("article-table");
    return doProcessTables(articleTableElements);
}

public CompletableFuture<CrawledResult> processFromUploadedHtml(Document document) {
    Elements articleTableElements = document.getElementsByClass("article-table");
    return doProcessTables(articleTableElements);
}
```

---

## Task 3: 修改 TownshipFandomCrawlingProcessFacade

**目标**: 为上传流程提供统一的入口点

**修改文件**: `src/main/java/zzk/townshipscheduler/backend/crawling/TownshipFandomCrawlingProcessFacade.java`

**添加方法**:
```java
@Service
public class TownshipFandomCrawlingProcessFacade {
    
    // ... 现有字段和方法
    
    /**
     * Process uploaded HTML file from user
     */
    public CompletableFuture<Void> processFromUploadedHtml(Document uploadedDocument) {
        return CompletableFuture.supplyAsync(() -> {
                    setCrawledResult(crawlingProcessor.processFromUploadedHtml(uploadedDocument));
                    return getCrawledResult();
                }, townshipExecutorService)
                .thenApplyAsync(this::parsingProcessor::process, townshipExecutorService)
                .thenApply(parsedResult -> {
                    setParsedResult(parsedResult);
                    return transferProcessor.process(parsedResult);
                })
                .thenAccept(this.transferProcessor::process)
                .thenAccept(_ -> hardcodeHotfixProcessor.process());
    }
}
```

---

## Task 4: 创建 Vaadin 上传视图组件

**目标**: 在 CrawlingWikiView 中添加文件上传 UI

**修改文件**: `src/main/java/zzk/townshipscheduler/ui/views/crawling/CrawlingWikiView.java`

### 4.1 添加必要的导入

```java
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.MultiFileMemoryBuffer;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
```

### 4.2 修改布局结构

**当前结构**:
```
VerticalLayout
└─ Button: "Start Crawling And Process"
```

**新结构**:
```
VerticalLayout
├─ HorizontalLayout (选项切换)
│  ├─ RadioButtonGroup: 爬取模式选择
│  │  ├─ "自动爬取"
│  │  └─ "手动上传"
│  └─ Button: 帮助提示
├─ VerticalLayout (动态内容区)
│  ├─ [自动爬取模式]
│  │  └─ Button: "Start Crawling And Process"
│  └─ [手动上传模式]
│     ├─ DownloadLink: "下载示例 ZIP 文件"
│     ├─ Upload: 文件上传组件
│     └─ Span: 使用说明
└─ Grid: 结果展示（保持不变）
```

### 4.3 实现上传逻辑

```java
private void setupUploadComponent() {
    // 使用 MultiFileMemoryBuffer 作为接收器
    MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
    Upload upload = new Upload(buffer);
    
    // 设置接受的文件类型
    upload.setAcceptedFileTypes("application/zip", ".zip");
    upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
    
    // 上传成功事件
    upload.addSucceededListener(event -> {
        try {
            InputStream inputStream = buffer.getInputStream(event.getFileName());
            Document uploadedDocument = htmlUploadService.processUploadedZip(inputStream);
            
            // 调用后台处理
            presenter.asyncProcessFromUploadedHtml(uploadedDocument)
                    .whenComplete((unused, throwable) -> {
                        currentUi.access(() -> {
                            if (throwable == null) {
                                add(prepareCoordCellGrid());
                                Notification.show("上传并处理成功！");
                            } else {
                                Notification.show("处理失败：" + throwable.getMessage(), 5000, Notification.Position.BOTTOM_CENTER);
                            }
                        });
                    });
        } catch (Exception e) {
            Notification.show("文件处理错误：" + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER);
        }
    });
    
    // 上传失败事件
    upload.addFailedListener(event -> {
        Notification.show("上传失败：" + event.getError().getMessage(), 5000, Notification.Position.BOTTOM_CENTER);
    });
    
    // 添加到布局
    uploadWrapper.add(upload);
}
```

### 4.4 模式切换逻辑

```java
private void updateModeVisibility(CrawlingMode mode) {
    if (mode == CrawlingMode.AUTO_CRAWL) {
        crawlButton.setVisible(true);
        uploadWrapper.setVisible(false);
    } else {
        crawlButton.setVisible(false);
        uploadWrapper.setVisible(true);
    }
}
```

---

## Task 5: 修改 CrawlingWikiViewPresenter

**目标**: 为上传流程提供 Presenter 支持

**修改文件**: `src/main/java/zzk/townshipscheduler/ui/views/crawling/CrawlingWikiViewPresenter.java`

**添加方法**:
```java
CompletableFuture<Void> asyncProcessFromUploadedHtml(Document uploadedDocument) {
    return townshipFandomCrawlingProcessFacade.processFromUploadedHtml(uploadedDocument)
            .whenCompleteAsync((unused, throwable) -> {
                if (throwable != null) {
                    logger.error("处理上传文件时出错：{}", throwable.getMessage());
                }
                logger.info("上传处理完成");
                townshipFandomCrawlingProcessFacade.clean();
            }, townshipFandomCrawlingProcessFacade.getTownshipExecutorService());
}
```

---

## Task 6: 创建示例 ZIP 文件下载功能

**目标**: 为用户提供正确的文件格式示例

**新增文件**: `src/main/java/zzk/townshipscheduler/ui/views/crawling/SampleZipDownloadService.java`

**职责**:
1. 准备示例 ZIP 文件的说明文档
2. 提供 ZIP 文件结构说明
3. 创建 README.txt 包含操作步骤

**实现**:
```java
@Service
public class SampleZipDownloadService {
    
    public byte[] generateSampleZipInstructions() {
        // 创建一个包含使用说明的 ZIP 文件
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 添加 README.txt
            ZipEntry readmeEntry = new ZipEntry("README.txt");
            zos.putNextEntry(readmeEntry);
            String instructions = """
                使用说明：
                1. 在浏览器中打开 https://township.fandom.com/wiki/Goods
                2. 按 Ctrl+S 另存为
                3. 保存类型选择"网页，全部 (*.htm;*.html)"
                4. 将生成的 .html 文件和同名文件夹一起压缩为 ZIP
                5. 上传此 ZIP 文件
                """;
            zos.write(instructions.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
```

---

## Task 7: 安全与验证

**目标**: 确保上传功能的安全性

### 7.1 文件大小限制

```java
@Configuration
public class UploadConfig {
    
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("50MB");
        factory.setMaxRequestSize("50MB");
        return factory.createMultipartConfig();
    }
}
```

### 7.2 文件类型验证

```java
private void validateZipFile(InputStream inputStream) throws IOException {
    // 检查 ZIP 文件头
    byte[] header = new byte[4];
    inputStream.read(header);
    
    // ZIP 文件头应该是 PK\003\004
    if (!(header[0] == (byte) 0x50 && 
          header[1] == (byte) 0x4b && 
          header[2] == (byte) 0x03 && 
          header[3] == (byte) 0x04)) {
        throw new IllegalArgumentException("不是有效的 ZIP 文件");
    }
}
```

### 7.3 HTML 内容验证

```java
private void validateHtmlContent(Document document) {
    // 检查是否包含预期的表格
    Elements tables = document.getElementsByClass("article-table");
    if (tables.isEmpty()) {
        throw new IllegalArgumentException("HTML 中未找到商品表格，请确认是正确的页面");
    }
}
```

---

## Task 8: 测试与文档

### 8.1 单元测试

**文件**: `src/test/java/zzk/townshipscheduler/backend/crawling/HtmlUploadServiceTest.java`

**测试用例**:
- ✅ 正确 ZIP 文件能成功解析
- ✅ 非 ZIP 文件抛出异常
- ✅ 过大的文件被拒绝
- ✅ 损坏的 HTML 文件处理
- ✅ 临时文件正确清理

### 8.2 集成测试

**文件**: `src/test/java/zzk/townshipscheduler/ui/views/crawling/CrawlingWikiViewIT.java`

**测试场景**:
- ✅ 切换到上传模式
- ✅ 上传有效 ZIP 文件
- ✅ 处理成功显示结果
- ✅ 上传失败显示错误提示

### 8.3 用户文档

**文件**: `readme/HTML_UPLOAD_GUIDE.md`

**内容包括**:
- 如何保存网页
- 如何打包 ZIP
- 上传步骤
- 常见问题解答

---

## 技术细节

### ZIP 文件结构预期

```
goods_page.zip
├── Goods.html              # 主 HTML 文件
└── Goods_files/            # 资源文件夹
    ├── css/
    ├── js/
    └── images/
```

### HTML 解析流程

```
用户上传 ZIP
  ↓
HtmlUploadService.processUploadedZip()
  ↓
解压到临时目录 → /tmp/upload123456/
  ↓
查找主 HTML 文件 → /tmp/upload123456/Goods.html
  ↓
Jsoup.parse(file) → Document
  ↓
返回给调用方
```

### Vaadin Upload 配置

```java
Upload upload = new Upload(new MultiFileMemoryBuffer());
upload.setAcceptedFileTypes(".zip");
upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
upload.addSucceededListener(this::handleUploadSuccess);
upload.addFailedListener(this::handleUploadFailed);
```

---

## 依赖检查

### 已有依赖 ✅
- Vaadin Flow (Upload 组件已包含)
- Jsoup (HTML 解析)
- SLF4J (日志)

### 需要添加的依赖 ❌
无（Vaadin 已包含所有需要的功能）

---

## 预计工作量

- **Task 1-2**: 后端服务开发 - 3-4 小时
- **Task 3-4**: UI 组件开发 - 3-4 小时
- **Task 5-7**: 安全与验证 - 2-3 小时
- **Task 8**: 测试与文档 - 2-3 小时

**总计**: 10-14 小时

---

## 风险评估

### 低风险 ✅
- Vaadin Upload 组件成熟稳定
- ZIP 解压是标准功能
- HTML 解析复用现有逻辑

### 中风险 ⚠️
- 用户可能上传错误的文件格式
- 大文件可能导致内存问题
- 不同浏览器保存的 HTML 结构可能有差异

### 应对措施
- 添加严格的文件验证
- 使用流式处理避免内存溢出
- 提供详细的格式说明和示例

---

## 验收标准

1. ✅ 用户可以选择自动爬取或手动上传
2. ✅ 上传 ZIP 文件后能正确解析 HTML
3. ✅ 解析后的数据能正常进入后续处理流程
4. ✅ 错误情况有清晰的提示信息
5. ✅ 提供示例文件和使用说明
6. ✅ 文件大小和类型受到限制
7. ✅ 临时文件正确清理

---

## 后续优化建议

1. **支持多种格式**: 除了"网页，全部"，支持单一 HTML 文件
2. **批量上传**: 允许多个页面同时上传
3. **断点续传**: 大文件分片上传
4. **进度显示**: 实时显示解析进度
5. **预览功能**: 上传后先预览再处理

---

现在开始执行 Task 1，我将逐步实现每个任务并在关键节点与你确认。