vjo.ctype('org.prototypejs.Element_Methods_Simulated') //< public

.props({
  /**
   *  Element.hasAttribute(@element, attribute) -> Boolean
   *  
   *  Simulates the standard compliant DOM method
   *  [`hasAttribute`](http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-ElHasAttr)
   *  for browsers missing it (Internet Explorer 6 and 7).
   *  
   *  ##### Example
   *  
   *      language: html
   *      <a id="link" href="http://prototypejs.org">Prototype</a>
   *
   *  Then:
   *
   *      $('link').hasAttribute('href');
   *      // -> true
  **/ 
	//> public boolean hasAttribute({String | Element} idOrElement, String attribute)
  hasAttribute: vjo.NEEDS_IMPL
	
})
.protos({
	
})
.endType();