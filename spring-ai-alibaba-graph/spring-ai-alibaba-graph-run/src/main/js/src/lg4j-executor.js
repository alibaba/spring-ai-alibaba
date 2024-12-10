import TWStyles from './twlit.js';

import { html, css, LitElement, CSSResult } from 'lit';

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

/**
 * Asynchronously waits for a specified number of milliseconds.
 * 
 * @param {number} ms - The number of milliseconds to wait.
 * @returns {Promise<void>} A promise that resolves after the specified delay.
 */
const delay = async (ms) => (new Promise(resolve => setTimeout(resolve, ms)));

/**
 * Asynchronously fetches data from a given fetch call and yields the data in chunks.
 * @async
 * @generator
 * @param {Response} response
 * @yields {Promise<string>} The decoded text chunk from the response stream.
 */
async function* streamingResponse(response) {
  // Attach Reader
  const reader = response.body?.getReader();

  while (true && reader) {
    // wait for next encoded chunk
    const { done, value } = await reader.read();
    // check if stream is done
    if (done) break;
    // Decodes data chunk and yields it
    yield (new TextDecoder().decode(value));
  }
}

/**
 * LG4JInputElement is a custom web component that extends LitElement.
 * It provides a styled input container with a placeholder.
 * 
 * @class
 * @extends {LitElement}
 */
export class LG4JExecutorElement extends LitElement {

  /**
   * Styles applied to the component.
   * 
   * @static
   * @type {Array<CSSResult>}
   */
  static styles = [TWStyles, css`
    .container {
      display: flex;
      flex-direction: column;
      row-gap: 10px;
    }

    .commands {
      display: flex;
      flex-direction: row;
      column-gap: 10px;
    }

    .item1 {
      flex-grow: 2;
    }
    .item2 {
      flex-grow: 2;
    }
  `];


  /**
   * Properties of the component.
   * 
   * @static
   * @type { import('lit').PropertyDeclarations }
   */
  static properties = {
    url: { type: String, reflect: true  },
    test: { type: Boolean, reflect: true },
    _executing: { state: true }
    
  }

  /**
   * @type {string | null }
   */
  url = null

  /**
   * current selected thread
   * 
   * @type {string|undefined} - thread id
   */
  #selectedThread

  /**
   * current state for update 
   * 
   * @type {UpdatedState|null}
   */
  #updatedState = null


  /**
   * Creates an instance of LG4JInputElement.
   * 
   * @constructor
   */
  constructor() {
    super();
    this.test = false
    this.formMetaData = {}
    this._executing = false
    
  }

