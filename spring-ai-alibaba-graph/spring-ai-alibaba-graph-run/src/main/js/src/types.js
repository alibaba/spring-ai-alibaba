/**
 * @module types
 */


/**
 * Represents an event triggered when an edit occurs.
 *
 * @typedef {Object} UpdatedState
 * @property {string} node - node id
 * @property {string} checkpoint - checkpoint id.
 * @property {Record<string, any>} data - the modified state.
 */

/**
 * @typedef {Object} ResultData
 * @property {string} node - node id 
 * @property {string} [checkpoint] - checkpoint id.
 * @property {Record<string,any>} state - state
 */

/**
 * Represents an event triggered when an edit occurs.
 *
 * @typedef {Object} EditEvent
 * @property {Record<string, any>} existing_src - The original source object before the edit.
 * @property {any} existing_value - The original value before the edit.
 * @property {string} name - The name of the field that was edited.
 * @property {string[]} namespace - The namespace path indicating where the edit occurred.
 * @property {any} new_value - The new value after the edit.
 * @property {Record<string, any>} updated_src - The updated source object after the edit.
 */
