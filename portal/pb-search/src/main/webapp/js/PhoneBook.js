var Manager;
(function ($) {

  //Initialize
  $( document ).ready(function() {
    $( "#pbSearchInput" ).focus();
    init();
  });

  $(function () {
    Manager = new AjaxSolr.Manager({
      solrUrl: 'http://localhost:8080/search-service/'
    });

    Manager.addWidget(new AjaxSolr.ResultWidget({
      id: 'result',
      target: '#pbDocs'
    }));

    Manager.addWidget(new AjaxSolr.SearchWidget({
      id: 'text',
      target: '#pbSearch'
    }));

    Manager.addWidget(new AjaxSolr.DetailLinkSearchWidget({
      id: 'detailLink',
      target: '#ddResult'
    }));

    Manager.addWidget(new AjaxSolr.PagerWidget({
      id: 'pager',
      target: '#pbPager',
      prevLabel: '<span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>',
      nextLabel: '<span class="glyphicon glyphicon-chevron-right" aria-hidden="true"></span>',
      innerWindow: 1,
      renderHeader: function (perPage, offset, total) {
        $('#pbPager-header').html('<span>Displaying ' + Math.min(total, offset + 1) + ' to ' + Math.min(total, offset + perPage) + ' of <span class="badge">' + total + '</span> results.' + '</br></span>');
      }
    }));

    Manager.init();
    Manager.store.addByValue('sort', 'person_sort_name asc');
    Manager.store.addByValue('q', 'data_class:PDS_Affiliate');
    Manager.doRequest(false, "search");


   /* Manager.addWidget(new AjaxSolr.DetailLinkSearchWidget({
      id: 'detailLink',
      target: '#ddResult'
    }));

    Manager.addWidget(new AjaxSolr.PagerWidget({
      id: 'pager',
      target: '#ddPager',
      prevLabel: '<span class="glyphicon glyphicon-chevron-left" aria-hidden="true"></span>',
      nextLabel: '<span class="glyphicon glyphicon-chevron-right" aria-hidden="true"></span>',
      innerWindow: 1,
      renderHeader: function (perPage, offset, total) {
        $('#ddPager-header').html('<span>Displaying ' + Math.min(total, offset + 1) + ' to ' + Math.min(total, offset + perPage) + ' of <span class="badge">' + total + '</span> results.' + '</br></span>');
      }
    }));*/

   /* Manager.init();
    Manager.store.addByValue('product-class', 'Product_Attribute_Definition');
    Manager.store.addByValue('sort', 'attribute_name asc');
    Manager.doRequest(false, "search");*/
  });

})(jQuery);

function init(){
  //Hide Detail Div
  $( "#pbDetailDiv" ).hide();

  //Set all radio Buttons to default:
  /*$('#sortOrderRadio2Div' ).attr("class", "radio");
  $('#sortOrderRadio2' ).attr('disabled', false);

  $("#sortOrderRadio1").prop("checked", true);
  $("#sortOrderRadio2").prop("checked", false);

  $("#objectTypeRadio1").prop("checked", true);
  $("#objectTypeRadio2").prop("checked", false);

  $("#pdsOrderRadio1").prop("checked", true);
  $("#pdsOrderRadio2").prop("checked", false);
  $("#pdsOrderRadio3").prop("checked", false);

  $('html,body').scrollTop(0);*/
}