  /**
   * Event handler for the 'update slected thread' event.
   * 
   * @param {CustomEvent<string>} e - The event object containing the updated data.
   */
  #onThreadUpdated( e ) {
    console.debug( 'thread-updated', e.detail )
    this.#selectedThread = e.detail
    this.#updatedState = null
    this.requestUpdate()
  }

  /**
   * 
   * @param {CustomEvent<UpdatedState>} e - The event object containing the result data.
   */
  #onNodeUpdated( e ) {
    console.debug( 'onNodeUpdated', e )
    this.#updatedState = e.detail
    this.requestUpdate()
  }

  /**
   * Lifecycle method called when the element is added to the document's DOM.
   */
  connectedCallback() {
    super.connectedCallback();

    // @ts-ignore
    this.addEventListener( "thread-updated", this.#onThreadUpdated );
    // @ts-ignore
    this.addEventListener( 'node-updated', this.#onNodeUpdated )

    if(this.test ) {
      this.#_test_callInit();
      return
    }

    this.#callInit()

  }

  disconnectedCallback() {
    super.disconnectedCallback();

    // @ts-ignore
    this.removeEventListener( "thread-updated", this.#onThreadUpdated )
    // @ts-ignore
    this.removeEventListener( 'node-updated', this.#onNodeUpdated )

  }

  /**
   * Renders the HTML template for the component.
   * 
   * @returns The rendered HTML template.
   */
  render() {

    // console.debug( 'render', this.formMetaData )
    return html`
        <div class="container">
          ${ Object.entries(this.formMetaData).map( ([key, _]) => 
             html`<textarea id="${key}" class="textarea textarea-primary" placeholder="${key}"></textarea>`
          )}
          <div class="commands">
            <button id="submit" ?disabled=${this._executing} @click="${this.#callSubmit}" class="btn btn-primary item1">Submit</button>
            <button id="resume" ?disabled=${!this.#updatedState || this._executing} @click="${this.#callResume}" class="btn btn-secondary item2">
            Resume ${ this.#updatedState ? '(from ' + this.#updatedState?.node + ')' : '' }
            </button>
          </div>
        </div>
        `;
  }

  async #callInit() {

    const initResponse = await fetch( `${this.url}/init` )

    const initData = await initResponse.json()
    
    console.debug( 'initData', initData );

    this.dispatchEvent( new CustomEvent( 'init', { 
      detail: initData,
      bubbles: true,
      composed: true,
      cancelable: true
    }));

    this.formMetaData = initData.args
    // this.#nodes = initData.nodes
    this.requestUpdate()
  }


async #callResume() {
  this._executing = true
  try {

    if(this.test ) {
      await this.#_test_callSubmitAction();
      return
    }

    await this.#callResumeAction()
    
    
  }
  finally {
    this._executing = false
  }

}  

async #callResumeAction() {

  const execResponse = await fetch(`${this.url}/stream?thread=${this.#selectedThread}&resume=true&node=${this.#updatedState?.node}&checkpoint=${this.#updatedState?.checkpoint}`, {
      method: 'POST', // *GET, POST, PUT, DELETE, etc.
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(this.#updatedState?.data)
  });

  this.#updatedState = null

  for await (let chunk of streamingResponse( execResponse )  ) {
    console.debug( chunk )

    this.dispatchEvent( new CustomEvent( 'result', { 
      detail: JSON.parse(chunk),
      bubbles: true,
      composed: true,
      cancelable: true
    } ) );

  }


}

async #callSubmit() {

  this._executing = true
  try {

    if(this.test ) {
      await this.#_test_callSubmitAction();
      return
    }

    await this.#callSubmitAction()
    
  }
  finally {
    this._executing = false
  }
}

async #callSubmitAction() {
  
    // Get input as object
    /**
     * @type { Record<string,any> } data
     */
    const data = Object.keys(this.formMetaData).reduce( (acc, key) => {
      // @ts-ignore
      acc[key] = this.shadowRoot?.getElementById(key)?.value
      return acc
    }, {});

    const execResponse = await fetch(`${this.url}/stream?thread=${this.#selectedThread}`, {
        method: 'POST', // *GET, POST, PUT, DELETE, etc.
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });

    for await (let chunk of streamingResponse( execResponse )  ) {
      console.debug( chunk )

      this.dispatchEvent( new CustomEvent( 'result', { 
        detail: JSON.parse(chunk),
        bubbles: true,
        composed: true,
        cancelable: true
      } ) );

    }
  
}
  
////////////////////////////////////////////////////////
// TEST
///////////////////////////////////////////////////////

  async #_test_callInit() {
      
    await delay( 1000 );
    this.dispatchEvent( new CustomEvent( 'init', { 
      detail: { 
        threads: [ ['default', [] ] ],
        title: 'SpringAiGraph : TEST',
        graph:`
---
title: TEST
---        
flowchart TD
  start((start))
  stop((stop))
  web_search("web_search")
  retrieve("retrieve")
  grade_documents("grade_documents")
  generate("generate")
  transform_query("transform_query")
  start:::start -->|web_search| web_search:::web_search
  start:::start -->|vectorstore| retrieve:::retrieve
  web_search:::web_search --> generate:::generate
  retrieve:::retrieve --> grade_documents:::grade_documents
  grade_documents:::grade_documents -->|transform_query| transform_query:::transform_query
  grade_documents:::grade_documents -->|generate| generate:::generate
  transform_query:::transform_query --> retrieve:::retrieve
  generate:::generate -->|not supported| generate:::generate
  generate:::generate -->|not useful| transform_query:::transform_query
  generate:::generate -->|useful| stop:::stop
      `
      },
      bubbles: true,
      composed: true,
      cancelable: true
    }));

    this.formMetaData = { 
      input: { type: 'string', required: true }
    }
    
    this.requestUpdate()

  }


  async #_test_callSubmitAction( ) {

    const thread = this.#selectedThread
    
    const send = async ( /** @type {string} */ nodeId ) => {
      await delay( 1000 );
      this.dispatchEvent( new CustomEvent( 'result', { 
        detail: [ thread, { 
          checkpoint: ( nodeId==='start' || nodeId==='stop') ? undefined : `checkpoint-${nodeId}`,
          node: nodeId, 
          state: { 
            input: "this is input",
            property1: { value: "value1", valid: true } , 
            property2: { value: "value2", children: { elements: [1,2,3]} } }}
          ],
        bubbles: true,
        composed: true,
        cancelable: true
      }));
    }

    await send( 'start' );
    await send( 'retrieve' );
    await send( 'grade_documents');
    await send( 'transform_query');
    await send( 'retrieve' );
    await send( 'grade_documents');
    await send( 'generate');
    await send( 'stop' );

  }

}


window.customElements.define('lg4j-executor', LG4JExecutorElement);
