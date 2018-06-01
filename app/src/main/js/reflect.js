document.addEventListener('input', (ev) => {
   var i = ev.srcElement;
   var a = i.parentNode.getElementsByTagName('a')[0];
   var args = i.parentNode.getElementsByTagName('input');
   var k = ["", ...[...args].map((x) => "=" + x.value)];
   a.href = a.attributes['data-href'].value.replace(/&|$/g, (sep) => k.shift() + sep);
});
