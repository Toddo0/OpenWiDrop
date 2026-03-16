package com.toddo.openwidrop;

import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class FileServer extends NanoHTTPD {

    private File rootDir;

    public FileServer(int port, File rootDir) {
        super(port);
        this.rootDir = rootDir;
    }

    @Override
    public Response serve(IHTTPSession session) {

        String uri = session.getUri();

        try {

            if(uri.equals("/")) {
                return newFixedLengthResponse(
                        Response.Status.OK,
                        "text/html",
                        generatePage()
                );
            }

            if(uri.startsWith("/download/")) {

                String name = uri.replace("/download/","");
                File f = new File(rootDir,name);

                if(f.exists()) {

                    return newChunkedResponse(
                            Response.Status.OK,
                            "application/octet-stream",
                            new FileInputStream(f)
                    );
                }
            }

            if(Method.POST.equals(session.getMethod())) {

                Map<String,String> files = new HashMap<>();
                session.parseBody(files);

                String tmp = files.get("file");

                if(tmp != null) {

                    File tmpFile = new File(tmp);
                    File dst = new File(rootDir,session.getParms().get("file"));

                    tmpFile.renameTo(dst);
                }

                return newFixedLengthResponse("Upload OK");
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse("404");
    }

    private String generatePage() {

        File[] files = rootDir.listFiles();

        StringBuilder list = new StringBuilder();

        if(files != null) {
            for(File f : files){

                list.append("<li>")
                        .append("<a href=\"/download/")
                        .append(f.getName())
                        .append("\">")
                        .append(f.getName())
                        .append("</a></li>");
            }
        }

        return """
<html>

<body>

<h2>File Share</h2>

<div id="drop" style="width:300px;height:150px;border:2px dashed gray;">
Drag & Drop Files Here
</div>

<br>

<input type="file" id="fileInput">

<button onclick="upload()">Upload</button>

<ul>
""" + list + """
</ul>

<script>

function upload(){

let f=document.getElementById("fileInput").files[0]

let fd=new FormData()

fd.append("file",f)

fetch("/",{
method:"POST",
body:fd
}).then(()=>location.reload())

}

let drop=document.getElementById("drop")

drop.ondrop=function(e){

e.preventDefault()

let f=e.dataTransfer.files[0]

let fd=new FormData()

fd.append("file",f)

fetch("/",{
method:"POST",
body:fd
}).then(()=>location.reload())

}

drop.ondragover=function(e){
e.preventDefault()
}

</script>

</body>

</html>
""";
    }
}