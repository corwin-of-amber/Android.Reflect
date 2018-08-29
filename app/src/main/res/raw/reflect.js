document.addEventListener('input', (ev) => {
   var i = ev.srcElement;
   if (i.parentNode.tagName == "LI") {
     var a = i.parentNode.getElementsByTagName('a')[0],
         args = i.parentNode.getElementsByTagName('input'),
         k = ["", ...[...args].map((x) => "=" + x.value)];
     a.href = a.attributes['data-href'].value.replace(/&|$/g, (sep) => k.shift() + sep);
   }
   else if (i.className == "this-ref") {
     var dups = document.body.getElementsByClassName('this-arg');
     Array.prototype.forEach.call(dups, (ii) => ii.value = i.value);
   }
});
