/**
 * Makes an element drop and fade out at the same time.
 * <p>
 * Examples
 * <code>
 * Effect.DropOut('id_of_element');
 * </code>
 * Notes
 * <p>
 * Works safely with most Block Elements, except tables.
 */
vjo.ftype('org.scriptaculous.Effect_DropOut') //< public
//> needs(org.scriptaculous.Effect_Parallel)
.props({
	/**
	 * 
	 */
	//> public Effect_Parallel _invoke_({String | Element} idOrElement)
	_invoke_: function() { return null ; }
})
.options({
	metatype: true
})
.endType();