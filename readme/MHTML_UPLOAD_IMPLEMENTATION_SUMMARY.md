# MHTML 上传功能实施总结

## 📋 项目概述

将 Township Wiki 数据获取方式从 **ZIP 压缩包** 改为 **MHTML 单文件格式**，解决 ZIP 解压兼容性问题和图片资源缺失问题。

---

## 🎯 为什么选择 MHTML？

### ZIP 方案的问题
1. ❌ **编码兼容性差**：Windows 中文系统使用 GBK 编码，Java 默认 UTF-8
2. ❌ **文件名问题**：特殊字符（空格、下划线）导致 `invalid LOC header` 错误
3. ❌ **用户体验复杂**：需要手动压缩 HTML 和文件夹
4. ❌ **图片可能丢失**：CDN 资源可能不被保存

### MHTML 方案的优势
1. ✅ **单个文件**：HTML + CSS + 图片全部打包在一起
2. ✅ **标准格式**：RFC 2557 标准，兼容性好
3. ✅ **无需压缩**：浏览器直接生成，跳过 ZIP 步骤
4. ✅ **资源完整**：包含所有 Base64 编码的图片和样式
5. ✅ **解析可靠**：MIME 格式，边界清晰

---

## 🔧 技术实现

### 核心组件

#### 1. MhtmlUploadService（新增）

**文件路径**: `src/main/java/zzk/townshipscheduler/backend/crawling/MhtmlUploadService.java`

**关键方法**:
```java
public Document processUploadedMhtml(InputStream mhtmlInputStream)

private String extractHtmlFromMhtml(String mhtmlContent)

private String decodeQuotedPrintable(String input)

public void validateMhtmlHeader(InputStream inputStream)
```

**工作原理**:
1. 读取 MHTML 文件内容（UTF-8 编码）
2. 验证 MIME 格式（检查 `MIME-Version:` 等标识）
3. 解析 MIME 边界（boundary）
4. 提取 HTML 部分（查找 `Content-Type: text/html`）
5. 解码 Quoted-Printable 编码
6. 返回 Jsoup Document

---

#### 2. CrawlingWikiView（修改）

**文件路径**: `src/main/java/zzk/townshipscheduler/ui/views/crawling/CrawlingWikiView.java`

**主要变更**:

##### 依赖注入
```java
// 旧代码
private final HtmlUploadService htmlUploadService;

// 新代码
private final MhtmlUploadService mhtmlUploadService;
```

##### 文件类型支持
```java
// 旧代码
upload.setAcceptedFileTypes("application/zip", ".zip");

// 新代码
upload.setAcceptedFileTypes("message/rfc822", ".mhtml", ".mht");
```

##### 上传处理逻辑
```java
// 旧代码（ZIP）
htmlUploadService.validateZipHeader(data);
var document = htmlUploadService.processUploadedZip(inputStream);

// 新代码（MHTML）
mhtmlUploadService.validateMhtmlHeader(inputStream);
inputStream.reset(); // 重置流位置
var document = mhtmlUploadService.processUploadedMhtml(inputStream);
```

##### 用户界面更新
```java
new Paragraph("3. 保存类型选择 \"MHTML 单个文件 (*.mhtml;*.mht)\""),
new Paragraph("4. 文件名会自动设置为 \"Goods _ Township Wiki _ Fandom.mhtml\""),
new Paragraph("5. 上传此 MHTML 文件到此处")
```

---

### 3. 使用说明文档

**生成方式**: 点击 "下载使用说明" 按钮下载 `mhtml_upload_instructions.txt`

**内容包括**:
- 保存步骤详解
- Windows/Mac 差异说明
- 注意事项和优势介绍

---

## 📊 代码统计

| 文件 | 新增行数 | 删除行数 | 说明 |
|------|---------|---------|------|
| MhtmlUploadService.java | 228 | - | 新建服务类 |
| CrawlingWikiView.java | 35 | 45 | UI 适配 |
| HtmlUploadService.java | - | 376 | 已弃用（保留） |
| **总计** | **263** | **421** | 净减少 158 行 |

---

## 🧪 测试建议

### 测试场景 1：标准 MHTML 上传

