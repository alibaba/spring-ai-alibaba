(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[9614],{7410:function(t){t.exports=function(t,e){var i=e.prototype,n=i.format;i.format=function(t){var e=this,i=this.$locale();if(!this.isValid())return n.bind(this)(t);var s=this.$utils(),r=(t||"YYYY-MM-DDTHH:mm:ssZ").replace(/\[([^\]]+)]|Q|wo|ww|w|WW|W|zzz|z|gggg|GGGG|Do|X|x|k{1,2}|S/g,function(t){switch(t){case"Q":return Math.ceil((e.$M+1)/3);case"Do":return i.ordinal(e.$D);case"gggg":return e.weekYear();case"GGGG":return e.isoWeekYear();case"wo":return i.ordinal(e.week(),"W");case"w":case"ww":return s.s(e.week(),"w"===t?1:2,"0");case"W":case"WW":return s.s(e.isoWeek(),"W"===t?1:2,"0");case"k":case"kk":return s.s(String(0===e.$H?24:e.$H),"k"===t?1:2,"0");case"X":return Math.floor(e.$d.getTime()/1e3);case"x":return e.$d.getTime();case"z":return"["+e.offsetName()+"]";case"zzz":return"["+e.offsetName("long")+"]";default:return t}});return n.bind(this)(r)}}},49980:function(t){t.exports=function(t,e,i){var n=function(t){return t.add(4-t.isoWeekday(),"day")},s=e.prototype;s.isoWeekYear=function(){return n(this).year()},s.isoWeek=function(t){if(!this.$utils().u(t))return this.add(7*(t-this.isoWeek()),"day");var e,s,r,a=n(this),o=(e=this.isoWeekYear(),r=4-(s=(this.$u?i.utc:i)().year(e).startOf("year")).isoWeekday(),s.isoWeekday()>4&&(r+=7),s.add(r,"day"));return a.diff(o,"week")+1},s.isoWeekday=function(t){return this.$utils().u(t)?this.day()||7:this.day(this.day()%7?t:t-7)};var r=s.startOf;s.startOf=function(t,e){var i=this.$utils(),n=!!i.u(e)||e;return"isoweek"===i.p(t)?n?this.date(this.date()-(this.isoWeekday()-1)).startOf("day"):this.date(this.date()-1-(this.isoWeekday()-1)+7).endOf("day"):r.bind(this)(t,e)}}},67600:function(t){t.exports=function(){"use strict";var t={LTS:"h:mm:ss A",LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D, YYYY",LLL:"MMMM D, YYYY h:mm A",LLLL:"dddd, MMMM D, YYYY h:mm A"},e=/(\[[^[]*\])|([-_:/.,()\s]+)|(A|a|Q|YYYY|YY?|ww?|MM?M?M?|Do|DD?|hh?|HH?|mm?|ss?|S{1,3}|z|ZZ?)/g,i=/\d/,n=/\d\d/,s=/\d\d?/,r=/\d*[^-_:/,()\s\d]+/,a={},o=function(t){return(t*=1)+(t>68?1900:2e3)},c=function(t){return function(e){this[t]=+e}},l=[/[+-]\d\d:?(\d\d)?|Z/,function(t){(this.zone||(this.zone={})).offset=function(t){if(!t||"Z"===t)return 0;var e=t.match(/([+-]|\d\d)/g),i=60*e[1]+(+e[2]||0);return 0===i?0:"+"===e[0]?-i:i}(t)}],d=function(t){var e=a[t];return e&&(e.indexOf?e:e.s.concat(e.f))},u=function(t,e){var i,n=a.meridiem;if(n){for(var s=1;s<=24;s+=1)if(t.indexOf(n(s,0,e))>-1){i=s>12;break}}else i=t===(e?"pm":"PM");return i},h={A:[r,function(t){this.afternoon=u(t,!1)}],a:[r,function(t){this.afternoon=u(t,!0)}],Q:[i,function(t){this.month=3*(t-1)+1}],S:[i,function(t){this.milliseconds=100*t}],SS:[n,function(t){this.milliseconds=10*t}],SSS:[/\d{3}/,function(t){this.milliseconds=+t}],s:[s,c("seconds")],ss:[s,c("seconds")],m:[s,c("minutes")],mm:[s,c("minutes")],H:[s,c("hours")],h:[s,c("hours")],HH:[s,c("hours")],hh:[s,c("hours")],D:[s,c("day")],DD:[n,c("day")],Do:[r,function(t){var e=a.ordinal,i=t.match(/\d+/);if(this.day=i[0],e)for(var n=1;n<=31;n+=1)e(n).replace(/\[|\]/g,"")===t&&(this.day=n)}],w:[s,c("week")],ww:[n,c("week")],M:[s,c("month")],MM:[n,c("month")],MMM:[r,function(t){var e=d("months"),i=(d("monthsShort")||e.map(function(t){return t.slice(0,3)})).indexOf(t)+1;if(i<1)throw Error();this.month=i%12||i}],MMMM:[r,function(t){var e=d("months").indexOf(t)+1;if(e<1)throw Error();this.month=e%12||e}],Y:[/[+-]?\d+/,c("year")],YY:[n,function(t){this.year=o(t)}],YYYY:[/\d{4}/,c("year")],Z:l,ZZ:l};return function(i,n,s){s.p.customParseFormat=!0,i&&i.parseTwoDigitYear&&(o=i.parseTwoDigitYear);var r=n.prototype,c=r.parse;r.parse=function(i){var n=i.date,r=i.utc,o=i.args;this.$u=r;var l=o[1];if("string"==typeof l){var d=!0===o[2],u=!0===o[3],f=o[2];u&&(f=o[2]),a=this.$locale(),!d&&f&&(a=s.Ls[f]),this.$d=function(i,n,s,r){try{if(["x","X"].indexOf(n)>-1)return new Date(("X"===n?1e3:1)*i);var o=(function(i){var n,s;n=i,s=a&&a.formats;for(var r=(i=n.replace(/(\[[^\]]+])|(LTS?|l{1,4}|L{1,4})/g,function(e,i,n){var r=n&&n.toUpperCase();return i||s[n]||t[n]||s[r].replace(/(\[[^\]]+])|(MMMM|MM|DD|dddd)/g,function(t,e,i){return e||i.slice(1)})})).match(e),o=r.length,c=0;c<o;c+=1){var l=r[c],d=h[l],u=d&&d[0],f=d&&d[1];r[c]=f?{regex:u,parser:f}:l.replace(/^\[|\]$/g,"")}return function(t){for(var e={},i=0,n=0;i<o;i+=1){var s=r[i];if("string"==typeof s)n+=s.length;else{var a=s.regex,c=s.parser,l=t.slice(n),d=a.exec(l)[0];c.call(e,d),t=t.replace(d,"")}}return function(t){var e=t.afternoon;if(void 0!==e){var i=t.hours;e?i<12&&(t.hours+=12):12===i&&(t.hours=0),delete t.afternoon}}(e),e}})(n)(i),c=o.year,l=o.month,d=o.day,u=o.hours,f=o.minutes,m=o.seconds,k=o.milliseconds,y=o.zone,g=o.week,p=new Date,x=d||(c||l?1:p.getDate()),v=c||p.getFullYear(),T=0;c&&!l||(T=l>0?l-1:p.getMonth());var b,w=u||0,$=f||0,_=m||0,D=k||0;return y?new Date(Date.UTC(v,T,x,w,$,_,D+60*y.offset*1e3)):s?new Date(Date.UTC(v,T,x,w,$,_,D)):(b=new Date(v,T,x,w,$,_,D),g&&(b=r(b).week(g).toDate()),b)}catch(t){return new Date("")}}(n,l,r,s),this.init(),f&&!0!==f&&(this.$L=this.locale(f).$L),(d||u)&&n!=this.format(l)&&(this.$d=new Date("")),a={}}else if(l instanceof Array)for(var m=l.length,k=1;k<=m;k+=1){o[1]=l[k-1];var y=s.apply(this,o);if(y.isValid()){this.$d=y.$d,this.$L=y.$L,this.init();break}k===m&&(this.$d=new Date(""))}else c.call(this,i)}}}()},79614:(t,e,i)=>{"use strict";i.d(e,{diagram:()=>tZ});var n,s,r,a=i(83900),o=i(11933),c=i(97967),l=i(31905),d=i(61850),u=i(77492),h=i(49980),f=i(67600),m=i(7410),k=i(94215),y=i(19399),g=i(86585),p=i(42828),x=i(88050),v=i(22709);function T(t){return t}function b(t){return"translate("+t+",0)"}function w(t){return"translate(0,"+t+")"}function $(){return!this.__axis}function _(t,e){var i=[],n=null,s=null,r=6,a=6,o=3,c="undefined"!=typeof window&&window.devicePixelRatio>1?0:.5,l=1===t||4===t?-1:1,d=4===t||2===t?"x":"y",u=1===t||3===t?b:w;function h(h){var f=null==n?e.ticks?e.ticks.apply(e,i):e.domain():n,m=null==s?e.tickFormat?e.tickFormat.apply(e,i):T:s,k=Math.max(r,0)+o,y=e.range(),g=+y[0]+c,p=+y[y.length-1]+c,x=(e.bandwidth?function(t,e){return e=Math.max(0,t.bandwidth()-2*e)/2,t.round()&&(e=Math.round(e)),i=>+t(i)+e}:function(t){return e=>+t(e)})(e.copy(),c),v=h.selection?h.selection():h,b=v.selectAll(".domain").data([null]),w=v.selectAll(".tick").data(f,e).order(),_=w.exit(),D=w.enter().append("g").attr("class","tick"),S=w.select("line"),C=w.select("text");b=b.merge(b.enter().insert("path",".tick").attr("class","domain").attr("stroke","currentColor")),w=w.merge(D),S=S.merge(D.append("line").attr("stroke","currentColor").attr(d+"2",l*r)),C=C.merge(D.append("text").attr("fill","currentColor").attr(d,l*k).attr("dy",1===t?"0em":3===t?"0.71em":"0.32em")),h!==v&&(b=b.transition(h),w=w.transition(h),S=S.transition(h),C=C.transition(h),_=_.transition(h).attr("opacity",1e-6).attr("transform",function(t){return isFinite(t=x(t))?u(t+c):this.getAttribute("transform")}),D.attr("opacity",1e-6).attr("transform",function(t){var e=this.parentNode.__axis;return u((e&&isFinite(e=e(t))?e:x(t))+c)})),_.remove(),b.attr("d",4===t||2===t?a?"M"+l*a+","+g+"H"+c+"V"+p+"H"+l*a:"M"+c+","+g+"V"+p:a?"M"+g+","+l*a+"V"+c+"H"+p+"V"+l*a:"M"+g+","+c+"H"+p),w.attr("opacity",1).attr("transform",function(t){return u(x(t)+c)}),S.attr(d+"2",l*r),C.attr(d,l*k).text(m),v.filter($).attr("fill","none").attr("font-size",10).attr("font-family","sans-serif").attr("text-anchor",2===t?"start":4===t?"end":"middle"),v.each(function(){this.__axis=x})}return h.scale=function(t){return arguments.length?(e=t,h):e},h.ticks=function(){return i=Array.from(arguments),h},h.tickArguments=function(t){return arguments.length?(i=null==t?[]:Array.from(t),h):i.slice()},h.tickValues=function(t){return arguments.length?(n=null==t?null:Array.from(t),h):n&&n.slice()},h.tickFormat=function(t){return arguments.length?(s=t,h):s},h.tickSize=function(t){return arguments.length?(r=a=+t,h):r},h.tickSizeInner=function(t){return arguments.length?(r=+t,h):r},h.tickSizeOuter=function(t){return arguments.length?(a=+t,h):a},h.tickPadding=function(t){return arguments.length?(o=+t,h):o},h.offset=function(t){return arguments.length?(c=+t,h):c},h}var D=i(13308),S=function(){var t=(0,l.K)(function(t,e,i,n){for(i=i||{},n=t.length;n--;i[t[n]]=e);return i},"o"),e=[6,8,10,12,13,14,15,16,17,18,20,21,22,23,24,25,26,27,28,29,30,31,33,35,36,38,40],i=[1,26],n=[1,27],s=[1,28],r=[1,29],a=[1,30],o=[1,31],c=[1,32],d=[1,33],u=[1,34],h=[1,9],f=[1,10],m=[1,11],k=[1,12],y=[1,13],g=[1,14],p=[1,15],x=[1,16],v=[1,19],T=[1,20],b=[1,21],w=[1,22],$=[1,23],_=[1,25],D=[1,35],S={trace:(0,l.K)(function(){},"trace"),yy:{},symbols_:{error:2,start:3,gantt:4,document:5,EOF:6,line:7,SPACE:8,statement:9,NL:10,weekday:11,weekday_monday:12,weekday_tuesday:13,weekday_wednesday:14,weekday_thursday:15,weekday_friday:16,weekday_saturday:17,weekday_sunday:18,weekend:19,weekend_friday:20,weekend_saturday:21,dateFormat:22,inclusiveEndDates:23,topAxis:24,axisFormat:25,tickInterval:26,excludes:27,includes:28,todayMarker:29,title:30,acc_title:31,acc_title_value:32,acc_descr:33,acc_descr_value:34,acc_descr_multiline_value:35,section:36,clickStatement:37,taskTxt:38,taskData:39,click:40,callbackname:41,callbackargs:42,href:43,clickStatementDebug:44,$accept:0,$end:1},terminals_:{2:"error",4:"gantt",6:"EOF",8:"SPACE",10:"NL",12:"weekday_monday",13:"weekday_tuesday",14:"weekday_wednesday",15:"weekday_thursday",16:"weekday_friday",17:"weekday_saturday",18:"weekday_sunday",20:"weekend_friday",21:"weekend_saturday",22:"dateFormat",23:"inclusiveEndDates",24:"topAxis",25:"axisFormat",26:"tickInterval",27:"excludes",28:"includes",29:"todayMarker",30:"title",31:"acc_title",32:"acc_title_value",33:"acc_descr",34:"acc_descr_value",35:"acc_descr_multiline_value",36:"section",38:"taskTxt",39:"taskData",40:"click",41:"callbackname",42:"callbackargs",43:"href"},productions_:[0,[3,3],[5,0],[5,2],[7,2],[7,1],[7,1],[7,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[19,1],[19,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,2],[9,2],[9,1],[9,1],[9,1],[9,2],[37,2],[37,3],[37,3],[37,4],[37,3],[37,4],[37,2],[44,2],[44,3],[44,3],[44,4],[44,3],[44,4],[44,2]],performAction:(0,l.K)(function(t,e,i,n,s,r,a){var o=r.length-1;switch(s){case 1:return r[o-1];case 2:case 6:case 7:this.$=[];break;case 3:r[o-1].push(r[o]),this.$=r[o-1];break;case 4:case 5:this.$=r[o];break;case 8:n.setWeekday("monday");break;case 9:n.setWeekday("tuesday");break;case 10:n.setWeekday("wednesday");break;case 11:n.setWeekday("thursday");break;case 12:n.setWeekday("friday");break;case 13:n.setWeekday("saturday");break;case 14:n.setWeekday("sunday");break;case 15:n.setWeekend("friday");break;case 16:n.setWeekend("saturday");break;case 17:n.setDateFormat(r[o].substr(11)),this.$=r[o].substr(11);break;case 18:n.enableInclusiveEndDates(),this.$=r[o].substr(18);break;case 19:n.TopAxis(),this.$=r[o].substr(8);break;case 20:n.setAxisFormat(r[o].substr(11)),this.$=r[o].substr(11);break;case 21:n.setTickInterval(r[o].substr(13)),this.$=r[o].substr(13);break;case 22:n.setExcludes(r[o].substr(9)),this.$=r[o].substr(9);break;case 23:n.setIncludes(r[o].substr(9)),this.$=r[o].substr(9);break;case 24:n.setTodayMarker(r[o].substr(12)),this.$=r[o].substr(12);break;case 27:n.setDiagramTitle(r[o].substr(6)),this.$=r[o].substr(6);break;case 28:this.$=r[o].trim(),n.setAccTitle(this.$);break;case 29:case 30:this.$=r[o].trim(),n.setAccDescription(this.$);break;case 31:n.addSection(r[o].substr(8)),this.$=r[o].substr(8);break;case 33:n.addTask(r[o-1],r[o]),this.$="task";break;case 34:this.$=r[o-1],n.setClickEvent(r[o-1],r[o],null);break;case 35:this.$=r[o-2],n.setClickEvent(r[o-2],r[o-1],r[o]);break;case 36:this.$=r[o-2],n.setClickEvent(r[o-2],r[o-1],null),n.setLink(r[o-2],r[o]);break;case 37:this.$=r[o-3],n.setClickEvent(r[o-3],r[o-2],r[o-1]),n.setLink(r[o-3],r[o]);break;case 38:this.$=r[o-2],n.setClickEvent(r[o-2],r[o],null),n.setLink(r[o-2],r[o-1]);break;case 39:this.$=r[o-3],n.setClickEvent(r[o-3],r[o-1],r[o]),n.setLink(r[o-3],r[o-2]);break;case 40:this.$=r[o-1],n.setLink(r[o-1],r[o]);break;case 41:case 47:this.$=r[o-1]+" "+r[o];break;case 42:case 43:case 45:this.$=r[o-2]+" "+r[o-1]+" "+r[o];break;case 44:case 46:this.$=r[o-3]+" "+r[o-2]+" "+r[o-1]+" "+r[o]}},"anonymous"),table:[{3:1,4:[1,2]},{1:[3]},t(e,[2,2],{5:3}),{6:[1,4],7:5,8:[1,6],9:7,10:[1,8],11:17,12:i,13:n,14:s,15:r,16:a,17:o,18:c,19:18,20:d,21:u,22:h,23:f,24:m,25:k,26:y,27:g,28:p,29:x,30:v,31:T,33:b,35:w,36:$,37:24,38:_,40:D},t(e,[2,7],{1:[2,1]}),t(e,[2,3]),{9:36,11:17,12:i,13:n,14:s,15:r,16:a,17:o,18:c,19:18,20:d,21:u,22:h,23:f,24:m,25:k,26:y,27:g,28:p,29:x,30:v,31:T,33:b,35:w,36:$,37:24,38:_,40:D},t(e,[2,5]),t(e,[2,6]),t(e,[2,17]),t(e,[2,18]),t(e,[2,19]),t(e,[2,20]),t(e,[2,21]),t(e,[2,22]),t(e,[2,23]),t(e,[2,24]),t(e,[2,25]),t(e,[2,26]),t(e,[2,27]),{32:[1,37]},{34:[1,38]},t(e,[2,30]),t(e,[2,31]),t(e,[2,32]),{39:[1,39]},t(e,[2,8]),t(e,[2,9]),t(e,[2,10]),t(e,[2,11]),t(e,[2,12]),t(e,[2,13]),t(e,[2,14]),t(e,[2,15]),t(e,[2,16]),{41:[1,40],43:[1,41]},t(e,[2,4]),t(e,[2,28]),t(e,[2,29]),t(e,[2,33]),t(e,[2,34],{42:[1,42],43:[1,43]}),t(e,[2,40],{41:[1,44]}),t(e,[2,35],{43:[1,45]}),t(e,[2,36]),t(e,[2,38],{42:[1,46]}),t(e,[2,37]),t(e,[2,39])],defaultActions:{},parseError:(0,l.K)(function(t,e){if(e.recoverable)this.trace(t);else{var i=Error(t);throw i.hash=e,i}},"parseError"),parse:(0,l.K)(function(t){var e=this,i=[0],n=[],s=[null],r=[],a=this.table,o="",c=0,d=0,u=0,h=r.slice.call(arguments,1),f=Object.create(this.lexer),m={};for(var k in this.yy)Object.prototype.hasOwnProperty.call(this.yy,k)&&(m[k]=this.yy[k]);f.setInput(t,m),m.lexer=f,m.parser=this,void 0===f.yylloc&&(f.yylloc={});var y=f.yylloc;r.push(y);var g=f.options&&f.options.ranges;function p(){var t;return"number"!=typeof(t=n.pop()||f.lex()||1)&&(t instanceof Array&&(t=(n=t).pop()),t=e.symbols_[t]||t),t}"function"==typeof m.parseError?this.parseError=m.parseError:this.parseError=Object.getPrototypeOf(this).parseError,(0,l.K)(function(t){i.length=i.length-2*t,s.length=s.length-t,r.length=r.length-t},"popStack"),(0,l.K)(p,"lex");for(var x,v,T,b,w,$,_,D,S,C={};;){if(T=i[i.length-1],this.defaultActions[T]?b=this.defaultActions[T]:(null==x&&(x=p()),b=a[T]&&a[T][x]),void 0===b||!b.length||!b[0]){var M="";for($ in S=[],a[T])this.terminals_[$]&&$>2&&S.push("'"+this.terminals_[$]+"'");M=f.showPosition?"Parse error on line "+(c+1)+":\n"+f.showPosition()+"\nExpecting "+S.join(", ")+", got '"+(this.terminals_[x]||x)+"'":"Parse error on line "+(c+1)+": Unexpected "+(1==x?"end of input":"'"+(this.terminals_[x]||x)+"'"),this.parseError(M,{text:f.match,token:this.terminals_[x]||x,line:f.yylineno,loc:y,expected:S})}if(b[0]instanceof Array&&b.length>1)throw Error("Parse Error: multiple actions possible at state: "+T+", token: "+x);switch(b[0]){case 1:i.push(x),s.push(f.yytext),r.push(f.yylloc),i.push(b[1]),x=null,v?(x=v,v=null):(d=f.yyleng,o=f.yytext,c=f.yylineno,y=f.yylloc,u>0&&u--);break;case 2:if(_=this.productions_[b[1]][1],C.$=s[s.length-_],C._$={first_line:r[r.length-(_||1)].first_line,last_line:r[r.length-1].last_line,first_column:r[r.length-(_||1)].first_column,last_column:r[r.length-1].last_column},g&&(C._$.range=[r[r.length-(_||1)].range[0],r[r.length-1].range[1]]),void 0!==(w=this.performAction.apply(C,[o,d,c,m,b[1],s,r].concat(h))))return w;_&&(i=i.slice(0,-1*_*2),s=s.slice(0,-1*_),r=r.slice(0,-1*_)),i.push(this.productions_[b[1]][0]),s.push(C.$),r.push(C._$),D=a[i[i.length-2]][i[i.length-1]],i.push(D);break;case 3:return!0}}return!0},"parse")};function C(){this.yy={}}return S.lexer={EOF:1,parseError:(0,l.K)(function(t,e){if(this.yy.parser)this.yy.parser.parseError(t,e);else throw Error(t)},"parseError"),setInput:(0,l.K)(function(t,e){return this.yy=e||this.yy||{},this._input=t,this._more=this._backtrack=this.done=!1,this.yylineno=this.yyleng=0,this.yytext=this.matched=this.match="",this.conditionStack=["INITIAL"],this.yylloc={first_line:1,first_column:0,last_line:1,last_column:0},this.options.ranges&&(this.yylloc.range=[0,0]),this.offset=0,this},"setInput"),input:(0,l.K)(function(){var t=this._input[0];return this.yytext+=t,this.yyleng++,this.offset++,this.match+=t,this.matched+=t,t.match(/(?:\r\n?|\n).*/g)?(this.yylineno++,this.yylloc.last_line++):this.yylloc.last_column++,this.options.ranges&&this.yylloc.range[1]++,this._input=this._input.slice(1),t},"input"),unput:(0,l.K)(function(t){var e=t.length,i=t.split(/(?:\r\n?|\n)/g);this._input=t+this._input,this.yytext=this.yytext.substr(0,this.yytext.length-e),this.offset-=e;var n=this.match.split(/(?:\r\n?|\n)/g);this.match=this.match.substr(0,this.match.length-1),this.matched=this.matched.substr(0,this.matched.length-1),i.length-1&&(this.yylineno-=i.length-1);var s=this.yylloc.range;return this.yylloc={first_line:this.yylloc.first_line,last_line:this.yylineno+1,first_column:this.yylloc.first_column,last_column:i?(i.length===n.length?this.yylloc.first_column:0)+n[n.length-i.length].length-i[0].length:this.yylloc.first_column-e},this.options.ranges&&(this.yylloc.range=[s[0],s[0]+this.yyleng-e]),this.yyleng=this.yytext.length,this},"unput"),more:(0,l.K)(function(){return this._more=!0,this},"more"),reject:(0,l.K)(function(){return this.options.backtrack_lexer?(this._backtrack=!0,this):this.parseError("Lexical error on line "+(this.yylineno+1)+". You can only invoke reject() in the lexer when the lexer is of the backtracking persuasion (options.backtrack_lexer = true).\n"+this.showPosition(),{text:"",token:null,line:this.yylineno})},"reject"),less:(0,l.K)(function(t){this.unput(this.match.slice(t))},"less"),pastInput:(0,l.K)(function(){var t=this.matched.substr(0,this.matched.length-this.match.length);return(t.length>20?"...":"")+t.substr(-20).replace(/\n/g,"")},"pastInput"),upcomingInput:(0,l.K)(function(){var t=this.match;return t.length<20&&(t+=this._input.substr(0,20-t.length)),(t.substr(0,20)+(t.length>20?"...":"")).replace(/\n/g,"")},"upcomingInput"),showPosition:(0,l.K)(function(){var t=this.pastInput(),e=Array(t.length+1).join("-");return t+this.upcomingInput()+"\n"+e+"^"},"showPosition"),test_match:(0,l.K)(function(t,e){var i,n,s;if(this.options.backtrack_lexer&&(s={yylineno:this.yylineno,yylloc:{first_line:this.yylloc.first_line,last_line:this.last_line,first_column:this.yylloc.first_column,last_column:this.yylloc.last_column},yytext:this.yytext,match:this.match,matches:this.matches,matched:this.matched,yyleng:this.yyleng,offset:this.offset,_more:this._more,_input:this._input,yy:this.yy,conditionStack:this.conditionStack.slice(0),done:this.done},this.options.ranges&&(s.yylloc.range=this.yylloc.range.slice(0))),(n=t[0].match(/(?:\r\n?|\n).*/g))&&(this.yylineno+=n.length),this.yylloc={first_line:this.yylloc.last_line,last_line:this.yylineno+1,first_column:this.yylloc.last_column,last_column:n?n[n.length-1].length-n[n.length-1].match(/\r?\n?/)[0].length:this.yylloc.last_column+t[0].length},this.yytext+=t[0],this.match+=t[0],this.matches=t,this.yyleng=this.yytext.length,this.options.ranges&&(this.yylloc.range=[this.offset,this.offset+=this.yyleng]),this._more=!1,this._backtrack=!1,this._input=this._input.slice(t[0].length),this.matched+=t[0],i=this.performAction.call(this,this.yy,this,e,this.conditionStack[this.conditionStack.length-1]),this.done&&this._input&&(this.done=!1),i)return i;if(this._backtrack)for(var r in s)this[r]=s[r];return!1},"test_match"),next:(0,l.K)(function(){if(this.done)return this.EOF;this._input||(this.done=!0),this._more||(this.yytext="",this.match="");for(var t,e,i,n,s=this._currentRules(),r=0;r<s.length;r++)if((i=this._input.match(this.rules[s[r]]))&&(!e||i[0].length>e[0].length)){if(e=i,n=r,this.options.backtrack_lexer){if(!1!==(t=this.test_match(i,s[r])))return t;if(!this._backtrack)return!1;e=!1;continue}if(!this.options.flex)break}return e?!1!==(t=this.test_match(e,s[n]))&&t:""===this._input?this.EOF:this.parseError("Lexical error on line "+(this.yylineno+1)+". Unrecognized text.\n"+this.showPosition(),{text:"",token:null,line:this.yylineno})},"next"),lex:(0,l.K)(function(){var t=this.next();return t||this.lex()},"lex"),begin:(0,l.K)(function(t){this.conditionStack.push(t)},"begin"),popState:(0,l.K)(function(){return this.conditionStack.length-1>0?this.conditionStack.pop():this.conditionStack[0]},"popState"),_currentRules:(0,l.K)(function(){return this.conditionStack.length&&this.conditionStack[this.conditionStack.length-1]?this.conditions[this.conditionStack[this.conditionStack.length-1]].rules:this.conditions.INITIAL.rules},"_currentRules"),topState:(0,l.K)(function(t){return(t=this.conditionStack.length-1-Math.abs(t||0))>=0?this.conditionStack[t]:"INITIAL"},"topState"),pushState:(0,l.K)(function(t){this.begin(t)},"pushState"),stateStackSize:(0,l.K)(function(){return this.conditionStack.length},"stateStackSize"),options:{"case-insensitive":!0},performAction:(0,l.K)(function(t,e,i,n){switch(i){case 0:return this.begin("open_directive"),"open_directive";case 1:return this.begin("acc_title"),31;case 2:return this.popState(),"acc_title_value";case 3:return this.begin("acc_descr"),33;case 4:return this.popState(),"acc_descr_value";case 5:this.begin("acc_descr_multiline");break;case 6:case 15:case 18:case 21:case 24:this.popState();break;case 7:return"acc_descr_multiline_value";case 8:case 9:case 10:case 12:case 13:break;case 11:return 10;case 14:this.begin("href");break;case 16:return 43;case 17:this.begin("callbackname");break;case 19:this.popState(),this.begin("callbackargs");break;case 20:return 41;case 22:return 42;case 23:this.begin("click");break;case 25:return 40;case 26:return 4;case 27:return 22;case 28:return 23;case 29:return 24;case 30:return 25;case 31:return 26;case 32:return 28;case 33:return 27;case 34:return 29;case 35:return 12;case 36:return 13;case 37:return 14;case 38:return 15;case 39:return 16;case 40:return 17;case 41:return 18;case 42:return 20;case 43:return 21;case 44:return"date";case 45:return 30;case 46:return"accDescription";case 47:return 36;case 48:return 38;case 49:return 39;case 50:return":";case 51:return 6;case 52:return"INVALID"}},"anonymous"),rules:[/^(?:%%\{)/i,/^(?:accTitle\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*\{\s*)/i,/^(?:[\}])/i,/^(?:[^\}]*)/i,/^(?:%%(?!\{)*[^\n]*)/i,/^(?:[^\}]%%*[^\n]*)/i,/^(?:%%*[^\n]*[\n]*)/i,/^(?:[\n]+)/i,/^(?:\s+)/i,/^(?:%[^\n]*)/i,/^(?:href[\s]+["])/i,/^(?:["])/i,/^(?:[^"]*)/i,/^(?:call[\s]+)/i,/^(?:\([\s]*\))/i,/^(?:\()/i,/^(?:[^(]*)/i,/^(?:\))/i,/^(?:[^)]*)/i,/^(?:click[\s]+)/i,/^(?:[\s\n])/i,/^(?:[^\s\n]*)/i,/^(?:gantt\b)/i,/^(?:dateFormat\s[^#\n;]+)/i,/^(?:inclusiveEndDates\b)/i,/^(?:topAxis\b)/i,/^(?:axisFormat\s[^#\n;]+)/i,/^(?:tickInterval\s[^#\n;]+)/i,/^(?:includes\s[^#\n;]+)/i,/^(?:excludes\s[^#\n;]+)/i,/^(?:todayMarker\s[^\n;]+)/i,/^(?:weekday\s+monday\b)/i,/^(?:weekday\s+tuesday\b)/i,/^(?:weekday\s+wednesday\b)/i,/^(?:weekday\s+thursday\b)/i,/^(?:weekday\s+friday\b)/i,/^(?:weekday\s+saturday\b)/i,/^(?:weekday\s+sunday\b)/i,/^(?:weekend\s+friday\b)/i,/^(?:weekend\s+saturday\b)/i,/^(?:\d\d\d\d-\d\d-\d\d\b)/i,/^(?:title\s[^\n]+)/i,/^(?:accDescription\s[^#\n;]+)/i,/^(?:section\s[^\n]+)/i,/^(?:[^:\n]+)/i,/^(?::[^#\n;]+)/i,/^(?::)/i,/^(?:$)/i,/^(?:.)/i],conditions:{acc_descr_multiline:{rules:[6,7],inclusive:!1},acc_descr:{rules:[4],inclusive:!1},acc_title:{rules:[2],inclusive:!1},callbackargs:{rules:[21,22],inclusive:!1},callbackname:{rules:[18,19,20],inclusive:!1},href:{rules:[15,16],inclusive:!1},click:{rules:[24,25],inclusive:!1},INITIAL:{rules:[0,1,3,5,8,9,10,11,12,13,14,17,23,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52],inclusive:!0}}},(0,l.K)(C,"Parser"),C.prototype=S,S.Parser=C,new C}();S.parser=S,u.extend(h),u.extend(f),u.extend(m);var C={friday:5,saturday:6},M="",K="",Y=void 0,E="",A=[],L=[],O=new Map,I=[],F=[],W="",P="",N=["active","done","crit","milestone","vert"],z=[],H="",B=!1,R=!1,G="sunday",j="saturday",V=0,U=(0,l.K)(function(){I=[],F=[],W="",z=[],tC=0,n=void 0,s=void 0,tE=[],M="",K="",P="",Y=void 0,E="",A=[],L=[],B=!1,R=!1,V=0,O=new Map,H="",(0,o.IU)(),G="sunday",j="saturday"},"clear"),Z=(0,l.K)(function(t){H=t},"setDiagramId"),X=(0,l.K)(function(t){K=t},"setAxisFormat"),q=(0,l.K)(function(){return K},"getAxisFormat"),Q=(0,l.K)(function(t){Y=t},"setTickInterval"),J=(0,l.K)(function(){return Y},"getTickInterval"),tt=(0,l.K)(function(t){E=t},"setTodayMarker"),te=(0,l.K)(function(){return E},"getTodayMarker"),ti=(0,l.K)(function(t){M=t},"setDateFormat"),tn=(0,l.K)(function(){B=!0},"enableInclusiveEndDates"),ts=(0,l.K)(function(){return B},"endDatesAreInclusive"),tr=(0,l.K)(function(){R=!0},"enableTopAxis"),ta=(0,l.K)(function(){return R},"topAxisEnabled"),to=(0,l.K)(function(t){P=t},"setDisplayMode"),tc=(0,l.K)(function(){return P},"getDisplayMode"),tl=(0,l.K)(function(){return M},"getDateFormat"),td=(0,l.K)((t,e)=>[...new Set([...t,...e.toLowerCase().split(/[\s,]+/).filter(t=>""!==t)])],"mergeTokens"),tu=(0,l.K)(function(t){A=td(A,t)},"setIncludes"),th=(0,l.K)(function(){return A},"getIncludes"),tf=(0,l.K)(function(t){L=td(L,t)},"setExcludes"),tm=(0,l.K)(function(){return L},"getExcludes"),tk=(0,l.K)(function(){return O},"getLinks"),ty=(0,l.K)(function(t){W=t,I.push(t)},"addSection"),tg=(0,l.K)(function(){return I},"getSections"),tp=(0,l.K)(function(){let t=tF(),e=0;for(;!t&&e<10;)t=tF(),e++;return F=tE},"getTasks"),tx=(0,l.K)(function(t,e,i,n){let s=t.format(e.trim()),r=t.format("YYYY-MM-DD");return!(n.includes(s)||n.includes(r))&&(!!(i.includes("weekends")&&(t.isoWeekday()===C[j]||t.isoWeekday()===C[j]+1)||i.includes(t.format("dddd").toLowerCase()))||i.includes(s)||i.includes(r))},"isInvalidDate"),tv=(0,l.K)(function(t){G=t},"setWeekday"),tT=(0,l.K)(function(){return G},"getWeekday"),tb=(0,l.K)(function(t){j=t},"setWeekend"),tw=(0,l.K)(function(t,e,i,n){let s;if(!i.length||t.manualEndTime)return;let[r,a]=t$(s=(s=t.startTime instanceof Date?u(t.startTime):u(t.startTime,e,!0)).add(1,"d"),t.endTime instanceof Date?u(t.endTime):u(t.endTime,e,!0),e,i,n);t.endTime=r.toDate(),t.renderEndTime=a},"checkTaskDates"),t$=(0,l.K)(function(t,e,i,n,s){let r=!1,a=null,o=e.add(1e4,"d");for(;t<=e;){if(r||(a=e.toDate()),(r=tx(t,i,n,s))&&(e=e.add(1,"d"))>o)throw Error("Failed to find a valid date that was not excluded by `excludes` after 10,000 iterations.");t=t.add(1,"d")}return[e,a]},"fixTaskDates"),t_=(0,l.K)(function(t,e,i){if(i=i.trim(),(0,l.K)(t=>{let e=t.trim();return"x"===e||"X"===e},"isTimestampFormat")(e)&&/^\d+$/.test(i))return new Date(Number(i));let n=/^after\s+(?<ids>[\d\w- ]+)/.exec(i);if(null!==n){let t=null;for(let e of n.groups.ids.split(" ")){let i=tO(e);void 0!==i&&(!t||i.endTime>t.endTime)&&(t=i)}if(t)return t.endTime;let e=new Date;return e.setHours(0,0,0,0),e}let s=u(i,e.trim(),!0);if(s.isValid())return s.toDate();{c.R.debug("Invalid date:"+i),c.R.debug("With date format:"+e.trim());let t=new Date(i);if(void 0===t||isNaN(t.getTime())||-1e4>t.getFullYear()||t.getFullYear()>1e4)throw Error("Invalid date:"+i);return t}},"getStartDate"),tD=(0,l.K)(function(t){let e=/^(\d+(?:\.\d+)?)([Mdhmswy]|ms)$/.exec(t.trim());return null!==e?[Number.parseFloat(e[1]),e[2]]:[NaN,"ms"]},"parseDuration"),tS=(0,l.K)(function(t,e,i,n=!1){i=i.trim();let s=/^until\s+(?<ids>[\d\w- ]+)/.exec(i);if(null!==s){let t=null;for(let e of s.groups.ids.split(" ")){let i=tO(e);void 0!==i&&(!t||i.startTime<t.startTime)&&(t=i)}if(t)return t.startTime;let e=new Date;return e.setHours(0,0,0,0),e}let r=u(i,e.trim(),!0);if(r.isValid())return n&&(r=r.add(1,"d")),r.toDate();let a=u(t),[o,c]=tD(i);if(!Number.isNaN(o)){let t=a.add(o,c);t.isValid()&&(a=t)}return a.toDate()},"getEndDate"),tC=0,tM=(0,l.K)(function(t){return void 0===t?"task"+(tC+=1):t},"parseId"),tK=(0,l.K)(function(t,e){let i=(":"===e.substr(0,1)?e.substr(1,e.length):e).split(","),n={};tG(i,n,N);for(let t=0;t<i.length;t++)i[t]=i[t].trim();let s="";switch(i.length){case 1:n.id=tM(),n.startTime=t.endTime,s=i[0];break;case 2:n.id=tM(),n.startTime=t_(void 0,M,i[0]),s=i[1];break;case 3:n.id=tM(i[0]),n.startTime=t_(void 0,M,i[1]),s=i[2]}return s&&(n.endTime=tS(n.startTime,M,s,B),n.manualEndTime=u(s,"YYYY-MM-DD",!0).isValid(),tw(n,M,L,A)),n},"compileData"),tY=(0,l.K)(function(t,e){let i=(":"===e.substr(0,1)?e.substr(1,e.length):e).split(","),n={};tG(i,n,N);for(let t=0;t<i.length;t++)i[t]=i[t].trim();switch(i.length){case 1:n.id=tM(),n.startTime={type:"prevTaskEnd",id:t},n.endTime={data:i[0]};break;case 2:n.id=tM(),n.startTime={type:"getStartDate",startData:i[0]},n.endTime={data:i[1]};break;case 3:n.id=tM(i[0]),n.startTime={type:"getStartDate",startData:i[1]},n.endTime={data:i[2]}}return n},"parseData"),tE=[],tA={},tL=(0,l.K)(function(t,e){let i={section:W,type:W,processed:!1,manualEndTime:!1,renderEndTime:null,raw:{data:e},task:t,classes:[]},n=tY(s,e);i.raw.startTime=n.startTime,i.raw.endTime=n.endTime,i.id=n.id,i.prevTaskId=s,i.active=n.active,i.done=n.done,i.crit=n.crit,i.milestone=n.milestone,i.vert=n.vert,i.vert?i.order=-1:(i.order=V,V++);let r=tE.push(i);s=i.id,tA[i.id]=r-1},"addTask"),tO=(0,l.K)(function(t){return tE[tA[t]]},"findTaskById"),tI=(0,l.K)(function(t,e){let i={section:W,type:W,description:t,task:t,classes:[]},s=tK(n,e);i.startTime=s.startTime,i.endTime=s.endTime,i.id=s.id,i.active=s.active,i.done=s.done,i.crit=s.crit,i.milestone=s.milestone,i.vert=s.vert,n=i,F.push(i)},"addTaskOrg"),tF=(0,l.K)(function(){let t=(0,l.K)(function(t){let e=tE[t],i="";switch(tE[t].raw.startTime.type){case"prevTaskEnd":{let t=tO(e.prevTaskId);e.startTime=t.endTime;break}case"getStartDate":(i=t_(void 0,M,tE[t].raw.startTime.startData))&&(tE[t].startTime=i)}return tE[t].startTime&&(tE[t].endTime=tS(tE[t].startTime,M,tE[t].raw.endTime.data,B),tE[t].endTime&&(tE[t].processed=!0,tE[t].manualEndTime=u(tE[t].raw.endTime.data,"YYYY-MM-DD",!0).isValid(),tw(tE[t],M,L,A))),tE[t].processed},"compileTask"),e=!0;for(let[i,n]of tE.entries())t(i),e=e&&n.processed;return e},"compileTasks"),tW=(0,l.K)(function(t,e){let i=e;"loose"!==(0,o.D7)().securityLevel&&(i=(0,d.J)(e)),t.split(",").forEach(function(t){void 0!==tO(t)&&(tz(t,()=>{window.open(i,"_self")}),O.set(t,i))}),tP(t,"clickable")},"setLink"),tP=(0,l.K)(function(t,e){t.split(",").forEach(function(t){let i=tO(t);void 0!==i&&i.classes.push(e)})},"setClass"),tN=(0,l.K)(function(t,e,i){if("loose"!==(0,o.D7)().securityLevel||void 0===e)return;let n=[];if("string"==typeof i){n=i.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);for(let t=0;t<n.length;t++){let e=n[t].trim();e.startsWith('"')&&e.endsWith('"')&&(e=e.substr(1,e.length-2)),n[t]=e}}0===n.length&&n.push(t),void 0!==tO(t)&&tz(t,()=>{a._K.runFunc(e,...n)})},"setClickFun"),tz=(0,l.K)(function(t,e){z.push(function(){let i=H?`${H}-${t}`:t,n=document.querySelector(`[id="${i}"]`);null!==n&&n.addEventListener("click",function(){e()})},function(){let i=H?`${H}-${t}`:t,n=document.querySelector(`[id="${i}-text"]`);null!==n&&n.addEventListener("click",function(){e()})})},"pushFun"),tH=(0,l.K)(function(t,e,i){t.split(",").forEach(function(t){tN(t,e,i)}),tP(t,"clickable")},"setClickEvent"),tB=(0,l.K)(function(t){z.forEach(function(e){e(t)})},"bindFunctions"),tR={getConfig:(0,l.K)(()=>(0,o.D7)().gantt,"getConfig"),clear:U,setDateFormat:ti,getDateFormat:tl,enableInclusiveEndDates:tn,endDatesAreInclusive:ts,enableTopAxis:tr,topAxisEnabled:ta,setAxisFormat:X,getAxisFormat:q,setTickInterval:Q,getTickInterval:J,setTodayMarker:tt,getTodayMarker:te,setAccTitle:o.SV,getAccTitle:o.iN,setDiagramTitle:o.ke,getDiagramTitle:o.ab,setDiagramId:Z,setDisplayMode:to,getDisplayMode:tc,setAccDescription:o.EI,getAccDescription:o.m7,addSection:ty,getSections:tg,getTasks:tp,addTask:tL,findTaskById:tO,addTaskOrg:tI,setIncludes:tu,getIncludes:th,setExcludes:tf,getExcludes:tm,setClickEvent:tH,setLink:tW,getLinks:tk,bindFunctions:tB,parseDuration:tD,isInvalidDate:tx,setWeekday:tv,getWeekday:tT,setWeekend:tb};function tG(t,e,i){let n=!0;for(;n;)n=!1,i.forEach(function(i){let s=RegExp("^\\s*"+i+"\\s*$");t[0].match(s)&&(e[i]=!0,t.shift(1),n=!0)})}(0,l.K)(tG,"getTaskTags"),u.extend(k);var tj=(0,l.K)(function(){c.R.debug("Something is calling, setConf, remove the call")},"setConf"),tV={monday:y.AB,tuesday:y.PG,wednesday:y.Gu,thursday:y.Mo,friday:y.TU,saturday:y.rG,sunday:y.YP},tU=(0,l.K)((t,e)=>{let i=[...t].map(()=>-1/0),n=[...t].sort((t,e)=>t.startTime-e.startTime||t.order-e.order),s=0;for(let t of n)for(let n=0;n<i.length;n++)if(t.startTime>=i[n]){i[n]=t.endTime,t.order=n+e,n>s&&(s=n);break}return s},"getMaxIntersections"),tZ={parser:S,db:tR,renderer:{setConf:tj,draw:(0,l.K)(function(t,e,i,n){let s,a=(0,o.D7)().gantt;n.db.setDiagramId(e);let d=(0,o.D7)().securityLevel;"sandbox"===d&&(s=(0,g.Lt)("#i"+e));let h="sandbox"===d?(0,g.Lt)(s.nodes()[0].contentDocument.body):(0,g.Lt)("body"),f="sandbox"===d?s.nodes()[0].contentDocument:document,m=f.getElementById(e);void 0===(r=m.parentElement.offsetWidth)&&(r=1200),void 0!==a.useWidth&&(r=a.useWidth);let k=n.db.getTasks(),T=k.filter(t=>!t.vert),b=[];for(let t of T)b.push(t.type);b=F(b);let w={},$=2*a.topPadding;if("compact"===n.db.getDisplayMode()||"compact"===a.displayMode){let t={};for(let e of T)void 0===t[e.section]?t[e.section]=[e]:t[e.section].push(e);let e=0;for(let i of Object.keys(t)){let n=tU(t[i],e)+1;e+=n,$+=n*(a.barHeight+a.barGap),w[i]=n}}else for(let t of($+=T.length*(a.barHeight+a.barGap),b))w[t]=T.filter(e=>e.type===t).length;m.setAttribute("viewBox","0 0 "+r+" "+$);let S=h.select(`[id="${e}"]`),C=(0,p.w7)().domain([(0,x.jk)(k,function(t){return t.startTime}),(0,x.T9)(k,function(t){return t.endTime})]).rangeRound([0,r-a.leftPadding-a.rightPadding]);function M(t,e){let i=t.startTime,n=e.startTime,s=0;return i>n?s=1:i<n&&(s=-1),s}function K(t,e,i){let s=a.barHeight,r=s+a.barGap,o=a.topPadding,c=a.leftPadding,l=(0,p.m4)().domain([0,b.length]).range(["#00B9FA","#F95002"]).interpolate(v.bE);E(r,o,c,e,i,t,n.db.getExcludes(),n.db.getIncludes()),L(c,o,e,i),Y(t,r,o,c,s,l,e,i),O(r,o,c,s,l),I(c,o,e,i)}function Y(t,i,s,r,c,l,d){t.sort((t,e)=>t.vert===e.vert?0:t.vert?1:-1);let u=t.filter(t=>!t.vert),h=[...new Set(u.map(t=>t.order))].map(t=>u.find(e=>e.order===t));S.append("g").selectAll("rect").data(h).enter().append("rect").attr("x",0).attr("y",function(t,e){return t.order*i+s-2}).attr("width",function(){return d-a.rightPadding/2}).attr("height",i).attr("class",function(t){for(let[e,i]of b.entries())if(t.type===i)return"section section"+e%a.numberSectionStyles;return"section section0"}).enter();let f=S.append("g").selectAll("rect").data(t).enter(),m=n.db.getLinks();if(f.append("rect").attr("id",function(t){return e+"-"+t.id}).attr("rx",3).attr("ry",3).attr("x",function(t){return t.milestone?C(t.startTime)+r+.5*(C(t.endTime)-C(t.startTime))-.5*c:C(t.startTime)+r}).attr("y",function(t,e){return(e=t.order,t.vert)?a.gridLineStartPadding:e*i+s}).attr("width",function(t){return t.milestone?c:t.vert?.08*c:C(t.renderEndTime||t.endTime)-C(t.startTime)}).attr("height",function(t){return t.vert?u.length*(a.barHeight+a.barGap)+2*a.barHeight:c}).attr("transform-origin",function(t,e){return e=t.order,(C(t.startTime)+r+.5*(C(t.endTime)-C(t.startTime))).toString()+"px "+(e*i+s+.5*c).toString()+"px"}).attr("class",function(t){let e="";t.classes.length>0&&(e=t.classes.join(" "));let i=0;for(let[e,n]of b.entries())t.type===n&&(i=e%a.numberSectionStyles);let n="";return t.active?t.crit?n+=" activeCrit":n=" active":t.done?n=t.crit?" doneCrit":" done":t.crit&&(n+=" crit"),0===n.length&&(n=" task"),t.milestone&&(n=" milestone "+n),t.vert&&(n=" vert "+n),n+=i,"task"+(n+=" "+e)}),f.append("text").attr("id",function(t){return e+"-"+t.id+"-text"}).text(function(t){return t.task}).attr("font-size",a.fontSize).attr("x",function(t){let e=C(t.startTime),i=C(t.renderEndTime||t.endTime);if(t.milestone&&(e+=.5*(C(t.endTime)-C(t.startTime))-.5*c,i=e+c),t.vert)return C(t.startTime)+r;let n=this.getBBox().width;return n>i-e?i+n+1.5*a.leftPadding>d?e+r-5:i+r+5:(i-e)/2+e+r}).attr("y",function(t,e){return t.vert?a.gridLineStartPadding+u.length*(a.barHeight+a.barGap)+60:t.order*i+a.barHeight/2+(a.fontSize/2-2)+s}).attr("text-height",c).attr("class",function(t){let e=C(t.startTime),i=C(t.endTime);t.milestone&&(i=e+c);let n=this.getBBox().width,s="";t.classes.length>0&&(s=t.classes.join(" "));let r=0;for(let[e,i]of b.entries())t.type===i&&(r=e%a.numberSectionStyles);let o="";return(t.active&&(o=t.crit?"activeCritText"+r:"activeText"+r),t.done?o=t.crit?o+" doneCritText"+r:o+" doneText"+r:t.crit&&(o=o+" critText"+r),t.milestone&&(o+=" milestoneText"),t.vert&&(o+=" vertText"),n>i-e)?i+n+1.5*a.leftPadding>d?s+" taskTextOutsideLeft taskTextOutside"+r+" "+o:s+" taskTextOutsideRight taskTextOutside"+r+" "+o+" width-"+n:s+" taskText taskText"+r+" "+o+" width-"+n}),"sandbox"===(0,o.D7)().securityLevel){let t=(0,g.Lt)("#i"+e).nodes()[0].contentDocument;f.filter(function(t){return m.has(t.id)}).each(function(i){var n=t.querySelector("#"+CSS.escape(e+"-"+i.id)),s=t.querySelector("#"+CSS.escape(e+"-"+i.id+"-text"));let r=n.parentNode;var a=t.createElement("a");a.setAttribute("xlink:href",m.get(i.id)),a.setAttribute("target","_top"),r.appendChild(a),a.appendChild(n),a.appendChild(s)})}}function E(t,i,s,r,o,l,d,h){let f,m;if(0===d.length&&0===h.length)return;for(let{startTime:t,endTime:e}of l)(void 0===f||t<f)&&(f=t),(void 0===m||e>m)&&(m=e);if(!f||!m)return;if(u(m).diff(u(f),"year")>5)return void c.R.warn("The difference between the min and max time is more than 5 years. This will cause performance issues. Skipping drawing exclude days.");let k=n.db.getDateFormat(),y=[],g=null,p=u(f);for(;p.valueOf()<=m;)n.db.isInvalidDate(p,k,d,h)?g?g.end=p:g={start:p,end:p}:g&&(y.push(g),g=null),p=p.add(1,"d");S.append("g").selectAll("rect").data(y).enter().append("rect").attr("id",t=>e+"-exclude-"+t.start.format("YYYY-MM-DD")).attr("x",t=>C(t.start.startOf("day"))+s).attr("y",a.gridLineStartPadding).attr("width",t=>C(t.end.endOf("day"))-C(t.start.startOf("day"))).attr("height",o-i-a.gridLineStartPadding).attr("transform-origin",function(e,i){return(C(e.start)+s+.5*(C(e.end)-C(e.start))).toString()+"px "+(i*t+.5*o).toString()+"px"}).attr("class","exclude-range")}function A(t,e,i,n){if(i<=0||t>e)return 1/0;let s=u.duration({[n??"day"]:i}).asMilliseconds();return s<=0?1/0:Math.ceil((e-t)/s)}function L(t,e,i,s){let r,o=n.db.getDateFormat(),l=n.db.getAxisFormat();r=l||("D"===o?"%d":a.axisFormat??"%Y-%m-%d");let d=_(3,C).tickSize(-s+e+a.gridLineStartPadding).tickFormat((0,D.DC)(r)),u=/^([1-9]\d*)(millisecond|second|minute|hour|day|week|month)$/.exec(n.db.getTickInterval()||a.tickInterval);if(null!==u){let t=parseInt(u[1],10);if(isNaN(t)||t<=0)c.R.warn(`Invalid tick interval value: "${u[1]}". Skipping custom tick interval.`);else{let e=u[2],i=n.db.getWeekday()||a.weekday,s=C.domain(),r=A(s[0],s[1],t,e);if(r>1e4)c.R.warn(`The tick interval "${t}${e}" would generate ${r} ticks, which exceeds the maximum allowed (10000). This may indicate an invalid date or time range. Skipping custom tick interval.`);else switch(e){case"millisecond":d.ticks(y.t6.every(t));break;case"second":d.ticks(y.uc.every(t));break;case"minute":d.ticks(y.wX.every(t));break;case"hour":d.ticks(y.Ag.every(t));break;case"day":d.ticks(y.UA.every(t));break;case"week":d.ticks(tV[i].every(t));break;case"month":d.ticks(y.Ui.every(t))}}}if(S.append("g").attr("class","grid").attr("transform","translate("+t+", "+(s-50)+")").call(d).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10).attr("dy","1em"),n.db.topAxisEnabled()||a.topAxis){let i=_(1,C).tickSize(-s+e+a.gridLineStartPadding).tickFormat((0,D.DC)(r));if(null!==u){let t=parseInt(u[1],10);if(isNaN(t)||t<=0)c.R.warn(`Invalid tick interval value: "${u[1]}". Skipping custom tick interval.`);else{let e=u[2],s=n.db.getWeekday()||a.weekday,r=C.domain();if(1e4>=A(r[0],r[1],t,e))switch(e){case"millisecond":i.ticks(y.t6.every(t));break;case"second":i.ticks(y.uc.every(t));break;case"minute":i.ticks(y.wX.every(t));break;case"hour":i.ticks(y.Ag.every(t));break;case"day":i.ticks(y.UA.every(t));break;case"week":i.ticks(tV[s].every(t));break;case"month":i.ticks(y.Ui.every(t))}}}S.append("g").attr("class","grid").attr("transform","translate("+t+", "+e+")").call(i).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10)}}function O(t,e){let i=0,n=Object.keys(w).map(t=>[t,w[t]]);S.append("g").selectAll("text").data(n).enter().append(function(t){let e=t[0].split(o.Y2.lineBreakRegex),i=-(e.length-1)/2,n=f.createElementNS("http://www.w3.org/2000/svg","text");for(let[t,s]of(n.setAttribute("dy",i+"em"),e.entries())){let e=f.createElementNS("http://www.w3.org/2000/svg","tspan");e.setAttribute("alignment-baseline","central"),e.setAttribute("x","10"),t>0&&e.setAttribute("dy","1em"),e.textContent=s,n.appendChild(e)}return n}).attr("x",10).attr("y",function(s,r){if(!(r>0))return s[1]*t/2+e;for(let a=0;a<r;a++)return i+=n[r-1][1],s[1]*t/2+i*t+e}).attr("font-size",a.sectionFontSize).attr("class",function(t){for(let[e,i]of b.entries())if(t[0]===i)return"sectionTitle sectionTitle"+e%a.numberSectionStyles;return"sectionTitle"})}function I(t,e,i,s){let r=n.db.getTodayMarker();if("off"===r)return;let o=S.append("g").attr("class","today"),c=new Date,l=o.append("line");l.attr("x1",C(c)+t).attr("x2",C(c)+t).attr("y1",a.titleTopMargin).attr("y2",s-a.titleTopMargin).attr("class","today"),""!==r&&l.attr("style",r.replace(/,/g,";"))}function F(t){let e={},i=[];for(let n=0,s=t.length;n<s;++n)Object.prototype.hasOwnProperty.call(e,t[n])||(e[t[n]]=!0,i.push(t[n]));return i}(0,l.K)(M,"taskCompare"),k.sort(M),K(k,r,$),(0,o.a$)(S,$,r,a.useMaxWidth),S.append("text").text(n.db.getDiagramTitle()).attr("x",r/2).attr("y",a.titleTopMargin).attr("class","titleText"),(0,l.K)(K,"makeGantt"),(0,l.K)(Y,"drawRects"),(0,l.K)(E,"drawExcludeDays"),(0,l.K)(A,"getEstimatedTickCount"),(0,l.K)(L,"makeGrid"),(0,l.K)(O,"vertLabels"),(0,l.K)(I,"drawToday"),(0,l.K)(F,"checkUnique")},"draw")},styles:(0,l.K)(t=>`
  .mermaid-main-font {
        font-family: ${t.fontFamily};
  }

  .exclude-range {
    fill: ${t.excludeBkgColor};
  }

  .section {
    stroke: none;
    opacity: 0.2;
  }

  .section0 {
    fill: ${t.sectionBkgColor};
  }

  .section2 {
    fill: ${t.sectionBkgColor2};
  }

  .section1,
  .section3 {
    fill: ${t.altSectionBkgColor};
    opacity: 0.2;
  }

  .sectionTitle0 {
    fill: ${t.titleColor};
  }

  .sectionTitle1 {
    fill: ${t.titleColor};
  }

  .sectionTitle2 {
    fill: ${t.titleColor};
  }

  .sectionTitle3 {
    fill: ${t.titleColor};
  }

  .sectionTitle {
    text-anchor: start;
    font-family: ${t.fontFamily};
  }


  /* Grid and axis */

  .grid .tick {
    stroke: ${t.gridColor};
    opacity: 0.8;
    shape-rendering: crispEdges;
  }

  .grid .tick text {
    font-family: ${t.fontFamily};
    fill: ${t.textColor};
  }

  .grid path {
    stroke-width: 0;
  }


  /* Today line */

  .today {
    fill: none;
    stroke: ${t.todayLineColor};
    stroke-width: 2px;
  }


  /* Task styling */

  /* Default task */

  .task {
    stroke-width: 2;
  }

  .taskText {
    text-anchor: middle;
    font-family: ${t.fontFamily};
  }

  .taskTextOutsideRight {
    fill: ${t.taskTextDarkColor};
    text-anchor: start;
    font-family: ${t.fontFamily};
  }

  .taskTextOutsideLeft {
    fill: ${t.taskTextDarkColor};
    text-anchor: end;
  }


  /* Special case clickable */

  .task.clickable {
    cursor: pointer;
  }

  .taskText.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideLeft.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideRight.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }


  /* Specific task settings for the sections*/

  .taskText0,
  .taskText1,
  .taskText2,
  .taskText3 {
    fill: ${t.taskTextColor};
  }

  .task0,
  .task1,
  .task2,
  .task3 {
    fill: ${t.taskBkgColor};
    stroke: ${t.taskBorderColor};
  }

  .taskTextOutside0,
  .taskTextOutside2
  {
    fill: ${t.taskTextOutsideColor};
  }

  .taskTextOutside1,
  .taskTextOutside3 {
    fill: ${t.taskTextOutsideColor};
  }


  /* Active task */

  .active0,
  .active1,
  .active2,
  .active3 {
    fill: ${t.activeTaskBkgColor};
    stroke: ${t.activeTaskBorderColor};
  }

  .activeText0,
  .activeText1,
  .activeText2,
  .activeText3 {
    fill: ${t.taskTextDarkColor} !important;
  }


  /* Completed task */

  .done0,
  .done1,
  .done2,
  .done3 {
    stroke: ${t.doneTaskBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
  }

  .doneText0,
  .doneText1,
  .doneText2,
  .doneText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  /* Done task text displayed outside the bar sits against the diagram background,
     not against the done-task bar, so it must use the outside/contrast color. */
  .doneText0.taskTextOutsideLeft,
  .doneText0.taskTextOutsideRight,
  .doneText1.taskTextOutsideLeft,
  .doneText1.taskTextOutsideRight,
  .doneText2.taskTextOutsideLeft,
  .doneText2.taskTextOutsideRight,
  .doneText3.taskTextOutsideLeft,
  .doneText3.taskTextOutsideRight {
    fill: ${t.taskTextOutsideColor} !important;
  }


  /* Tasks on the critical line */

  .crit0,
  .crit1,
  .crit2,
  .crit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.critBkgColor};
    stroke-width: 2;
  }

  .activeCrit0,
  .activeCrit1,
  .activeCrit2,
  .activeCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.activeTaskBkgColor};
    stroke-width: 2;
  }

  .doneCrit0,
  .doneCrit1,
  .doneCrit2,
  .doneCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
    cursor: pointer;
    shape-rendering: crispEdges;
  }

  .milestone {
    transform: rotate(45deg) scale(0.8,0.8);
  }

  .milestoneText {
    font-style: italic;
  }
  .doneCritText0,
  .doneCritText1,
  .doneCritText2,
  .doneCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  /* Done-crit task text outside the bar \u2014 same reasoning as doneText above. */
  .doneCritText0.taskTextOutsideLeft,
  .doneCritText0.taskTextOutsideRight,
  .doneCritText1.taskTextOutsideLeft,
  .doneCritText1.taskTextOutsideRight,
  .doneCritText2.taskTextOutsideLeft,
  .doneCritText2.taskTextOutsideRight,
  .doneCritText3.taskTextOutsideLeft,
  .doneCritText3.taskTextOutsideRight {
    fill: ${t.taskTextOutsideColor} !important;
  }

  .vert {
    stroke: ${t.vertLineColor};
  }

  .vertText {
    font-size: 15px;
    text-anchor: middle;
    fill: ${t.vertLineColor} !important;
  }

  .activeCritText0,
  .activeCritText1,
  .activeCritText2,
  .activeCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  .titleText {
    text-anchor: middle;
    font-size: 18px;
    fill: ${t.titleColor||t.textColor};
    font-family: ${t.fontFamily};
  }
