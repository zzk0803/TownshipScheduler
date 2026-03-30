# MHTML 上传功能 - 完整学习路径

## 📚 学习目标

通过本指南，你将理解：
1. ✅ 为什么选择 MHTML 格式
2. ✅ JavaMail API 如何解析 MIME 格式
3. ✅ 完整的代码实现逻辑
4. ✅ 数据流转过程
5. ✅ 如何调试和扩展

---

## 🎯 第一部分：背景与决策

### 问题起源

```
原始需求：获取 Township Wiki 的游戏数据（货物、建筑等）

遇到的障碍：
├─ Fandom Wiki 启用 Cloudflare 防护
├─ 直接爬虫被拦截 (HTTP 403)
├─ MediaWiki API 也被阻挡
└─ 需要寻找替代方案
```

### 解决方案演进

```
方案 1: ZIP 压缩包（❌ 失败）
  └─ 问题：Windows GBK 编码 vs Java UTF-8
  └─ 错误：invalid LOC header

方案 2: 手动解析 MHTML（❌ 复杂）
  └─ 问题：不同浏览器 boundary 格式不同
  └─ 错误：未找到 MHTML 边界标识符

方案 3: JavaMail API（✅ 成功）
  └─ 优势：行业标准、自动处理所有格式
  └─ 结果：稳定可靠
```

---

## 🔧 第二部分：核心技术

### 1. MHTML 文件格式

**什么是 MHTML？**
```
MHTML = MIME HTML
本质：一个电子邮件格式的 HTML 网页
标准：RFC 2557
```

**文件结构：**
```text
From: <Saved by Blink>
Snapshot-Content-Location: https://township.fandom.com/wiki/Goods
MIME-Version: 1.0
Content-Type: multipart/related;
    boundary="----=_NextPart_000_0000_01234567.89ABCDEF"

------=_NextPart_000_0000_01234567.89ABCDEF
Content-Type: text/html; charset="utf-8"
Content-Transfer-Encoding: quoted-printable

<!DOCTYPE html><html>...</html>

------=_NextPart_000_0000_01234567.89ABCDEF
Content-Type: image/png
Content-Transfer-Encoding: base64
Content-Location: goods_icon.png

iVBORw0KGgoAAAANSUhEUg...

------=_NextPart_000_0000_01234567.89ABCDEF--
```

**关键点：**
- `boundary` = 分隔符（每个部分用此分隔）
- 每个部分有独立的头部（Content-Type, Content-Transfer-Encoding）
- HTML 主文档 + 资源（图片、CSS）打包在一起

---

### 2. JavaMail API 核心概念

#### Session（会话）
```java
Properties props = new Properties();
Session session = Session.getDefaultInstance(props, null);
```
- **作用**：JavaMail 的上下文环境
- **注意**：这里不需要配置邮件服务器！仅用于 MIME 解析

#### MimeMessage（MIME 消息）
```java
MimeMessage message = new MimeMessage(session, inputStream);
```
- **作用**：表示整个 MHTML 文件
- **功能**：自动解析 MIME 结构

#### Multipart（多部分）
```java
Object content = message.getContent();
if (content instanceof Multipart) {
    Multipart multipart = (Multipart) content;
}
```
- **作用**：包含多个 BodyPart
- **类比**：一个包裹里有多个小包裹

#### BodyPart（身体部分）
```java
for (int i = 0; i < multipart.getCount(); i++) {
    BodyPart part = multipart.getBodyPart(i);
    String contentType = part.getContentType();
}
```
- **作用**：MHTML 的一个组成部分
- **类型**：可能是 HTML、图片、CSS 等

---

## 💻 第三部分：代码详解

### MhtmlUploadService 完整解析

#### 入口方法
```java
public Document processUploadedMhtml(InputStream mhtmlInputStream) 
        throws IOException {
    
    // Step 1: 读取文件到内存
    byte[] mhtmlBytes = mhtmlInputStream.readAllBytes();
    
    // Step 2: 创建 Session（无需配置）
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    
    // Step 3: 解析 MIME 格式
    MimeMessage message = new MimeMessage(session, 
        new ByteArrayInputStream(mhtmlBytes));
    
    // Step 4: 获取内容
    Object content = message.getContent();
    
    if (content instanceof Multipart) {
        return extractHtmlFromMultipart((Multipart) content);
    } else {
        return Jsoup.parse((String) content);
    }
}
```

**流程图：**
```
用户上传 .mhtml 文件
      ↓
读取为 byte[] 数组
      ↓
创建 JavaMail Session
      ↓
构造 MimeMessage 对象
      ↓
调用 getContent()
      ↓
如果是 Multipart → 遍历查找 HTML 部分
如果是 String → 直接解析
      ↓
返回 Jsoup Document
```

---

