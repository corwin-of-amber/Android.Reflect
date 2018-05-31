document.addEventListener('input', (ev) => {
   var i = ev.srcElement;
   var a = i.parentNode.getElementsByTagName('a')[0];
   a.href = `${a.attributes['data-href'].value}=${i.value}`;
});
