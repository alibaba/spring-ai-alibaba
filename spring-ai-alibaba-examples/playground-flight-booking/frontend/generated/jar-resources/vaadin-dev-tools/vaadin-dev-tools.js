import{LitElement as O,css as L,html as m,nothing as $}from"lit";import{property as S,query as D,state as M,customElement as V}from"lit/decorators.js";function v(s,e,o,t){var n=arguments.length,i=n<3?e:t===null?t=Object.getOwnPropertyDescriptor(e,o):t,d;if(typeof Reflect=="object"&&typeof Reflect.decorate=="function")i=Reflect.decorate(s,e,o,t);else for(var h=s.length-1;h>=0;h--)(d=s[h])&&(i=(n<3?d(i):n>3?d(e,o,i):d(e,o))||i);return n>3&&i&&Object.defineProperty(e,o,i),i}const E=1e3,I=(s,e)=>{const o=Array.from(s.querySelectorAll(e.join(", "))),t=Array.from(s.querySelectorAll("*")).filter(n=>n.shadowRoot).flatMap(n=>I(n.shadowRoot,e));return[...o,...t]};let A=!1;const u=(s,e)=>{A||(window.addEventListener("message",n=>{n.data==="validate-license"&&window.location.reload()},!1),A=!0);const o=s._overlayElement;if(o){if(o.shadowRoot){const n=o.shadowRoot.querySelector("slot:not([name])");if(n&&n.assignedElements().length>0){u(n.assignedElements()[0],e);return}}u(o,e);return}const t=e.messageHtml?e.messageHtml:`${e.message} <p>Component: ${e.product.name} ${e.product.version}</p>`.replace(/https:([^ ]*)/g,"<a href='https:$1'>https:$1</a>");s.isConnected&&(s.outerHTML=`<no-license style="display:flex;align-items:center;text-align:center;justify-content:center;"><div>${t}</div></no-license>`)},g={},T={},f={},N={},c=s=>`${s.name}_${s.version}`,x=s=>{const{cvdlName:e,version:o}=s.constructor,t={name:e,version:o},n=s.tagName.toLowerCase();g[e]=g[e]??[],g[e].push(n);const i=f[c(t)];i&&setTimeout(()=>u(s,i),E),f[c(t)]||N[c(t)]||T[c(t)]||(T[c(t)]=!0,window.Vaadin.devTools.checkLicense(t))},G=s=>{N[c(s)]=!0,console.debug("License check ok for",s)},R=s=>{const e=s.product.name;f[c(s.product)]=s,console.error("License check failed for",e);const o=g[e];(o==null?void 0:o.length)>0&&I(document,o).forEach(t=>{setTimeout(()=>u(t,f[c(s.product)]),E)})},U=s=>{const e=s.message,o=s.product.name;s.messageHtml=`No license found. <a target=_blank onclick="javascript:window.open(this.href);return false;" href="${e}">Go here to start a trial or retrieve your license.</a>`,f[c(s.product)]=s,console.error("No license found when checking",o);const t=g[o];(t==null?void 0:t.length)>0&&I(document,t).forEach(n=>{setTimeout(()=>u(n,f[c(s.product)]),E)})},P=s=>s.command==="license-check-ok"?(G(s.data),!0):s.command==="license-check-failed"?(R(s.data),!0):s.command==="license-check-nokey"?(U(s.data),!0):!1,B=()=>{window.Vaadin.devTools.createdCvdlElements.forEach(s=>{x(s)}),window.Vaadin.devTools.createdCvdlElements={push:s=>{x(s)}}};var a;(function(s){s.ACTIVE="active",s.INACTIVE="inactive",s.UNAVAILABLE="unavailable",s.ERROR="error"})(a||(a={}));class p{constructor(){this.status=a.UNAVAILABLE}onHandshake(){}onConnectionError(e){}onStatusChange(e){}setActive(e){!e&&this.status===a.ACTIVE?this.setStatus(a.INACTIVE):e&&this.status===a.INACTIVE&&this.setStatus(a.ACTIVE)}setStatus(e){this.status!==e&&(this.status=e,this.onStatusChange(e))}}p.HEARTBEAT_INTERVAL=18e4;class F extends p{constructor(e){super(),this.webSocket=new WebSocket(e),this.webSocket.onmessage=o=>this.handleMessage(o),this.webSocket.onerror=o=>this.handleError(o),this.webSocket.onclose=o=>{this.status!==a.ERROR&&this.setStatus(a.UNAVAILABLE),this.webSocket=void 0},setInterval(()=>{this.webSocket&&self.status!==a.ERROR&&this.status!==a.UNAVAILABLE&&this.webSocket.send("")},p.HEARTBEAT_INTERVAL)}onReload(){}handleMessage(e){let o;try{o=JSON.parse(e.data)}catch(t){this.handleError(`[${t.name}: ${t.message}`);return}o.command==="hello"?(this.setStatus(a.ACTIVE),this.onHandshake()):o.command==="reload"?this.status===a.ACTIVE&&this.onReload():this.handleError(`Unknown message from the livereload server: ${e}`)}handleError(e){console.error(e),this.setStatus(a.ERROR),e instanceof Event&&this.webSocket?this.onConnectionError(`Error in WebSocket connection to ${this.webSocket.url}`):this.onConnectionError(e)}}const _=16384;class C extends p{constructor(e){if(super(),this.canSend=!1,!e)return;const o={transport:"websocket",fallbackTransport:"websocket",url:e,contentType:"application/json; charset=UTF-8",reconnectInterval:5e3,timeout:-1,maxReconnectOnClose:1e7,trackMessageLength:!0,enableProtocol:!0,handleOnlineOffline:!1,executeCallbackBeforeReconnect:!0,messageDelimiter:"|",onMessage:t=>{const n={data:t.responseBody};this.handleMessage(n)},onError:t=>{this.canSend=!1,this.handleError(t)},onOpen:()=>{this.canSend=!0},onClose:()=>{this.canSend=!1},onClientTimeout:()=>{this.canSend=!1},onReconnect:()=>{this.canSend=!1},onReopen:()=>{this.canSend=!0}};H().then(t=>{this.socket=t.subscribe(o)})}onReload(){}onUpdate(e,o){}onMessage(e){}handleMessage(e){let o;try{o=JSON.parse(e.data)}catch(t){this.handleError(`[${t.name}: ${t.message}`);return}o.command==="hello"?(this.setStatus(a.ACTIVE),this.onHandshake()):o.command==="reload"?this.status===a.ACTIVE&&this.onReload():o.command==="update"?this.status===a.ACTIVE&&this.onUpdate(o.path,o.content):this.onMessage(o)}handleError(e){console.error(e),this.setStatus(a.ERROR),this.onConnectionError(e)}send(e,o){if(!this.socket||!this.canSend){y(()=>this.socket&&this.canSend,d=>this.send(e,o));return}const t=JSON.stringify({command:e,data:o});let i=t.length+"|"+t;for(;i.length;)this.socket.push(i.substring(0,_)),i=i.substring(_)}}C.HEARTBEAT_INTERVAL=18e4;function y(s,e){const o=s();o?e(o):setTimeout(()=>y(s,e),50)}function H(){return new Promise((s,e)=>{y(()=>{var o;return(o=window==null?void 0:window.vaadinPush)==null?void 0:o.atmosphere},s)})}var r,w;(function(s){s.LOG="log",s.INFORMATION="information",s.WARNING="warning",s.ERROR="error"})(w||(w={}));let l=r=class extends O{constructor(){super(...arguments),this.unhandledMessages=[],this.conf={enable:!1,url:"",liveReloadPort:-1},this.notifications=[],this.frontendStatus=a.UNAVAILABLE,this.javaStatus=a.UNAVAILABLE,this.componentPickActive=!1,this.nextMessageId=1,this.transitionDuration=0}static get styles(){return[L`
        :host {
          --dev-tools-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen-Sans, Ubuntu, Cantarell,
            'Helvetica Neue', sans-serif;
          --dev-tools-font-family-monospace: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
            monospace;

          --dev-tools-font-size: 0.8125rem;
          --dev-tools-font-size-small: 0.75rem;

          --dev-tools-text-color: rgba(255, 255, 255, 0.8);
          --dev-tools-text-color-secondary: rgba(255, 255, 255, 0.65);
          --dev-tools-text-color-emphasis: rgba(255, 255, 255, 0.95);
          --dev-tools-text-color-active: rgba(255, 255, 255, 1);

          --dev-tools-background-color-inactive: rgba(45, 45, 45, 0.25);
          --dev-tools-background-color-active: rgba(45, 45, 45, 0.98);
          --dev-tools-background-color-active-blurred: rgba(45, 45, 45, 0.85);

          --dev-tools-border-radius: 0.5rem;
          --dev-tools-box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.4);

          --dev-tools-blue-hsl: 206, 100%, 70%;
          --dev-tools-blue-color: hsl(var(--dev-tools-blue-hsl));
          --dev-tools-green-hsl: 145, 80%, 42%;
          --dev-tools-green-color: hsl(var(--dev-tools-green-hsl));
          --dev-tools-grey-hsl: 0, 0%, 50%;
          --dev-tools-grey-color: hsl(var(--dev-tools-grey-hsl));
          --dev-tools-yellow-hsl: 38, 98%, 64%;
          --dev-tools-yellow-color: hsl(var(--dev-tools-yellow-hsl));
          --dev-tools-red-hsl: 355, 100%, 68%;
          --dev-tools-red-color: hsl(var(--dev-tools-red-hsl));

          /* Needs to be in ms, used in JavaScript as well */
          --dev-tools-transition-duration: 180ms;

          all: initial;

          direction: ltr;
          cursor: default;
          font: normal 400 var(--dev-tools-font-size) / 1.125rem var(--dev-tools-font-family);
          color: var(--dev-tools-text-color);
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
          color-scheme: dark;

          position: fixed;
          z-index: 20000;
          pointer-events: none;
          bottom: 0;
          right: 0;
          width: 100%;
          height: 100%;
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
        }

        .dev-tools {
          pointer-events: auto;
          display: flex;
          align-items: center;
          position: fixed;
          z-index: inherit;
          right: 0.5rem;
          bottom: 0.5rem;
          min-width: 1.75rem;
          height: 1.75rem;
          max-width: 1.75rem;
          border-radius: 0.5rem;
          padding: 0.375rem;
          box-sizing: border-box;
          background-color: var(--dev-tools-background-color-inactive);
          box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          white-space: nowrap;
          line-height: 1rem;
        }

        .dev-tools:hover,
        .dev-tools.active {
          background-color: var(--dev-tools-background-color-active);
          box-shadow: var(--dev-tools-box-shadow);
        }

        .dev-tools.active {
          max-width: calc(100% - 1rem);
        }

        .dev-tools .status-description {
          overflow: hidden;
          text-overflow: ellipsis;
          padding: 0 0.25rem;
        }

        .dev-tools.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.15);
          animation: bounce 0.5s;
          animation-iteration-count: 2;
        }

        .window.hidden {
          opacity: 0;
          transform: scale(0);
          position: absolute;
        }

        .window.visible {
          transform: none;
          opacity: 1;
          pointer-events: auto;
        }

        .window.visible ~ .dev-tools {
          opacity: 0;
          pointer-events: none;
        }

        .window.visible ~ .dev-tools .dev-tools-icon,
        .window.visible ~ .dev-tools .status-blip {
          transition: none;
          opacity: 0;
        }

        .window {
          border-radius: var(--dev-tools-border-radius);
          overflow: auto;
          margin: 0.5rem;
          min-width: 30rem;
          max-width: calc(100% - 1rem);
          max-height: calc(100vh - 1rem);
          flex-shrink: 1;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          display: flex;
          flex-direction: column;
          box-shadow: var(--dev-tools-box-shadow);
          outline: none;
        }

        .window-toolbar {
          display: flex;
          flex: none;
          align-items: center;
          padding: 0.375rem;
          white-space: nowrap;
          order: 1;
          background-color: rgba(0, 0, 0, 0.2);
          gap: 0.5rem;
        }

        .ahreflike {
          font-weight: 500;
          color: var(--dev-tools-text-color-secondary);
          text-decoration: underline;
          cursor: pointer;
        }

        .ahreflike:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .button {
          all: initial;
          font-family: inherit;
          font-size: var(--dev-tools-font-size-small);
          line-height: 1;
          white-space: nowrap;
          background-color: rgba(0, 0, 0, 0.2);
          color: inherit;
          font-weight: 600;
          padding: 0.25rem 0.375rem;
          border-radius: 0.25rem;
        }

        .button:focus,
        .button:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .message.information {
          --dev-tools-notification-color: var(--dev-tools-blue-color);
        }

        .message.warning {
          --dev-tools-notification-color: var(--dev-tools-yellow-color);
        }

        .message.error {
          --dev-tools-notification-color: var(--dev-tools-red-color);
        }

        .message {
          display: flex;
          padding: 0.1875rem 0.75rem 0.1875rem 2rem;
          background-clip: padding-box;
        }

        .message.log {
          padding-left: 0.75rem;
        }

        .message-content {
          margin-right: 0.5rem;
          -webkit-user-select: text;
          -moz-user-select: text;
          user-select: text;
        }

        .message-heading {
          position: relative;
          display: flex;
          align-items: center;
          margin: 0.125rem 0;
        }

        .message.log {
          color: var(--dev-tools-text-color-secondary);
        }

        .message:not(.log) .message-heading {
          font-weight: 500;
        }

        .message.has-details .message-heading {
          color: var(--dev-tools-text-color-emphasis);
          font-weight: 600;
        }

        .message-heading::before {
          position: absolute;
          margin-left: -1.5rem;
          display: inline-block;
          text-align: center;
          font-size: 0.875em;
          font-weight: 600;
          line-height: calc(1.25em - 2px);
          width: 14px;
          height: 14px;
          box-sizing: border-box;
          border: 1px solid transparent;
          border-radius: 50%;
        }

        .message.information .message-heading::before {
          content: 'i';
          border-color: currentColor;
          color: var(--dev-tools-notification-color);
        }

        .message.warning .message-heading::before,
        .message.error .message-heading::before {
          content: '!';
          color: var(--dev-tools-background-color-active);
          background-color: var(--dev-tools-notification-color);
        }

        .features-tray {
          padding: 0.75rem;
          flex: auto;
          overflow: auto;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          user-select: text;
        }

        .features-tray p {
          margin-top: 0;
          color: var(--dev-tools-text-color-secondary);
        }

        .features-tray .feature {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding-bottom: 0.5em;
        }

        .message .message-details {
          font-weight: 400;
          color: var(--dev-tools-text-color-secondary);
          margin: 0.25rem 0;
        }

        .message .message-details[hidden] {
          display: none;
        }

        .message .message-details p {
          display: inline;
          margin: 0;
          margin-right: 0.375em;
          word-break: break-word;
        }

        .message .persist {
          color: var(--dev-tools-text-color-secondary);
          white-space: nowrap;
          margin: 0.375rem 0;
          display: flex;
          align-items: center;
          position: relative;
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
        }

        .message .persist::before {
          content: '';
          width: 1em;
          height: 1em;
          border-radius: 0.2em;
          margin-right: 0.375em;
          background-color: rgba(255, 255, 255, 0.3);
        }

        .message .persist:hover::before {
          background-color: rgba(255, 255, 255, 0.4);
        }

        .message .persist.on::before {
          background-color: rgba(255, 255, 255, 0.9);
        }

        .message .persist.on::after {
          content: '';
          order: -1;
          position: absolute;
          width: 0.75em;
          height: 0.25em;
          border: 2px solid var(--dev-tools-background-color-active);
          border-width: 0 0 2px 2px;
          transform: translate(0.05em, -0.05em) rotate(-45deg) scale(0.8, 0.9);
        }

        .message .dismiss-message {
          font-weight: 600;
          align-self: stretch;
          display: flex;
          align-items: center;
          padding: 0 0.25rem;
          margin-left: 0.5rem;
          color: var(--dev-tools-text-color-secondary);
        }

        .message .dismiss-message:hover {
          color: var(--dev-tools-text-color);
        }

        .notification-tray {
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
          margin: 0.5rem;
          flex: none;
        }

        .window.hidden + .notification-tray {
          margin-bottom: 3rem;
        }

        .notification-tray .message {
          pointer-events: auto;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          max-width: 30rem;
          box-sizing: border-box;
          border-radius: var(--dev-tools-border-radius);
          margin-top: 0.5rem;
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          animation: slideIn var(--dev-tools-transition-duration);
          box-shadow: var(--dev-tools-box-shadow);
          padding-top: 0.25rem;
          padding-bottom: 0.25rem;
        }

        .notification-tray .message.animate-out {
          animation: slideOut forwards var(--dev-tools-transition-duration);
        }

        .notification-tray .message .message-details {
          max-height: 10em;
          overflow: hidden;
        }

        .message-tray {
          flex: auto;
          overflow: auto;
          max-height: 20rem;
          user-select: text;
        }

        .message-tray .message {
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          padding-left: 2.25rem;
        }

        .message-tray .message.warning {
          background-color: hsla(var(--dev-tools-yellow-hsl), 0.09);
        }

        .message-tray .message.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.09);
        }

        .message-tray .message.error .message-heading {
          color: hsl(var(--dev-tools-red-hsl));
        }

        .message-tray .message.warning .message-heading {
          color: hsl(var(--dev-tools-yellow-hsl));
        }

        .message-tray .message + .message {
          border-top: 1px solid rgba(255, 255, 255, 0.07);
        }

        .message-tray .dismiss-message,
        .message-tray .persist {
          display: none;
        }

        @keyframes slideIn {
          from {
            transform: translateX(100%);
            opacity: 0;
          }
          to {
            transform: translateX(0%);
            opacity: 1;
          }
        }

        @keyframes slideOut {
          from {
            transform: translateX(0%);
            opacity: 1;
          }
          to {
            transform: translateX(100%);
            opacity: 0;
          }
        }

        @keyframes fade-in {
          0% {
            opacity: 0;
          }
        }

        @keyframes bounce {
          0% {
            transform: scale(0.8);
          }
          50% {
            transform: scale(1.5);
            background-color: hsla(var(--dev-tools-red-hsl), 1);
          }
          100% {
            transform: scale(1);
          }
        }

        @supports (backdrop-filter: blur(1px)) {
          .dev-tools,
          .window,
          .notification-tray .message {
            backdrop-filter: blur(8px);
          }
          .dev-tools:hover,
          .dev-tools.active,
          .window,
          .notification-tray .message {
            background-color: var(--dev-tools-background-color-active-blurred);
          }
        }
      `]}static get isActive(){const e=window.sessionStorage.getItem(r.ACTIVE_KEY_IN_SESSION_STORAGE);return e===null||e!=="false"}static notificationDismissed(e){const o=window.localStorage.getItem(r.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);return o!==null&&o.includes(e)}elementTelemetry(){let e={};try{const o=localStorage.getItem("vaadin.statistics.basket");if(!o)return;e=JSON.parse(o)}catch{return}this.frontendConnection&&this.frontendConnection.send("reportTelemetry",{browserData:e})}openWebSocketConnection(){if(this.frontendStatus=a.UNAVAILABLE,this.javaStatus=a.UNAVAILABLE,!this.conf.token){console.error("Dev tools functionality denied for this host."),this.log(w.LOG,"See Vaadin documentation on how to configure devmode.hostsAllowed property.",void 0,"https://vaadin.com/docs/latest/configuration/properties#properties",void 0);return}const e=i=>console.error(i),o=()=>{this.showSplashMessage("Reloading…");const i=window.sessionStorage.getItem(r.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE),d=i?parseInt(i,10)+1:1;window.sessionStorage.setItem(r.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE,d.toString()),window.sessionStorage.setItem(r.TRIGGERED_KEY_IN_SESSION_STORAGE,"true"),window.location.reload()},t=(i,d)=>{let h=document.head.querySelector(`style[data-file-path='${i}']`);h?(h.textContent=d,document.dispatchEvent(new CustomEvent("vaadin-theme-updated"))):o()},n=new C(this.getDedicatedWebSocketUrl());n.onHandshake=()=>{r.isActive||n.setActive(!1),this.elementTelemetry()},n.onConnectionError=e,n.onReload=o,n.onUpdate=t,n.onStatusChange=i=>{this.frontendStatus=i},n.onMessage=i=>this.handleFrontendMessage(i),this.frontendConnection=n,this.conf.backend===r.SPRING_BOOT_DEVTOOLS&&(this.javaConnection=new F(this.getSpringBootWebSocketUrl(window.location)),this.javaConnection.onHandshake=()=>{r.isActive||this.javaConnection.setActive(!1)},this.javaConnection.onReload=o,this.javaConnection.onConnectionError=e,this.javaConnection.onStatusChange=i=>{this.javaStatus=i}),this.conf.backend||this.showNotification(w.WARNING,"Java live reload unavailable","Live reload for Java changes is currently not set up. Find out how to make use of this functionality to boost your workflow.","https://vaadin.com/docs/latest/flow/configuration/live-reload","liveReloadUnavailable")}tabHandleMessage(e,o){const t=e;return t.handleMessage&&t.handleMessage.call(e,o)}handleFrontendMessage(e){e.command==="featureFlags"||P(e)||this.unhandledMessages.push(e)}getDedicatedWebSocketUrl(){function e(t){const n=document.createElement("div");return n.innerHTML=`<a href="${t}"/>`,n.firstChild.href}if(this.conf.url===void 0)return;const o=e(this.conf.url);if(!o.startsWith("http://")&&!o.startsWith("https://")){console.error("The protocol of the url should be http or https for live reload to work.");return}return`${o}?v-r=push&debug_window&token=${this.conf.token}`}getSpringBootWebSocketUrl(e){const{hostname:o}=e,t=e.protocol==="https:"?"wss":"ws";if(o.endsWith("gitpod.io")){const n=o.replace(/.*?-/,"");return`${t}://${this.conf.liveReloadPort}-${n}`}else return`${t}://${o}:${this.conf.liveReloadPort}`}connectedCallback(){if(super.connectedCallback(),this.conf=window.Vaadin.devToolsConf||this.conf,this.disableEventListener=n=>this.demoteSplashMessage(),document.body.addEventListener("focus",this.disableEventListener),document.body.addEventListener("click",this.disableEventListener),window.sessionStorage.getItem(r.TRIGGERED_KEY_IN_SESSION_STORAGE)){const n=new Date,i=`${`0${n.getHours()}`.slice(-2)}:${`0${n.getMinutes()}`.slice(-2)}:${`0${n.getSeconds()}`.slice(-2)}`;this.showSplashMessage(`Page reloaded at ${i}`),window.sessionStorage.removeItem(r.TRIGGERED_KEY_IN_SESSION_STORAGE)}this.transitionDuration=parseInt(window.getComputedStyle(this).getPropertyValue("--dev-tools-transition-duration"),10);const o=window;o.Vaadin=o.Vaadin||{},o.Vaadin.devTools=Object.assign(this,o.Vaadin.devTools);const t=window.Vaadin;t.devToolsPlugins&&(Array.from(t.devToolsPlugins).forEach(n=>this.initPlugin(n)),t.devToolsPlugins={push:n=>this.initPlugin(n)}),this.openWebSocketConnection(),B()}async initPlugin(e){const o=this;e.init({send:function(t,n){o.frontendConnection.send(t,n)}})}format(e){return e.toString()}disconnectedCallback(){this.disableEventListener&&(document.body.removeEventListener("focus",this.disableEventListener),document.body.removeEventListener("click",this.disableEventListener)),super.disconnectedCallback()}showSplashMessage(e){this.splashMessage=e,this.splashMessage&&setTimeout(()=>{this.demoteSplashMessage()},r.AUTO_DEMOTE_NOTIFICATION_DELAY)}demoteSplashMessage(){this.showSplashMessage(void 0)}checkLicense(e){this.frontendConnection?this.frontendConnection.send("checkLicense",e):R({message:"Internal error: no connection",product:e})}showNotification(e,o,t,n,i,d){if(i===void 0||!r.notificationDismissed(i)){if(this.notifications.filter(b=>b.persistentId===i).filter(b=>!b.deleted).length>0)return;const k=this.nextMessageId;this.nextMessageId+=1,this.notifications.push({id:k,type:e,message:o,details:t,link:n,persistentId:i,dontShowAgain:!1,dontShowAgainMessage:d,deleted:!1}),n===void 0&&setTimeout(()=>{this.dismissNotification(k)},r.AUTO_DEMOTE_NOTIFICATION_DELAY),this.requestUpdate()}}dismissNotification(e){const o=this.findNotificationIndex(e);if(o!==-1&&!this.notifications[o].deleted){const t=this.notifications[o];if(t.dontShowAgain&&t.persistentId&&!r.notificationDismissed(t.persistentId)){let n=window.localStorage.getItem(r.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);n=n===null?t.persistentId:`${n},${t.persistentId}`,window.localStorage.setItem(r.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE,n)}t.deleted=!0,setTimeout(()=>{const n=this.findNotificationIndex(e);n!==-1&&(this.notifications.splice(n,1),this.requestUpdate())},this.transitionDuration)}}findNotificationIndex(e){let o=-1;return this.notifications.some((t,n)=>t.id===e?(o=n,!0):!1),o}toggleDontShowAgain(e){const o=this.findNotificationIndex(e);if(o!==-1&&!this.notifications[o].deleted){const t=this.notifications[o];t.dontShowAgain=!t.dontShowAgain,this.requestUpdate()}}setActive(e){var o,t;(o=this.frontendConnection)==null||o.setActive(e),(t=this.javaConnection)==null||t.setActive(e),window.sessionStorage.setItem(r.ACTIVE_KEY_IN_SESSION_STORAGE,e?"true":"false")}renderMessage(e){return m`
      <div
        class="message ${e.type} ${e.deleted?"animate-out":""} ${e.details||e.link?"has-details":""}"
      >
        <div class="message-content">
          <div class="message-heading">${e.message}</div>
          <div class="message-details" ?hidden="${!e.details&&!e.link}">
            ${e.details?m`<p>${e.details}</p>`:""}
            ${e.link?m`<a class="ahreflike" href="${e.link}" target="_blank">Learn more</a>`:""}
          </div>
          ${e.persistentId?m`<div
                class="persist ${e.dontShowAgain?"on":"off"}"
                @click=${()=>this.toggleDontShowAgain(e.id)}
              >
                ${e.dontShowAgainMessage||"Don’t show again"}
              </div>`:""}
        </div>
        <div class="dismiss-message" @click=${()=>this.dismissNotification(e.id)}>Dismiss</div>
      </div>
    `}render(){return m` 
      <div class="notification-tray">${this.notifications.map(e=>this.renderMessage(e))}</div>
      <div
        style="display: none"
        class="dev-tools ${this.splashMessage?"active":""}"
      >
        ${this.splashMessage?m`<span class="status-description">${this.splashMessage}</span></div>`:$}
      </div>`}setJavaLiveReloadActive(e){var o;this.javaConnection?this.javaConnection.setActive(e):(o=this.frontendConnection)==null||o.setActive(e)}};l.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE="vaadin.live-reload.dismissedNotifications";l.ACTIVE_KEY_IN_SESSION_STORAGE="vaadin.live-reload.active";l.TRIGGERED_KEY_IN_SESSION_STORAGE="vaadin.live-reload.triggered";l.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE="vaadin.live-reload.triggeredCount";l.AUTO_DEMOTE_NOTIFICATION_DELAY=5e3;l.HOTSWAP_AGENT="HOTSWAP_AGENT";l.JREBEL="JREBEL";l.SPRING_BOOT_DEVTOOLS="SPRING_BOOT_DEVTOOLS";l.BACKEND_DISPLAY_NAME={HOTSWAP_AGENT:"HotswapAgent",JREBEL:"JRebel",SPRING_BOOT_DEVTOOLS:"Spring Boot Devtools"};v([S({type:String,attribute:!1})],l.prototype,"splashMessage",void 0);v([S({type:Array,attribute:!1})],l.prototype,"notifications",void 0);v([S({type:String,attribute:!1})],l.prototype,"frontendStatus",void 0);v([S({type:String,attribute:!1})],l.prototype,"javaStatus",void 0);v([D(".window")],l.prototype,"root",void 0);v([M()],l.prototype,"componentPickActive",void 0);l=r=v([V("vaadin-dev-tools")],l);