#### 提取 HTML 方法
```java
private String extractHtmlFromMultipart(Multipart multipart) 
        throws MessagingException, IOException {
    
    int count = multipart.getCount(); // 有多少个部分
    
    for (int i = 0; i < count; i++) {
        BodyPart part = multipart.getBodyPart(i);
        String contentType = part.getContentType().toLowerCase();
        
        // 查找 HTML 类型的内容
        if (contentType.startsWith("text/html")) {
            logger.info("Found HTML part at index {}", i);
            
            // 获取并返回 HTML 内容
            Object partContent = part.getContent();
            if (partContent instanceof String) {
                return (String) partContent;
            }
        }
    }
    
    throw new IOException("MHTML 中未找到 HTML 内容部分");
}
```

**关键理解：**
- `multipart.getCount()` = 总共有多少个部分
- `part.getContentType()` = 这个部分的类型（text/html, image/png 等）
- `part.getContent()` = 解码后的实际内容

---

### CrawlingWikiView UI 层

#### 上传处理
```java
private void handleUploadSuccess(byte[] data, String fileName) {
    try (var inputStream = new ByteArrayInputStream(data)) {
        
        // 1. 验证文件头
        mhtmlUploadService.validateMhtmlHeader(inputStream);
        
        // 2. 重置流位置
        inputStream.reset();
        
        // 3. 解析 MHTML
        var document = mhtmlUploadService.processUploadedMhtml(inputStream);
        
        // 4. 交给后端处理
        presenter.asyncProcessFromUploadedHtml(document)
                .whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        add(prepareCoordCellGrid()); // 显示表格
                    }
                });
    }
}
```

**用户操作流程：**
```
1. 用户在浏览器保存网页为 .mhtml
      ↓
2. 访问 /crawling 页面
      ↓
3. 选择"手动上传"模式
      ↓
4. 拖拽文件到上传区域
      ↓
5. 等待解析和处理
      ↓
6. 查看结果表格
```

---

## 🔄 第四部分：数据流转全过程

### 完整生命周期

```
┌─────────────────────┐
│  1. 用户保存网页    │
│  Goods.mhtml        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  2. 浏览器打包      │
│  HTML + CSS + Images│
│  为 MIME 格式        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  3. Vaadin Upload   │
│  接收文件为 byte[]  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  4. JavaMail 解析   │
│  Session            │
│  ├─ MimeMessage     │
│  │  └─ Multipart    │
│  │     └─ BodyPart  │
│  └─ 提取 HTML       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  5. Jsoup 解析      │
│  Document 对象      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  6. 业务逻辑处理    │
│  TownshipDataCrawl… │
│  解析表格数据       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  7. 持久化存储      │
│  JPA Repository     │
│  保存到 H2 数据库    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  8. Grid 展示       │
│  Vaadin Grid        │
│  显示游戏数据       │
└─────────────────────┘
```

---

## 🛠️ 第五部分：关键技术点

### 1. 为什么使用 `javax.mail` 而不是 `jakarta.mail`？

```xml
<!-- 我们使用的版本 -->
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>
```

**原因：**
- JavaMail 1.6.x 使用 `javax.mail` 包名
- Jakarta Mail 2.x 使用 `jakarta.mail` 包名（Jakarta EE 9+）
- Spring Boot 3.x 虽然默认 Jakarta EE，但可以兼容 javax
- JavaMail 1.6.2 更稳定、经过长期验证

---

### 2. 编码自动处理

**MHTML 中常见的编码：**

| 编码方式 | 用途 | JavaMail 处理 |
|---------|------|-------------|
| `quoted-printable` | HTML 文本 | ✅ 自动解码 |
| `base64` | 图片、二进制 | ✅ 自动解码 |
| `8bit` | 纯文本 | ✅ 直接读取 |
| `binary` | 原始二进制 | ✅ 流式传输 |

**示例：**
```java
// 旧方案需要手动解码
if (encoding.equals("quoted-printable")) {
    content = decodeQuotedPrintable(content);
}

// 新方案自动完成
Object content = part.getContent(); // 已解码
```

---

### 3. 边界分隔符自动识别

**不同浏览器的 boundary：**

| 浏览器 | Boundary 格式 | JavaMail 识别 |
|--------|-------------|-------------|
| Chrome/Edge | `----MultipartBoundary--xxxx` | ✅ 自动 |
| Windows IE | `----=_NextPart_xxxx` | ✅ 自动 |
| Firefox | `----=_Part_x_xxxx.xxxx` | ✅ 自动 |
| Safari | `----=_Part_0_xxxx` | ✅ 自动 |

**原理：**
```java
// JavaMail 内部会：
MimeMessage message = new MimeMessage(session, inputStream);
// 1. 读取 Content-Type 头部
// 2. 提取 boundary 参数
// 3. 使用 boundary 分割内容
// 4. 为每个部分创建 BodyPart 对象
```

---

## 🐛 第六部分：调试技巧

### 1. 启用详细日志

