(window.webpackJsonp=window.webpackJsonp||[]).push([[1],{11:function(e,t,n){"use strict";n.r(t),n.d(t,"default",(function(){return b}));var r=n(0),o=n(13),a=n(1),i=function(){},c=function(e,t,n,r){return new(n||(n=Promise))((function(o,a){function i(e){try{u(r.next(e))}catch(e){a(e)}}function c(e){try{u(r.throw(e))}catch(e){a(e)}}function u(e){var t;e.done?o(e.value):(t=e.value,t instanceof n?t:new n((function(e){e(t)}))).then(i,c)}u((r=r.apply(e,t||[])).next())}))},u=function(e,t){var n,r,o,a,i={label:0,sent:function(){if(1&o[0])throw o[1];return o[1]},trys:[],ops:[]};return a={next:c(0),throw:c(1),return:c(2)},"function"==typeof Symbol&&(a[Symbol.iterator]=function(){return this}),a;function c(a){return function(c){return function(a){if(n)throw new TypeError("Generator is already executing.");for(;i;)try{if(n=1,r&&(o=2&a[0]?r.return:a[0]?r.throw||((o=r.return)&&o.call(r),0):r.next)&&!(o=o.call(r,a[1])).done)return o;switch(r=0,o&&(a=[2&a[0],o.value]),a[0]){case 0:case 1:o=a;break;case 4:return i.label++,{value:a[1],done:!1};case 5:i.label++,r=a[1],a=[0];continue;case 7:a=i.ops.pop(),i.trys.pop();continue;default:if(!(o=(o=i.trys).length>0&&o[o.length-1])&&(6===a[0]||2===a[0])){i=0;continue}if(3===a[0]&&(!o||a[1]>o[0]&&a[1]<o[3])){i.label=a[1];break}if(6===a[0]&&i.label<o[1]){i.label=o[1],o=a;break}if(o&&i.label<o[2]){i.label=o[2],i.ops.push(a);break}o[2]&&i.ops.pop(),i.trys.pop();continue}a=t.call(e,i)}catch(e){a=[6,e],r=0}finally{n=o=0}if(5&a[0])throw a[1];return{value:a[0]?a[1]:void 0,done:!0}}([a,c])}}};function s(e,t){return c(this,void 0,void 0,(function(){return u(this,(function(n){return[2,new Promise((function(n,r){!function(e){var t=window.hasOwnProperty("XDomainRequest"),n=t?new window.XDomainRequest:new XMLHttpRequest;if(n.open(e.type||"GET",e.url,!0),n.success=e.success||i,n.fail=e.fail||i,n.withCredentials=!!e.withCredentials,t?(n.onload=e.success||i,n.onerror=e.fail||i,n.onprogress=i):n.onreadystatechange=function(){if(4==n.readyState)if(n.status>=200)try{var t=JSON.parse(n.responseText);e.success&&e.success(t)}catch(t){e.fail&&e.fail(t)}else e.fail&&e.fail(n.statusText)},"POST"===e.type){if(e.header&&!t)for(var r in e.header)e.header.hasOwnProperty(r)&&n.setRequestHeader(r,e.header[r]);n.send(e.data)}else n.send()}({url:e,type:"POST",data:JSON.stringify(t),withCredentials:!0,header:{"Content-Type":"application/json",Accept:"application/json,text/javascript"},success:function(e){n(e)},fail:function(e){r(e||new Error("Ajax error"))}})}))]}))}))}var l,f=n(2),p=function(){return(p=Object.assign||function(e){for(var t,n=1,r=arguments.length;n<r;n++)for(var o in t=arguments[n])Object.prototype.hasOwnProperty.call(t,o)&&(e[o]=t[o]);return e}).apply(this,arguments)},h=function(e,t,n,r){return new(n||(n=Promise))((function(o,a){function i(e){try{u(r.next(e))}catch(e){a(e)}}function c(e){try{u(r.throw(e))}catch(e){a(e)}}function u(e){var t;e.done?o(e.value):(t=e.value,t instanceof n?t:new n((function(e){e(t)}))).then(i,c)}u((r=r.apply(e,t||[])).next())}))},d=function(e,t){var n,r,o,a,i={label:0,sent:function(){if(1&o[0])throw o[1];return o[1]},trys:[],ops:[]};return a={next:c(0),throw:c(1),return:c(2)},"function"==typeof Symbol&&(a[Symbol.iterator]=function(){return this}),a;function c(a){return function(c){return function(a){if(n)throw new TypeError("Generator is already executing.");for(;i;)try{if(n=1,r&&(o=2&a[0]?r.return:a[0]?r.throw||((o=r.return)&&o.call(r),0):r.next)&&!(o=o.call(r,a[1])).done)return o;switch(r=0,o&&(a=[2&a[0],o.value]),a[0]){case 0:case 1:o=a;break;case 4:return i.label++,{value:a[1],done:!1};case 5:i.label++,r=a[1],a=[0];continue;case 7:a=i.ops.pop(),i.trys.pop();continue;default:if(!(o=(o=i.trys).length>0&&o[o.length-1])&&(6===a[0]||2===a[0])){i=0;continue}if(3===a[0]&&(!o||a[1]>o[0]&&a[1]<o[3])){i.label=a[1];break}if(6===a[0]&&i.label<o[1]){i.label=o[1],o=a;break}if(o&&i.label<o[2]){i.label=o[2],i.ops.push(a);break}o[2]&&i.ops.pop(),i.trys.pop();continue}a=t.call(e,i)}catch(e){a=[6,e],r=0}finally{n=o=0}if(5&a[0])throw a[1];return{value:a[0]?a[1]:void 0,done:!0}}([a,c])}}};function b(e){return h(this,void 0,void 0,(function(){var t,n,i,c,u,b,y,w;return d(this,(function(g){switch(g.label){case 0:if(o.b.idbIsSupported())return[3,1];throw new Error(r.b.DB_NOT_SUPPORT);case 1:return l||(l=new o.b(a.a.get("dbName"))),[4,l.getLogDaysInfo(e.fromDayString,e.toDayString)];case 2:t=g.sent(),n=t.reduce((function(e,t){var n;return p(((n={})[t[o.a]]=t.reportPagesInfo.pageSizes.map((function(e,n){return l.logReportNameFormatter(t[o.a],n)})),n),e)}),{}),i={},c=Object(f.d)(e.fromDayString),u=Object(f.d)(e.toDayString),b=+c,g.label=3;case 3:if(!(b<=+u))return[3,10];if(y=Object(f.c)(new Date(b)),!n[y])return[3,8];g.label=4;case 4:return g.trys.push([4,6,,7]),[4,Promise.all(n[y].map((function(t){return function(e,t){return h(this,void 0,void 0,(function(){var n,r;return d(this,(function(o){switch(o.label){case 0:return[4,l.getLogsByReportName(e)];case 1:return n=o.sent(),r=l.logReportNameParser(e),[4,s(t.reportUrl||a.a.get("reportUrl"),{client:"Web",webSource:""+(t.webSource||""),deviceId:t.deviceId,environment:""+(t.environment||""),customInfo:""+(t.customInfo||""),logPageNo:r.pageIndex+1,fileDate:r.logDay,logArray:n.map((function(e){return encodeURIComponent(e.logString)})).toString()})];case 2:return[2,o.sent()]}}))}))}(t,e)})))];case 5:return g.sent().forEach((function(e){if(200!==e.code)throw new Error("Server error: "+e.code)})),i[y]={msg:r.b.REPORT_LOG_SUCC},[3,7];case 6:return w=g.sent(),i[y]={msg:r.b.REPORT_LOG_FAIL,desc:w.message||w.stack||JSON.stringify(w)},[3,7];case 7:return[3,9];case 8:i[y]={msg:r.b.NO_LOG},g.label=9;case 9:return b+=f.b,[3,3];case 10:return[2,i]}}))}))}}}]);