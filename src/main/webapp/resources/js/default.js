/**
 * HELPER FUNCTIONS FOR FORM INPUT
 * -----------------------------------------------------------------------------
 */
/**
 * check if the string ends with special chars
 * 
 * @param {type}
 *            str
 * @param {type}
 *            suffix
 * @returns {Boolean}
 */
function endsWith(str, suffix) {
	return str.indexOf(suffix, str.length - suffix.length) !== -1;
}
/**
 * Return a number which can be validated
 * 
 * @param n
 * @returns
 */
function formatNumber(n) {
	return n.replace(",", '.').replace(" ", '');
	;
}
/**
 * true if the input is a number
 * 
 * @param n
 * @returns {Boolean}
 */
function isNumber(n) {
	n = formatNumber(n);
	return !isNaN(parseFloat(n)) && isFinite(n);
}

/**
 * void function to sort select-tags with attribute data-sort="sort" by text
 */
function sortOptionsByText() {
	var selectElement, optionElements, options;
	selectElement = jQuery('select[data-sort="sort"]');
	if (selectElement) {
		optionElements = jQuery(selectElement.find('option'));
		options = jQuery.makeArray(optionElements).sort(function(a, b) {
			return (a.innerHTML > b.innerHTML) ? 1 : -1;
		});
		selectElement.html(options);
	}
};

/*
 * global function to load content via ajax, function use jQuery the callback
 * function get the target and returndata
 */
function loadContent(loadURL, target, callback) {
	$.ajax({
		type : "GET",
		url : loadURL,
		cache : false,
		success : function(returndata) {
			if (target) {
				$(target).html(returndata);
			}
			if (callback) {
				setTimeout(callback, 15, target, returndata);
			}
		}
	});
}

/**
 * Validate the value of the imput number used in
 * resources/components/list/batchEditList_singleStatement.xhtml
 * 
 * @param input
 */
function validateInputNumber(input) {
	input.value = formatNumber(input.value);
	if (!isNumber(input.value)) {
		input.value = '';
	}
}

/**
 * Parse the id define in the css class by id_class
 * 
 * @param classname
 * @param [classRef]
 * @returns
 */
function parseId(classname, classRef) {
	var ptn;
	if (classRef) {
		ptn = new RegExp(classRef + '(\\S*)');
	} else {
		ptn = /id_(\S*)/;
	}
	ptn.exec(classname);
	if (RegExp.$1 !== "") {
		return RegExp.$1;
	} else {
		return false;
	}
};

/**
 * check if items given with the parent id. It's triggered through mouse-over
 * event in highlighter function.
 * 
 * @param id
 */
function checkForChilds(id) {
	var childs, curId;
	// all childs have a parent-class with the parent id
	childs = jQuery('.parent_' + id);

	if (childs.length > 0) { // if childs given
		childs.each(function(i, obj) { // loop through the childs an check if
			// themselves have also childs
			jQuery(this).addClass('imj_highlightDependencies');
			curId = parseId(jQuery(this).attr('class'));
			if (curId) {
				checkForChilds(curId);
			}
		});
	}
};

/**
 * Highlight the element with id passed in the parameter. If it has children
 * highlight them. This method should be triggered on mouse over. This element
 * is then recognized by the css class "id_ +id"
 * 
 * @param id
 * @param [alter]
 */
function highlight(id, alter) {
	var items;
	items = (alter) ? $('.' + alter + id) : $('.id_' + id);
	items.addClass("imj_highlightDependencies");
};
/**
 * Reset highlighted element to their original value. Should be triggered on
 * mouse out DELETE FUNCTION HIGHLIGHT + DEPENDENCIES and create/use css
 * definitions
 */
function reset_highlight() {
	$('.imj_highlightDependencies').removeClass("imj_highlightDependencies");
};
/**
 * JQuery event for Highlight methods used in:
 * templates/component/images/image_details.xhtml, used in:
 * templates/sub_template/template_metadata_profileEdit.xhtml
 */