**application.properties:**
```properties
logging.level.zzk.townshipscheduler.backend.crawling=DEBUG
logging.level.javax.mail=DEBUG
```

**输出示例：**
```
DEBUG: Processing uploaded MHTML file using JavaMail API
DEBUG: Multipart contains 15 parts
DEBUG: Part 0: Content-Type=text/html; charset="utf-8"
INFO: Found HTML part at index 0
DEBUG: Successfully parsed HTML document
```

---

### 2. 检查 MHTML 内容

**添加调试代码：**
```java
byte[] mhtmlBytes = mhtmlInputStream.readAllBytes();

// 打印前 500 字符查看格式
String preview = new String(mhtmlBytes, StandardCharsets.UTF_8);
logger.debug("MHTML preview:\n{}", preview.substring(0, Math.min(500, preview.length())));
```

---

### 3. 常见错误排查

#### 错误 1: "未找到 HTML 内容"
```
可能原因：
- 保存的不是 MHTML 格式
- 文件损坏

解决：
- 重新保存为 "MHTML 单个文件"
- 用记事本打开检查是否包含 MIME-Version
```

#### 错误 2: "文件过大"
```
可能原因：
- MHTML > 50MB

解决：
- 调整 MAX_FILE_SIZE 限制
- 或使用流式处理
```

---

## 🚀 第七部分：扩展方向

### 1. 提取图片资源

```java
private Map<String, byte[]> extractImages(Multipart multipart) 
        throws Exception {
    
    Map<String, byte[]> images = new HashMap<>();
    
    for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart part = multipart.getBodyPart(i);
        
        if (part.getContentType().startsWith("image/")) {
            // 获取图片数据
            InputStream is = part.getInputStream();
            byte[] imageData = is.readAllBytes();
            
            // 获取建议文件名
            String[] locations = part.getHeader("Content-Location");
            String filename = (locations != null) ? locations[0] : "image_" + i;
            
            images.put(filename, imageData);
        }
    }
    
    return images;
}
```

---

### 2. CID 引用替换

**问题：** HTML 中可能有 `<img src="cid:image001.png">`

**解决：**
```java
String html = extractHtml(multipart);
Map<String, String> cidMap = extractCidMappings(multipart);

// 替换 cid: 引用为实际路径
for (Map.Entry<String, String> entry : cidMap.entrySet()) {
    html = html.replace(
        "cid:" + entry.getKey(),
        "data:image/png;base64," + entry.getValue()
    );
}
```

---

### 3. 批量处理

```java
public List<Document> processMultipleMhtml(List<File> files) {
    return files.stream()
        .map(file -> {
            try (InputStream is = new FileInputStream(file)) {
                return processUploadedMhtml(is);
            } catch (IOException e) {
                logger.error("处理失败：{}", file.getName(), e);
                return null;
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
}
```

---

## 📖 第八部分：学习资源

### 官方文档
- [JavaMail API Spec](https://javaee.github.io/javamail/docs/api/)
- [RFC 2557 - MIME Encapsulation of Aggregate Documents](https://tools.ietf.org/html/rfc2557)

### 深入理解
- 《JavaMail API Tutorial》
- 《MIME Type Specification》

### 实践项目
- 尝试解析其他 MIME 格式（如 .eml 邮件）
- 实现图片提取功能
- 添加进度条显示

---

## ✅ 自测清单

学完后，你应该能够回答：

- [ ] MHTML 的本质是什么？
- [ ] 为什么选择 JavaMail 而不是手动解析？
- [ ] Session 的作用是什么？需要配置吗？
- [ ] 如何从 Multipart 中提取 HTML 部分？
- [ ] JavaMail 如何处理 quoted-printable 编码？
- [ ] 不同浏览器的 boundary 格式差异大吗？JavaMail 能自动识别吗？
- [ ] 如果需要提取图片，应该如何修改代码？

---

## 🎓 总结

### 核心要点
```
1. MHTML = MIME 格式的 HTML 网页（单文件）
2. JavaMail = 行业标准 MIME 解析库
3. Session = 无需配置的轻量级上下文
4. MimeMessage = 自动解析 MIME 结构
5. Multipart = 包含多个 BodyPart 的容器
6. BodyPart = 单个部分（HTML/图片/CSS）
7. getContent() = 自动解码并返回内容
```

### 技术栈
```
前端：Vaadin 24.10
后端：Spring Boot 3.5 + JavaMail 1.6.2
解析：Jsoup + JavaMail
数据库：H2
```

### 优势
```
✅ 不重复造轮子（使用成熟库）
✅ 代码简洁（180 行 vs 370 行）
✅ 可靠性高（工业级标准）
✅ 易于维护（清晰的逻辑）
```

---

**学习愉快！** 🎉

如有问题，随时询问。现在你可以：
1. 重新阅读代码，加深理解
2. 尝试添加新功能（如图片提取）
3. 调试并观察日志输出

实践是最好的老师！💪
