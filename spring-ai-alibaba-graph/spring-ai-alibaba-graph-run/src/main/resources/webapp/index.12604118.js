var e=globalThis,t={},s={},a=e.parcelRequire0031;null==a&&((a=function(e){if(e in t)return t[e].exports;if(e in s){var a=s[e];delete s[e];var i={id:e,exports:{}};return t[e]=i,a.call(i.exports,i,i.exports),i.exports}var l=Error("Cannot find module '"+e+"'");throw l.code="MODULE_NOT_FOUND",l}).register=function(e,t){s[e]=t},e.parcelRequire0031=a),a.register;var i=a("hNeh9"),l=a("800sp");class d extends l.LitElement{static styles=[i.default,(0,l.css)`
  json-viewer {
    --font-size: .8rem;
  }`];static properties={};threadMap=new Map;#e;get selectedTab(){return this.#e}set selectedTab(e){this.#e=e,this.dispatchEvent(new CustomEvent("thread-updated",{detail:e,bubbles:!0,composed:!0,cancelable:!0}))}constructor(){super()}connectedCallback(){super.connectedCallback(),this.addEventListener("result",this.#t),this.addEventListener("init-threads",this.#s),this.addEventListener("node-updated",this.#a)}disconnectedCallback(){super.disconnectedCallback(),this.removeEventListener("result",this.#t),this.removeEventListener("init-threads",this.#s),this.removeEventListener("node-updated",this.#a)}#s=e=>{let{detail:t=[]}=e;console.debug("threads",t),this.threadMap=new Map(t),t&&t.length>0&&(this.selectedTab=t[0][0],this.requestUpdate())};#t=e=>{let[t,s]=e.detail;if(console.debug("onResult",t,s),!this.threadMap.has(t))throw Error("result doesn't contain a valid thread!");console.debug("onResult",t);let a=this.threadMap.get(t),i=a.push(s);this.threadMap.set(t,a),this.dispatchEvent(new CustomEvent("graph-active",{detail:s.node,bubbles:!0,composed:!0,cancelable:!0})),this.requestUpdate(),this.updateComplete.then(()=>{let e=`#json${i-1}`,t=this.shadowRoot.querySelectorAll(e);for(let s of(console.debug(e,t),t))s.expandAll()})};#i(e){console.debug(e.target.id),this.selectedTab=e.target.id,this.requestUpdate()}#l(e){console.debug("NEW TAB",e);let t=`Thread-${this.threadMap.size+1}`;this.threadMap.set(t,[]),this.selectedTab=t,this.requestUpdate()}#a(e){console.debug("onNodeUpdated",e)}#d(e,t){return(0,l.html)`
    <div class="collapse collapse-arrow bg-base-200">
      <input type="radio" name="item-1" checked="checked" />
      <div class="collapse-title text-ml font-bold">${e.node}</div>
      <div class="collapse-content">
        <lg4j-node-output>${JSON.stringify(e).trim()}</log4j-node-output>  
      </div>
    </div>
    `}#r(){let e=[...this.threadMap.keys()];return(0,l.html)`
    ${e.map(e=>(0,l.html)`<a id="${e}" @click="${this.#i}" role="tab" class="tab ${this.selectedTab===e?"tab-active":""}" >${e}</a>`)}
    `}render(){return(0,l.html)`
      
      <div class="h-full">
        <div role="tablist" class="tabs tabs-bordered">
            ${this.#r()}
            <a role="tab" class="tab" @click="${this.#l}">
              <svg  xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20">
                <circle cx="10" cy="10" r="9" fill="none" stroke="white" stroke-width="1.5"/>
                <line x1="5" y1="10" x2="15" y2="10" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
                <line x1="10" y1="5" x2="10" y2="15" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </a>
          </div>
            <div class="max-h-[95%] overflow-x-auto bg-slate-500">
              <table class="table table-pin-rows">
                <tbody>
                    ${this.threadMap.get(this.selectedTab)?.map((e,t)=>l.html`<tr><td>${this.#d(e,t)}</td></tr>`)}
                </tbody>
              </table>
            </div>
        </div> 
       
    `}#o(e,t){return(0,l.html)`
      <div class="collapse collapse-arrow bg-base-200">
        <input type="radio" name="item-1" checked="checked" />
        <div class="collapse-title text-ml font-bold">${e.node}</div>
        <div class="collapse-content">
        ${Object.entries(e.state).map(([e,s])=>(0,l.html)`
            <div>
                <h4 class="italic">${e}</h4>
                <p class="my-3">
                  <json-viewer id="json${t}">
                    ${JSON.stringify(s)}
                  </json-viewer>
                </p>
              </div>
          `)}
        </div>
      </div>
      `}#n(e,t){return(0,l.html)`
    <div class="card bg-neutral text-neutral-content">
    <div class="card-body">
      <h2 class="card-title">${e.node}</h2>
      <div class="collapse collapse-arrow bg-base-200">
        <input type="radio" name="item-1" checked="checked" />
        <div class="collapse-content">
        ${Object.entries(e.state).map(([e,s])=>(0,l.html)`
          <div>
              <h4 class="italic">${e}</h4>
              <p class="my-3">
                <json-viewer id="json${t}">
                ${JSON.stringify(s)}
                </json-viewer>
              </p>
            </div>
        `)}
        </div>
        </div>
    </div>
  </div>   `}}window.customElements.define("lg4j-result",d);
//# sourceMappingURL=index.12604118.js.map