function highlighter() {

	var areas = $(".highlight_area");

	// in case of ajax reloading and other dynamics
	// remove the old mouseover/mouseout events and attach them new
	areas.off("mouseover");
	areas.off("mouseout");
	areas.mouseover(function() {
		var itemId = parseId(jQuery(this).attr('class'));
		var prntId = parseId(jQuery(this).attr('class'), 'parent_');
		highlight(itemId); // highlight the current element
		checkForChilds(itemId);// highlight the child elements - recursive
		// - of the current item, if it's given
		if (prntId) { // check if the current item is a child of another
			highlight(prntId, 'id_'); // hightlight the next parent item,
			// if the current item is a child
		}
	}).mouseout(function() {
		reset_highlight();
	});

};

/**
 * When a confirmation is confirmed, make the panel emty until the method called
 * is done
 * 
 * @param panelId
 * @param message
 */
// seems to be unused - March 31th, 2014
function submitPanel(panelId, message) {
	var panel = document.getElementById(panelId);
	if (panel != null) {
		panel.innerHTML = '<h2><span class="free_area0_p8 xTiny_marginLExcl">'
				+ message + '</span></h2>';
	}
}

/*
 * open a dialog functions are shifted and modified from old template.xhtml
 */
function openDialog(id) {
	/* set the dialog in center of the screen */
	var dialog = $(document.getElementById(id));
	dialog.css("left", Math.max(0,
			Math
					.round(($(window).width() - $(dialog)
							.outerWidth()) / 2)
					+ $(window).scrollLeft())
			+ "px");
	/* open the dialog */
	dialog.show();
	$(".imj_modalDialogBackground").show();
}
/* close a dialog */
function closeDialog(id) {
	var dialog = $(document.getElementById(id));
	$(".imj_modalDialogBackground").hide();
	dialog.hide();
}

$(window).resize(
		function(evt) {
			var dialog = $('.imj_modalDialogBox:visible');
			if (dialog.length > 0) {
				dialog.css("left", Math.max(0,
						Math
								.round(($(window).width() - $(dialog)
										.outerWidth()) / 2)
								+ $(window).scrollLeft())
						+ "px");
			}
		});

// Initialize a global swc object for easy handling
var swcObject = {};

/*
 * initialize the rendering of a SWC file @param swcdata: swc file content in
 * clear format
 */
function initSWC(swcdomelement) {
	var shark, canvas, placeholder;
	swcObject.data = $(swcdomelement).text();
	swcObject.json = swc_parser(swcObject.data);
	canvas = document.createElement('canvas');

	if (window.WebGLRenderingContext
			&& (canvas.getContext("webgl") || canvas
					.getContext("experimental-webgl"))) {
		placeholder = $('*[id*=' + swcObject.placeholderID + ']');
		placeholder.get(0).style.display = "none";
		shark = new SharkViewer({
			swc : swcObject.json,
			dom_element : swcObject.displayID,
			WIDTH : swcObject.width,
			HEIGHT : swcObject.height,
			center_node : -1,
			show_stats : false,
			screenshot : false
		});
		shark.init();
		shark.animate();
	} else {
		document.getElementById(swcObject.failedMsgID).style.display = "block";
	}
}

/*
 * start function to load the SWC file @param src: dom-source element with
 * parameter
 */
function loadSWC(src, element_name) {
	var source, swc;
	source = $(src);
	swcObject = {
		domSource : src,
		dataURL : source.data("swc-source") || undefined,
		serviceURL : source.data("swc-service") || undefined,
		elementID : element_name,
		displayID : (source.data("target-id")[0] === '#') ? source.data(
				"target-id").substring(1) : source.data("target-id"),
		width : source.data("target-width"),
		height : source.data("target-height"),
		placeholderID : (source.data("placeholder-id")[0] === '#') ? source
				.data("placeholder-id").substring(1) : source
				.data("placeholder-id"),
		failedMsgID : (source.data("failed-msg-id")[0] === '#') ? source.data(
				"failed-msg-id").substring(1) : source.data("failed-msg-id")
	};
	// loadContent(swcObject.dataURL, '#'+swcObject.elementID, initSWC);
	initSWC('#' + swcObject.elementID);
}

/**
 * Avoid double click submit for all submit buttons
 * 
 * @param data
 */
function handleDisableButton(data) {
	if (data.source.type !== "submit") {
		return;
	}

	switch (data.status) {
	case "begin":
		data.source.disabled = true;
		break;
	case "complete":
		data.source.disabled = false;
		break;
	}
}

