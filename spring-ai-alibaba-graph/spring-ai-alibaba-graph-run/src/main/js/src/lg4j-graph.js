import mermaid from 'mermaid';
//const mermaidAPI = mermaid.mermaidAPI;
import * as d3 from 'd3'

/**
 * WcMermaid
 * @class
 */
export class LG4jMermaid extends HTMLElement {

  constructor() {
    super();

    
    mermaid.initialize({
      logLevel: 'none',
      startOnLoad: false,
      theme: 'dark',
      flowchart: {
        useMaxWidth: false
      }
    });

    this._content = null
    this._activeClass = null
    // @ts-ignore
    this._lastTransform = null


    const shadowRoot = this.attachShadow({ mode: "open" });

    const style = document.createElement("style");
    style.textContent = `
    // :host {
    //   display: block;
    //   width: 100%;
    //   height: 100%;
    // }
    .h-full {
      height: 100%;
    }
    .w-full {
      width: 100%;
    }
    // .flex {
    //   display: flex;
    // }
    .items-center {
      align-items: center;
    }
    .justify-center {
      justify-content: center;
    }
    .bg-neutral {
      --tw-bg-opacity: 1;
      background-color: var(--fallback-n,oklch(var(--n)/var(--tw-bg-opacity)));
  }
    `
    shadowRoot.appendChild(style);

    const container = document.createElement('div')
    container.classList.add("h-full");
    container.classList.add("w-full");
    container.classList.add("flex");
    container.classList.add("items-center");
    container.classList.add("justify-center");
    container.classList.add("bg-neutral");
    container.classList.add("mermaid");

    // const pre = document.createElement('pre')
    // pre.classList.add("mermaid");
    // container.appendChild( pre );

    shadowRoot.appendChild( container );

    this.#renderDiagram()

  }


  
  /**
   * @returns {ChildNode[]}
   */
  get #textNodes() {
    return Array.from(this.childNodes).filter(
      node => node.nodeType === this.TEXT_NODE
    );
  }

  /**
   * @returns {string}
   */
  get #textContent() {
    
    if( this._content ) {

      if( this._activeClass ) {
        return `
        ${this._content}
        classDef ${this._activeClass} fill:#f96
        `
      }
      
      return this._content
    }

    return "flowchart TD" //this.#textNodes.map(node => node.textContent?.trim()).join('');
  }


  async #renderDiagram( ) {
    const svgContainer = this.shadowRoot?.querySelector('.mermaid')

    if( !svgContainer ) {
      console.error( 'svgcontainer not found!')
      return
    } 

    // console.debug( svgContainer );
    return mermaid.render( 'graph', this.#textContent )
        .then( res => { 
          console.debug( "RENDER COMPLETE", svgContainer );
          // svgContainer.innerHTML = res.svg
          const { width, height } = svgContainer.getBoundingClientRect();
          // console.debug( res.svg )
          console.debug( 'width:', width, 'height:', height);
          const translated = res.svg
            .replace( /height="[\d\.]+"/, `height="${height}"`) 
            .replace( /width="[\d\.]+"/, `width="${width}"`);
          // console.debug( translated );
          svgContainer.innerHTML = translated;
        })
        .then( () => this.#svgPanZoom() )
        .then( () => {
          console.debug( "boundingClientRect", svgContainer.getBoundingClientRect() );
          for( const rc of svgContainer.getClientRects() ) {
            console.debug( rc );
          }
        })
        .catch( e => console.error( "RENDER ERROR", e ) )

  }

  #svgPanZoom() {

    console.debug( '_lastTransform', this._lastTransform )

    // @ts-ignore
    const svgs = d3.select( this.shadowRoot ).select(".mermaid svg");
    // console.debug( 'svgs', svgs )

    const self = this;

    svgs.each( function() {
      // 'this' refers to the current DOM element
      const svg = d3.select(this);
      
      // console.debug( 'svg', svg );
      svg.html("<g>" + svg.html() + "</g>");

      const inner = svg.select("g");
      // console.debug( 'inner', inner )
   
      const zoom = d3.zoom().on("zoom", event => {
          inner.attr("transform", event.transform);
          self._lastTransform = event.transform;
        }); 
      
      // @ts-ignore
      const selection = svg.call(zoom);

      if( self._lastTransform !== null ) {
        inner.attr("transform", self._lastTransform)
        // [D3.js Set initial zoom level](https://stackoverflow.com/a/46437252/521197)
        // @ts-ignore
        selection.call(zoom.transform, self._lastTransform);
      }  

    });

  }

  /**
   * Handles the content event to update the diagram content.
   *
   * @param {CustomEvent} e - The event object containing the new content detail.
   */
  #onContent(e) {
    const { detail: newContent } = e;

    this._content = newContent;
    this.#renderDiagram();
  }

  /**
   * Handles the active class event to update the active class in the diagram.
   *
   * @param {CustomEvent} e - The event object containing the active class detail.
   */
  #onActive(e) {
    const { detail: activeClass } = e;

    this._activeClass = activeClass;
    this.#renderDiagram();
  }

  /**
   * Handles the resize event to re-render the diagram.
   */
  #resizeHandler = () => this.#renderDiagram();

  /**
   * Called when the element is connected to the document's DOM.
   * Sets up event listeners for graph content and active class updates, and window resize.
   */
  connectedCallback() {
    // @ts-ignore
    this.addEventListener('graph', this.#onContent);
    // @ts-ignore
    this.addEventListener('graph-active', this.#onActive);
    window.addEventListener('resize', this.#resizeHandler);
  }

  /**
   * Called when the element is disconnected from the document's DOM.
   * Cleans up event listeners for graph content and active class updates, and window resize.
   */
  disconnectedCallback() {
    // @ts-ignore
    this.removeEventListener('graph', this.#onContent);
    // @ts-ignore
    this.removeEventListener('graph-active', this.#onActive);
    window.removeEventListener('resize', this.#resizeHandler);
  }

  /**
   * Renders the diagram with the current content and runs the mermaid library.
   * This method is deprecated and should not be used in new code.
   *
   * @deprecated
   * @returns {Promise<void>} A promise that resolves when the diagram rendering and mermaid run are complete.
   */
  // @ts-ignore
  async #renderDiagramWithRun() {
    const pres = this.shadowRoot?.querySelectorAll('.mermaid');
    
    // @ts-ignore
    pres[0].textContent = this.#textContent;

    return mermaid.run({
      // @ts-ignore
      nodes: pres,
      suppressErrors: true
    })
    .then(() => console.debug("RUN COMPLETE"))
    .then(() => this.#svgPanZoom())
    .catch(e => console.error("RUN ERROR", e));
  }


}

window.customElements.define('lg4j-graph', LG4jMermaid);