import React from 'react'
import ReactDOM from 'react-dom/client'; 
import ReactJson from '@microlink/react-json-view'


/**
 * @file
 * @typedef {import('./types.js').ResultData} ResultData * 
 */

/**
 * @file
 * @typedef {import('./types.js').EditEvent} EditEvent
 */

/**
 * @file
 * @typedef {import('./types.js').UpdatedState} UpdatedState
 */

export class LG4JNodeOutput extends HTMLElement {
    
  static get observedAttributes() {
      return ['value'];
  }

  constructor() {
      super()

      const shadowRoot = this.attachShadow({ mode: "open" });
      
      const style = document.createElement("style");
      style.textContent = `
      <style>
      </style>
      `
      
      shadowRoot.appendChild(style);

  }

  /**
   * @param {string} name
   * @param {any} oldValue
   * @param {any} newValue
   */
  attributeChangedCallback(name, oldValue, newValue) {
      if (name === 'value') {
        if (newValue !== null) {
          console.debug( "attributeChangedCallback.value", newValue )
        }
      }
  }

  connectedCallback() {

      const value = this.textContent ?? '{}'
      
      console.debug( "value", value )

      this.root = this.#createRoot( JSON.parse(value) )
      
  }

  disconnectedCallback() {

    this.root?.unmount()

  }

  get isCollapsed() {
    return this.getAttribute('collapsed') === 'true'
  }




  /**
   * 
   * @param {EditEvent} e
   * @param {ResultData} result
   */
  #onEdit( e, result ) {

    if( result.checkpoint ) {

      /**
       * @type {UpdatedState}
       */
      const detail = {
        node: result.node,
        checkpoint: result.checkpoint,
        data: e.updated_src
      }

      this.dispatchEvent( new CustomEvent( 'node-updated', { 
        detail,
        bubbles: true,
        composed: true,
        cancelable: true
      }));
      
      return true;
    }

    return false;
  }



  /**
   * 
   * @param {ResultData} value 
   * @returns 
   */
  #createRoot( value ) {

    const mountPoint = document.createElement('span');
    this.shadowRoot?.appendChild(mountPoint);

    const root = ReactDOM.createRoot(mountPoint);

    // @ts-ignore
    const component = React.createElement( ReactJson, { 
      src: value.state,
      enableClipboard: false,
      displayDataTypes: false,
      name: false,
      collapsed: this.isCollapsed,
      theme: 'monokai',
      onEdit: (/** @type {any} */ e) => this.#onEdit(e, value ),
      validationMessage: 'Read only'

    } )
    
    root.render( component )

    return root
  }
}


window.customElements.define('lg4j-node-output', LG4JNodeOutput);