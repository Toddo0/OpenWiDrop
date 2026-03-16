package com.toddo.openwidrop;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

public class FileServer extends NanoHTTPD {

    private final File rootDir;
    private static final String TAG = "FileServer";

    public FileServer(int port, File rootDir) {
        super(port);
        this.rootDir = rootDir;
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        // KRİTİK DÜZELTME: NanoHTTPD'nin Android'de çökmaması için temp dizinini rootDir yapıyoruz
        System.setProperty("java.io.tmpdir", rootDir.getAbsolutePath());

        Log.d(TAG, "Sunucu başlatıldı. Port: " + port + ", Klasör: " + rootDir.getAbsolutePath());
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, List<String>> params = session.getParameters();
        String subPath = (params.get("path") != null) ? params.get("path").get(0) : "";

        // Güvenlik: Root dışına çıkışı engelle
        File currentDir = new File(rootDir, subPath);
        try {
            if (!currentDir.getCanonicalPath().startsWith(rootDir.getCanonicalPath())) {
                return errorResponse("Erişim reddedildi!");
            }
        } catch (IOException e) {
            return errorResponse("Yol hatası.");
        }

        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            // 1. Ana Sayfa ve Klasör Listeleme
            if (uri.equals("/")) {
                if (Method.POST.equals(method)) {
                    return handleFileUpload(session, currentDir);
                }
                return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", generatePage(currentDir, subPath));
            }

            // 2. Dosya İndirme
            if (uri.startsWith("/download/")) {
                String fileName = uri.substring("/download/".length());
                fileName = URLDecoder.decode(fileName, "UTF-8");

                File file = new File(rootDir, fileName);
                if (file.exists() && file.isFile()) {
                    FileInputStream fis = new FileInputStream(file);
                    Response res = newChunkedResponse(Response.Status.OK, getMimeType(fileName), fis);
                    res.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
                    return res;
                } else {
                    return errorResponse("Dosya bulunamadı.");
                }
            }
        } catch (Exception e) {
            return errorResponse("Sistem Hatası: " + e.getMessage());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Bulunamadı.");
    }

    private Response handleFileUpload(IHTTPSession session, File currentDir) {
        Map<String, String> files = new HashMap<>();
        try {
            // NanoHTTPD dosyaları geçici dizine alır ve yollarını files map'ine koyar
            session.parseBody(files);
            Map<String, List<String>> params = session.getParameters();
            int uploadedCount = 0;

            // Formdan gelen tüm dosyaları döngüyle işle
            for (String key : files.keySet()) {
                String tmpFilePath = files.get(key);
                List<String> originalFileNameList = params.get(key);

                if (tmpFilePath != null && originalFileNameList != null && !originalFileNameList.isEmpty()) {
                    String fileName = originalFileNameList.get(0);
                    File tmpFile = new File(tmpFilePath);
                    File destFile = getUniqueFile(new File(currentDir, fileName));

                    copyFile(tmpFile, destFile);
                    uploadedCount++;
                }
            }

            String currentPath = currentDir.getCanonicalPath().replace(rootDir.getCanonicalPath(), "");
            if (currentPath.startsWith(File.separator)) currentPath = currentPath.substring(1);

            return newFixedLengthResponse(Response.Status.OK, "text/html",
                    "<html><body><script>alert('Başarıyla yüklendi: " + uploadedCount + " dosya'); " +
                            "window.location.href='/?path=" + URLEncoder.encode(currentPath, "UTF-8") + "';</script></body></html>");

        } catch (Exception e) {
            return errorResponse("Yükleme Hatası: " + e.getMessage());
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream is = new FileInputStream(source);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024 * 8];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private Response errorResponse(String msg) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", msg);
    }

