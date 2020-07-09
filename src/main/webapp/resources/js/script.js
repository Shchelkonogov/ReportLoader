// window.addEventListener("load", function(){
//     unsetWidth();
// });

function _(el) {
    return document.getElementById(el);
}

var filesCount;
var loadedFilesCount;

function uploadFile() {
    _("status").innerHTML = "Подготовка";
    _("statusIcon").setAttribute('class', 'pi pi-spin pi-spinner');
    var filesLength=_("upload:file").files.length;
    filesCount = filesLength;
    loadedFilesCount = 0;
    var formData = new FormData();
    for(var i=0;i<filesLength;i++){
        if (hasExtension(_("upload:file").files[i].name.toLowerCase(), ['.xml', '.pdf', '.html'])) {
            formData.append("file[]", _("upload:file").files[i]);
            loadedFilesCount++;
        }
    }
    formData.append("UUID", _("UUID").value);
    var ajax = new XMLHttpRequest();
    ajax.upload.addEventListener("progress", progressHandler, false);
    ajax.addEventListener("load", completeHandler, false);
    ajax.addEventListener("error", errorHandler, false);
    ajax.addEventListener("abort", abortHandler, false);
    ajax.open("POST", "upload");
    ajax.send(formData);
}

function progressHandler(event) {
    _("loaded_n_total").innerHTML = "Загружено " + event.loaded + " байт из " + event.total;
    // "Uploaded " + event.loaded + " bytes of " + event.total
    var percent = (event.loaded / event.total) * 100;
    _("progressBar").value = Math.round(percent);
    _("status").innerHTML = Math.round(percent) + "% загрузка... пожалуйста подождите";
    if (Math.round(percent) === 100) {
        _("status").innerHTML = "Завершение";
    }
    // "% uploaded... please wait"
}

function completeHandler(event) {
    _("status").innerHTML = event.target.responseText;
    _("statusIcon").removeAttribute('class');
    _("loaded_n_total").innerHTML += " (" + loadedFilesCount + " файлов нужного формата из " + filesCount + ")";
    _("progressBar").value = 0; //wil clear progress bar after successful upload
    updateTree();
}

function errorHandler(event) {
    _("status").innerHTML = "Upload Failed";
}

function abortHandler(event) {
    _("status").innerHTML = "Upload Aborted";
}

function hasExtension(fileName, exts) {
    return (new RegExp('(' + exts.join('|').replace(/\./g, '\\.') + ')$')).test(fileName);
}

var interval;

function start() {
    interval = setInterval(checkUpdate, 500);
}

function stop() {
    clearInterval(interval);

    $("table tr a").on('click', function(e){
        clickElement = $(this).closest('td').parent()[0].id;
    });
}

var clickElement;

function updateStatus() {
    document.getElementById(clickElement).getElementsByTagName('i')[0].setAttribute('class', 'pi pi-spin pi-spinner');
}

// function unsetWidth() {
//     document.getElementById('treeTable:treeTableData:treeTableName').style.width = "unset";
//     document.getElementById('treeTable:treeTableData:treeTableName_clone').style.width = "unset";
//     document.getElementById('treeTable:treeTableData').getElementsByClassName('ui-treetable-scrollable-body')[0].style.height = '100%';
//     document.getElementById('treeTable:treeTableData').getElementsByClassName('ui-treetable-scrollable-body')[0].style.maxHeight = '400px';
//     checkMargin();
// }
//
// function checkMargin() {
//     var scroll = document.getElementById('treeTable:treeTableData').getElementsByClassName('ui-treetable-scrollable-body')[0];
//     var data = document.getElementById('treeTable:treeTableData_data');
//     if (scroll.clientHeight > data.clientHeight) {
//         document.getElementById('treeTable:treeTableData').getElementsByClassName('ui-treetable-scrollable-header-box')[0].style.marginRight = '0px';
//     } else {
//         document.getElementById('treeTable:treeTableData').getElementsByClassName('ui-treetable-scrollable-header-box')[0].style.marginRight = '15px';
//     }
// }