**步骤**:
1. 打开 Edge 浏览器
2. 访问 https://township.fandom.com/wiki/Goods
3. 按 Ctrl+S → 保存类型选择 "MHTML 单个文件"
4. 确认文件名为 `Goods _ Township Wiki _ Fandom.mhtml`
5. 在 Crawling 页面选择 "手动上传"
6. 上传 MHTML 文件

**预期结果**:
- ✅ 文件验证通过
- ✅ 成功解析 HTML 内容
- ✅ 表格数据完整
- ✅ 图片 URL 正确提取

---

### 测试场景 2：大文件测试

**测试文件**: 包含大量图片的 MHTML（>10MB）

**关注点**:
- 上传超时时间
- 内存占用
- 解析速度

---

### 测试场景 3：编码兼容性

**测试用例**:
- 包含中文字符的 MHTML
- 包含特殊符号的 MHTML
- Quoted-Printable 编码的内容

---

## 🚀 用户使用指南

### 保存为 MHTML 的步骤

#### Windows（Edge/IE）
1. 打开 Township Wiki Goods 页面
2. 按 `Ctrl+S`
3. 保存类型：**MHTML 单个文件 (*.mhtml;*.mht)**
4. 文件名自动填充为 `Goods _ Township Wiki _ Fandom`
5. 点击"保存"

#### macOS（Safari）
1. 打开 Township Wiki Goods 页面
2. 按 `Cmd+S`
3. 格式选择：**Web Archive** 或 **MIME HTML**（如有）
4. 保存

#### 注意事项
- ⚠️ Chrome 可能需要安装扩展才能保存为 MHTML
- ⚠️ Firefox 默认不支持 MHTML，建议使用 Edge
- ⚠️ 确保文件大小不超过 50MB

---

## 📈 对比分析

| 特性 | ZIP 方案 | MHTML 方案 |
|------|---------|-----------|
| **文件格式** | 压缩包 | 单文件 |
| **包含资源** | 需手动选择 | 自动包含 |
| **兼容性** | 差（编码问题） | 好（标准 MIME） |
| **用户操作** | 3-4 步 | 2 步 |
| **解析复杂度** | 高（解压+查找） | 中（MIME 解析） |
| **图片完整性** | 不确定 | 高 |
| **推荐度** | ❌ | ✅ |

---

## 🔮 未来优化方向

### 短期（1-2 周）
1. 添加图片下载功能（从 MHTML 中提取并保存）
2. 支持批量上传多个 MHTML 文件
3. 优化解析性能（流式处理）

### 中期（1 个月）
1. 集成 Puppeteer 进行页面渲染（获取懒加载图片）
2. 添加进度条显示
3. 错误恢复机制

### 长期（3 个月+）
1. 搭建代理服务器绕过 Cloudflare
2. 实现定时自动爬取
3. 数据差异对比功能

---

## 📝 Git 提交记录

```bash
commit f916bc9 (HEAD -> feature/html-upload-support)
Author: AI Assistant
Date:   Mon Mar 30 2026

    feat: migrate from ZIP to MHTML single-file upload
    
    - Create MhtmlUploadService to parse MHTML format
    - Remove ZIP extraction logic and dependencies
    - Update CrawlingWikiView UI for MHTML upload
    - Support .mhtml and .mht file formats
    - Add detailed user instructions for saving as MHTML
    - Fixed filename to 'Goods _ Township Wiki _ Fandom.mhtml'
    
    Benefits:
    ✓ Single file format (no ZIP compatibility issues)
    ✓ Contains all resources (images, CSS, etc.)
    ✓ More reliable than ZIP approach
    ✓ Easier for users to manage
```

---

## ✅ 验收清单

- [x] MhtmlUploadService 创建完成
- [x] CrawlingWikiView UI 更新
- [x] 文件类型支持配置正确
- [x] 编译通过（BUILD SUCCESS）
- [x] Git 提交规范
- [ ] 运行时测试（待用户验证）
- [ ] 图片提取功能（后续添加）

---

## 📞 技术支持

如遇到问题，请提供以下信息：

1. **浏览器版本**：Edge xx.x.x / Chrome xx.x.x
2. **保存的 MHTML 文件大小**
3. **错误日志**（控制台输出）
4. **MHTML 文件头 1KB**（用于诊断格式问题）

---

**实施日期**: 2026 年 3 月 30 日  
**实施者**: AI Assistant  
**状态**: ✅ 开发完成，待测试