/** START * */
if (typeof jsf !== 'undefined') {
	jsf.ajax.addOnEvent(function(data) {
		if (data.status === "success") {
			fixViewState(data.responseXML);
		}
		handleDisableButton(data);
	});
}

function fixViewState(responseXML) {
	var viewState = getViewState(responseXML);

	if (viewState) {
		for (var i = 0; i < document.forms.length; i++) {
			var form = document.forms[i];

			if (form.method.toLowerCase() === "post") {
				if (!hasViewState(form)) {
					createViewState(form, viewState);
				}
			} else { // PrimeFaces also adds them to GET forms!
				removeViewState(form);
			}
		}
	}
}

function getViewState(responseXML) {
	var updates = responseXML.getElementsByTagName("update");

	for (var i = 0; i < updates.length; i++) {
		var update = updates[i];

		if (update.getAttribute("id").match(
				/^([\w]+:)?javax\.faces\.ViewState(:[0-9]+)?$/)) {
			return update.firstChild.nodeValue;
		}
	}

	return null;
}

function hasViewState(form) {
	for (var i = 0; i < form.elements.length; i++) {
		if (form.elements[i].name == "javax.faces.ViewState") {
			return true;
		}
	}

	return false;
}

function createViewState(form, viewState) {
	var hidden;

	try {
		hidden = document.createElement("<input name='javax.faces.ViewState'>"); // IE6-8.
	} catch (e) {
		hidden = document.createElement("input");
		hidden.setAttribute("name", "javax.faces.ViewState");
	}

	hidden.setAttribute("type", "hidden");
	hidden.setAttribute("value", viewState);
	hidden.setAttribute("autocomplete", "off");
	form.appendChild(hidden);
}

function removeViewState(form) {
	for (var i = 0; i < form.elements.length; i++) {
		var element = form.elements[i];
		if (element.name == "javax.faces.ViewState") {
			element.parentNode.removeChild(element);
		}
	}
}

/** END * */

/**
 * call init
 * -----------------------------------------------------------------------------
 */
/*
 * Extended usability function to set the content width of overlay menu to the
 * minimum of the trigger width. It will be called one time after page loading
 * is finished
 */
$(function() {
	$('.imj_overlayMenu').each(function(i, obj) {
		var menuHeaderWidth = $(this).find(".imj_menuHeader").width();
		var menuBody = $(this).find(".imj_menuBody");
		if (menuHeaderWidth > menuBody.width()) {
			menuBody.width(menuHeaderWidth);
		}
	});
});
/*
 * For menu on the right side: set the margin of the body to avoid to be out of page
 */
function menuRightOffset(){
	$('.imj_overlayMenu.imj_menuRight').each(function(i, obj) {
		var menuHeaderWidth = $(this).find(".imj_menuHeader").width();
		var menuBodyWidth = $(this).find(".imj_menuBody").width();
		var width = menuHeaderWidth - menuBodyWidth;
		$(this).find(".imj_menuBody").css("margin-left",width + "px");
	});
}

/**
 * Method called when page is ready
 */
jQuery(document).ready(function() {
	highlighter();
	menuRightOffset();
});


/*******************************************************************************
 * 
 * SIMPLE SEARCH
 * 
 ******************************************************************************/
var selectedSearch;
var numberOfContext = $('.imj_bodyContextSearch li').length;


/**
 * Trigger the simple search, according to the currently selected context
 * @returns {Boolean}
 */
function submitSimpleSearch() {
	if ($('#simpleSearchInputText').val() != '') {
		goToSearch(selectedSearch);
	}
	return false;
};

$(".imj_bodyContextSearch li").click(function(){
	 goToSearch($(this).index() + 1);
});
/**
 * Open a search page according to the type 
 * @param type
 */
function goToSearch(index) {
	var appendChar="?";
	var url=$('.imj_bodyContextSearch li:nth-child('+ index +')').data('url');
	if(url.includes("?")){
		appendChar="&";
	}
	window.open(url + appendChar+'q=' + encodeURIComponent($('#simpleSearchInputText').val()),
	"_self");
};

/**
 * Actions for the search menu: open, navigate with array keys
 */
$("#simpleSearchInput").focusin(function() {
	$(".imj_menuSimpleSearch").show();
}).keyup(function(event) {
	if (event.which == 40) {
		incrementSelectedSearch();
		highlightSearch();
	}
	else if (event.which == 38) {
		decrementSelectedSearch();
		highlightSearch();
	}
	else if ($(this).val() != '') {
		$(".imj_menuSimpleSearch").show();
	}
});