`,"getStyles")}},94215:function(t){t.exports=function(){"use strict";var t,e,i=/^(-|\+)?P(?:([-+]?[0-9,.]*)Y)?(?:([-+]?[0-9,.]*)M)?(?:([-+]?[0-9,.]*)W)?(?:([-+]?[0-9,.]*)D)?(?:T(?:([-+]?[0-9,.]*)H)?(?:([-+]?[0-9,.]*)M)?(?:([-+]?[0-9,.]*)S)?)?$/,n=/\[([^\]]+)]|YYYY|YY|Y|M{1,2}|D{1,2}|H{1,2}|m{1,2}|s{1,2}|SSS/g,s={years:31536e6,months:2628e6,days:864e5,hours:36e5,minutes:6e4,seconds:1e3,milliseconds:1,weeks:6048e5},r=function(t){return t instanceof u},a=function(t,e,i){return new u(t,i,e.$l)},o=function(t){return e.p(t)+"s"},c=function(t){return t<0},l=function(t){return c(t)?Math.ceil(t):Math.floor(t)},d=function(t,e){return t?c(t)?{negative:!0,format:""+Math.abs(t)+e}:{negative:!1,format:""+t+e}:{negative:!1,format:""}},u=function(){function c(t,e,n){var r=this;if(this.$d={},this.$l=n,void 0===t&&(this.$ms=0,this.parseFromMilliseconds()),e)return a(t*s[o(e)],this);if("number"==typeof t)return this.$ms=t,this.parseFromMilliseconds(),this;if("object"==typeof t)return Object.keys(t).forEach(function(e){r.$d[o(e)]=t[e]}),this.calMilliseconds(),this;if("string"==typeof t){var c=t.match(i);if(c){var l=c.slice(2).map(function(t){return null!=t?Number(t):0});return this.$d.years=l[0],this.$d.months=l[1],this.$d.weeks=l[2],this.$d.days=l[3],this.$d.hours=l[4],this.$d.minutes=l[5],this.$d.seconds=l[6],this.calMilliseconds(),this}}return this}var u=c.prototype;return u.calMilliseconds=function(){var t=this;this.$ms=Object.keys(this.$d).reduce(function(e,i){return e+(t.$d[i]||0)*s[i]},0)},u.parseFromMilliseconds=function(){var t=this.$ms;this.$d.years=l(t/31536e6),t%=31536e6,this.$d.months=l(t/2628e6),t%=2628e6,this.$d.days=l(t/864e5),t%=864e5,this.$d.hours=l(t/36e5),t%=36e5,this.$d.minutes=l(t/6e4),t%=6e4,this.$d.seconds=l(t/1e3),t%=1e3,this.$d.milliseconds=t},u.toISOString=function(){var t=d(this.$d.years,"Y"),e=d(this.$d.months,"M"),i=+this.$d.days||0;this.$d.weeks&&(i+=7*this.$d.weeks);var n=d(i,"D"),s=d(this.$d.hours,"H"),r=d(this.$d.minutes,"M"),a=this.$d.seconds||0;this.$d.milliseconds&&(a+=this.$d.milliseconds/1e3,a=Math.round(1e3*a)/1e3);var o=d(a,"S"),c=t.negative||e.negative||n.negative||s.negative||r.negative||o.negative,l=s.format||r.format||o.format?"T":"",u=(c?"-":"")+"P"+t.format+e.format+n.format+l+s.format+r.format+o.format;return"P"===u||"-P"===u?"P0D":u},u.toJSON=function(){return this.toISOString()},u.format=function(t){var i={Y:this.$d.years,YY:e.s(this.$d.years,2,"0"),YYYY:e.s(this.$d.years,4,"0"),M:this.$d.months,MM:e.s(this.$d.months,2,"0"),D:this.$d.days,DD:e.s(this.$d.days,2,"0"),H:this.$d.hours,HH:e.s(this.$d.hours,2,"0"),m:this.$d.minutes,mm:e.s(this.$d.minutes,2,"0"),s:this.$d.seconds,ss:e.s(this.$d.seconds,2,"0"),SSS:e.s(this.$d.milliseconds,3,"0")};return(t||"YYYY-MM-DDTHH:mm:ss").replace(n,function(t,e){return e||String(i[t])})},u.as=function(t){return this.$ms/s[o(t)]},u.get=function(t){var e=this.$ms,i=o(t);return"milliseconds"===i?e%=1e3:e="weeks"===i?l(e/s[i]):this.$d[i],e||0},u.add=function(t,e,i){var n;return n=e?t*s[o(e)]:r(t)?t.$ms:a(t,this).$ms,a(this.$ms+n*(i?-1:1),this)},u.subtract=function(t,e){return this.add(t,e,!0)},u.locale=function(t){var e=this.clone();return e.$l=t,e},u.clone=function(){return a(this.$ms,this)},u.humanize=function(e){return t().add(this.$ms,"ms").locale(this.$l).fromNow(!e)},u.valueOf=function(){return this.asMilliseconds()},u.milliseconds=function(){return this.get("milliseconds")},u.asMilliseconds=function(){return this.as("milliseconds")},u.seconds=function(){return this.get("seconds")},u.asSeconds=function(){return this.as("seconds")},u.minutes=function(){return this.get("minutes")},u.asMinutes=function(){return this.as("minutes")},u.hours=function(){return this.get("hours")},u.asHours=function(){return this.as("hours")},u.days=function(){return this.get("days")},u.asDays=function(){return this.as("days")},u.weeks=function(){return this.get("weeks")},u.asWeeks=function(){return this.as("weeks")},u.months=function(){return this.get("months")},u.asMonths=function(){return this.as("months")},u.years=function(){return this.get("years")},u.asYears=function(){return this.as("years")},c}(),h=function(t,e,i){return t.add(e.years()*i,"y").add(e.months()*i,"M").add(e.days()*i,"d").add(e.hours()*i,"h").add(e.minutes()*i,"m").add(e.seconds()*i,"s").add(e.milliseconds()*i,"ms")};return function(i,n,s){t=s,e=s().$utils(),s.duration=function(t,e){return a(t,{$l:s.locale()},e)},s.isDuration=r;var o=n.prototype.add,c=n.prototype.subtract;n.prototype.add=function(t,e){return r(t)?h(this,t,1):o.bind(this)(t,e)},n.prototype.subtract=function(t,e){return r(t)?h(this,t,-1):c.bind(this)(t,e)}}}()}}]);