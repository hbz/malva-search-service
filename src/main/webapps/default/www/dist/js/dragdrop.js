$(document).ready(function() {
    var dropbox;
    dropbox = document.getElementById("dropbox");
    dropbox.addEventListener("dragenter", dragenter, false);
    dropbox.addEventListener("dragleave", dragleave, false);
    dropbox.addEventListener("dragover", dragover, false);
    dropbox.addEventListener("drop", drop, false);
    function defaults(e) {
       e.stopPropagation();
       e.preventDefault();
    };
    function dragenter(e) {
       $(this).addClass("active");
       defaults(e);
    };
    function dragover(e) {
       defaults(e);
    };
    function dragleave(e) {
       $(this).removeClass("active");
       defaults(e);
    };
    function drop(e) {
       $(this).removeClass("active");
       defaults(e);
       var dt = e.dataTransfer;
       var files = dt.files;
       handleFiles(files, e);
    }
    handleFiles = function(files,e) {
        var file = files[0];
        if (!file.type == 'application/pdf') {
          alert("Die Datei " + file.name + " ist keine PDF-Datei");
          return false;
        }
        var info = '<div class="preview active-win"><div class="preview-image"><img ></div><div class="progress-holder"><span id="progress"></span></div><span class="percents"></span><div style="float:left;">Hochgeladen <span class="up-done"></span> KB von '+parseInt(file.size / 1024)+' KB</div>';
        $(".upload-progress").show("fast", function() {
            $(".upload-progress").html(info);
            uploadFile(file);
        });
    };
    uploadFile = function(file) {
        if (typeof FileReader == "undefined") {
            alert("HTML5 FileReader nicht unterstützt");
        } else {
          reader = new FileReader();
          reader.onload = function(e) {
            $('.preview img').attr('src', pdfImgUrl).css("width","64px").css("height","64px");
          }
          reader.readAsDataURL(file);
          xhr = new XMLHttpRequest();
          xhr.open("post", uploadUrl, true);
          xhr.upload.addEventListener("progress", function (event) {
            if (event.lengthComputable) {
                $("#progress").css("width",(event.loaded / event.total) * 100 + "%");
                $(".percents").html(" "+((event.loaded / event.total) * 100).toFixed() + "%");
                $(".up-done").html((parseInt(event.loaded / 1024)).toFixed(0));
            }
            else {
                alert("Dateigrösse nicht ermittelbar");
            }
          }, false);
          xhr.onreadystatechange = function(oEvent) {
            if (xhr.readyState === 4) {
              if (xhr.status === 200) {
                $("#progress").css("width","100%");
                $(".percents").html("100%");
                $(".up-done").html((parseInt(file.size / 1024)).toFixed(0));
                location.reload(true);
              } else {
                alert("Fehler "+ xhr.statusText);
              }
            }
          };
          xhr.setRequestHeader("Content-Type", "multipart/form-data");
          xhr.setRequestHeader("X-File-Name", file.name);
          xhr.setRequestHeader("X-File-Size", file.size);
          xhr.setRequestHeader("X-File-Type", file.type);
          var key = $("#key").val();
          xhr.setRequestHeader("X-File-Key", key);
          xhr.send(file);
        }
    };