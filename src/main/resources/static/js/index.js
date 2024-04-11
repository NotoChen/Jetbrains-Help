window.submitLicenseInfo = function () {
    let licenseeName = document.getElementById('licenseeName').value
    let assigneeName = document.getElementById('assigneeName').value
    let expiryDate = document.getElementById('expiryDate').value
    let licenseInfo = {
        licenseeName: licenseeName,
        assigneeName: assigneeName,
        expiryDate: expiryDate
    }
    localStorage.setItem('licenseInfo', JSON.stringify(licenseInfo))
    document.getElementById('mask').style.display = 'none'
    document.getElementById('form').style.display = 'none'
}
document.getElementById('search').oninput = function (e) {
    $("#product-list").load('/search?search=' + e.target.value)
}
window.showLicenseForm = function () {
    let licenseInfo = localStorage.getItem('licenseInfo');
    if (licenseInfo !== null) {
        licenseInfo = JSON.parse(licenseInfo)
        document.getElementById('licenseeName').value = licenseInfo.licenseeName
        document.getElementById('assigneeName').value = licenseInfo.assigneeName
        document.getElementById('expiryDate').value = licenseInfo.expiryDate
    } else {
        document.getElementById('licenseeName').value = '光云'
        document.getElementById('assigneeName').value = '藏柏'
        document.getElementById('expiryDate').value = '2111-11-11'
    }
    document.getElementById('mask').style.display = 'block'
    document.getElementById('form').style.display = 'block'
}
window.showVmoptins = function () {
    alert("-javaagent:/(Your Path)/ja-netfilter/ja-netfilter.jar\n" +
        "--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED\n" +
        "--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED")
}
//@see https://zhuanlan.zhihu.com/p/597944027
const copyText = async (val) => {
    if (navigator.clipboard && navigator.permissions) {
        await navigator.clipboard.writeText(val)
    } else {
        const textArea = document.createElement('textArea')
        textArea.value = val
        textArea.style.width = 0
        textArea.style.position = 'fixed'
        textArea.style.left = '-999px'
        textArea.style.top = '10px'
        textArea.setAttribute('readonly', 'readonly')
        document.body.appendChild(textArea)

        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
    }
}
window.copyLicense = async function (e) {

    while (localStorage.getItem('licenseInfo') === null) {
        document.getElementById('mask').style.display = 'block'
        document.getElementById('form').style.display = 'block'
        await new Promise(r => setTimeout(r, 1000));
    }
    let licenseInfo = JSON.parse(localStorage.getItem('licenseInfo'))
    let productCode = e.closest('.card').dataset.productCodes;
    let data = {
        "licenseeName": licenseInfo.licenseeName,
        "assigneeName": licenseInfo.assigneeName,
        "expiryDate": licenseInfo.expiryDate,
        "productCode": productCode,
    }
    let resp = await fetch('/generateLicense', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    }).then(response => response.text())

    copyText(resp)
        .then(() => {
            alert("The activation code has been copied");
        })
}