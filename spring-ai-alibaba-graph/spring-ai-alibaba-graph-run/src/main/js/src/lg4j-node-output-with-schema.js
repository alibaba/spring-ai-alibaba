// @ts-nocheck
import { JSONEditor } from '@json-editor/json-editor'

export class LG4JNodeOutputWithSchema extends HTMLElement {
    
    static get observedAttributes() {
        return ['value'];
    }

    #editor 

    constructor() {
        super()

        const shadowRoot = this.attachShadow({ mode: "open" });
        
        const style = document.createElement("style");
        style.textContent = `
        <style>

            .je-indented-panel {
                margin-top: 0 !important;
                margin-bottom: 0 !important;
            }

            .je-child-editor-holder {
                margin-top: 0 !important;
                margin-bottom: 0 !important;
            }

            .je-object__controls {
                display: none;
            }

            .je-header.je-object__title {
                display: none;
            }

            select.je-switcher {
                pointer-events: none;
                cursor: not-allowed;
                visibility: hidden;
            }

            
        </style>
        `
        
        shadowRoot.appendChild(style);


    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (name === 'value') {
          if (newValue !== null) {
            console.debug( "attributeChangedCallback.value", newValue )
            this.#editor?.setValue( JSON.parse(newValue) );

          }
        }
    }

    connectedCallback() {

        const value = this.textContent
        console.debug( "value", value )
        this.#initialize( JSON.parse(value) )
        
    }
    
    disconnectedCallback() {

        this.#editor.destroy();
        
        this.#editor = null
    }

    #initialize( value ) {
        const schema = {
            "type": "object",
            
            "additionalProperties": {
                "options": {
                  "collapsed": true,
                },
                "type": [
                  "array",
                ]
              },
  
            "additionalProperties": {
              "options": {
              },
              "type": [
                "string", "number", "boolean", "object", "array",
              ]
            }
            
        };

        const container = document.createElement('div')

        this.shadowRoot.appendChild( container );

        this.#editor = new JSONEditor(container, {
            schema: schema,
            no_additional_properties: false,
            required_by_default: true,
            disable_edit_json: true,
            disable_properties: true,
            disable_array_delete_all_rows: true,
            disable_array_add: true,
            array_controls_top: true,
            disable_array_delete: true,
            disable_array_reorder: true,

            startval: value
          });

    }
}


window.customElements.define('lg4j-node-output', LG4JNodeOutputWithSchema);