    private File getUniqueFile(File file) {
        if (!file.exists()) return file;
        String name = file.getName();
        String baseName = name;
        String extension = "";
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        }
        int counter = 1;
        File newFile;
        do {
            newFile = new File(file.getParent(), baseName + " (" + counter + ")" + extension);
            counter++;
        } while (newFile.exists());
        return newFile;
    }

    private String generatePage(File currentDir, String relativePath) {
        File[] files = currentDir.listFiles();
        StringBuilder listHtml = new StringBuilder();
        int itemCount = (files != null) ? files.length : 0;

        // Üst Klasör butonu
        if (!relativePath.isEmpty()) {
            String parentPath = "";
            if (relativePath.contains("/")) {
                parentPath = relativePath.substring(0, relativePath.lastIndexOf("/"));
            }
            listHtml.append(createFolderItem("..", parentPath, "Üst Klasöre Dön"));
        }

        if (files != null) {
            // Önce Klasörler
            for (File f : files) {
                if (f.isDirectory()) {
                    String folderPath = relativePath.isEmpty() ? f.getName() : relativePath + "/" + f.getName();
                    listHtml.append(createFolderItem(f.getName(), folderPath, "Klasör"));
                }
            }
            // Sonra Dosyalar
            for (File f : files) {
                if (f.isFile()) {
                    String downloadPath = relativePath.isEmpty() ? f.getName() : relativePath + "/" + f.getName();
                    listHtml.append(createFileItem(f, downloadPath));
                }
            }
        }

        return buildFinalHtml(listHtml.toString(), currentDir.getAbsolutePath(), itemCount);
    }

    private String createFolderItem(String name, String path, String meta) {
        return "<li><div class='file-item'><div class='file-info'>" +
                "<span class='file-icon'>📁</span>" +
                "<div class='file-details'>" +
                "<a href='/?path=" + URLEncoder.encode(path) + "' class='file-name-link'>" + name + "</a>" +
                "<span class='file-meta'>" + meta + "</span>" +
                "</div></div></div></li>";
    }

    private String createFileItem(File f, String downloadPath) {
        String size = formatFileSize(f.length());
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(f.lastModified()));
        return "<li><div class='file-item'><div class='file-info'>" +
                "<span class='file-icon'>" + getFileIcon(f.getName()) + "</span>" +
                "<div class='file-details'>" +
                "<span class='file-name'>" + f.getName() + "</span>" +
                "<span class='file-meta'>" + size + " • " + date + "</span>" +
                "</div></div>" +
                "<div class='file-actions'><a href='/download/" + URLEncoder.encode(downloadPath) + "' class='download-btn'>📥</a></div>" +
                "</div></li>";
    }

    private String buildFinalHtml(String content, String absPath, int count) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>OpenWidrop</title><style>" +
                "body{font-family:-apple-system,sans-serif;background:linear-gradient(135deg,#667eea,#764ba2);min-height:100vh;padding:20px;margin:0;}" +
                ".container{max-width:900px;margin:0 auto;background:#fff;border-radius:20px;box-shadow:0 10px 30px rgba(0,0,0,0.2);overflow:hidden;}" +
                ".header{background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;padding:30px;text-align:center;}" +
                ".content{padding:25px;}" +
                ".folder-info{background:#f8f9fa;padding:15px;border-radius:12px;margin-bottom:20px;display:flex;align-items:center;gap:10px;font-size:14px;border:1px solid #eee;}" +
                ".folder-path{flex:1;font-family:monospace;background:#fff;padding:5px 10px;border-radius:5px;border:1px solid #ddd;overflow:hidden;text-overflow:ellipsis;}" +
                ".upload-area{background:#f8f9fa;border:2px dashed #ccd;border-radius:15px;padding:30px;text-align:center;margin-bottom:20px;transition:0.3s;cursor:pointer;}" +
                ".upload-area:hover, .upload-area.dragover{border-color:#667eea;background:#f0f4ff;}" +
                ".file-list{list-style:none;padding:0;margin:0;}" +
                ".file-item{display:flex;align-items:center;justify-content:space-between;padding:12px 20px;border-bottom:1px solid #f0f0f0;transition:0.2s;}" +
                ".file-item:hover{background:#fafafa;}" +
                ".file-info{display:flex;align-items:center;gap:15px;}" +
                ".file-icon{font-size:24px;}" +
                ".file-name{font-weight:500;color:#333;display:block;}" +
                ".file-name-link{font-weight:600;color:#667eea;text-decoration:none;}" +
                ".file-meta{font-size:12px;color:#888;}" +
                ".download-btn{background:#eef2ff;color:#667eea;width:35px;height:35px;border-radius:50%;display:flex;align-items:center;justify-content:center;text-decoration:none;transition:0.2s;}" +
                ".download-btn:hover{background:#667eea;color:#fff;}" +
                "progress{width:100%;height:20px;border-radius:10px;margin-top:15px;}" +
                "progress::-webkit-progress-bar{background-color:#eee;border-radius:10px;}" +
                "progress::-webkit-progress-value{background-color:#667eea;border-radius:10px;}" +
                "</style></head><body>" +
                "<div class='container'><div class='header'><h1>📁 OpenWidrop</h1><p>Dosya Paylaşım Sunucusu</p></div>" +
                "<div class='content'><div class='folder-info'><span>📍</span><span class='folder-path'>" + absPath + "</span><span>" + count + " öğe</span></div>" +
                "<div class='upload-area' id='uploadArea' onclick=\"document.getElementById('fileInput').click()\">" +
                "<div id='uploadContent'><div style='font-size:40px'>📤</div><h3>Yüklemek için tıklayın veya dosyaları sürükleyin</h3></div>" +
                "<input type='file' id='fileInput' multiple style='display:none' onchange='uploadFiles(this.files)'></div>" +
                "<ul class='file-list'>" + content + "</ul></div></div>" +
                "<script>" +
                "const dropZone = document.getElementById('uploadArea');" +
                "dropZone.addEventListener('dragover', e => { e.preventDefault(); dropZone.classList.add('dragover'); });" +
                "dropZone.addEventListener('dragleave', e => { e.preventDefault(); dropZone.classList.remove('dragover'); });" +
                "dropZone.addEventListener('drop', e => { e.preventDefault(); dropZone.classList.remove('dragover'); document.getElementById('fileInput').files = e.dataTransfer.files; uploadFiles(e.dataTransfer.files); });" +
                "function uploadFiles(files){" +
                "  if(!files || files.length === 0) return;" +
                "  const formData = new FormData();" +
                "  for(let i=0; i<files.length; i++){ formData.append('file_' + i, files[i]); }" +
                "  const path = new URLSearchParams(window.location.search).get('path') || '';" +
                "  " +
                "  const xhr = new XMLHttpRequest();" +
                "  xhr.open('POST', '/?path=' + encodeURIComponent(path), true);" +
                "  " +
                "  document.getElementById('uploadContent').innerHTML = '<h3>Yükleniyor... %<span id=\"prog\">0</span></h3><progress id=\"pbar\" value=\"0\" max=\"100\"></progress>';" +
                "  " +
                "  xhr.upload.onprogress = function(e) { " +
                "    if(e.lengthComputable) { " +
                "      const p = Math.round((e.loaded / e.total) * 100); " +
                "      document.getElementById('prog').innerText = p; " +
                "      document.getElementById('pbar').value = p; " +
                "    }" +
                "  };" +
                "  " +
                "  xhr.onload = function() { " +
                "    if(xhr.status === 200) { " +
                "      document.open(); document.write(xhr.responseText); document.close(); " +
                "    } else { " +
                "      alert('Hata kodu: ' + xhr.status); location.reload(); " +
                "    }" +
                "  };" +
                "  " +
                "  xhr.onerror = function() { " +
                "    alert('Bağlantı koptu! Dosya çok büyük olabilir veya telefon bağlantıyı kesti.'); location.reload(); " +
                "  };" +
                "  " +
                "  xhr.send(formData);" +
                "}" +
                "</script>" +
                "</body></html>";
    }

    private String getFileIcon(String name) {
        String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).toLowerCase() : "";
        switch (ext) {
            case "jpg": case "jpeg": case "png": case "gif": return "🖼️";
            case "mp3": case "wav": return "🎵";
            case "mp4": case "mkv": return "🎬";
            case "pdf": return "📕";
            case "zip": case "rar": return "🗜️";
            case "apk": return "📱";
            default: return "📄";
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int i = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, i), units[i]);
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}