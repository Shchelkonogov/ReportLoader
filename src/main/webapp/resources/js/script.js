// Часть кода что бы не прыгал размер таблицы
// window.addEventListener("load", function(){
//     unsetWidth();
// });

// Общее количество файлов и количество файлов нужного формата
let filesCount;
let loadedFilesCount;

// Функция загрузки файлов на сервер
function uploadFile() {
    let filesLength = _("upload:file").files.length;
    filesCount = filesLength;

    if (filesCount > 1000) {
        alert("Вы можете загрузить максимум 1000 файлов");
        return;
    }

    _("status").innerHTML = "Подготовка";
    _("statusIcon").setAttribute('class', 'pi pi-spin pi-spinner');

    loadedFilesCount = 0;
    let formData = new FormData();
    for(let i = 0; i < filesLength; i++){
        if (hasExtension(_("upload:file").files[i].name.toLowerCase(), ['.xml', '.pdf', '.html'])) {
            formData.append("file[]", _("upload:file").files[i]);
            loadedFilesCount++;
        }
    }
    formData.append("UUID", _("UUID").value);
    let ajax = new XMLHttpRequest();
    ajax.upload.addEventListener("progress", progressHandler, false);
    ajax.addEventListener("load", completeHandler, false);
    ajax.addEventListener("error", errorHandler, false);
    ajax.addEventListener("abort", abortHandler, false);
    ajax.open("POST", "upload");
    ajax.send(formData);
}

function progressHandler(event) {
    _("loaded_n_total").innerHTML = "Загружено " + formatBytes(event.loaded) + " из " + formatBytes(event.total); // "Uploaded " + event.loaded + " bytes of " + event.total
    let percent = (event.loaded / event.total) * 100;
    _("progressBar").value = Math.round(percent);
    _("status").innerHTML = Math.round(percent) + "% загрузка... пожалуйста подождите"; // "% uploaded... please wait"
    if (Math.round(percent) === 100) {
        _("status").innerHTML = "Завершение";
    }
}

function completeHandler(event) {
    _("status").innerHTML = event.target.responseText;
    _("statusIcon").removeAttribute('class');
    _("loaded_n_total").innerHTML += " (" + loadedFilesCount + " файлов нужного формата из " + filesCount + ")";
    _("progressBar").value = 0;
    updateTree();
}

function errorHandler(event) {
    _("status").innerHTML = "Upload Failed";
}

function abortHandler(event) {
    _("status").innerHTML = "Upload Aborted";
}


// таймер, для обновления таблицы
let interval;

// Функция стартует таймер
function start() {
    interval = setInterval(checkUpdate, 500);
}

// Функция останавливает таймер
function stop() {
    clearInterval(interval);

    addListener();
}



// Элемент на который нажали "связать"
let clickElement;

// Функция, которая добавляет слушатели ко всем ссылкам "a", что бы понять на какую кнопку "связать" нажали
function addListener() {
    $("table tr a").on('click', function(e){
        clickElement = $(this).closest('td').parent()[0].id;
    });
}

// Функция включает крутилку на строке, где нажали кнопку "ассоциировать"
function updateStatus() {
    document.getElementById(clickElement).getElementsByTagName('i')[0].setAttribute('class', 'pi pi-spin pi-spinner');
}



// Функция привод байты к нормальному виду
function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

// Функция возвращает расширение файла
function hasExtension(fileName, exts) {
    return (new RegExp('(' + exts.join('|').replace(/\./g, '\\.') + ')$')).test(fileName);
}

// Функция по id возвращает елемент
function _(el) {
    return document.getElementById(el);
}

// Часть кода что бы не прыгал размер таблицы
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