// Set the correct context for the search according to the current page
$( document ).ready(function() {
	selectedSearch = 1;
	var path = window.location.pathname;
	$("ul.imj_bodyContextSearch li" ).each(function( index ) {
		if($(this).data('url').indexOf(path) !== -1){
			selectedSearch = index + 1;
			return false;
		}
	});
	highlightSearch();
});

function changePlaceholder(){
	var placeholder = $("ul.imj_bodyContextSearch li:nth-child(" + selectedSearch + ")").data('placeholder');
	$("#simpleSearchInputText").attr("placeholder", placeholder);
}

/**
 * Close the search menu
 */
$(".imj_simpleSearch").focusout(function() {
	$(".imj_menuSimpleSearch").delay(200).hide(0);
	
});
/**
 * On mouse over, unselect the previously selected menu
 */
$("ul.imj_bodyContextSearch li").mouseover(function() {
	//$(".hovered").removeClass("hovered");
	//selectedSearch = $(this).index() +1;
});

$("ul.imj_bodyContextSearch li").mouseout(function() {
	highlightSearch();
});

/**
 * Highlight the currently selected search
 */
function highlightSearch() {
	$("ul.imj_bodyContextSearch li").removeClass("hovered");
	$("ul.imj_bodyContextSearch li:nth-child(" + selectedSearch + ")").addClass("hovered");
	changePlaceholder();
}
/**
 * Select the next search 
 */
function incrementSelectedSearch() {
	if (selectedSearch < numberOfContext) {
		selectedSearch = selectedSearch + 1;
	}
}
/**
 * Select the previous search
 */
function decrementSelectedSearch() {
	if (selectedSearch > 1) {
		selectedSearch = selectedSearch - 1;
	}
}

/*******************************************************************************
 * 
 * END - SIMPLE SEARCH
 * 
 ******************************************************************************/


function lockPage(){
	$(".loaderWrapper").show();
}

function unlockPage(){
	$(".loaderWrapper").hide();
}

// LOADER
function startLoader(){
	$(".loaderWrapper").show();
	$(".loader").show();
}

//LOADER
function stopLoader(){
	$(".loaderWrapper").hide();
	$(".loader").hide();
}

// JSF AJAX EVENTS
jsf.ajax.addOnEvent(function(data) {
    var ajaxstatus = data.status; // Can be "begin", "complete" and "success"
    switch (ajaxstatus) {
        case "begin":
            break;
        case "complete":
        	stopLoader();
        	break;

        case "success":
            break;
    }
});

// Edit selected items table
$(function(){
    $(".scrollbarTopWrapper").scroll(function(){
        $(".edit_selected_table_wrapper")
            .scrollLeft($(".scrollbarTopWrapper").scrollLeft());
    });
    $(".edit_selected_table_wrapper").scroll(function(){
        $(".scrollbarTopWrapper")
            .scrollLeft($(".edit_selected_table_wrapper").scrollLeft());
    });
});
//Close success message after 2s
setTimeout(function() {
    $('.imj_messageSuccess').slideUp(200);
}, 2000);

function closeSuccessMessage(){
setTimeout(function() {
    $('.imj_messageSuccess').slideUp(200);
}, 2000);
	
}

function showAndFocus(focusid, showClass){
	$("."+ showClass).toggle();
	document.getElementById(focusid).focus();
	/*$("." + showClass).focusout(function(event) {
		$("." + showClass).hide(0);
		$("." + showClass).delay(500).hide(0);
	});	*/
}

$(".selectMetadata-content").click(function(e) {
	  e.stopPropagation(); //stops click event from reaching document
	});

$(document).click(function() {
  $(".selectMetadata-content").hide(); //click came from somewhere else
});

function hideOnFocusOut(showClass){
	/*$("." + showClass).focusout(function(event) {
		console.log('focusout');
		$("."+ showClass).delay(0).queue(function() {
	        $(this).css('visibility', 'hidden').dequeue();
	    });
		//$("." + showClass).delay(200).hide(0);
	});	*/
}

$(".selectMetadata-content").focusout(function(event) {
	$(this).delay(200).hide(0);
});	